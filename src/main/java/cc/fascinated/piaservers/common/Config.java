package cc.fascinated.piaservers.common;

public class Config {

    /**
     * Are we in production?
     *
     * @return If we are in production
     */
    public static boolean isProduction() {
        return System.getenv().containsKey("ENVIRONMENT") && System.getenv("ENVIRONMENT").equals("production");
    }
}
