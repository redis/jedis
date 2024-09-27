package redis.clients.jedis.modules.gears;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.gears.TFunctionListParams;
import redis.clients.jedis.gears.TFunctionLoadParams;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;
import redis.clients.jedis.gears.resps.GearsLibraryInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Ignore
@RunWith(Parameterized.class)
public class GearsTest extends RedisModuleCommandsTestBase {

  private static final String BAD_FUNCTION = "All Your Base Are Belong to Us";

  private static final String[] LIBRARIES_ARRAY = new String[]{
      "streamTriggers", "withFlags", "pingpong", "keyspaceTriggers", "hashitout", "withConfig"};

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }

  public GearsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @After
  @Override
  public void tearDown() throws Exception {
    deleteFunctions(); // delete functions before closing connections
    super.tearDown();
  }

  protected void deleteFunctions() {
    List<GearsLibraryInfo> libraries = client.tFunctionList();
    libraries.stream().map(GearsLibraryInfo::getName).forEach(library -> client.tFunctionDelete(library));
  }

  @Test
  public void testFunctionLoad() throws IOException {
    client.tFunctionLoad(readLibrary("pingpong.js"));

    List<GearsLibraryInfo> libraries = client.tFunctionList();
    assertEquals(Collections.singletonList("pingpong"),
        libraries.stream().map(GearsLibraryInfo::getName).collect(Collectors.toList()));
  }

  @Test(expected = JedisDataException.class)
  public void testFunctionLoadAlreadyLoadedFails() throws IOException {
    client.tFunctionLoad(readLibrary("pingpong.js"));
    client.tFunctionLoad(readLibrary("pingpong.js"));
  }

  @Test
  public void testFunctionLoadWithReplace() throws IOException {
    client.tFunctionLoad(readLibrary("pingpong.js"));
    client.tFunctionLoad(readLibrary("pingpong.js"), TFunctionLoadParams.loadParams().replace());

    List<GearsLibraryInfo> libraries = client.tFunctionList();
    assertEquals(Collections.singletonList("pingpong"),
        libraries.stream().map(GearsLibraryInfo::getName).collect(Collectors.toList()));
  }

  @Test(expected = JedisDataException.class)
  public void testBadFunctionLoad() {
    client.tFunctionLoad(BAD_FUNCTION);
  }

  private static void assertAllLibrariesNoCode(List<GearsLibraryInfo> libraryInfos) {
    assertThat(libraryInfos, Matchers.hasSize(LIBRARIES_ARRAY.length));

    List<String> libraryNames = libraryInfos.stream().map(GearsLibraryInfo::getName).collect(Collectors.toList());
    assertThat(libraryNames, Matchers.containsInAnyOrder(LIBRARIES_ARRAY));

    libraryInfos.stream().map(GearsLibraryInfo::getCode).forEach(Assert::assertNull);
  }

  private static void assertAllLibrariesWithCode(List<GearsLibraryInfo> libraryInfos) {
    assertThat(libraryInfos, Matchers.hasSize(LIBRARIES_ARRAY.length));

    List<String> libraryNames = libraryInfos.stream().map(GearsLibraryInfo::getName).collect(Collectors.toList());
    assertThat(libraryNames, Matchers.containsInAnyOrder(LIBRARIES_ARRAY));

    libraryInfos.stream().map(GearsLibraryInfo::getCode).forEach(Assert::assertNotNull);
  }

  @Test
  public void tFunctionListAll() throws IOException {
    loadAllLibraries();

    List<GearsLibraryInfo> libraryInfos = client.tFunctionList();
    assertAllLibrariesNoCode(libraryInfos);
  }

  @Test
  public void testFunctionListNoCodeVerboseZero() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().verbose(0));
    assertAllLibrariesNoCode(libraryInfos);

    Map<String, List<Predicate<GearsLibraryInfo>>> libraryConditions = initializeTestLibraryConditions();

    libraryConditions.get("streamTriggers").add(lib -> lib.getStreamTriggers().stream().anyMatch(trigger -> "consumer".equalsIgnoreCase(trigger.getName())));
    libraryConditions.get("withFlags").add(lib -> lib.getFunctions().stream().anyMatch(func -> "my_set".equalsIgnoreCase(func.getName())));
    libraryConditions.get("pingpong").add(lib -> lib.getFunctions().stream().anyMatch(func -> "playPingPong".equalsIgnoreCase(func.getName())));
    libraryConditions.get("keyspaceTriggers").add(lib -> lib.getKeyspaceTriggers().stream().anyMatch(trigger -> "consumer".equalsIgnoreCase(trigger.getName())));
    libraryConditions.get("hashitout").add(lib -> lib.getFunctions().stream().anyMatch(func -> "hashy".equalsIgnoreCase(func.getName())));
    libraryConditions.get("withConfig").add(lib -> lib.getFunctions().stream().anyMatch(func -> "hset".equalsIgnoreCase(func.getName())));

    for (GearsLibraryInfo libraryInfo : libraryInfos) {
      List<Predicate<GearsLibraryInfo>> conditions = libraryConditions.get(libraryInfo.getName());
      if (conditions != null && !conditions.isEmpty()) {
        conditions.forEach(c -> c.test(libraryInfo));
      }
    }
  }

  @Test
  public void testFunctionListNoCodeVerboseOne() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().verbose(1));
    assertAllLibrariesNoCode(libraryInfos);

    Map<String, List<Predicate<GearsLibraryInfo>>> libraryConditions = initializeTestLibraryConditions();

    libraryConditions.get("streamTriggers").add(lib -> lib.getStreamTriggers().stream().anyMatch(trigger -> "consumer".equalsIgnoreCase(trigger.getName())));
    libraryConditions.get("streamTriggers").add(lib -> lib.getStreamTriggers().stream().anyMatch(trigger -> "stream".equalsIgnoreCase(trigger.getPrefix())));
    libraryConditions.get("withFlags").add(lib -> lib.getFunctions().stream().anyMatch(func -> "my_set".equalsIgnoreCase(func.getName())));
    libraryConditions.get("withFlags").add(lib -> lib.getFunctions().stream().anyMatch(func -> func.getFlags().contains("raw-arguments")));
    libraryConditions.get("pingpong").add(lib -> lib.getFunctions().stream().anyMatch(func -> "playPingPong".equalsIgnoreCase(func.getName())));
    libraryConditions.get("keyspaceTriggers").add(lib -> lib.getKeyspaceTriggers().stream().anyMatch(trigger -> "consumer".equalsIgnoreCase(trigger.getName())));
    libraryConditions.get("hashitout").add(lib -> lib.getFunctions().stream().anyMatch(func -> "hashy".equalsIgnoreCase(func.getName())));
    libraryConditions.get("withConfig").add(lib -> lib.getFunctions().stream().anyMatch(func -> "hset".equalsIgnoreCase(func.getName())));

    for (GearsLibraryInfo libraryInfo : libraryInfos) {
      List<Predicate<GearsLibraryInfo>> conditions = libraryConditions.get(libraryInfo.getName());
      if (conditions != null && !conditions.isEmpty()) {
        conditions.forEach(c -> c.test(libraryInfo));
      }
    }
  }

  @Test
  public void testFunctionListNoCodeVerboseTwo() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().verbose(2));
    assertAllLibrariesNoCode(libraryInfos);

    Map<String, List<Predicate<GearsLibraryInfo>>> libraryConditions = initializeTestLibraryConditions();

    libraryConditions.get("streamTriggers").add(lib -> lib.getStreamTriggers().stream().anyMatch(trigger -> "consumer".equalsIgnoreCase(trigger.getName())));
    libraryConditions.get("streamTriggers").add(lib -> lib.getStreamTriggers().stream().anyMatch(trigger -> "stream".equalsIgnoreCase(trigger.getPrefix())));
    libraryConditions.get("withFlags").add(lib -> lib.getFunctions().stream().anyMatch(func -> "my_set".equalsIgnoreCase(func.getName())));
    libraryConditions.get("withFlags").add(lib -> lib.getFunctions().stream().anyMatch(func -> func.getFlags().contains("raw-arguments")));
    libraryConditions.get("pingpong").add(lib -> lib.getFunctions().stream().anyMatch(func -> "playPingPong".equalsIgnoreCase(func.getName())));
    libraryConditions.get("keyspaceTriggers").add(lib -> lib.getKeyspaceTriggers().stream().anyMatch(trigger -> "consumer".equalsIgnoreCase(trigger.getName())));
    libraryConditions.get("hashitout").add(lib -> lib.getFunctions().stream().anyMatch(func -> "hashy".equalsIgnoreCase(func.getName())));
    libraryConditions.get("withConfig").add(lib -> lib.getFunctions().stream().anyMatch(func -> "hset".equalsIgnoreCase(func.getName())));

    for (GearsLibraryInfo libraryInfo : libraryInfos) {
      List<Predicate<GearsLibraryInfo>> conditions = libraryConditions.get(libraryInfo.getName());
      if (conditions != null && !conditions.isEmpty()) {
        conditions.forEach(c -> c.test(libraryInfo));
      }
    }
  }

  @Test
  public void testFunctionListNoCodeVerboseThree() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().verbose(3));
    assertAllLibrariesNoCode(libraryInfos);

    Map<String, List<Predicate<GearsLibraryInfo>>> libraryConditions = initializeTestLibraryConditions();

    libraryConditions.get("streamTriggers").add(lib -> lib.getStreamTriggers().stream().anyMatch(trigger -> "consumer".equalsIgnoreCase(trigger.getName())));
    libraryConditions.get("streamTriggers").add(lib -> lib.getStreamTriggers().stream().anyMatch(trigger -> "stream".equalsIgnoreCase(trigger.getPrefix())));
    libraryConditions.get("withFlags").add(lib -> lib.getFunctions().stream().anyMatch(func -> "my_set".equalsIgnoreCase(func.getName())));
    libraryConditions.get("withFlags").add(lib -> lib.getFunctions().stream().anyMatch(func -> func.getFlags().contains("raw-arguments")));
    libraryConditions.get("pingpong").add(lib -> lib.getFunctions().stream().anyMatch(func -> "playPingPong".equalsIgnoreCase(func.getName())));
    libraryConditions.get("keyspaceTriggers").add(lib -> lib.getKeyspaceTriggers().stream().anyMatch(trigger -> "consumer".equalsIgnoreCase(trigger.getName())));
    libraryConditions.get("hashitout").add(lib -> lib.getFunctions().stream().anyMatch(func -> "hashy".equalsIgnoreCase(func.getName())));
    libraryConditions.get("withConfig").add(lib -> lib.getFunctions().stream().anyMatch(func -> "hset".equalsIgnoreCase(func.getName())));

    for (GearsLibraryInfo libraryInfo : libraryInfos) {
      List<Predicate<GearsLibraryInfo>> conditions = libraryConditions.get(libraryInfo.getName());
      if (conditions != null && !conditions.isEmpty()) {
        conditions.forEach(c -> c.test(libraryInfo));
      }
    }
  }

  @Test
  public void testFunctionListWithCodeVerboseZero() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().withCode().verbose(0));
    assertAllLibrariesWithCode(libraryInfos);

    Map<String, List<Predicate<GearsLibraryInfo>>> libraryConditions = initializeTestLibraryConditions();

    libraryConditions.get("streamTriggers").add(lib -> lib.getStreamTriggers().stream().anyMatch(trigger -> "consumer".equalsIgnoreCase(trigger.getName())));
    libraryConditions.get("withFlags").add(lib -> lib.getFunctions().stream().anyMatch(func -> "my_set".equalsIgnoreCase(func.getName())));
    libraryConditions.get("pingpong").add(lib -> lib.getFunctions().stream().anyMatch(func -> "playPingPong".equalsIgnoreCase(func.getName())));
    libraryConditions.get("keyspaceTriggers").add(lib -> lib.getKeyspaceTriggers().stream().anyMatch(trigger -> "consumer".equalsIgnoreCase(trigger.getName())));
    libraryConditions.get("hashitout").add(lib -> lib.getFunctions().stream().anyMatch(func -> "hashy".equalsIgnoreCase(func.getName())));
    libraryConditions.get("withConfig").add(lib -> lib.getFunctions().stream().anyMatch(func -> "hset".equalsIgnoreCase(func.getName())));

    for (GearsLibraryInfo libraryInfo : libraryInfos) {
      List<Predicate<GearsLibraryInfo>> conditions = libraryConditions.get(libraryInfo.getName());
      if (conditions != null && !conditions.isEmpty()) {
        conditions.forEach(c -> c.test(libraryInfo));
      }
    }
  }

  @Test
  public void testFunctionListWithCodeVerboseOne() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().withCode().verbose(1));
    assertAllLibrariesWithCode(libraryInfos);

    Map<String, List<Predicate<GearsLibraryInfo>>> libraryConditions = initializeTestLibraryConditions();

    libraryConditions.get("streamTriggers").add(lib -> lib.getStreamTriggers().stream().anyMatch(trigger -> "consumer".equalsIgnoreCase(trigger.getName())));
    libraryConditions.get("streamTriggers").add(lib -> lib.getStreamTriggers().stream().anyMatch(trigger -> "stream".equalsIgnoreCase(trigger.getPrefix())));
    libraryConditions.get("withFlags").add(lib -> lib.getFunctions().stream().anyMatch(func -> "my_set".equalsIgnoreCase(func.getName())));
    libraryConditions.get("withFlags").add(lib -> lib.getFunctions().stream().anyMatch(func -> func.getFlags().contains("raw-arguments")));
    libraryConditions.get("pingpong").add(lib -> lib.getFunctions().stream().anyMatch(func -> "playPingPong".equalsIgnoreCase(func.getName())));
    libraryConditions.get("keyspaceTriggers").add(lib -> lib.getKeyspaceTriggers().stream().anyMatch(trigger -> "consumer".equalsIgnoreCase(trigger.getName())));
    libraryConditions.get("hashitout").add(lib -> lib.getFunctions().stream().anyMatch(func -> "hashy".equalsIgnoreCase(func.getName())));
    libraryConditions.get("withConfig").add(lib -> lib.getFunctions().stream().anyMatch(func -> "hset".equalsIgnoreCase(func.getName())));

    for (GearsLibraryInfo libraryInfo : libraryInfos) {
      List<Predicate<GearsLibraryInfo>> conditions = libraryConditions.get(libraryInfo.getName());
      if (conditions != null && !conditions.isEmpty()) {
        conditions.forEach(c -> c.test(libraryInfo));
      }
    }
  }

  @Test
  public void testFunctionListWithCodeVerboseTwo() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().withCode().verbose(2));
    assertAllLibrariesWithCode(libraryInfos);

    Map<String, List<Predicate<GearsLibraryInfo>>> libraryConditions = initializeTestLibraryConditions();

    libraryConditions.get("streamTriggers").add(lib -> lib.getStreamTriggers().stream().anyMatch(trigger -> "consumer".equalsIgnoreCase(trigger.getName())));
    libraryConditions.get("streamTriggers").add(lib -> lib.getStreamTriggers().stream().anyMatch(trigger -> "stream".equalsIgnoreCase(trigger.getPrefix())));
    libraryConditions.get("withFlags").add(lib -> lib.getFunctions().stream().anyMatch(func -> "my_set".equalsIgnoreCase(func.getName())));
    libraryConditions.get("withFlags").add(lib -> lib.getFunctions().stream().anyMatch(func -> func.getFlags().contains("raw-arguments")));
    libraryConditions.get("pingpong").add(lib -> lib.getFunctions().stream().anyMatch(func -> "playPingPong".equalsIgnoreCase(func.getName())));
    libraryConditions.get("keyspaceTriggers").add(lib -> lib.getKeyspaceTriggers().stream().anyMatch(trigger -> "consumer".equalsIgnoreCase(trigger.getName())));
    libraryConditions.get("hashitout").add(lib -> lib.getFunctions().stream().anyMatch(func -> "hashy".equalsIgnoreCase(func.getName())));
    libraryConditions.get("withConfig").add(lib -> lib.getFunctions().stream().anyMatch(func -> "hset".equalsIgnoreCase(func.getName())));

    for (GearsLibraryInfo libraryInfo : libraryInfos) {
      List<Predicate<GearsLibraryInfo>> conditions = libraryConditions.get(libraryInfo.getName());
      if (conditions != null && !conditions.isEmpty()) {
        conditions.forEach(c -> c.test(libraryInfo));
      }
    }
  }

  @Test
  public void testFunctionListWithCodeVerboseThree() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().withCode().verbose(3));
    assertAllLibrariesWithCode(libraryInfos);

    Map<String, List<Predicate<GearsLibraryInfo>>> libraryConditions = initializeTestLibraryConditions();

    libraryConditions.get("streamTriggers").add(lib -> lib.getStreamTriggers().stream().anyMatch(trigger -> "consumer".equalsIgnoreCase(trigger.getName())));
    libraryConditions.get("streamTriggers").add(lib -> lib.getStreamTriggers().stream().anyMatch(trigger -> "stream".equalsIgnoreCase(trigger.getPrefix())));
    libraryConditions.get("withFlags").add(lib -> lib.getFunctions().stream().anyMatch(func -> "my_set".equalsIgnoreCase(func.getName())));
    libraryConditions.get("withFlags").add(lib -> lib.getFunctions().stream().anyMatch(func -> func.getFlags().contains("raw-arguments")));
    libraryConditions.get("pingpong").add(lib -> lib.getFunctions().stream().anyMatch(func -> "playPingPong".equalsIgnoreCase(func.getName())));
    libraryConditions.get("keyspaceTriggers").add(lib -> lib.getKeyspaceTriggers().stream().anyMatch(trigger -> "consumer".equalsIgnoreCase(trigger.getName())));
    libraryConditions.get("hashitout").add(lib -> lib.getFunctions().stream().anyMatch(func -> "hashy".equalsIgnoreCase(func.getName())));
    libraryConditions.get("withConfig").add(lib -> lib.getFunctions().stream().anyMatch(func -> "hset".equalsIgnoreCase(func.getName())));

    for (GearsLibraryInfo libraryInfo : libraryInfos) {
      List<Predicate<GearsLibraryInfo>> conditions = libraryConditions.get(libraryInfo.getName());
      if (conditions != null && !conditions.isEmpty()) {
        conditions.forEach(c -> c.test(libraryInfo));
      }
    }
  }

  @Test
  public void testFunctionLibraryListNoCodeVerboseZero() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().library("pingpong"));
    assertEquals(1, libraryInfos.size());
    assertEquals("pingpong", libraryInfos.get(0).getName());
    assertNull(libraryInfos.get(0).getFunctions().get(0).getDescription());
    assertNull(libraryInfos.get(0).getCode());
  }

  @Test
  public void testFunctionLibraryListNoCodeVerboseOne() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().library("pingpong").verbose(1));

    assertEquals(1, libraryInfos.size());
    assertEquals("pingpong", libraryInfos.get(0).getName());
    assertEquals("You PING, we PONG", libraryInfos.get(0).getFunctions().get(0).getDescription());
    assertNull(libraryInfos.get(0).getCode());
  }

  @Test
  public void testFunctionLibraryListNoCodeVerboseTwo() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().library("pingpong").verbose(2));
    assertEquals(1, libraryInfos.size());
    assertEquals("pingpong", libraryInfos.get(0).getName());
    assertEquals("You PING, we PONG", libraryInfos.get(0).getFunctions().get(0).getDescription());
    assertNull(libraryInfos.get(0).getCode());
  }

  @Test
  public void testFunctionLibraryListNoCodeVerboseThree() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().library("pingpong").verbose(3));
    assertEquals(1, libraryInfos.size());
    assertEquals("pingpong", libraryInfos.get(0).getName());
    assertEquals("You PING, we PONG", libraryInfos.get(0).getFunctions().get(0).getDescription());
    assertNull(libraryInfos.get(0).getCode());
  }

  @Test
  public void testFunctionLibraryListWithCodeVerboseZero() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().library("pingpong").withCode());
    assertEquals(1, libraryInfos.size());
    assertEquals("pingpong", libraryInfos.get(0).getName());
    assertNull(libraryInfos.get(0).getFunctions().get(0).getDescription());
    assertNotNull(libraryInfos.get(0).getCode());
  }

  @Test
  public void testFunctionLibraryListWithCodeVerboseOne() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().library("pingpong").withCode().verbose(1));
    assertEquals(1, libraryInfos.size());
    assertEquals("pingpong", libraryInfos.get(0).getName());
    assertEquals("You PING, we PONG", libraryInfos.get(0).getFunctions().get(0).getDescription());
    assertNotNull(libraryInfos.get(0).getCode());
  }

  @Test
  public void testFunctionLibraryListWithCodeVerboseTwo() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().library("pingpong").withCode().verbose(2));
    assertEquals(1, libraryInfos.size());
    assertEquals("pingpong", libraryInfos.get(0).getName());
    assertEquals("You PING, we PONG", libraryInfos.get(0).getFunctions().get(0).getDescription());
    assertNotNull(libraryInfos.get(0).getCode());
  }

  @Test
  public void testFunctionLibraryListWithCodeVerboseThree() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().library("pingpong").withCode().verbose(3));
    assertEquals(1, libraryInfos.size());
    assertEquals("pingpong", libraryInfos.get(0).getName());
    assertEquals("You PING, we PONG", libraryInfos.get(0).getFunctions().get(0).getDescription());
    assertNotNull(libraryInfos.get(0).getCode());
  }

  @Test
  public void testLibraryDelete() throws IOException {
    loadAllLibraries();
    assertEquals("OK", client.tFunctionDelete("pingpong"));
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList();
    assertEquals(LIBRARIES_ARRAY.length - 1, libraryInfos.size());
  }

  @Test
  public void testLibraryCallStringResult() throws IOException {
    loadAllLibraries();
    Object result = client.tFunctionCall("pingpong", "playPingPong", Collections.emptyList(),
        Collections.emptyList());
    assertEquals(String.class, result.getClass());
    assertEquals("PONG", result);
  }

  @Test
  public void testLibraryCallSetValueResult() throws IOException {
    loadAllLibraries();
    Object result = client.tFunctionCall("withFlags", "my_set", Collections.singletonList("MY_KEY"),
        Collections.singletonList("MY_VALUE"));
    assertEquals(String.class, result.getClass());
    assertEquals("OK", result);
    assertEquals("MY_VALUE", client.get("MY_KEY"));
  }

  @Test
  public void testLibraryCallHashResult() throws IOException {
    loadAllLibraries();
    Map<String, String> payload = new HashMap<>();
    payload.put("C", "Dennis Ritchie");
    payload.put("Python", "Guido van Rossum");
    payload.put("C++", "Bjarne Stroustrup");
    payload.put("JavaScript", "Brendan Eich");
    payload.put("Java", "James Gosling");
    payload.put("Ruby", "Yukihiro Matsumoto");

    client.hmset("hash1", payload);

    Object result = client.tFunctionCall("hashitout", "hashy", Collections.singletonList("hash1"),
        Collections.emptyList());

    final Map<String, String> asMap;

    if (protocol != RedisProtocol.RESP3) {
      final List<String> asList = (List) result;
      asMap = flatMapToMap(asList);

    } else {
      asMap = (Map) result;
    }

    payload.forEach((language, author) -> {
      assertThat(asMap, Matchers.hasEntry(language, author));
    });
    assertThat(Long.parseLong(asMap.get("__last_updated__")), Matchers.greaterThan(0L));
  }

  @Test
  public void testFunctionLoadWithConfig() throws IOException {
    loadAllLibraries();
    List<String> argsBefore = Arrays.asList("Dictionary1", "Pollito", "Chicken");
    client.tFunctionCall("withConfig", "hset", Collections.emptyList(), argsBefore);

    String config = "{\"last_modified_field_name\":\"changed_on\"}";
    client.tFunctionLoad(readLibrary("withConfig.js"), TFunctionLoadParams.loadParams().replace().config(config));

    List<String> argsAfter = Arrays.asList("Dictionary2", "Gallina", "Hen");
    Object result = client.tFunctionCall("withConfig", "hset", Collections.emptyList(), argsAfter);
    assertEquals(2L, result);

    Map<String, String> dict1 = client.hgetAll("Dictionary1");
    Map<String, String> dict2 = client.hgetAll("Dictionary2");

    assertTrue(dict1.containsKey("Pollito"));
    assertTrue(dict1.containsKey("__last_modified__"));
    assertFalse(dict1.containsKey("changed_on"));

    assertTrue(dict2.containsKey("Gallina"));
    assertTrue(dict2.containsKey("changed_on"));
    assertFalse(dict2.containsKey("__last_modified__"));
  }

  @Test
  public void testLibraryCallSetValueResultAsync() throws IOException {
    loadAllLibraries();
    Object result = client.tFunctionCallAsync("withFlags", "my_set", Collections.singletonList("KEY_TWO"),
        Collections.singletonList("KEY_TWO_VALUE"));
    assertEquals(String.class, result.getClass());
    assertEquals("OK", result);
    assertEquals("KEY_TWO_VALUE", client.get("KEY_TWO"));
  }

  private static String readLibrary(String filename) throws IOException {
    Path path = Paths.get("src/test/resources/functions/" + filename);
    return String.join("\n", Files.readAllLines(path));
  }

  private void loadAllLibraries() throws IOException {
    try (Stream<Path> walk = Files.walk(Paths.get("src/test/resources/functions/"))) {
      List<String> libs = walk
          .filter(p -> !Files.isDirectory(p)) //
          .map(Path::toString) //
          .filter(f -> f.endsWith(".js")) //
          .collect(Collectors.toList());

      libs.forEach(lib -> {
        String code;
        try {
          code = String.join("\n", Files.readAllLines(Paths.get(lib)));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        client.tFunctionLoad(code, TFunctionLoadParams.loadParams().replace());
      });
    }
  }

  private static Map<String, List<Predicate<GearsLibraryInfo>>> initializeTestLibraryConditions() {
    Map<String, List<Predicate<GearsLibraryInfo>>> libraryConditions = new HashMap<>();
    libraryConditions.put("streamTriggers", new ArrayList<>());
    libraryConditions.put("withFlags", new ArrayList<>());
    libraryConditions.put("pingpong", new ArrayList<>());
    libraryConditions.put("keyspaceTriggers", new ArrayList<>());
    libraryConditions.put("hashitout", new ArrayList<>());
    libraryConditions.put("withConfig", new ArrayList<>());

    return libraryConditions;
  }

  private static Map flatMapToMap(List list) {
    Map map = new HashMap(list.size() / 2);
    for (int i = 0; i < list.size(); i += 2) {
      map.put(list.get(i), list.get(i + 1));
    }
    return map;
  }
}
