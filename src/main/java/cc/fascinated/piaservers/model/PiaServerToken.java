package cc.fascinated.piaservers.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class PiaServerToken {
    /**
     * The ip for this server.
     */
    private final String ip;

    /**
     * The region this server is in.
     */
    private final String region;
}
