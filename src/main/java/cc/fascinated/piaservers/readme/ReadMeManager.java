package cc.fascinated.piaservers.readme;

import cc.fascinated.piaservers.Main;
import cc.fascinated.piaservers.model.PiaServer;
import cc.fascinated.piaservers.pia.PiaManager;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReadMeManager {

    @SneakyThrows
    public ReadMeManager() {
        InputStream readmeStream = Main.class.getResourceAsStream("/README.md");
        if (readmeStream == null) {
            System.out.println("Failed to find README.md");
            return;
        }
        File readmeFile = new File("README.md");
        if (!readmeFile.exists()) { // Create the file if it doesn't exist
            readmeFile.createNewFile();
        }
        // Get the contents of the README.md
        String contents = new String(readmeStream.readAllBytes());

        // Replace the placeholders in the README.md file
        contents = contents.replace("{server_count}", String.valueOf(PiaManager.SERVERS.size()));
        contents = contents.replace("{last_update}", new Date().toString().replaceAll(" ", "_"));

        // Write total servers per-region
        Map<String, Integer> regionCounts = new HashMap<>();
        for (PiaServer server : PiaManager.SERVERS) {
            String region = server.getRegion();
            regionCounts.put(region, regionCounts.getOrDefault(region, 0) + 1);
        }
        contents = contents.replace("{server_table}", regionCounts.entrySet().stream()
                .map(entry -> "| " + entry.getKey() + " | " + entry.getValue() + " |") // Map the region to the count
                .reduce((a, b) -> a + "\n" + b).orElse("")); // Reduce the entries to a single string

        Files.write(readmeFile.toPath(), contents.getBytes());
    }
}