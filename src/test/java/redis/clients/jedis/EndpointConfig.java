package redis.clients.jedis;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import redis.clients.jedis.util.JedisURIHelper;
import redis.clients.jedis.util.TlsUtil;

import java.io.FileReader;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class EndpointConfig {

    private final boolean tls;
    private final String username;
    private final String password;
    private final int bdbId;
    private final List<URI> endpoints;
    private final String tlsCertPath;
    private final List<String> discoveryEndpoints;

    /** Port of the Redis Enterprise discovery service; hard-coded and non-configurable in RE. */
    private static final int DISCOVERY_SERVICE_PORT = 8001;

    public EndpointConfig(HostAndPort hnp, String username, String password, boolean tls, String tlsCertPath) {
        this.tls = tls;
        this.username = username;
        this.password = password;
        this.bdbId = 0;
        this.endpoints = Collections.singletonList(
            URI.create(getURISchema(tls) + hnp.getHost() + ":" + hnp.getPort()));
        this.tlsCertPath = tlsCertPath;
        this.discoveryEndpoints = null;
    }

    public HostAndPort getHostAndPort() {
        return JedisURIHelper.getHostAndPort(endpoints.get(0));
    }

    public HostAndPort getHostAndPort(int index) {
        return JedisURIHelper.getHostAndPort(endpoints.get(index));
    }

    public List<HostAndPort> getHostsAndPorts() {
        List<HostAndPort> result = new ArrayList<>();
        for (URI endpoint : endpoints) {
            result.add(JedisURIHelper.getHostAndPort(endpoint));
        }
        return result;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username == null? "default" : username;
    }

    public String getHost() {
        return getHostAndPort().getHost();
    }

    public int getPort() {
        return getHostAndPort().getPort();
    }

    public Boolean isTls() {
        return tls;
    }

    public Path getCertificatesLocation() {
        return Paths.get(tlsCertPath);
    }

    public int getBdbId() { return bdbId; }

    public URI getURI() {
        return endpoints.get(0);
    }

    public class EndpointURIBuilder {
        private boolean tls;

        private String username;

        private String password;

        private String path;

        public EndpointURIBuilder() {
            this.username = "";
            this.password = "";
            this.path = "";
            this.tls = EndpointConfig.this.tls;
        }

        public EndpointURIBuilder defaultCredentials() {
            this.username = EndpointConfig.this.username == null ? "" : getUsername();
            this.password = EndpointConfig.this.getPassword();
            return this;
        }

        public EndpointURIBuilder tls(boolean v) {
            this.tls = v;
            return this;
        }

        public EndpointURIBuilder path(String v) {
            this.path = v;
            return this;
        }

        public EndpointURIBuilder credentials(String u, String p) {
            this.username = u;
            this.password = p;
            return this;
        }

        public URI build() {
            String userInfo = !(this.username.isEmpty() && this.password.isEmpty()) ?
                this.username + ':' + this.password + '@' :
                "";
            return URI.create(
                getURISchema(this.tls) + userInfo + getHost() + ":" + getPort() + this.path);
        }
    }

    public EndpointURIBuilder getURIBuilder() {
        return new EndpointURIBuilder();
    }

    public DefaultJedisClientConfig.Builder getClientConfigBuilder() {
      DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder()
          .password(password).ssl(tls);

        if (tls && tlsCertPath != null) {
          builder.sslSocketFactory(TlsUtil.sslSocketFactoryForEnv(Paths.get(tlsCertPath)));
        }

        if (username != null) {
          return builder.user(username);
        }

        return builder;
    }

    protected String getURISchema(boolean tls) {
        return (tls ? "rediss" : "redis") + "://";
    }

    public static HashMap<String, EndpointConfig> loadFromJSON(String filePath) throws Exception {
        Gson gson = new GsonBuilder().setFieldNamingPolicy(
            FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

        HashMap<String, EndpointConfig> configs;
        try (FileReader reader = new FileReader(filePath)) {
            configs = gson.fromJson(reader, new TypeToken<HashMap<String, EndpointConfig>>() {
            }.getType());
        }
        return configs;
    }

    /**
     * Addresses of the Redis Enterprise discovery service (Sentinel-compatible API), as
     * {@code host:port} strings - one entry per cluster node. Empty when the endpoint configuration
     * does not advertise them.
     */
    public List<String> getDiscoveryEndpoints() {
        return discoveryEndpoints == null ? Collections.<String> emptyList() : discoveryEndpoints;
    }

    /**
     * {@link #getDiscoveryEndpoints()} parsed into {@link HostAndPort}. An entry without a port
     * defaults to the Redis Enterprise discovery-service port 8001.
     */
    public List<HostAndPort> getDiscoveryHostsAndPorts() {
        List<HostAndPort> result = new ArrayList<>();
        for (String discoveryEndpoint : getDiscoveryEndpoints()) {
            int separator = discoveryEndpoint.lastIndexOf(':');
            if (separator > 0) {
                result.add(new HostAndPort(discoveryEndpoint.substring(0, separator),
                    Integer.parseInt(discoveryEndpoint.substring(separator + 1))));
            } else {
                result.add(new HostAndPort(discoveryEndpoint, DISCOVERY_SERVICE_PORT));
            }
        }
        return result;
    }
}
