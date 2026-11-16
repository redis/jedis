package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import redis.clients.jedis.CommandFlagsRegistry.CommandFlag;
import redis.clients.jedis.CommandFlagsRegistry.RequestPolicy;
import redis.clients.jedis.CommandFlagsRegistry.ResponsePolicy;
import redis.clients.jedis.Protocol.Command;

public class StaticCommandFlagsRegistryCreateTest {

  private static final String CUSTOM_METADATA_JSON = "{\n" //
      + "  \"commands\": {\n" //
      + "    \"GET\": {\"flags\": [\"write\", \"denyoom\"], \"tips\": [], \"firstKey\": 1,\n" //
      + "      \"lastKey\": 1, \"step\": 1, \"keySpecs\": [{\"flags\": [\"rw\"]}]},\n" //
      + "    \"CONFIG\": {\"flags\": [], \"tips\": [], \"firstKey\": 0, \"lastKey\": 0, \"step\": 0,\n" //
      + "      \"subcommands\": {\"CONFIG|SET\": {\"flags\": [\"admin\"],\n" //
      + "        \"tips\": [\"request_policy:all_nodes\", \"response_policy:all_succeeded\"],\n" //
      + "        \"firstKey\": 0, \"lastKey\": 0, \"step\": 0}}}\n" //
      + "  }\n" //
      + "}\n";

  @TempDir
  Path tempDir;

  private CommandMetadataResolver customResolver() throws IOException {
    Path file = tempDir.resolve("custom-metadata.json");
    Files.write(file, CUSTOM_METADATA_JSON.getBytes(StandardCharsets.UTF_8));
    return CommandMetadataResolverFactory.fromFile(file);
  }

  @Test
  public void createBuildsRegistryFromTheGivenResolver() throws IOException {
    StaticCommandFlagsRegistry registry = StaticCommandFlagsRegistry.create(customResolver());

    EnumSet<CommandFlag> flags = registry.getFlags(new CommandArguments(Command.GET).key("k"));
    assertTrue(flags.contains(CommandFlag.WRITE));
    assertTrue(flags.contains(CommandFlag.DENYOOM));
    assertFalse(flags.contains(CommandFlag.READONLY));
  }

  @Test
  public void createRegistersSubcommandsWithPolicies() throws IOException {
    StaticCommandFlagsRegistry registry = StaticCommandFlagsRegistry.create(customResolver());

    CommandArguments configSet = new CommandArguments(Command.CONFIG).add("SET");
    assertTrue(registry.getFlags(configSet).contains(CommandFlag.ADMIN));
    assertEquals(RequestPolicy.ALL_NODES, registry.getRequestPolicy(configSet));
    assertEquals(ResponsePolicy.ALL_SUCCEEDED, registry.getResponsePolicy(configSet));
  }

  @Test
  public void createdRegistryTreatsCommandsOutsideTheResolverAsUnknown() throws IOException {
    StaticCommandFlagsRegistry registry = StaticCommandFlagsRegistry.create(customResolver());

    CommandArguments set = new CommandArguments(Command.SET).key("k").add("v");
    assertEquals(StaticCommandFlagsRegistry.EMPTY_FLAGS, registry.getFlags(set));
    assertEquals(RequestPolicy.DEFAULT, registry.getRequestPolicy(set));
    assertEquals(ResponsePolicy.DEFAULT, registry.getResponsePolicy(set));
  }

  @Test
  public void createLeavesTheSharedSingletonUntouched() throws IOException {
    StaticCommandFlagsRegistry.create(customResolver());

    EnumSet<CommandFlag> sharedFlags = StaticCommandFlagsRegistry.registry()
        .getFlags(new CommandArguments(Command.GET).key("k"));
    assertTrue(sharedFlags.contains(CommandFlag.READONLY));
    assertFalse(sharedFlags.contains(CommandFlag.WRITE));
  }

  @Test
  public void createRejectsNullResolver() {
    assertThrows(IllegalArgumentException.class, () -> StaticCommandFlagsRegistry.create(null));
  }
}
