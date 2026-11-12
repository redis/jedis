package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.MetadataResolver.CommandMetadata;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.bloom.RedisBloomProtocol.BloomFilterCommand;
import redis.clients.jedis.bloom.RedisBloomProtocol.CountMinSketchCommand;
import redis.clients.jedis.bloom.RedisBloomProtocol.CuckooFilterCommand;
import redis.clients.jedis.bloom.RedisBloomProtocol.TDigestCommand;
import redis.clients.jedis.bloom.RedisBloomProtocol.TopKCommand;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.json.JsonProtocol.JsonCommand;
import redis.clients.jedis.search.SearchProtocol.SearchCommand;
import redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesCommand;
import redis.clients.jedis.util.SafeEncoder;

public class MetadataResolverTest {

  @Test
  public void resolvesGeneratedMetadata() {
    MetadataResolver resolver = new MetadataResolver();
    CommandMetadata get = resolver.resolve("GET");
    assertNotNull(get);
    assertTrue(get.hasFlag("readonly"));
    assertFalse(get.hasFlag("write"));
    assertEquals(1, get.getFirstKey());
  }

  @Test
  public void resolvesContainerSubcommandsByFullName() {
    MetadataResolver resolver = new MetadataResolver();
    CommandMetadata usage = resolver.resolve("MEMORY|USAGE");
    assertNotNull(usage);
    assertTrue(usage.hasFlag("readonly"));
  }

  /** Fixes only add tips; the server-provided flags, tips, and key metadata are respected. */
  @Test
  public void knownServerMetadataFixesAppliedAsLaterStep() {
    MetadataResolver resolver = new MetadataResolver();

    CommandMetadata touch = resolver.resolve("TOUCH");
    assertTrue(touch.hasTip("dont_cache")); // added by the fix
    assertTrue(touch.hasFlag("readonly")); // server metadata retained
    assertTrue(touch.hasFlag("fast"));
    assertTrue(touch.hasTip("request_policy:multi_shard"));

    CommandMetadata vrandmember = resolver.resolve("VRANDMEMBER");
    assertTrue(vrandmember.hasTip("nondeterministic_output"));
    assertTrue(vrandmember.hasFlag("readonly"));
    assertTrue(vrandmember.hasFlag("module"));
  }

  /** A subcommand declared on CommandArguments resolves its PARENT|CHILD metadata. */
  @Test
  public void resolvesContainerSubcommandsFromDeclaredSubcommand() {
    MetadataResolver resolver = new MetadataResolver();

    CommandArguments xinfoStream = new CommandArguments(Command.XINFO,
        redis.clients.jedis.Protocol.Keyword.STREAM).key("k");
    CommandMetadata streamInfo = resolver
        .resolve(SafeEncoder.encode(xinfoStream.getCommand().getRaw()) + "|"
            + SafeEncoder.encode(xinfoStream.getSubcommand().getRaw()));
    assertEquals("XINFO|STREAM", streamInfo.getName());

    // without a declared subcommand, the command resolves by its own name
    CommandArguments get = new CommandArguments(Command.GET).key("some-key");
    assertNull(get.getSubcommand());
    assertEquals("GET", resolver.resolve(SafeEncoder.encode(get.getCommand().getRaw())).getName());
  }

  @Test
  public void unknownCommandResolvesToNull() {
    assertNull(new MetadataResolver().resolve("NO.SUCH.COMMAND"));
  }

  @Test
  public void metadataCarriesSetsAndSubcommands() {
    CommandMetadata m = new CommandMetadata("X", new HashMap<String, String>().keySet(), // empty
                                                                                         // set
        java.util.Collections.singleton("dont_cache"), 1, -1, 2, false);
    assertFalse(m.hasFlag("readonly"));
    assertTrue(m.hasTip("dont_cache"));
    assertEquals(-1, m.getLastKey());
    assertEquals(2, m.getStep());
    assertFalse(m.hasKeyNameSpec());

    CommandMetadata sub = new CommandMetadata("X|Y", java.util.Collections.singleton("readonly"),
        java.util.Collections.emptySet(), 2, 2, 1, true);
    m.addSubcommand(sub);
    assertTrue(m.getSubcommands().contains(sub));
  }

  /**
   * Every command Jedis can issue has metadata in the resolver, except the documented gaps. A
   * failure here means a protocol enum gained a command without regenerated metadata.
   */
  @Test
  public void allJedisCommandsAreCoveredByMetadata() {
    // commands the metadata source does not describe; keep in sync with regeneration warnings
    List<String> knownGaps = Collections.singletonList("SENTINEL");

    List<ProtocolCommand[]> protocolEnums = Arrays.asList(Command.values(), JsonCommand.values(),
      SearchCommand.values(), TimeSeriesCommand.values(), BloomFilterCommand.values(),
      CuckooFilterCommand.values(), CountMinSketchCommand.values(), TopKCommand.values(),
      TDigestCommand.values());
    MetadataResolver resolver = new MetadataResolver();
    List<String> missing = new ArrayList<>();
    for (ProtocolCommand[] values : protocolEnums) {
      for (ProtocolCommand command : values) {
        String name = SafeEncoder.encode(command.getRaw());
        if (resolver.resolve(name) == null && !knownGaps.contains(name)) {
          missing.add(name);
        }
      }
    }
    assertEquals(Collections.emptyList(), missing);
    // keep the gap list honest: remove SENTINEL from it once metadata covers it
    assertNull(resolver.resolve("SENTINEL"));
  }

  /**
   * Resolver equivalence: the generated table and a file-backed table parsed from
   * {@code CommandMetadata.json} must carry the same effective metadata for every command, which
   * guarantees the same cacheability decision for both sources.
   */
  @Test
  public void generatedAndFileBackedMetadataAreEquivalent() {
    Map<String, MetadataResolver.CommandMetadata> generated = MetadataResolver
        .generatedCommandTable();
    Map<String, MetadataResolver.CommandMetadata> parsed = MetadataReader
        .read(java.nio.file.Paths.get("CommandMetadata.json"));

    assertEquals(generated.keySet(), parsed.keySet());
    for (String name : generated.keySet()) {
      MetadataResolver.CommandMetadata g = generated.get(name);
      MetadataResolver.CommandMetadata f = parsed.get(name);
      assertEquals(g.getFlags(), f.getFlags(), "flags differ for " + name);
      assertEquals(g.getTips(), f.getTips(), "tips differ for " + name);
      assertEquals(g.getFirstKey(), f.getFirstKey(), "firstKey differs for " + name);
      assertEquals(g.getLastKey(), f.getLastKey(), "lastKey differs for " + name);
      assertEquals(g.getStep(), f.getStep(), "step differs for " + name);
      assertEquals(g.hasKeyNameSpec(), f.hasKeyNameSpec(), "keySpec evidence differs for " + name);
    }
  }

}
