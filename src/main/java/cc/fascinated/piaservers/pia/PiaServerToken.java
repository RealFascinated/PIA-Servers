package cc.fascinated.piaservers.pia;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class PiaServerToken {
    /**
     * The hostname for this server.
     */
    private final String hostname;

    /**
     * The region this server is in.
     */
    private final String region;
}
