package redis.clients.jedis.builders;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import redis.clients.jedis.util.ReflectionTestUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import redis.clients.jedis.*;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.util.CompareCondition;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.json.JsonObjectMapper;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.search.FTSearchParams;

@ExtendWith(MockitoExtension.class)
class ClientBuilderTest {

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

  @Test
  void appliesKeyPreprocessorToCommandObjects() {
    try (RedisClient client = RedisClient.builder().commandExecutor(exec)
        .connectionProvider(provider).keyPreProcessor(k -> "prefix:" + k).build()) {

      client.set("key", "v");
    }
    verify(exec).executeCommand(cap.capture());
    assertThat(argsToStrings(cap.getValue()), contains("SET", "prefix:key", "v"));
  }

  @Test
  void appliesJsonObjectMapper() {
    JsonObjectMapper mapper = mock(JsonObjectMapper.class);
    when(mapper.toJson(any())).thenReturn("JSON:{a=1}");

    try (RedisClient client = RedisClient.builder().commandExecutor(exec)
        .connectionProvider(provider).jsonObjectMapper(mapper).build()) {

      client.jsonSetWithEscape("k", Path2.ROOT_PATH, Collections.singletonMap("a", 1));
    }
    verify(exec).executeCommand(cap.capture());
    assertThat(argsToStrings(cap.getValue()), contains("JSON.SET", "k", "$", "JSON:{a=1}"));
  }

  @Test
  void appliesSearchDialect() {
    try (RedisClient client = RedisClient.builder().commandExecutor(exec)
        .connectionProvider(provider).searchDialect(3).build()) {

      client.ftSearch("idx", "q", new FTSearchParams());
    }
    verify(exec, atLeastOnce()).executeCommand(cap.capture());
    List<String> args = argsToStrings(cap.getValue());
    assertThat(args, contains("FT.SEARCH", "idx", "q", "DIALECT", "3"));
  }

  @Test
  void cacheRequiresRESP3() {
    Cache cache = mock(Cache.class);

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> RedisClient
        .builder().commandExecutor(exec).connectionProvider(provider).cache(cache).build(),
      "Cache requires RESP3");

