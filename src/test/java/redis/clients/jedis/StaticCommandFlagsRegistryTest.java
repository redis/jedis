package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.CommandFlagsRegistry.CommandFlag;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Unit tests for StaticCommandFlagsRegistry. Tests the retrieval of command flags for various Redis
 * commands, including commands with subcommands.
 */
public class StaticCommandFlagsRegistryTest {

  private StaticCommandFlagsRegistry registry;

  @BeforeEach
  public void setUp() {
    registry = new StaticCommandFlagsRegistry();
  }

  /**
   * Test that FUNCTION LOAD command returns the correct flags. FUNCTION LOAD should have: DENYOOM,
   * NOSCRIPT, WRITE flags.
   */
  @Test
  public void testFunctionLoadCommandFlags() {
    // Create a CommandArguments for "FUNCTION LOAD"
    CommandArguments functionLoadArgs = new CommandArguments(Protocol.Command.FUNCTION).add("LOAD");

    EnumSet<CommandFlag> flags = registry.getFlags(functionLoadArgs);

    assertNotNull(flags, "Flags should not be null");
    assertFalse(flags.isEmpty(), "FUNCTION LOAD should have flags");
    assertEquals(3, flags.size(), "FUNCTION LOAD should have exactly 3 flags");
    assertTrue(flags.contains(CommandFlag.DENYOOM), "FUNCTION LOAD should have DENYOOM flag");
    assertTrue(flags.contains(CommandFlag.NOSCRIPT), "FUNCTION LOAD should have NOSCRIPT flag");
    assertTrue(flags.contains(CommandFlag.WRITE), "FUNCTION LOAD should have WRITE flag");
  }

  /**
   * Test that FUNCTION DELETE command returns the correct flags. FUNCTION DELETE should have:
   * NOSCRIPT, WRITE flags.
   */
  @Test
  public void testFunctionDeleteCommandFlags() {
    CommandArguments functionDeleteArgs = new CommandArguments(Protocol.Command.FUNCTION)
        .add("DELETE");

    EnumSet<CommandFlag> flags = registry.getFlags(functionDeleteArgs);

    assertNotNull(flags, "Flags should not be null");
    assertFalse(flags.isEmpty(), "FUNCTION DELETE should have flags");
    assertEquals(2, flags.size(), "FUNCTION DELETE should have exactly 2 flags");
    assertTrue(flags.contains(CommandFlag.NOSCRIPT), "FUNCTION DELETE should have NOSCRIPT flag");
    assertTrue(flags.contains(CommandFlag.WRITE), "FUNCTION DELETE should have WRITE flag");
  }

  /**
   * Test other subcommand examples: ACL SETUSER
   */
  @Test
  public void testAclSetUserCommandFlags() {
    CommandArguments aclSetUserArgs = new CommandArguments(Protocol.Command.ACL).add("SETUSER");

    EnumSet<CommandFlag> flags = registry.getFlags(aclSetUserArgs);

    assertNotNull(flags, "Flags should not be null");
    assertFalse(flags.isEmpty(), "ACL SETUSER should have flags");
    assertTrue(flags.contains(CommandFlag.ADMIN), "ACL SETUSER should have ADMIN flag");
    assertTrue(flags.contains(CommandFlag.NOSCRIPT), "ACL SETUSER should have NOSCRIPT flag");
  }

  /**
   * Test other subcommand examples: CONFIG GET
   */
  @Test
  public void testConfigGetCommandFlags() {
    CommandArguments configGetArgs = new CommandArguments(Protocol.Command.CONFIG).add("GET");

    EnumSet<CommandFlag> flags = registry.getFlags(configGetArgs);

    assertNotNull(flags, "Flags should not be null");
    assertFalse(flags.isEmpty(), "CONFIG GET should have flags");
    assertTrue(flags.contains(CommandFlag.ADMIN), "CONFIG GET should have ADMIN flag");
    assertTrue(flags.contains(CommandFlag.LOADING), "CONFIG GET should have LOADING flag");
    assertTrue(flags.contains(CommandFlag.STALE), "CONFIG GET should have STALE flag");
  }

  /**
   * Test simple command without subcommands: GET
   */
  @Test
  public void testGetCommandFlags() {
    CommandArguments getArgs = new CommandArguments(Protocol.Command.GET).add("key");

    EnumSet<CommandFlag> flags = registry.getFlags(getArgs);

    assertNotNull(flags, "Flags should not be null");
    assertFalse(flags.isEmpty(), "GET should have flags");
    assertTrue(flags.contains(CommandFlag.READONLY), "GET should have READONLY flag");
    assertTrue(flags.contains(CommandFlag.FAST), "GET should have FAST flag");
  }

  /**
   * Test simple command without subcommands: SET
   */
  @Test
  public void testSetCommandFlags() {
    CommandArguments setArgs = new CommandArguments(Protocol.Command.SET).add("key").add("value");

    EnumSet<CommandFlag> flags = registry.getFlags(setArgs);

    assertNotNull(flags, "Flags should not be null");
    assertFalse(flags.isEmpty(), "SET should have flags");
    assertTrue(flags.contains(CommandFlag.WRITE), "SET should have WRITE flag");
    assertTrue(flags.contains(CommandFlag.DENYOOM), "SET should have DENYOOM flag");
  }

  /**
   * Test that unknown commands return empty flags
   */
  @Test
  public void testUnknownCommandReturnsEmptyFlags() {
    ProtocolCommand unknownCommand = () -> SafeEncoder.encode("UNKNOWN_COMMAND_XYZ");
    CommandArguments unknownArgs = new CommandArguments(unknownCommand);

    EnumSet<CommandFlag> flags = registry.getFlags(unknownArgs);

    assertNotNull(flags, "Flags should not be null");
    assertTrue(flags.isEmpty(), "Unknown command should return empty flags");
  }

  /**
   * Test case insensitivity - command names should be normalized to uppercase
   */
  @Test
  public void testCaseInsensitivity() {
    ProtocolCommand functionCommand = () -> SafeEncoder.encode("function");
    CommandArguments functionLoadArgs = new CommandArguments(functionCommand).add("load");

    EnumSet<CommandFlag> flags = registry.getFlags(functionLoadArgs);

    assertNotNull(flags, "Flags should not be null");
    assertFalse(flags.isEmpty(), "function load (lowercase) should have flags");
    assertEquals(3, flags.size(), "function load should have exactly 3 flags");
    assertTrue(flags.contains(CommandFlag.DENYOOM), "function load should have DENYOOM flag");
    assertTrue(flags.contains(CommandFlag.NOSCRIPT), "function load should have NOSCRIPT flag");
    assertTrue(flags.contains(CommandFlag.WRITE), "function load should have WRITE flag");
  }

  /**
   * Test that unknown subcommands of parent commands fall back to parent command flags. If the
   * parent command also doesn't exist, it should return empty flags.
   */
  @Test
  public void testUnknownSubcommandFallback() {
    // Create a CommandArguments for "FUNCTION UNKNOWN_SUBCOMMAND"
    // This subcommand doesn't exist, so it should fall back to "FUNCTION" parent flags
    // Since "FUNCTION" parent has empty flags, it should return empty flags
    CommandArguments unknownSubcommandArgs = new CommandArguments(Protocol.Command.FUNCTION)
        .add("UNKNOWN_SUBCOMMAND");

    EnumSet<CommandFlag> flags = registry.getFlags(unknownSubcommandArgs);

    assertNotNull(flags, "Flags should not be null");
    assertTrue(flags.isEmpty(),
      "Unknown FUNCTION subcommand should return empty flags (parent flags)");
  }
}
