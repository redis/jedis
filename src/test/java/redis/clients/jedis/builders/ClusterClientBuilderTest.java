package redis.clients.jedis.builders;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.RedisClusterClient;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.json.JsonObjectMapper;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.search.FTSearchParams;

@ExtendWith(MockitoExtension.class)
class ClusterClientBuilderTest {

  @Mock
  CommandExecutor exec;
  @Mock
  ConnectionProvider provider;
  @Captor
  ArgumentCaptor<CommandObject<?>> cap;

  private static List<String> argsToStrings(CommandObject<?> co) {
    List<String> out = new ArrayList<>();
    for (Rawable r : co.getArguments()) {
      out.add(new String(r.getRaw(), StandardCharsets.UTF_8));
    }
    return out;
  }

  private static Set<HostAndPort> someNodes() {
    Set<HostAndPort> nodes = new HashSet<>();
    nodes.add(new HostAndPort("127.0.0.1", 7000));
    return nodes;
  }

  @Test
  void clusterNodesEmptyShouldThrow() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
      () -> RedisClusterClient.builder().nodes(new HashSet<>()).build());

    assertThat(ex.getMessage(),
      containsString("At least one cluster node must be specified for cluster mode"));
  }

  @Test
  void negativeMaxTotalRetriesDurationShouldThrow() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
      () -> RedisClusterClient.builder().nodes(someNodes())
          .maxTotalRetriesDuration(Duration.ofMillis(-1)).build());

    assertThat(ex.getMessage(),
      containsString("Max total retries duration cannot be negative for cluster mode"));
  }

  @Test
  void negativeTopologyRefreshShouldThrow() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
      () -> RedisClusterClient.builder().nodes(someNodes())
          .topologyRefreshPeriod(Duration.ofMillis(-1)).build());

    assertThat(ex.getMessage(),
      containsString("Topology refresh period cannot be negative for cluster mode"));
  }

  @Test
  void buildWithPositiveDurationsAndConfig_usesProvidedExecAndProvider() {
    try (RedisClusterClient client = RedisClusterClient.builder().nodes(someNodes())
        .clientConfig(redis.clients.jedis.DefaultJedisClientConfig.builder().build()).maxAttempts(3)
        .maxTotalRetriesDuration(Duration.ofMillis(10)).topologyRefreshPeriod(Duration.ofMillis(50))
        .connectionProvider(provider).commandExecutor(exec).build()) {

      client.ping();
    }
    verify(exec, atLeastOnce()).broadcastCommand(cap.capture());
    assertThat(argsToStrings(cap.getValue()).get(0), containsString("PING"));
  }

  @Test
  void nodesNotProvidedShouldThrow() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
      () -> RedisClusterClient.builder().build());

    assertThat(ex.getMessage(),
      containsString("At least one cluster node must be specified for cluster mode"));
  }

  @Test
  void searchDialectZeroShouldThrow() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
      () -> RedisClusterClient.builder().searchDialect(0));

    assertThat(ex.getMessage(), containsString("DIALECT=0 cannot be set."));
  }

  @Test
  void keyPreprocessorAppliedInCluster() {
    try (RedisClusterClient client = RedisClusterClient.builder().nodes(someNodes())
        .connectionProvider(provider).commandExecutor(exec).keyPreProcessor(k -> "prefix:" + k)
        .build()) {

      client.set("k", "v");
      verify(exec).executeCommand(cap.capture());
      List<String> args = argsToStrings(cap.getValue());
      // SET prefix:k v
      assertThat(args.get(0), containsString("SET"));
      assertEquals("prefix:k", args.get(1));
      assertEquals("v", args.get(2));
    }
  }

  @Test
  void jsonObjectMapperAppliedInCluster() {
    JsonObjectMapper mapper = Mockito.mock(JsonObjectMapper.class);
    when(mapper.toJson(Mockito.any())).thenReturn("JSON:obj");

    try (RedisClusterClient client = RedisClusterClient.builder().nodes(someNodes())
        .connectionProvider(provider).commandExecutor(exec).jsonObjectMapper(mapper).build()) {

      client.jsonSetWithEscape("k", Path2.ROOT_PATH, Collections.singletonMap("a", 1));
      verify(exec).executeCommand(cap.capture());
      List<String> args = argsToStrings(cap.getValue());
      // JSON.SET k $ JSON:obj
      assertEquals("JSON.SET", args.get(0));
      assertEquals("k", args.get(1));
      assertEquals("$", args.get(2));
      assertEquals("JSON:obj", args.get(3));
    }
  }

  @Test
  void searchDialectAppliedInCluster() {
    try (RedisClusterClient client = RedisClusterClient.builder().nodes(someNodes())
        .connectionProvider(provider).commandExecutor(exec).searchDialect(3).build()) {

      client.ftSearch("idx", "q", new FTSearchParams());
      verify(exec, atLeastOnce()).executeCommand(cap.capture());
      List<String> args = argsToStrings(cap.getValue());
      // FT.SEARCH idx q DIALECT 3
      assertEquals("FT.SEARCH", args.get(0));
      assertEquals("idx", args.get(1));
      assertEquals("q", args.get(2));
      assertEquals("DIALECT", args.get(3));
      assertEquals("3", args.get(4));
    }
  }
}
