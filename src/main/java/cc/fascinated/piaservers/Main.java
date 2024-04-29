package cc.fascinated.piaservers;

import cc.fascinated.piaservers.pia.PiaManager;
import cc.fascinated.piaservers.readme.ReadMeManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.SneakyThrows;

import java.util.concurrent.TimeUnit;

public class Main {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    @SneakyThrows
    public static void main(String[] args) {
        PiaManager.updateServers();
        Thread.sleep(TimeUnit.MINUTES.toMillis(3));
        PiaManager.updateServers();
        new ReadMeManager();
    }
}