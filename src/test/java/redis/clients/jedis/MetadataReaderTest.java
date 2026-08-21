package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import redis.clients.jedis.MetadataReader;
import redis.clients.jedis.MetadataResolver.CommandMetadata;

public class MetadataReaderTest {

  /** The repository CommandMetadata.json parses into the normalized metadata shape. */
  @Test
  public void parsesCommandMetadataJsonLayout() {
    Map<String, CommandMetadata> parsed = MetadataReader.read(Paths.get("CommandMetadata.json"));

    CommandMetadata get = parsed.get("GET");
    assertTrue(get.hasFlag("readonly"));
    assertEquals(1, get.getFirstKey());
    assertTrue(get.hasKeyNameSpec());

    // key-name evidence available only through key specs (firstKey == 0)
    CommandMetadata sdiffcard = parsed.get("SDIFFCARD");
    assertEquals(0, sdiffcard.getFirstKey());
    assertTrue(sdiffcard.hasKeyNameSpec());

    assertTrue(parsed.get("XPENDING").hasTip("nondeterministic_output"));
    assertTrue(parsed.size() > 600);
  }

  /** Container subcommand names keep the {@code PARENT|CHILD} pipe syntax as map keys. */
  @Test
  public void pipeSeparatedSubcommandNamesAreParsed() {
    Map<String, CommandMetadata> parsed = MetadataReader.read(Paths.get("CommandMetadata.json"));
    CommandMetadata memoryUsage = parsed.get("MEMORY|USAGE");
    assertNotNull(memoryUsage);
    assertEquals("MEMORY|USAGE", memoryUsage.getName());
    assertTrue(memoryUsage.hasFlag("readonly"));
    assertNotNull(parsed.get("XINFO|STREAM"));
    assertNotNull(parsed.get("OBJECT|ENCODING"));
  }

  @Test
  public void missingMetadataFileFailsFast(@TempDir Path dir) {
    Path missing = dir.resolve("no-such-file.json");
    assertThrows(IllegalStateException.class, () -> MetadataReader.read(missing));
  }

  @Test
  public void malformedMetadataFileFailsFast(@TempDir Path dir) throws IOException {
    Path malformed = dir.resolve("malformed.json");
    Files.write(malformed, "{ not json".getBytes(StandardCharsets.UTF_8));
    assertThrows(IllegalStateException.class, () -> MetadataReader.read(malformed));
  }
}
