package cc.fascinated.piaservers.pia;

import cc.fascinated.piaservers.Main;
import cc.fascinated.piaservers.common.GitUtils;
import cc.fascinated.piaservers.model.PiaServer;
import cc.fascinated.piaservers.readme.ReadMeManager;
import com.google.gson.reflect.TypeToken;
import lombok.SneakyThrows;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.io.File;
import java.io.IOException;
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
    public static Set<PiaServer> SERVERS = new HashSet<>();
    private static Path README_PATH;

    @SneakyThrows
    public PiaManager() {
        File serversFile = new File("servers.json");
        if (!serversFile.exists()) {
            System.out.println("The servers file doesn't exist, creating it...");
            serversFile.createNewFile();
        }
        // Load the serversFile from the file
        SERVERS = Main.GSON.fromJson(Files.readString(serversFile.toPath()), new TypeToken<Set<PiaServer>>() {}.getType());
        if (SERVERS == null) {
            SERVERS = new HashSet<>();
        }
        System.out.printf("Loaded %s servers from the file%n", SERVERS.size());

        // Set the DNS resolver to Cloudflare
        Lookup.setDefaultResolver(new SimpleResolver("1.1.1.1"));

        GitUtils.cloneRepo(); // Clone the repository

        // Update the servers every 5 minutes
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateServers(serversFile); // Update the servers
                README_PATH = ReadMeManager.updateReadme(); // Update the README.md
            }
        }, 0, TimeUnit.MINUTES.toMillis(5));

        // Commit the files every hour
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                GitUtils.commitFiles("Scheduled update", serversFile.toPath(), README_PATH); // Commit the files
            }
        }, TimeUnit.MINUTES.toMillis(3), TimeUnit.HOURS.toMillis(1));
    }

    @SneakyThrows
    public static void updateServers(File serversFile) {
        List<PiaServer> servers = getPiaServers();

        // Remove the servers that haven't been active in 2 weeks
        int before = SERVERS.size();
        SERVERS.removeIf(server -> System.currentTimeMillis() - server.getLastSeen().getTime() > REMOVAL_THRESHOLD);
        System.out.printf("Removed %s servers that haven't been active in 2 weeks%n", before - SERVERS.size());

        // Add the new servers to the list
        int newServers = 0;
        for (PiaServer piaServer : servers) {
            boolean newServer = SERVERS.stream().noneMatch(server -> server.getIp().equals(piaServer.getIp()));
            if (newServer) {
                newServers++;
            }

            // Add the server to the list
            SERVERS.add(piaServer);
        }

        // Save the servers to the file
        Files.writeString(serversFile.toPath(), Main.GSON.toJson(SERVERS));
        System.out.printf("Wrote %s servers to the file (+%s new)%n", SERVERS.size(), newServers);
    }

    @SneakyThrows
    private static List<PiaServer> getPiaServers() {
        long start = System.currentTimeMillis();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PIA_OPENVPN_CONFIGS_URL))
                .GET()
                .build();
        HttpResponse<Path> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(Files.createTempFile("openvpn", ".zip")));
        if (response.statusCode() != 200) {
            throw new IOException("Failed to get the PIA OpenVPN configs, status code: " + response.statusCode());
        }
        System.out.printf("Downloaded the OpenVPN configs in %sms%n", System.currentTimeMillis() - start);
        Path downloadedFile = response.body();
        File tempDir = Files.createTempDirectory("openvpn").toFile();
        ZipUnArchiver unArchiver = new ZipUnArchiver();
        unArchiver.setSourceFile(downloadedFile.toFile());
        unArchiver.setDestDirectory(tempDir);
        unArchiver.extract();

        File[] files = tempDir.listFiles();
        if (files == null || files.length == 0) {
            throw new IOException("Failed to extract the OpenVPN configs");
        }

        List<PiaServer> servers = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory() || !file.getName().endsWith(".ovpn")) {
                continue;
            }
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines) {
                if (line.startsWith("remote ")) {
                    String[] parts = line.split(" ");
                    String hostname = parts[1];
                    String region = file.getName().split("\\.")[0];
                    Record[] records = new Lookup(hostname, Type.A).run();
                    if (records != null) {
                        for (Record record : records) {
                            ARecord aRecord = (ARecord) record;
                            servers.add(new PiaServer(aRecord.getAddress().getHostAddress(), region, new Date()));
                        }
                    }
                    break;
                }
            }
        }
        return servers;
    }
}
