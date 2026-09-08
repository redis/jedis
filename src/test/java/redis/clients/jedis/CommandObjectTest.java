package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

public class CommandObjectTest {

  private CommandObject<String> command() {
    return new CommandObject<>(new CommandArguments(Protocol.Command.GET).key("k"),
        BuilderFactory.STRING);
  }

  @Test
  public void hooksAreEmptyByDefault() {
    assertTrue(command().getPreProcessHooks().isEmpty());
  }

  @Test
  public void withPreProcessHookAppendsInOrderWithoutTouchingSource() {
    CommandObject<String> source = command();
    List<String> ran = new ArrayList<>();
    Consumer<Connection> first = conn -> ran.add("first");
    Consumer<Connection> second = conn -> ran.add("second");

    CommandObject<String> modified = source.withPreProcessHook(first).withPreProcessHook(second);

    assertTrue(source.getPreProcessHooks().isEmpty());
    assertEquals(2, modified.getPreProcessHooks().size());
    modified.getPreProcessHooks().forEach(hook -> hook.accept(null));
    assertEquals("first", ran.get(0));
    assertEquals("second", ran.get(1));
  }

  @Test
  public void withPreProcessHookOnCopyKeepsExistingHooks() {
    List<String> ran = new ArrayList<>();
    CommandObject<String> once = command().withPreProcessHook(conn -> ran.add("first"));
    CommandObject<String> twice = once.withPreProcessHook(conn -> ran.add("second"));

    assertEquals(1, once.getPreProcessHooks().size());
    assertEquals(2, twice.getPreProcessHooks().size());
    twice.getPreProcessHooks().forEach(hook -> hook.accept(null));
    assertEquals("first", ran.get(0));
    assertEquals("second", ran.get(1));
  }

  @Test
  public void modifiedCopyIsSameCommand() {
    CommandObject<String> source = command();
    CommandObject<String> modified = source.withPreProcessHook(conn -> {
    });
    assertEquals(source, modified);
    assertEquals(source.hashCode(), modified.hashCode());
  }

  @Test
  public void hookListIsUnmodifiable() {
    CommandObject<String> modified = command().withPreProcessHook(conn -> {
    });
    assertThrows(UnsupportedOperationException.class,
      () -> modified.getPreProcessHooks().add(conn -> {
      }));
  }
}