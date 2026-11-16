package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


public class CommandMetadataResolverFactoryTest {

  private static final String CUSTOM_METADATA_JSON = "{\n" //
      + "  \"commands\": {\n" //
      + "    \"GET\": {\"flags\": [\"write\"], \"tips\": [], \"firstKey\": 1, \"lastKey\": 1,\n" //
      + "      \"step\": 1, \"keySpecs\": [{\"flags\": [\"rw\"]}]},\n" //
      + "    \"TOUCH\": {\"flags\": [\"fast\", \"readonly\"], \"tips\": [], \"firstKey\": 1,\n" //
      + "      \"lastKey\": -1, \"step\": 1, \"keySpecs\": [{\"flags\": [\"ro\"]}]},\n" //
      + "    \"MEMORY\": {\"flags\": [], \"tips\": [], \"firstKey\": 0, \"lastKey\": 0, \"step\": 0,\n" //
      + "      \"subcommands\": {\"MEMORY|USAGE\": {\"flags\": [\"readonly\"], \"tips\": [],\n" //
      + "        \"firstKey\": 1, \"lastKey\": 1, \"step\": 1}}}\n" //
      + "  }\n" //
      + "}\n";

  @TempDir
  Path tempDir;

  private Path customMetadataFile() throws IOException {
    Path file = tempDir.resolve("custom-metadata.json");
    Files.write(file, CUSTOM_METADATA_JSON.getBytes(StandardCharsets.UTF_8));
    return file;
  }

  @Test
  public void fromFileResolvesOnlyTheGivenFile() throws IOException {
    CommandMetadataResolver resolver = CommandMetadataResolverFactory
        .fromFile(customMetadataFile());

    CommandMetadata get = resolver.resolve("GET");
    assertNotNull(get);
    assertTrue(get.hasFlag("write"));
    assertFalse(get.hasFlag("readonly"));

    // a command not in the file is unknown to this resolver, even though the shared metadata has it
    assertNull(resolver.resolve("SET"));
  }

  @Test
  public void fromFileResolvesContainerSubcommands() throws IOException {
    CommandMetadataResolver resolver = CommandMetadataResolverFactory
        .fromFile(customMetadataFile());

    CommandMetadata usage = resolver.resolve("MEMORY|USAGE");
    assertNotNull(usage);
    assertTrue(usage.hasFlag("readonly"));

    CommandMetadata memory = resolver.resolve("MEMORY");
    assertTrue(memory.getSubcommands().contains(usage));
  }

  @Test
  public void fromFileAppliesKnownServerMetadataFixes() throws IOException {
    CommandMetadataResolver resolver = CommandMetadataResolverFactory
        .fromFile(customMetadataFile());

    CommandMetadata touch = resolver.resolve("TOUCH");
    assertTrue(touch.hasTip("dont_cache")); // added by the fix, the file itself carries no tips
    assertTrue(touch.hasFlag("readonly")); // file-provided flags retained
  }

  @Test
  public void fromFileTableIsSealed() throws IOException {
    CommandMetadataResolver resolver = CommandMetadataResolverFactory
        .fromFile(customMetadataFile());
    assertThrows(UnsupportedOperationException.class,
      () -> resolver.resolve("GET").getFlags().add("readonly"));
    assertThrows(UnsupportedOperationException.class,
      () -> resolver.resolve("GET").getTips().add("dont_cache"));
    assertThrows(UnsupportedOperationException.class,
      () -> resolver.resolve("MEMORY").getSubcommands().clear());
  }

  @Test
  public void fromFileFailsFastOnShortSubcommandKeys() throws IOException {
    String shortKeyed = "{\"commands\": {\"MEMORY\": {\"flags\": [], \"tips\": [], \"firstKey\": 0,"
        + " \"lastKey\": 0, \"step\": 0, \"subcommands\": {\"USAGE\": {\"flags\": [\"readonly\"],"
        + " \"tips\": [], \"firstKey\": 1, \"lastKey\": 1, \"step\": 1}}}}}";
    Path file = tempDir.resolve("short-subcommand-keys.json");
    Files.write(file, shortKeyed.getBytes(StandardCharsets.UTF_8));

    IllegalStateException e = assertThrows(IllegalStateException.class,
      () -> CommandMetadataResolverFactory.fromFile(file));
    assertTrue(e.getMessage().contains("PARENT|CHILD"));
  }

  @Test
  public void fromFileDoesNotAffectTheDefaultResolver() throws IOException {
    CommandMetadataResolver custom = CommandMetadataResolverFactory
        .fromFile(customMetadataFile());
    CommandMetadataResolver shared = CommandMetadataResolverFactory.defaultResolver();

    assertTrue(custom.resolve("GET").hasFlag("write"));
    assertTrue(shared.resolve("GET").hasFlag("readonly"));
    assertFalse(shared.resolve("GET").hasFlag("write"));
  }

  @Test
  public void allCommandMetadataEnumeratesCommandsAndSubcommands() throws IOException {
    CommandMetadataResolver resolver = CommandMetadataResolverFactory
        .fromFile(customMetadataFile());

    Set<String> names = resolver.allCommandMetadata().stream().map(CommandMetadata::getName)
        .collect(Collectors.toSet());
    assertEquals(4, names.size());
    assertTrue(names.contains("GET"));
    assertTrue(names.contains("TOUCH"));
    assertTrue(names.contains("MEMORY"));
    assertTrue(names.contains("MEMORY|USAGE"));
  }

