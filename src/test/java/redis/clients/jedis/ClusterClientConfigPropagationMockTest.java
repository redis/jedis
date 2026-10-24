package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.json.JsonObjectMapper;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.util.PrefixedKeyArgumentPreProcessor;
import redis.clients.jedis.util.SafeEncoder;
import redis.clients.jedis.util.server.RespResponse;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * The CommandObjects-level settings carried by a {@link JedisClientConfig} (key pre-processor, JSON
 * mapper, search dialect) must reach the commands built by {@link ClusterPipeline} and
 * {@link JedisCluster} when they are constructed from that config, exactly as they do for a
 * {@link RedisClusterClient} built from the same config.
 */
public class ClusterClientConfigPropagationMockTest {

  private static final String PREFIX = "tenant-a:";

  private TcpMockServer mockServer;
  private final List<String> received = new CopyOnWriteArrayList<>();
  private Set<HostAndPort> nodes;

  @BeforeEach
  public void setUp() throws IOException {
    mockServer = new TcpMockServer();
    mockServer.setCommandHandler((args, clientId) -> {
      String command = SafeEncoder.encode(args.getCommand().getRaw());
      if ("CLUSTER".equals(command)) {
        // a single primary owning every slot, announced on the mock server's own address
        return "*1\r\n*3\r\n:0\r\n:16383\r\n*2\r\n$9\r\n127.0.0.1\r\n:" + mockServer.getPort()
            + "\r\n";
      }
      if ("SET".equals(command) || "JSON.SET".equals(command)) {
        received.add(render(args));
        return RespResponse.ok();
      }
      return null; // HELLO and CLIENT fall through to the mock server's defaults
    });
    mockServer.start();
    nodes = Collections.singleton(new HostAndPort("127.0.0.1", mockServer.getPort()));
  }

  @AfterEach
  public void tearDown() throws IOException {
    mockServer.stop();
  }

  private static String render(CommandArguments args) {
    StringBuilder command = new StringBuilder();
    for (Rawable arg : args) {
      if (command.length() > 0) {
        command.append(' ');
      }
      command.append(SafeEncoder.encode(arg.getRaw()));
    }
    return command.toString();
  }

  private static JedisClientConfig prefixingConfig() {
    return DefaultJedisClientConfig.builder()
        .commandKeyArgumentPreProcessor(new PrefixedKeyArgumentPreProcessor(PREFIX)).build();
  }

  private static JsonObjectMapper customMapper() {
    return new JsonObjectMapper() {
      @Override
      public <T> T fromJson(String value, Class<T> valueType) {
        throw new UnsupportedOperationException();
      }

      @Override
      public String toJson(Object value) {
        return "custom:" + value;
      }
    };
  }

  @Test
  public void clusterPipelineFromClientConfigAppliesKeyPreProcessor() {
    try (ClusterPipeline pipeline = new ClusterPipeline(nodes, prefixingConfig())) {
      pipeline.set("key", "value");
      pipeline.sync();
    }
    assertThat(received, contains("SET " + PREFIX + "key value"));
  }

  @Test
  public void clusterPipelineFromClientConfigAppliesJsonObjectMapper() {
    JedisClientConfig config = DefaultJedisClientConfig.builder().jsonObjectMapper(customMapper())
        .build();
    try (ClusterPipeline pipeline = new ClusterPipeline(nodes, config)) {
      pipeline.jsonSetWithEscape("doc", Path2.ROOT_PATH, "value");
      pipeline.sync();
    }
    assertThat(received, contains("JSON.SET doc $ custom:value"));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void jedisClusterFromClientConfigAppliesKeyPreProcessor() {
    try (JedisCluster cluster = new JedisCluster(nodes, prefixingConfig())) {
      cluster.set("key", "value");
    }
    assertThat(received, contains("SET " + PREFIX + "key value"));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void jedisClusterFromClientConfigAppliesJsonObjectMapper() {
    JedisClientConfig config = DefaultJedisClientConfig.builder().jsonObjectMapper(customMapper())
        .build();
    try (JedisCluster cluster = new JedisCluster(nodes, config)) {
      cluster.jsonSetWithEscape("doc", Path2.ROOT_PATH, "value");
    }
    assertThat(received, contains("JSON.SET doc $ custom:value"));
  }
}
