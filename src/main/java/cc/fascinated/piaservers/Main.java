package cc.fascinated.piaservers;

import cc.fascinated.piaservers.pia.PiaServer;
import cc.fascinated.piaservers.pia.PiaServerToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import lombok.SneakyThrows;

import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final String PIA_OPENVPN_CONFIGS_URL = "https://www.privateinternetaccess.com/openvpn/openvpn.zip";
    private static final long REMOVAL_THRESHOLD = TimeUnit.DAYS.toMicros(14); // 2 weeks

    @SneakyThrows
    public static void main(String[] args) {
        File serversFile = new File("servers.json");
        if (!serversFile.exists()) {
            System.out.println("serversFile.json does not exist, creating...");
            serversFile.createNewFile();
        }

        List<PiaServerToken> serverDomains = getServerDomains();
        System.out.println("Found " + serverDomains.size() + " server domains");

        // Load the serversFile from the file
        List<PiaServer> servers = GSON.fromJson(Files.readString(serversFile.toPath()), new TypeToken<PiaServer>() {}.getType());
        if (servers == null) {
            servers = new ArrayList<>();
        }
        List<PiaServer> toRemove = new ArrayList<>();

        System.out.println("Removing old servers...");
        // Get the servers that need to be removed
        for (PiaServer server : servers) {
            if (server.getLastSeen().getTime() < System.currentTimeMillis() - REMOVAL_THRESHOLD) {
                toRemove.add(server);
            }
        }
        servers.removeAll(toRemove); // Remove the servers
        System.out.printf("Removed %s old servers\n", toRemove.size());

        // Add the new servers to the list
        for (PiaServerToken serverToken : serverDomains) {
            InetAddress address = InetAddress.getByName(serverToken.getHostname());

            // Add the server to the list
            servers.add(new PiaServer(address.getHostAddress(), serverToken.getRegion(), new Date()));
        }

        // Save the servers to the file
        Files.writeString(serversFile.toPath(), GSON.toJson(servers));
        System.out.printf("Wrote %s servers to the file\n", servers.size());
    }

    @SneakyThrows
    private static List<PiaServerToken> getServerDomains() {
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
                    String domain = parts[1];
                    String region = file.getName().split("\\.")[0];

                    domains.add(new PiaServerToken(domain, region));
                    break;
                }
            }
        }

        return domains;
    }
}