package redis.clients.jedis;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import redis.clients.jedis.util.JedisURIHelper;

import java.io.FileReader;
import java.net.URI;
import java.util.HashMap;
import java.util.List;

public class EndpointConfig {

  private boolean tls;
  private String username;
  private String password;
  private int bdbId;
  private Object rawEndpoints;

  private List<URI> endpoints;

  public EndpointConfig(boolean tls, String username, String password, int bdbId, Object rawEndpoints) {
    this.tls = tls;
    this.username = username;
    this.password = password;
    this.bdbId = bdbId;
    this.rawEndpoints = rawEndpoints;
  }

  public HostAndPort getHostAndPort() {
    return JedisURIHelper.getHostAndPort(endpoints.get(0));
  }

  public String getPassword() {
    return password;
  }

  public String getUsername() {
    return username;
  }

  public String getHost() {
    return getHostAndPort().getHost();
  }

  public int getPort() {
    return getHostAndPort().getPort();
  }

  public URI getURI() {
    return endpoints.get(0);
  }

  public URI getCustomizedURI(boolean withCredentials, String path)
  {
    return getCustomizedURI(withCredentials ? username : "", withCredentials ? password : "", path);
  }

  public URI getCustomizedURI(String u, String p, String path)
  {
    String userInfo = !(u.isEmpty() && p.isEmpty()) ? u + ":" + p + "@" : "";
    return URI.create((tls ? "rediss" : "redis") + "://" + userInfo + getHost() + ":" + getPort() + path);
  }

  public Connection getConnection() {
    return new Connection(getHostAndPort(), getClientConfigBuilder().build());
  }

  public Connection getConnection(int timeoutMillis) {
    return new Connection(getHostAndPort(), getClientConfigBuilder().timeoutMillis(timeoutMillis).build());
  }

  public Jedis getJedis() {
    return new Jedis(getHostAndPort(), getClientConfigBuilder().build());
  }

  public Jedis getJedis(int timeoutMillis) {
    return new Jedis(getHostAndPort(), getClientConfigBuilder().timeoutMillis(timeoutMillis).build());
  }

  public DefaultJedisClientConfig.Builder getClientConfigBuilder() {
    return DefaultJedisClientConfig.builder().user(username).password(password);
  }

  public static HashMap<String, EndpointConfig> loadFromJSON(String filePath) throws Exception {
    Gson gson = new Gson();
    HashMap<String, EndpointConfig> configs;
    try (FileReader reader = new FileReader(filePath)) {
      configs = gson.fromJson(reader, new TypeToken<HashMap<String, EndpointConfig>>() {
      }.getType());
    }
    return configs;
  }
}
