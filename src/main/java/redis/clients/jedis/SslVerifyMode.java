package redis.clients.jedis;

/**
 * Enumeration of SSL/TLS hostname verification modes.
 */
public enum SslVerifyMode {

    INSECURE,

    CA,

    FULL;
}
