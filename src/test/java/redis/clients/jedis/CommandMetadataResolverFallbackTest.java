package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class CommandMetadataResolverFallbackTest {

  private static CommandMetadata metadata(String name, String flag) {
    return new CommandMetadata(name, new HashSet<>(Collections.singletonList(flag)),
        Collections.emptySet(), 1, 1, 1, true);
  }

  private static CommandMetadataResolver resolver(CommandMetadata... entries) {
    Map<String, CommandMetadata> table = new LinkedHashMap<>();
    for (CommandMetadata entry : entries) {
      table.put(entry.getName(), entry);
    }
    return new CommandMetadataResolver() {
      @Override
      public CommandMetadata resolve(String commandName) {
        return table.get(commandName);
      }

      @Override
      public Collection<CommandMetadata> allCommandMetadata() {
        return table.values();
      }
    };
  }

  private static Set<String> names(Collection<CommandMetadata> all) {
    Set<String> names = new HashSet<>();
    for (CommandMetadata metadata : all) {
      names.add(metadata.getName());
    }
    return names;
  }

  @Test
  public void primaryWinsOverTheFallback() {
    CommandMetadata primaryGet = metadata("GET", "write");
    CommandMetadataResolver chained = resolver(primaryGet)
        .withFallback(resolver(metadata("GET", "readonly")));

    assertSame(primaryGet, chained.resolve("GET"));
  }

  @Test
  public void unknownCommandsFallThrough() {
    CommandMetadata fallbackHget = metadata("HGET", "readonly");
    CommandMetadataResolver chained = resolver(metadata("GET", "readonly"))
        .withFallback(resolver(fallbackHget));

    assertSame(fallbackHget, chained.resolve("HGET"));
    assertNull(chained.resolve("NO.SUCH.COMMAND"));
  }

  @Test
  public void allCommandMetadataIsTheUnionWithThePrimaryTakingPrecedence() {
    CommandMetadata primaryGet = metadata("GET", "write");
    CommandMetadataResolver chained = resolver(primaryGet)
        .withFallback(resolver(metadata("GET", "readonly"), metadata("HGET", "readonly")));

    Collection<CommandMetadata> all = chained.allCommandMetadata();
    assertEquals(new HashSet<>(Arrays.asList("GET", "HGET")), names(all));
    assertEquals(2, all.size());
    assertSame(primaryGet,
      all.stream().filter(m -> m.getName().equals("GET")).findFirst().get());
  }

  @Test
  public void containerKnownToThePrimaryOwnsItsSubcommands() {
    CommandMetadataResolver chained = resolver(metadata("XINFO", "readonly")).withFallback(
      resolver(metadata("XINFO", "readonly"), metadata("XINFO|STREAM", "readonly"),
        metadata("MEMORY", "readonly"), metadata("MEMORY|USAGE", "readonly")));

    assertNull(chained.resolve("XINFO|STREAM"));
    assertEquals("MEMORY|USAGE", chained.resolve("MEMORY|USAGE").getName());
    assertEquals(new HashSet<>(Arrays.asList("XINFO", "MEMORY", "MEMORY|USAGE")),
      names(chained.allCommandMetadata()));
  }

  @Test
  public void fallbacksChain() {
    CommandMetadataResolver chained = resolver(metadata("GET", "readonly"))
        .withFallback(resolver(metadata("HGET", "readonly")))
        .withFallback(resolver(metadata("LLEN", "readonly")));

    assertEquals("GET", chained.resolve("GET").getName());
    assertEquals("HGET", chained.resolve("HGET").getName());
    assertEquals("LLEN", chained.resolve("LLEN").getName());
  }

  @Test
  public void nullFallbackIsRejected() {
    assertThrows(IllegalArgumentException.class,
      () -> resolver(metadata("GET", "readonly")).withFallback(null));
  }
}
