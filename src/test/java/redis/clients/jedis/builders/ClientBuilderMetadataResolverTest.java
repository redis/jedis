package redis.clients.jedis.builders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandFlagsRegistry;
import redis.clients.jedis.CommandFlagsRegistry.CommandFlag;
import redis.clients.jedis.CommandMetadataResolver;
import redis.clients.jedis.CommandMetadataResolverFactory;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.StaticCommandFlagsRegistry;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.csc.CacheKey;
import redis.clients.jedis.executors.CommandExecutor;
import redis.clients.jedis.providers.ConnectionProvider;

@ExtendWith(MockitoExtension.class)
class ClientBuilderMetadataResolverTest {

  private static final String CUSTOM_METADATA_JSON = "{\n" //
      + "  \"commands\": {\n" //
      + "    \"GET\": {\"flags\": [\"write\"], \"tips\": [], \"firstKey\": 1, \"lastKey\": 1,\n" //
      + "      \"step\": 1, \"keySpecs\": [{\"flags\": [\"rw\"]}]}\n" //
      + "  }\n" //
      + "}\n";

  @Mock
  CommandExecutor exec;
  @Mock
  ConnectionProvider provider;

  @TempDir
  Path tempDir;

  private CommandMetadataResolver customResolver() throws IOException {
    Path file = tempDir.resolve("custom-metadata.json");
    Files.write(file, CUSTOM_METADATA_JSON.getBytes(StandardCharsets.UTF_8));
    return CommandMetadataResolverFactory.fromFile(file);
  }

  private static CacheKey<String> getCacheKey() {
    return new CacheKey<>(
        new CommandObject<>(new CommandArguments(Command.GET).key("k"), BuilderFactory.STRING));
  }

  private static JedisClientConfig resp3Config() {
    return DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP3).build();
  }

  @Test
  void clientLevelResolverDrivesCacheability() throws IOException {
    try (
        RedisClient client = RedisClient.builder().clientConfig(resp3Config()).commandExecutor(exec)
            .connectionProvider(provider).cacheConfig(CacheConfig.builder().build())
            .commandMetadataResolver(customResolver()).build()) {

      assertFalse(client.getCache().isCacheable(getCacheKey())); // write in the custom metadata
    }
  }

  @Test
  void withoutResolverTheSharedMetadataDecides() {
    try (
        RedisClient client = RedisClient.builder().clientConfig(resp3Config()).commandExecutor(exec)
            .connectionProvider(provider).cacheConfig(CacheConfig.builder().build()).build()) {

      assertTrue(client.getCache().isCacheable(getCacheKey()));
    }
  }

  @Test
  void clusterDefaultFlagsRegistryComesFromTheResolver() throws IOException {
    ClusterClientBuilder<Object> builder = clusterBuilder();
    builder.commandMetadataResolver(customResolver());

    EnumSet<CommandFlag> flags = builder.getCommandFlags()
        .getFlags(new CommandArguments(Command.GET).key("k"));
    assertTrue(flags.contains(CommandFlag.WRITE));
    assertFalse(flags.contains(CommandFlag.READONLY));
  }

  @Test
  void clusterWithoutResolverUsesTheSharedRegistry() {
    assertSame(StaticCommandFlagsRegistry.registry(), clusterBuilder().getCommandFlags());
  }

  @Test
  void explicitCommandFlagsWinOverTheResolver() throws IOException {
    CommandFlagsRegistry explicit = StaticCommandFlagsRegistry.registry();
    ClusterClientBuilder<Object> builder = clusterBuilder();
    builder.commandFlags(explicit).commandMetadataResolver(customResolver());

    assertSame(explicit, builder.getCommandFlags());
  }

  private static ClusterClientBuilder<Object> clusterBuilder() {
    return new ClusterClientBuilder<Object>() {
      @Override
      protected Object createClient() {
        return null;
      }
    };
  }
}
