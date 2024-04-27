package cc.fascinated.piaservers.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

@AllArgsConstructor @Getter
public class PiaServer {
    /**
     * The IP of this server.
     */
    private final String ip;

    /**
     * The region this server is in.
     */
    private final String region;

    /**
     * The last time this IP was seen.
     */
    private Date lastSeen;
}
