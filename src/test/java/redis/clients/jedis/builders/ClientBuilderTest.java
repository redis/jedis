package redis.clients.jedis.builders;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisSentineled;
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
    try (JedisPooled client = JedisPooled.builder().commandExecutor(exec)
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

    try (JedisPooled client = JedisPooled.builder().commandExecutor(exec)
        .connectionProvider(provider).jsonObjectMapper(mapper).build()) {

      client.jsonSetWithEscape("k", Path2.ROOT_PATH, Collections.singletonMap("a", 1));
    }
    verify(exec).executeCommand(cap.capture());
    assertThat(argsToStrings(cap.getValue()), contains("JSON.SET", "k", "$", "JSON:{a=1}"));
  }

  @Test
  void appliesSearchDialect() {
    try (JedisPooled client = JedisPooled.builder().commandExecutor(exec)
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

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> JedisPooled
        .builder().commandExecutor(exec).connectionProvider(provider).cache(cache).build(),
      "Cache requires RESP3");

    assertThat(ex.getMessage(), containsString("Client-side caching is only supported with RESP3"));

  }

  @Test
  void standaloneValidateHostPortRequired() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
      () -> JedisPooled.builder().hostAndPort(null).build());

    assertThat(ex.getMessage(), containsString("Either URI or host/port must be specified"));
  }

  @Test
  void sentinelValidateMasterAndSentinels() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
      () -> JedisSentineled.builder().build());
    assertThat(ex.getMessage(), containsString("Master name is required for Sentinel mode"));

    ex = assertThrows(IllegalArgumentException.class,
      () -> JedisSentineled.builder().masterName("mymaster").build());
    assertThat(ex.getMessage(),
      containsString("At least one sentinel must be specified for Sentinel mode"));

    ex = assertThrows(IllegalArgumentException.class, () -> JedisSentineled.builder()
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
}
