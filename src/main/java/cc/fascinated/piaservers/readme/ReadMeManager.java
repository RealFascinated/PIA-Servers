package cc.fascinated.piaservers.readme;

import cc.fascinated.piaservers.Main;
import cc.fascinated.piaservers.pia.PiaManager;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;

public class ReadMeManager {

    @SneakyThrows
    public ReadMeManager() {
        InputStream readmeStream = Main.class.getResourceAsStream("/README.md");
        if (readmeStream == null) {
            System.out.println("Failed to find README.md");
            return;
        }
        File readmeFile = new File("README.md");
        if (!readmeFile.exists()) {
            readmeFile.createNewFile();
        }

        String contents = new String(readmeStream.readAllBytes());
        contents = contents.replace("{server_count}", String.valueOf(PiaManager.SERVERS.size()));
        contents = contents.replace("{last_update}", new Date().toString().replaceAll(" ", "_"));

        Files.write(readmeFile.toPath(), contents.getBytes());
    }
}
