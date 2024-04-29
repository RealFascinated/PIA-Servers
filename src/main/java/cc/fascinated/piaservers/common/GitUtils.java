package cc.fascinated.piaservers.common;

import java.nio.file.Path;

public class GitUtils {

    /**
     * Commit files to git
     *
     * @param message The commit message
     * @param files The files to commit
     */
    public static void commitFiles(String message, Path... files) {
        if (System.getenv("ENVIRONMENT").equals("production")) {
            runCommand("git", "config", "--global", "user.email", "liam+pia-servers-ci@fascinated.cc");
            runCommand("git", "config", "--global", "user.name", "PIA Servers CI");
        }
        for (Path file : files) {
            runCommand("git", "add", file.toAbsolutePath().toString());
        }
        runCommand("git", "commit", "-m", message);
        runCommand("git", "push", "https://pia-servers-ci:%s@git.fascinated.cc/Fascinated/PIA-Servers".formatted(System.getenv("AUTH_TOKEN")));
    }

    /**
     * Clone the repository
     */
    public static void cloneRepo() {
        if (System.getenv("ENVIRONMENT").equals("production")) {
            runCommand("git", "clone", "https://git.fascinated.cc/Fascinated/PIA-Servers.git");
        }
    }

    /**
     * Run a system command
     *
     * @param args The command to run (with arguments)
     */
    private static void runCommand(String... args) {
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        try {
            Process process = processBuilder.start();
            process.waitFor();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
