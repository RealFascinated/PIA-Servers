package cc.fascinated.piaservers;

import cc.fascinated.piaservers.pia.PiaManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.SneakyThrows;

public class Main {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    @SneakyThrows
    public static void main(String[] args) {
        new PiaManager();
    }
}