  @Test
  public void fromFileLoadsTheRepositoryMetadataFile() {
    CommandMetadataResolver resolver = CommandMetadataResolverFactory
        .fromFile(Paths.get("CommandMetadata.json"));
    CommandMetadata get = resolver.resolve("GET");
    assertEquals(new MetadataResolver().resolve("GET").getFlags(), get.getFlags());
    assertFalse(resolver.allCommandMetadata().isEmpty());
  }

  @Test
  public void fromFileFailsFastOnMissingFile() {
    assertThrows(IllegalStateException.class,
      () -> CommandMetadataResolverFactory.fromFile(tempDir.resolve("no-such-file.json")));
  }

  @Test
  public void fromFileFailsFastOnMalformedFile() throws IOException {
    Path malformed = tempDir.resolve("malformed.json");
    Files.write(malformed, "{ not json".getBytes(StandardCharsets.UTF_8));
    assertThrows(IllegalStateException.class,
      () -> CommandMetadataResolverFactory.fromFile(malformed));
  }

  @Test
  public void fromFileRejectsNullPath() {
    assertThrows(IllegalArgumentException.class, () -> CommandMetadataResolverFactory.fromFile(null));
  }

  @Test
  public void defaultResolverExposesSharedMetadata() {
    CommandMetadataResolver resolver = CommandMetadataResolverFactory.defaultResolver();
    assertEquals(new MetadataResolver().resolve("GET").getFlags(),
      resolver.resolve("GET").getFlags());
    assertFalse(resolver.allCommandMetadata().isEmpty());
  }

  @Test
  public void ofResolvesTheGivenRecordsOnly() {
    CommandMetadata get = CommandMetadata.builder("get").flags("ReadOnly", "fast")
        .tips("Dont_Cache").keyPositions(1, 1, 1).keyNameSpec(true).build();
    CommandMetadataResolver resolver = CommandMetadataResolverFactory.of(get);

    CommandMetadata resolved = resolver.resolve("GET");
    assertNotNull(resolved);
    assertEquals("GET", resolved.getName()); // name upper-cased, flags and tips lower-cased
    assertTrue(resolved.hasFlag("readonly"));
    assertTrue(resolved.hasTip("dont_cache"));
    assertEquals(1, resolved.getFirstKey());
    assertTrue(resolved.hasKeyNameSpec());
    assertNull(resolver.resolve("SET"));
    assertEquals(1, resolver.allCommandMetadata().size());
  }

  @Test
  public void ofIndexesSubcommandsOfAContainerRecord() {
    CommandMetadata usage = CommandMetadata.builder("MEMORY|USAGE").flags("readonly")
        .keyPositions(1, 1, 1).build();
    CommandMetadata memory = CommandMetadata.builder("MEMORY").subcommand(usage).build();
    CommandMetadataResolver resolver = CommandMetadataResolverFactory.of(memory);

    assertNotNull(resolver.resolve("MEMORY|USAGE"));
    assertEquals(1, resolver.resolve("MEMORY").getSubcommands().size());
    assertEquals(2, resolver.allCommandMetadata().size());
  }

  @Test
  public void ofRejectsDuplicateAndDetachedSubcommandRecords() {
    CommandMetadata get = CommandMetadata.builder("GET").build();
    CommandMetadata usage = CommandMetadata.builder("MEMORY|USAGE").build();

    assertThrows(IllegalArgumentException.class, () -> CommandMetadataResolverFactory.of(get, get));
    assertThrows(IllegalArgumentException.class, () -> CommandMetadataResolverFactory.of(usage));
    assertThrows(IllegalArgumentException.class,
      () -> CommandMetadataResolverFactory.of((CommandMetadata) null));
  }

  @Test
  public void builtRecordsAreImmutableAndCopiesAreIndependent() {
    CommandMetadata get = CommandMetadataResolverFactory.defaultResolver().resolve("GET");
    CommandMetadata denied = get.toBuilder().tips("dont_cache").removeFlags("fast").build();

    assertTrue(denied.hasTip("dont_cache"));
    assertFalse(denied.hasFlag("fast"));
    assertTrue(denied.hasFlag("readonly"));
    assertEquals(get.getFirstKey(), denied.getFirstKey());
    // the source record is untouched
    assertFalse(get.hasTip("dont_cache"));
    assertTrue(get.hasFlag("fast"));
    assertThrows(UnsupportedOperationException.class, () -> denied.getTips().add("x"));
    assertThrows(UnsupportedOperationException.class, () -> denied.getFlags().add("x"));
  }

  @Test
  public void subcommandMustBeNamedUnderItsContainer() {
    CommandMetadata foreign = CommandMetadata.builder("XINFO|STREAM").build();
    assertThrows(IllegalArgumentException.class,
      () -> CommandMetadata.builder("MEMORY").subcommand(foreign));
    assertThrows(IllegalArgumentException.class, () -> CommandMetadata.builder(" "));
  }
}
