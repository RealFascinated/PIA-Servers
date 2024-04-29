package cc.fascinated.piaservers.pia;

import cc.fascinated.piaservers.Main;
import cc.fascinated.piaservers.common.GitUtils;
import cc.fascinated.piaservers.model.PiaServer;
import cc.fascinated.piaservers.model.PiaServerToken;
import cc.fascinated.piaservers.readme.ReadMeManager;
import com.google.gson.reflect.TypeToken;
import lombok.SneakyThrows;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PiaManager {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final String PIA_OPENVPN_CONFIGS_URL = "https://www.privateinternetaccess.com/openvpn/openvpn.zip";
    private static final long REMOVAL_THRESHOLD = TimeUnit.DAYS.toMicros(14); // 2 weeks
    public static List<PiaServer> SERVERS = new ArrayList<>();

    @SneakyThrows
    public PiaManager() {
        File serversFile = new File("servers.json");
        if (!serversFile.exists()) {
            System.out.println("serversFile.json does not exist, creating...");
            serversFile.createNewFile();
        }
        // Load the serversFile from the file
        SERVERS = Main.GSON.fromJson(Files.readString(serversFile.toPath()), new TypeToken<List<PiaServer>>() {}.getType());
        if (SERVERS == null) {
            SERVERS = new ArrayList<>();
        }

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateServers(serversFile); // Update the servers
                Path readmePath = ReadMeManager.updateReadme(); // Update the README.md

                // Commit the changes to the git repository
                GitUtils.commitFiles("Scheduled update", serversFile.toPath(), readmePath);
            }
        }, 0, TimeUnit.MINUTES.toMillis(5));
    }

    @SneakyThrows
    public static void updateServers(File serversFile) {
        List<PiaServerToken> piaDomain = getPiaDomains();
        System.out.println("Found " + piaDomain.size() + " pia domains");

        List<PiaServer> toRemove = new ArrayList<>();

        System.out.println("Removing old servers...");
        // Get the servers that need to be removed
        for (PiaServer server : SERVERS) {
            if (server.getLastSeen().getTime() < System.currentTimeMillis() - REMOVAL_THRESHOLD) {
                toRemove.add(server);
            }
        }
        SERVERS.removeAll(toRemove); // Remove the servers
        System.out.printf("Removed %s old servers\n", toRemove.size());

        // Add the new servers to the list
        for (PiaServerToken serverToken : piaDomain) {
            InetAddress address = InetAddress.getByName(serverToken.getHostname());

            // Add the server to the list
            SERVERS.add(new PiaServer(address.getHostAddress(), serverToken.getRegion(), new Date()));
        }

        // Save the servers to the file
        Files.writeString(serversFile.toPath(), Main.GSON.toJson(SERVERS));
        System.out.printf("Wrote %s servers to the file\n", SERVERS.size());
    }

    @SneakyThrows
    private static List<PiaServerToken> getPiaDomains() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PIA_OPENVPN_CONFIGS_URL))
                .GET()
                .build();
        // Send the request and get the response
        HttpResponse<Path> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(Files.createTempFile("openvpn", ".zip")));
        if (response.statusCode() != 200) {
            System.out.println("Failed to get the PIA OpenVPN configs, status code: " + response.statusCode());
            System.exit(1);
        }
        Path downloadedFile = response.body();
        File tempDir = Files.createTempDirectory("openvpn").toFile();
        ZipUnArchiver unArchiver = new ZipUnArchiver();

        // Extract the downloaded file
        unArchiver.setSourceFile(downloadedFile.toFile());
        unArchiver.setDestDirectory(tempDir);
        unArchiver.extract();

        // Get the extracted files
        File[] files = tempDir.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("Failed to extract the OpenVPN configs");
            System.exit(1);
        }

        // Set the DNS resolver to Cloudflare
        Lookup.setDefaultResolver(new SimpleResolver("1.1.1.1"));

        // Search for the server domains
        List<PiaServerToken> domains = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory()) {
                continue;
            }
            if (!file.getName().endsWith(".ovpn")) {
                continue;
            }
            // Read the file and get the server domain
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines) {
                if (line.startsWith("remote ")) {
                    String[] parts = line.split(" ");
                    String hostname = parts[1];
                    String region = file.getName().split("\\.")[0];

                    Record[] records = new Lookup(hostname, Type.A).run();
                    if (records == null) {
                        continue;
                    }
                    for (Record record : records) {
                        ARecord aRecord = (ARecord) record;
                        domains.add(new PiaServerToken(aRecord.getAddress().getHostAddress(), region));
                    }
                    break;
                }
            }
        }

        return domains;
    }
}
