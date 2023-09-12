package redis.clients.jedis.modules.gears;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.gears.TFunctionListParams;
import redis.clients.jedis.gears.TFunctionLoadParams;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;
import redis.clients.jedis.gears.resps.GearsLibraryInfo;
import redis.clients.jedis.util.KeyValue;
import redis.clients.jedis.util.RedisProtocolUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class GearsTest extends RedisModuleCommandsTestBase {
  private static final String BAD_FUNCTION = "All Your Base Are Belong to Us";
  private static final int NUMBER_OF_LIBS = 6;
  private static final List<String> LOADED_LIBS = Arrays.asList("streamTriggers", "withFlags", "pingpong", "keyspaceTriggers", "hashitout", "withConfig");

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }

  @Before
  public void deleteFunctions() {
    List<GearsLibraryInfo> libraries = client.tFunctionList();
    libraries.stream().map(GearsLibraryInfo::getName).forEach(library -> client.tFunctionDelete(library));
  }

  @Test
  public void testFunctionLoad() throws IOException {
    client.tFunctionLoad(readLibrary("pingpong.js"));

    List<GearsLibraryInfo> libraries = client.tFunctionList();
    assertTrue(libraries.stream().map(GearsLibraryInfo::getName).collect(Collectors.toList()).contains("pingpong"));
  }

  @Test(expected = JedisDataException.class)
  public void testFunctionLoadAlreadyLoadedFails() throws IOException {
    client.tFunctionLoad(readLibrary("pingpong.js"));
    client.tFunctionLoad(readLibrary("pingpong.js"));

    List<GearsLibraryInfo> libraries = client.tFunctionList();
    assertTrue(libraries.stream().map(GearsLibraryInfo::getName).collect(Collectors.toList()).contains("pingpong"));
  }

  @Test
  public void testFunctionLoadWithReplace() throws IOException {
    client.tFunctionLoad(readLibrary("pingpong.js"));
    client.tFunctionLoad(readLibrary("pingpong.js"), TFunctionLoadParams.loadParams().replace());

    List<GearsLibraryInfo> libraries = client.tFunctionList();
    assertTrue(libraries.stream().map(GearsLibraryInfo::getName).collect(Collectors.toList()).contains("pingpong"));
  }

  @Test(expected = JedisDataException.class)
  public void testBadFunctionLoad() {
    client.tFunctionLoad(BAD_FUNCTION);
  }

  @Test
  public void testFunctionListNoCodeVerboseZero() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList();
    assertEquals(NUMBER_OF_LIBS, libraryInfos.size());

    List<String> libraryNames = libraryInfos.stream().map(GearsLibraryInfo::getName).collect(Collectors.toList());
    assertTrue(libraryNames.containsAll(LOADED_LIBS));

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
    assertEquals(NUMBER_OF_LIBS, libraryInfos.size());

    List<String> libraryNames = libraryInfos.stream().map(GearsLibraryInfo::getName).collect(Collectors.toList());
    assertTrue(libraryNames.containsAll(LOADED_LIBS));

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
    assertEquals(NUMBER_OF_LIBS, libraryInfos.size());

    List<String> libraryNames = libraryInfos.stream().map(GearsLibraryInfo::getName).collect(Collectors.toList());
    assertTrue(libraryNames.containsAll(LOADED_LIBS));

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
    assertEquals(NUMBER_OF_LIBS, libraryInfos.size());

    List<String> libraryNames = libraryInfos.stream().map(GearsLibraryInfo::getName).collect(Collectors.toList());
    assertTrue(libraryNames.containsAll(LOADED_LIBS));

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
    assertEquals(NUMBER_OF_LIBS, libraryInfos.size());

    List<String> libraryNames = libraryInfos.stream().map(GearsLibraryInfo::getName).collect(Collectors.toList());
    assertTrue(libraryNames.containsAll(LOADED_LIBS));

    List<String> sources = libraryInfos.stream().map(GearsLibraryInfo::getCode).collect(Collectors.toList());
    assertTrue(sources.stream().allMatch(Objects::nonNull));

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

    assertEquals(NUMBER_OF_LIBS, libraryInfos.size());

    List<String> libraryNames = libraryInfos.stream().map(GearsLibraryInfo::getName).collect(Collectors.toList());
    assertTrue(libraryNames.containsAll(LOADED_LIBS));

    List<String> sources = libraryInfos.stream().map(GearsLibraryInfo::getCode).collect(Collectors.toList());
    assertTrue(sources.stream().allMatch(Objects::nonNull));

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
    assertEquals(NUMBER_OF_LIBS, libraryInfos.size());

    List<String> libraryNames = libraryInfos.stream().map(GearsLibraryInfo::getName).collect(Collectors.toList());
    assertTrue(libraryNames.containsAll(LOADED_LIBS));

    List<String> sources = libraryInfos.stream().map(GearsLibraryInfo::getCode).collect(Collectors.toList());
    assertTrue(sources.stream().allMatch(Objects::nonNull));

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
    assertEquals(NUMBER_OF_LIBS, libraryInfos.size());

    List<String> libraryNames = libraryInfos.stream().map(GearsLibraryInfo::getName).collect(Collectors.toList());
    assertTrue(libraryNames.containsAll(LOADED_LIBS));

    List<String> sources = libraryInfos.stream().map(GearsLibraryInfo::getCode).collect(Collectors.toList());
    assertTrue(sources.stream().allMatch(Objects::nonNull));

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
    assertTrue(libraryInfos.get(0).getCode().isEmpty());
  }

  @Test
  public void testFunctionLibraryListNoCodeVerboseOne() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().library("pingpong").verbose(1));

    assertEquals(1, libraryInfos.size());
    assertEquals("pingpong", libraryInfos.get(0).getName());
    assertEquals("You PING, we PONG", libraryInfos.get(0).getFunctions().get(0).getDescription());
    assertTrue(libraryInfos.get(0).getCode().isEmpty());
  }

  @Test
  public void testFunctionLibraryListNoCodeVerboseTwo() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().library("pingpong").verbose(2));
    assertEquals(1, libraryInfos.size());
    assertEquals("pingpong", libraryInfos.get(0).getName());
    assertEquals("You PING, we PONG", libraryInfos.get(0).getFunctions().get(0).getDescription());
    assertTrue(libraryInfos.get(0).getCode().isEmpty());
  }

  @Test
  public void testFunctionLibraryListNoCodeVerboseThree() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().library("pingpong").verbose(3));
    assertEquals(1, libraryInfos.size());
    assertEquals("pingpong", libraryInfos.get(0).getName());
    assertEquals("You PING, we PONG", libraryInfos.get(0).getFunctions().get(0).getDescription());
    assertTrue(libraryInfos.get(0).getCode().isEmpty());
  }

  @Test
  public void testFunctionLibraryListWithCodeVerboseZero() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().library("pingpong").withCode());
    assertEquals(1, libraryInfos.size());
    assertEquals("pingpong", libraryInfos.get(0).getName());
    assertNull(libraryInfos.get(0).getFunctions().get(0).getDescription());
    assertFalse(libraryInfos.get(0).getCode().isEmpty());
  }

  @Test
  public void testFunctionLibraryListWithCodeVerboseOne() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().library("pingpong").withCode().verbose(1));
    assertEquals(1, libraryInfos.size());
    assertEquals("pingpong", libraryInfos.get(0).getName());
    assertEquals("You PING, we PONG", libraryInfos.get(0).getFunctions().get(0).getDescription());
    assertFalse(libraryInfos.get(0).getCode().isEmpty());
  }

  @Test
  public void testFunctionLibraryListWithCodeVerboseTwo() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().library("pingpong").withCode().verbose(2));
    assertEquals(1, libraryInfos.size());
    assertEquals("pingpong", libraryInfos.get(0).getName());
    assertEquals("You PING, we PONG", libraryInfos.get(0).getFunctions().get(0).getDescription());
    assertFalse(libraryInfos.get(0).getCode().isEmpty());
  }

  @Test
  public void testFunctionLibraryListWithCodeVerboseThree() throws IOException {
    loadAllLibraries();
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList(TFunctionListParams.listParams().library("pingpong").withCode().verbose(3));
    assertEquals(1, libraryInfos.size());
    assertEquals("pingpong", libraryInfos.get(0).getName());
    assertEquals("You PING, we PONG", libraryInfos.get(0).getFunctions().get(0).getDescription());
    assertFalse(libraryInfos.get(0).getCode().isEmpty());
  }

  @Test
  public void testLibraryDelete() throws IOException {
    loadAllLibraries();
    Object result = client.tFunctionDelete("pingpong");
    assertEquals("OK", result);
    List<GearsLibraryInfo> libraryInfos = client.tFunctionList();
    assertEquals(NUMBER_OF_LIBS - 1, libraryInfos.size());
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
    Map<String,String> payload = new HashMap<>();
    payload.put("C", "Dennis Ritchie");
    payload.put("Python", "Guido van Rossum");
    payload.put("C++", "Bjarne Stroustrup");
    payload.put("JavaScript", "Brendan Eich");
    payload.put("Java", "James Gosling");
    payload.put("Ruby", "Yukihiro Matsumoto");

    client.hmset("hash1", payload);

    Object result = client.tFunctionCall("hashitout", "hashy", Collections.singletonList("hash1"),
      Collections.emptyList());
    assertEquals(ArrayList.class, result.getClass());
    List<Object> list = (List)result;
    assertFalse(list.isEmpty());
    boolean isResp3 = list.get(0) instanceof KeyValue;

    assertEquals(isResp3 ? 7 : 14, list.size());

    if (!isResp3) {
      List<String> asList = (List)result;
      int indexOfJava = asList.indexOf("Java");
      assertTrue(indexOfJava >= 0);
      assertEquals("James Gosling", asList.get(indexOfJava+1));
      int indexOfJavaScript = asList.indexOf("JavaScript");
      assertTrue(indexOfJavaScript >= 0);
      assertEquals("Brendan Eich", asList.get(indexOfJavaScript+1));
      int indexOfC = asList.indexOf("C");
      assertTrue(indexOfC >= 0);
      assertEquals("Dennis Ritchie", asList.get(indexOfC+1));
      int indexOfRuby = asList.indexOf("Ruby");
      assertTrue(indexOfRuby >= 0);
      assertEquals("Yukihiro Matsumoto", asList.get(indexOfRuby+1));
      int indexOfPython = asList.indexOf("Python");
      assertTrue(indexOfPython >= 0);
      assertEquals("Guido van Rossum", asList.get(indexOfPython+1));
      int indexOfCPP = asList.indexOf("C++");
      assertTrue(indexOfCPP >= 0);
      assertEquals("Bjarne Stroustrup", asList.get(indexOfCPP+1));
      int indexOfLastUpdated = asList.indexOf("__last_updated__");
      assertTrue(indexOfLastUpdated >= 0);
      assertTrue(Integer.parseInt(asList.get(indexOfLastUpdated+1)) > 0);
    } else {
      for (KeyValue kv : (List<KeyValue>) result) {
        if (!kv.getKey().toString().equalsIgnoreCase("__last_updated__")) {
          assertTrue(payload.containsKey(kv.getKey()));
          assertEquals(payload.get(kv.getKey()), kv.getValue());
        }
      }
    }
  }

  @Test
  public void testFunctionLoadWithConfig() throws IOException {
    loadAllLibraries();
    List<String> argsBefore = Arrays.asList("Dictionary1", "Pollito", "Chicken");
    client.tFunctionCall("withConfig", "hset", Collections.emptyList(), argsBefore);

    String config = "{\"last_modified_field_name\":\"changed_on\"}";
    client.tFunctionLoad(readLibrary("withConfig.js"), TFunctionLoadParams.loadParams().replace().withConfig(config));

    List<String> argsAfter = Arrays.asList("Dictionary2", "Gallina", "Hen");
    Object result = client.tFunctionCall("withConfig", "hset", Collections.emptyList(), argsAfter);
    System.out.println(result);

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

  private Map<String, List<Predicate<GearsLibraryInfo>>> initializeTestLibraryConditions() {
    Map<String, List<Predicate<GearsLibraryInfo>>> libraryConditions = new HashMap<>();
    libraryConditions.put("streamTriggers", new ArrayList<>());
    libraryConditions.put("withFlags", new ArrayList<>());
    libraryConditions.put("pingpong", new ArrayList<>());
    libraryConditions.put("keyspaceTriggers", new ArrayList<>());
    libraryConditions.put("hashitout", new ArrayList<>());
    libraryConditions.put("withConfig", new ArrayList<>());

    return libraryConditions;
  }
}
