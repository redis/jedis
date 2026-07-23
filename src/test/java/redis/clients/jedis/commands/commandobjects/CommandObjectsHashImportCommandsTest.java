package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArgumentCount;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArguments;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Pure unit tests asserting the exact wire arguments and order for the {@code HIMPORT} command
 * factories in {@link CommandObjects}, for both the String and binary surfaces.
 */
public class CommandObjectsHashImportCommandsTest {

  private final CommandObjects commandObjects = new CommandObjects(RedisProtocol.RESP2);

  @Test
  public void prepareString() {
    assertThat(commandObjects.himportPrepare("u", "name", "email", "age").getArguments(),
      hasArguments(Command.HIMPORT, Keyword.PREPARE, RawableFactory.from("u"),
        RawableFactory.from("name"), RawableFactory.from("email"), RawableFactory.from("age")));
  }

  @Test
  public void setString() {
    // HIMPORT SET key fieldset value... — key is at position 2 (after HIMPORT SET)
    assertThat(
      commandObjects.himportSet("user:1", "u", "alice", "alice@example.com", "30").getArguments(),
      hasArguments(Command.HIMPORT, Keyword.SET, RawableFactory.from("user:1"),
        RawableFactory.from("u"), RawableFactory.from("alice"),
        RawableFactory.from("alice@example.com"), RawableFactory.from("30")));
  }

  @Test
  public void discardString() {
    assertThat(commandObjects.himportDiscard("u").getArguments(),
      hasArguments(Command.HIMPORT, Command.DISCARD, RawableFactory.from("u")));
  }

  @Test
  public void discardAll() {
    assertThat(commandObjects.himportDiscardAll().getArguments(),
      hasArguments(Command.HIMPORT, Keyword.DISCARDALL));
  }

  @Test
  public void prepareBinary() {
    byte[] fs = SafeEncoder.encode("u");
    byte[] f1 = SafeEncoder.encode("name");
    byte[] f2 = SafeEncoder.encode("age");
    assertThat(commandObjects.himportPrepare(fs, f1, f2).getArguments(),
      hasArguments(Command.HIMPORT, Keyword.PREPARE, RawableFactory.from(fs),
        RawableFactory.from(f1), RawableFactory.from(f2)));
  }

  @Test
  public void setBinary() {
    byte[] key = SafeEncoder.encode("user:1");
    byte[] fs = SafeEncoder.encode("u");
    byte[] v1 = SafeEncoder.encode("alice");
    byte[] v2 = SafeEncoder.encode("30");
    assertThat(commandObjects.himportSet(key, fs, v1, v2).getArguments(),
      hasArguments(Command.HIMPORT, Keyword.SET, RawableFactory.from(key), RawableFactory.from(fs),
        RawableFactory.from(v1), RawableFactory.from(v2)));
  }

  @Test
  public void discardBinary() {
    byte[] fs = SafeEncoder.encode("u");
    assertThat(commandObjects.himportDiscard(fs).getArguments(),
      hasArguments(Command.HIMPORT, Command.DISCARD, RawableFactory.from(fs)));
  }

  @Test
  public void prepareWithNoFields() {
    // Arity is server-authoritative; the factory emits exactly what it is given.
    assertThat(commandObjects.himportPrepare("u").getArguments(), hasArgumentCount(3));
  }
}
