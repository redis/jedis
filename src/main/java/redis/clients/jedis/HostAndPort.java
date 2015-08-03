package redis.clients.jedis;

import java.net.InetAddress;

public class HostAndPort {
    public static final String LOCALHOST_STR;

    static {
        String localAddress = null;
        try {
            localAddress = InetAddress.getLocalHost().getHostAddress();
        }
        catch (Exception e) {
            localAddress = "localhost";
        }
        LOCALHOST_STR = localAddress;
    }

    private String host;
    private int port;

    public HostAndPort(String host, int port) {
        this.host = convertHost(host);
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof HostAndPort) {
            HostAndPort hp = (HostAndPort) obj;
            return port == hp.port && host.equals(hp.host);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return 31 * host.hashCode() + port;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }

    private String convertHost(String host) {
        if (host.equals("127.0.0.1") || host.startsWith("localhost") || host.equals("0.0.0.0") || host.startsWith("169.254")) {
            return LOCALHOST_STR;
        }
        else if (host.startsWith("::1") || host.startsWith("0:0:0:0:0:0:0:1")) {
            return LOCALHOST_STR;
        }

        return host;
    }
}