    assertThat(ex.getMessage(), containsString("Client-side caching is only supported with RESP3"));

  }

  @Test
  void standaloneValidateHostPortRequired() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
      () -> RedisClient.builder().hostAndPort(null).build());

    assertThat(ex.getMessage(), containsString("Either URI or host/port must be specified"));
  }

  @Test
  void sentinelValidateMasterAndSentinels() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
      () -> RedisSentinelClient.builder().build());
    assertThat(ex.getMessage(), containsString("Master name is required for Sentinel mode"));

    ex = assertThrows(IllegalArgumentException.class,
      () -> RedisSentinelClient.builder().masterName("mymaster").build());
    assertThat(ex.getMessage(),
      containsString("At least one sentinel must be specified for Sentinel mode"));

    ex = assertThrows(IllegalArgumentException.class, () -> RedisSentinelClient.builder()
        .masterName("mymaster").sentinels(Collections.emptySet()).build());
    assertThat(ex.getMessage(),
      containsString("At least one sentinel must be specified for Sentinel mode"));
  }

  @Test
  void setWithValueCondition() {
    try (JedisPooled client = JedisPooled.builder().commandExecutor(exec)
        .connectionProvider(provider).build()) {

      client.set("key", "value",
        SetParams.setParams().xx().condition(CompareCondition.valueEq("oldValue")));
    }
    verify(exec).executeCommand(cap.capture());
    assertThat(argsToStrings(cap.getValue()),
      contains("SET", "key", "value", "IFEQ", "oldValue", "XX"));
  }

  @Test
  void setWithDigestCondition() {
    try (JedisPooled client = JedisPooled.builder().commandExecutor(exec)
        .connectionProvider(provider).build()) {

      client.set("key", "value", SetParams.setParams().nx().ex(100)
          .condition(CompareCondition.digestEq("0123456789abcdef")));
    }
    verify(exec).executeCommand(cap.capture());
    assertThat(argsToStrings(cap.getValue()),
      contains("SET", "key", "value", "IFDEQ", "0123456789abcdef", "NX", "EX", "100"));
  }

  @Test
  void delexWithValueCondition() {
    when(exec.executeCommand(any())).thenReturn(1L);

    try (JedisPooled client = JedisPooled.builder().commandExecutor(exec)
        .connectionProvider(provider).build()) {

      client.delex("key", CompareCondition.valueNe("value"));
    }
    verify(exec).executeCommand(cap.capture());
    assertThat(argsToStrings(cap.getValue()), contains("DELEX", "key", "IFNE", "value"));
  }

  @Test
  void delexWithDigestCondition() {
    when(exec.executeCommand(any())).thenReturn(1L);

    try (JedisPooled client = JedisPooled.builder().commandExecutor(exec)
        .connectionProvider(provider).build()) {

      client.delex("key", CompareCondition.digestNe("fedcba9876543210"));
    }
    verify(exec).executeCommand(cap.capture());
    assertThat(argsToStrings(cap.getValue()),
      contains("DELEX", "key", "IFDNE", "fedcba9876543210"));
  }

  @Test
  void digestKey() {
    try (JedisPooled client = JedisPooled.builder().commandExecutor(exec)
        .connectionProvider(provider).build()) {

      client.digestKey("key");
    }
    verify(exec).executeCommand(cap.capture());
    assertThat(argsToStrings(cap.getValue()), contains("DIGEST", "key"));
  }

  @SuppressWarnings("deprecation")
  @Test
  void fromURI_withInvalidURI_throwsException() {
    // URI with credentials
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
      () -> RedisClient.builder().fromURI("uriuser:uripass@localhost:6379"));

    assertThat(ex.getMessage(), containsString("Invalid Redis URI"));
  }

  @Test
  @SuppressWarnings("deprecation")
  void fromUri_existingClientConfigIsPreserved() {
    JedisClientConfig config = DefaultJedisClientConfig.builder().user("testuser")
        .password("testpass").connectionTimeoutMillis(5000).build();

    // URI without credentials - should preserve config credentials
    StandaloneClientBuilder<RedisClient> builder = RedisClient.builder().clientConfig(config)
        .fromURI("redis://localhost:6379");

    JedisClientConfig resultConfig = getClientConfig(builder);
    assertThat(resultConfig.getUser(), equalTo("testuser"));
    assertThat(resultConfig.getPassword(), equalTo("testpass"));
    assertThat(resultConfig.getConnectionTimeoutMillis(), equalTo(5000));
  }

  @Test
  @SuppressWarnings("deprecation")
  void fromURI_WithCredentials_overridesClientConfigCredentials() {
    JedisClientConfig config = DefaultJedisClientConfig.builder().user("olduser")
        .password("oldpass").build();

    // URI with credentials should override config credentials
    StandaloneClientBuilder<RedisClient> builder = RedisClient.builder().clientConfig(config)
        .fromURI("redis://newuser:newpass@localhost:6379");

    JedisClientConfig resultConfig = getClientConfig(builder);
    assertThat(resultConfig.getUser(), equalTo("newuser"));
    assertThat(resultConfig.getPassword(), equalTo("newpass"));
  }

  @Test
  @SuppressWarnings("deprecation")
  void fromURI_ThenClientConfig_configWins() {
    // URI with credentials
    StandaloneClientBuilder<RedisClient> builder = RedisClient.builder()
        .fromURI("redis://uriuser:uripass@localhost:6379").clientConfig(
          DefaultJedisClientConfig.builder().user("configuser").password("configpass").build());

    JedisClientConfig resultConfig = getClientConfig(builder);
    assertThat(resultConfig.getUser(), equalTo("configuser"));
    assertThat(resultConfig.getPassword(), equalTo("configpass"));
  }

  @Test
  @SuppressWarnings("deprecation")
  void fromUri_multipleFromURICalls_lastWins() {
    StandaloneClientBuilder<RedisClient> builder = RedisClient.builder()
        .fromURI("redis://user1:pass1@localhost:6379/1")
        .fromURI("redis://user2:pass2@localhost:6380/2");

    JedisClientConfig resultConfig = getClientConfig(builder);
    assertThat(resultConfig.getUser(), equalTo("user2"));
    assertThat(resultConfig.getPassword(), equalTo("pass2"));
    assertThat(resultConfig.getDatabase(), equalTo(2));
  }

  @Test
  @SuppressWarnings("deprecation")
  void fromUri_partialURIOverride_preservesNonURIValues() {
    // Real-world scenario from issue #4416
    JedisClientConfig config = DefaultJedisClientConfig.builder().user("mark").password("secret")
        .connectionTimeoutMillis(5000).socketTimeoutMillis(3000).build();

    // URI without credentials, only host/port
    StandaloneClientBuilder<RedisClient> builder = RedisClient.builder().clientConfig(config)
        .fromURI("redis://localhost:6379");

    JedisClientConfig resultConfig = getClientConfig(builder);
    // Should preserve credentials and timeouts from config
    assertThat(resultConfig.getUser(), equalTo("mark"));
    assertThat(resultConfig.getPassword(), equalTo("secret"));
    assertThat(resultConfig.getConnectionTimeoutMillis(), equalTo(5000));
    assertThat(resultConfig.getSocketTimeoutMillis(), equalTo(3000));
  }

  @Test
  void fromUri_withProtocol_OverridesClientConfig() {
    JedisClientConfig config = DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP2)
        .build();
    StandaloneClientBuilder<RedisClient> builder = RedisClient.builder().clientConfig(config)
        .fromURI("redis://localhost:6379?protocol=3");

    JedisClientConfig resultConfig = getClientConfig(builder);
    assertThat(resultConfig.getRedisProtocol(), equalTo(RedisProtocol.RESP3));
  }

  @Test
  void fromUri_noProtocol_PreservesClientConfig() {
    JedisClientConfig config = DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP2)
        .build();
    StandaloneClientBuilder<RedisClient> builder = RedisClient.builder().clientConfig(config)
        .fromURI("redis://localhost:6379");

    JedisClientConfig resultConfig = getClientConfig(builder);
    assertThat(resultConfig.getRedisProtocol(), equalTo(RedisProtocol.RESP2));
  }

  @Test
  void fromUri_noProtocol_preservesDefault() {
    StandaloneClientBuilder<RedisClient> builder = RedisClient.builder()
        .fromURI("redis://localhost:6379");

    JedisClientConfig resultConfig = getClientConfig(builder);
    assertNull(resultConfig.getRedisProtocol());
  }

  /**
   * Helper method to access the protected clientConfig field from the builder using reflection.
   */
  private JedisClientConfig getClientConfig(
      redis.clients.jedis.builders.AbstractClientBuilder<?, ?> builder) {
    return ReflectionTestUtil.getField(builder, "clientConfig");
  }
}
