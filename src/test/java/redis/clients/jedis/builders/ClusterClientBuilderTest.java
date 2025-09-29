package redis.clients.jedis.builders;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.providers.ConnectionProvider;

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
    assertThrows(IllegalArgumentException.class, () -> new JedisCluster.Builder() {
    }.nodes(new HashSet<>()).build());
  }

  @Test
  void negativeMaxTotalRetriesDurationShouldThrow() {
    assertThrows(IllegalArgumentException.class, () -> new JedisCluster.Builder() {
    }.nodes(someNodes()).maxTotalRetriesDuration(Duration.ofMillis(-1)).build());
  }

  @Test
  void negativeTopologyRefreshShouldThrow() {
    assertThrows(IllegalArgumentException.class, () -> new JedisCluster.Builder() {
    }.nodes(someNodes()).topologyRefreshPeriod(Duration.ofMillis(-1)).build());
  }

  @Test
  void buildWithPositiveDurationsAndConfig_usesProvidedExecAndProvider() {
    JedisCluster client = assertDoesNotThrow(() -> JedisCluster.builder().nodes(someNodes())
        .clientConfig(redis.clients.jedis.DefaultJedisClientConfig.builder().build()).maxAttempts(3)
        .maxTotalRetriesDuration(Duration.ofMillis(10)).topologyRefreshPeriod(Duration.ofMillis(50))
        .connectionProvider(provider).commandExecutor(exec).build());

    client.ping();
    verify(exec, atLeastOnce()).broadcastCommand(cap.capture());
    assertThat(argsToStrings(cap.getValue()).get(0), containsString("PING"));
  }

  @Test
  void nodesNotProvidedShouldThrow() {
    assertThrows(IllegalArgumentException.class, () -> new JedisCluster.Builder() {
    }.build());
  }

  @Test
  void searchDialectZeroShouldThrow() {
    assertThrows(IllegalArgumentException.class, () -> JedisCluster.builder().searchDialect(0));
  }

  @Test
  void keyPreprocessorAppliedInCluster() {
    JedisCluster client = JedisCluster.builder().nodes(someNodes()).connectionProvider(provider)
        .commandExecutor(exec).keyPreProcessor(k -> "prefix:" + k).build();

    client.set("k", "v");
    verify(exec).executeCommand(cap.capture());
    List<String> args = argsToStrings(cap.getValue());
    // SET prefix:k v
    assertThat(args.get(0), containsString("SET"));
    org.junit.jupiter.api.Assertions.assertEquals("prefix:k", args.get(1));
    org.junit.jupiter.api.Assertions.assertEquals("v", args.get(2));
  }

  @Test
  void jsonObjectMapperAppliedInCluster() {
    redis.clients.jedis.json.JsonObjectMapper mapper = org.mockito.Mockito
        .mock(redis.clients.jedis.json.JsonObjectMapper.class);
    org.mockito.Mockito.when(mapper.toJson(org.mockito.Mockito.any())).thenReturn("JSON:obj");

    JedisCluster client = JedisCluster.builder().nodes(someNodes()).connectionProvider(provider)
        .commandExecutor(exec).jsonObjectMapper(mapper).build();

    client.jsonSetWithEscape("k", redis.clients.jedis.json.Path2.ROOT_PATH,
      java.util.Collections.singletonMap("a", 1));
    verify(exec).executeCommand(cap.capture());
    List<String> args = argsToStrings(cap.getValue());
    // JSON.SET k $ JSON:obj
    org.junit.jupiter.api.Assertions.assertEquals("JSON.SET", args.get(0));
    org.junit.jupiter.api.Assertions.assertEquals("k", args.get(1));
    org.junit.jupiter.api.Assertions.assertEquals("$", args.get(2));
    org.junit.jupiter.api.Assertions.assertEquals("JSON:obj", args.get(3));
  }

  @Test
  void searchDialectAppliedInCluster() {
    JedisCluster client = JedisCluster.builder().nodes(someNodes()).connectionProvider(provider)
        .commandExecutor(exec).searchDialect(3).build();

    client.ftSearch("idx", "q", new redis.clients.jedis.search.FTSearchParams());
    verify(exec, atLeastOnce()).executeCommand(cap.capture());
    List<String> args = argsToStrings(cap.getValue());
    // FT.SEARCH idx q DIALECT 3
    org.junit.jupiter.api.Assertions.assertEquals("FT.SEARCH", args.get(0));
    org.junit.jupiter.api.Assertions.assertEquals("idx", args.get(1));
    org.junit.jupiter.api.Assertions.assertEquals("q", args.get(2));
    org.junit.jupiter.api.Assertions.assertEquals("DIALECT", args.get(3));
    org.junit.jupiter.api.Assertions.assertEquals("3", args.get(4));
  }
}
