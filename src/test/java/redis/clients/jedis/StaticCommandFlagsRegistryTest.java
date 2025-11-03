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
 * Unit tests for StaticCommandFlagsRegistry.
 * Tests the retrieval of command flags for various Redis commands,
 * including commands with subcommands.
 */
public class StaticCommandFlagsRegistryTest {

  private StaticCommandFlagsRegistry registry;

  @BeforeEach
  public void setUp() {
    registry = new StaticCommandFlagsRegistry();
  }

  /**
   * Test that FUNCTION LOAD command returns the correct flags.
   * FUNCTION LOAD should have: DENYOOM, NOSCRIPT, WRITE flags.
   */
  @Test
  public void testFunctionLoadCommandFlags() {
    // Create a mock ProtocolCommand for "FUNCTION LOAD"
    ProtocolCommand functionLoadCommand = () -> SafeEncoder.encode("FUNCTION LOAD");

    EnumSet<CommandFlag> flags = registry.getFlags(functionLoadCommand);

    assertNotNull(flags, "Flags should not be null");
    assertFalse(flags.isEmpty(), "FUNCTION LOAD should have flags");
    assertEquals(3, flags.size(), "FUNCTION LOAD should have exactly 3 flags");
    assertTrue(flags.contains(CommandFlag.DENYOOM), "FUNCTION LOAD should have DENYOOM flag");
    assertTrue(flags.contains(CommandFlag.NOSCRIPT), "FUNCTION LOAD should have NOSCRIPT flag");
    assertTrue(flags.contains(CommandFlag.WRITE), "FUNCTION LOAD should have WRITE flag");
  }

  /**
   * Test that FUNCTION DELETE command returns the correct flags.
   * FUNCTION DELETE should have: NOSCRIPT, WRITE flags.
   */
  @Test
  public void testFunctionDeleteCommandFlags() {
    ProtocolCommand functionDeleteCommand = () -> SafeEncoder.encode("FUNCTION DELETE");

    EnumSet<CommandFlag> flags = registry.getFlags(functionDeleteCommand);

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
    ProtocolCommand aclSetUserCommand = () -> SafeEncoder.encode("ACL SETUSER");

    EnumSet<CommandFlag> flags = registry.getFlags(aclSetUserCommand);

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
    ProtocolCommand configGetCommand = () -> SafeEncoder.encode("CONFIG GET");

    EnumSet<CommandFlag> flags = registry.getFlags(configGetCommand);

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
    ProtocolCommand getCommand = () -> SafeEncoder.encode("GET");

    EnumSet<CommandFlag> flags = registry.getFlags(getCommand);

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
    ProtocolCommand setCommand = () -> SafeEncoder.encode("SET");

    EnumSet<CommandFlag> flags = registry.getFlags(setCommand);

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

    EnumSet<CommandFlag> flags = registry.getFlags(unknownCommand);

    assertNotNull(flags, "Flags should not be null");
    assertTrue(flags.isEmpty(), "Unknown command should return empty flags");
  }

  /**
   * Test case insensitivity - command names should be normalized to uppercase
   */
  @Test
  public void testCaseInsensitivity() {
    ProtocolCommand lowerCaseCommand = () -> SafeEncoder.encode("function load");

    EnumSet<CommandFlag> flags = registry.getFlags(lowerCaseCommand);

    assertNotNull(flags, "Flags should not be null");
    assertFalse(flags.isEmpty(), "function load (lowercase) should have flags");
    assertEquals(3, flags.size(), "function load should have exactly 3 flags");
    assertTrue(flags.contains(CommandFlag.DENYOOM), "function load should have DENYOOM flag");
    assertTrue(flags.contains(CommandFlag.NOSCRIPT), "function load should have NOSCRIPT flag");
    assertTrue(flags.contains(CommandFlag.WRITE), "function load should have WRITE flag");
  }

  /**
   * Test that unknown subcommands of parent commands fall back to parent command flags.
   * If the parent command also doesn't exist, it should return empty flags.
   */
  @Test
  public void testUnknownSubcommandFallback() {
    // Create a mock ProtocolCommand for "FUNCTION UNKNOWN_SUBCOMMAND"
    // This subcommand doesn't exist, so it should fall back to "FUNCTION" parent
    // Since "FUNCTION" parent also doesn't exist in the registry, it should return empty flags
    ProtocolCommand unknownSubcommand = () -> SafeEncoder.encode("FUNCTION UNKNOWN_SUBCOMMAND");

    EnumSet<CommandFlag> flags = registry.getFlags(unknownSubcommand);

    assertNotNull(flags, "Flags should not be null");
    assertTrue(flags.isEmpty(), "Unknown FUNCTION subcommand should return empty flags when parent doesn't exist");
  }
}

