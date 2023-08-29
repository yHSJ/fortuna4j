package me.jshy.fortuna4j;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvManager {
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    public static String get(String key) {
        return dotenv.get(key.toUpperCase());
    }

}
