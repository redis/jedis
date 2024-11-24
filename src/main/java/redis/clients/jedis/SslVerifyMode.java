package redis.clients.jedis;

/**
 * Enumeration of SSL/TLS hostname verification modes.
 */
public enum SslVerifyMode {

    /**
     * No verification at all.
     */
    INSECURE,

    /**
     * Verify the CA and certificate without verifying that the hostname matches.
     */
    CA,

    /**
     * Full certificate verification.
     */
    FULL;
}
