package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.redis.test.annotations.SinceRedisVersion;
import io.redis.test.utils.RedisVersion;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.FlushMode;
import redis.clients.jedis.args.FunctionRestorePolicy;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.resps.FunctionStats;
import redis.clients.jedis.resps.LibraryInfo;
import redis.clients.jedis.util.RedisVersionUtil;

/**
 * Tests related to <a href="https://redis.io/commands/?group=scripting">Scripting</a> commands.
 */
public class CommandObjectsScriptingCommandsTest extends CommandObjectsStandaloneTestBase {

  public CommandObjectsScriptingCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Before
  @Override
  public void setUp() {
    super.setUp();
    if (RedisVersionUtil.getRedisVersion(endpoint).isGreaterThanOrEqualTo(RedisVersion.V7_0_0)) {
      assertThat(exec(commandObjects.functionFlush(FlushMode.SYNC)), equalTo("OK"));
    }
  }

  @Test
  public void testEvalWithOnlyScript() {
    String set = exec(commandObjects.set("foo", "bar"));
    assertThat(set, equalTo("OK"));

    String script = "return redis.call('get', 'foo')";

    Object eval = exec(commandObjects.eval(script));
    assertThat(eval, equalTo("bar"));

    Object evalBinary = exec(commandObjects.eval(script.getBytes()));
    assertThat(evalBinary, equalTo("bar".getBytes()));

    // eval with incorrect script
    assertThrows(JedisException.class,
        () -> exec(commandObjects.eval("return x")));
  }

  @Test
  public void testEvalWithScriptAndSampleKey() {
    String set = exec(commandObjects.set("foo", "bar"));
    assertThat(set, equalTo("OK"));

    String script = "return redis.call('get', 'foo');";

    Object eval = exec(commandObjects.eval(script, "sampleKey"));
    assertThat(eval, equalTo("bar"));

    Object evalBinary = exec(commandObjects.eval(script.getBytes(), "sampleKey".getBytes()));
    assertThat(evalBinary, equalTo("bar".getBytes()));
  }

  @Test
  public void testEvalWithScriptKeyCountAndParams() {
    exec(commandObjects.set("key1", "value1"));
    exec(commandObjects.set("key2", "value2"));

    // Script to get values of two keys and compare them
    String script = "if redis.call('get', KEYS[1]) == ARGV[1] and redis.call('get', KEYS[2]) == ARGV[2] then return 'true' else return 'false' end";

    Object evalTrue = exec(commandObjects.eval(
        script, 2, "key1", "key2", "value1", "value2"));

    assertThat(evalTrue, equalTo("true"));

    Object evalTrueBinary = exec(commandObjects.eval(
        script.getBytes(), 2, "key1".getBytes(), "key2".getBytes(), "value1".getBytes(), "value2".getBytes()));

    assertThat(evalTrueBinary, equalTo("true".getBytes()));

    Object evalFalse = exec(commandObjects.eval(
        script, 2, "key1", "key2", "value1", "value3"));

    assertThat(evalFalse, equalTo("false"));

    Object evalFalseBinary = exec(commandObjects.eval(
        script.getBytes(), 2, "key1".getBytes(), "key2".getBytes(), "value1".getBytes(), "value3".getBytes()));

    assertThat(evalFalseBinary, equalTo("false".getBytes()));

    // Incorrect number of keys specified
    assertThrows(JedisException.class,
        () -> exec(commandObjects.eval(script, 1, "key1", "value1", "value2")));
  }

  @Test
  public void testEvalWithScriptKeysAndArgsList() {
    exec(commandObjects.hset("fruits", "apples", "5"));
    exec(commandObjects.hset("fruits", "bananas", "3"));
    exec(commandObjects.hset("fruits", "oranges", "4"));

    // Script to sum the values for the fruits provided as args. The hash name is provided as key.
    // The sum is written to a string value whose name is also provided as keys.
    String script = "local sum = 0\n" +
        "for i, fruitKey in ipairs(ARGV) do\n" +
        "    local value = redis.call('HGET', KEYS[1], fruitKey)\n" +
        "    if value then\n" +
        "        sum = sum + tonumber(value)\n" +
        "    end\n" +
        "end\n" +
        "redis.call('SET', KEYS[2], sum)\n" +
        "return sum";

    String initialTotal = exec(commandObjects.get("total"));
    assertThat(initialTotal, nullValue());

    Object eval = exec(commandObjects.eval(script,
        Arrays.asList("fruits", "total"), Arrays.asList("apples", "bananas", "oranges")));

    assertThat(eval, equalTo(12L));

    String totalAfterEval = exec(commandObjects.get("total"));
    assertThat(totalAfterEval, equalTo("12"));

    // reset
    assertThat(exec(commandObjects.del("total")), equalTo(1L));

    // binary
    String initialTotalBinary = exec(commandObjects.get("total"));
    assertThat(initialTotalBinary, nullValue());

    Object evalBinary = exec(commandObjects.eval(script.getBytes(),
        Arrays.asList("fruits".getBytes(), "total".getBytes()), Arrays.asList("apples".getBytes(), "bananas".getBytes(), "oranges".getBytes())));

    assertThat(evalBinary, equalTo(12L));

    String totalAfterEvalBinary = exec(commandObjects.get("total"));
    assertThat(totalAfterEvalBinary, equalTo("12"));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testEvalReadonlyWithScriptKeysAndArgsList() {
    exec(commandObjects.set("readonlyKey1", "readonlyValue1"));
    exec(commandObjects.set("readonlyKey2", "readonlyValue2"));

    // Script to retrieve values for provided keys, concatenates
    String script = "return redis.call('get', KEYS[1]) .. redis.call('get', KEYS[2])";

    Object eval = exec(commandObjects.evalReadonly(
        script, Arrays.asList("readonlyKey1", "readonlyKey2"), Collections.emptyList()));

    assertThat(eval, equalTo("readonlyValue1readonlyValue2"));

    Object evalBinary = exec(commandObjects.evalReadonly(
        script.getBytes(), Arrays.asList("readonlyKey1".getBytes(), "readonlyKey2".getBytes()), Collections.emptyList()));

    assertThat(evalBinary, equalTo("readonlyValue1readonlyValue2".getBytes()));
  }

  @Test
  public void testEvalshaWithSha1() {
    String script = "return 42";
    String sha1 = exec(commandObjects.scriptLoad(script));
    assertThat(sha1, notNullValue());

    Object eval = exec(commandObjects.evalsha(sha1));
    assertThat(eval, equalTo(42L));

    Object evalBinary = exec(commandObjects.evalsha(sha1.getBytes()));
    assertThat(evalBinary, equalTo(42L));

    // incorrect SHA1 hash
    assertThrows(JedisException.class,
        () -> exec(commandObjects.evalsha("incorrectSha1")));
  }

  @Test
  public void testEvalshaWithSha1AndSampleKey() {
    String script = "return redis.call('get', 'foo')";
    String sha1 = exec(commandObjects.scriptLoad(script));
    assertThat(sha1, notNullValue());

    exec(commandObjects.set("foo", "bar"));

    Object eval = exec(commandObjects.evalsha(sha1, "sampleKey"));

    assertThat(eval, equalTo("bar"));

    Object evalBinary = exec(commandObjects.evalsha(sha1.getBytes(), "sampleKey".getBytes()));

    assertThat(evalBinary, equalTo("bar".getBytes()));
  }

  @Test
  public void testEvalWithScriptKeyCountAndParamsSha() {
    exec(commandObjects.set("key1", "value1"));
    exec(commandObjects.set("key2", "value2"));

    // Script to get values of two keys and compare them with expected values
    String script = "if redis.call('get', KEYS[1]) == ARGV[1] and redis.call('get', KEYS[2]) == ARGV[2] then return 'true' else return 'false' end";
    String sha1 = exec(commandObjects.scriptLoad(script));
    assertThat(sha1, notNullValue());

    Object evalTrue = exec(commandObjects.evalsha(
        sha1, 2, "key1", "key2", "value1", "value2"));

    assertThat(evalTrue, equalTo("true"));

    Object evalTrueBinary = exec(commandObjects.evalsha(
        sha1.getBytes(), 2, "key1".getBytes(), "key2".getBytes(), "value1".getBytes(), "value2".getBytes()));

    assertThat(evalTrueBinary, equalTo("true".getBytes()));

    Object evalFalse = exec(commandObjects.evalsha(
        sha1, 2, "key1", "key2", "value1", "value3"));

    assertThat(evalFalse, equalTo("false"));

    Object evalFalseBinary = exec(commandObjects.evalsha(
        sha1.getBytes(), 2, "key1".getBytes(), "key2".getBytes(), "value1".getBytes(), "value3".getBytes()));

    assertThat(evalFalseBinary, equalTo("false".getBytes()));

    // Incorrect number of keys
    assertThrows(JedisException.class,
        () -> exec(commandObjects.evalsha(sha1, 1, "key1", "value1", "value2")));
  }

  @Test
  public void testEvalWithScriptKeysAndArgsListSha() {
    exec(commandObjects.hset("fruits", "apples", "5"));
    exec(commandObjects.hset("fruits", "bananas", "3"));
    exec(commandObjects.hset("fruits", "oranges", "4"));

    // Sums the values for given fruits, stores the result, and returns it
    String script = "local sum = 0\n" +
        "for i, fruitKey in ipairs(ARGV) do\n" +
        "    local value = redis.call('HGET', KEYS[1], fruitKey)\n" +
        "    if value then\n" +
        "        sum = sum + tonumber(value)\n" +
        "    end\n" +
        "end\n" +
        "redis.call('SET', KEYS[2], sum)\n" +
        "return sum";
    String sha1 = exec(commandObjects.scriptLoad(script));
    assertThat(sha1, notNullValue());

    String initialTotal = exec(commandObjects.get("total"));
    assertThat(initialTotal, nullValue());

    Object eval = exec(commandObjects.evalsha(
        sha1, Arrays.asList("fruits", "total"), Arrays.asList("apples", "bananas", "oranges")));

    assertThat(eval, equalTo(12L));

    String totalAfterEval = exec(commandObjects.get("total"));
    assertThat(totalAfterEval, equalTo("12"));

    // reset
    assertThat(exec(commandObjects.del("total")), equalTo(1L));

    // binary
    String initialTotalBinary = exec(commandObjects.get("total"));
    assertThat(initialTotalBinary, nullValue());

    Object evalBinary = exec(commandObjects.evalsha(
        sha1.getBytes(),
        Arrays.asList("fruits".getBytes(), "total".getBytes()),
        Arrays.asList("apples".getBytes(), "bananas".getBytes(), "oranges".getBytes())));

    assertThat(evalBinary, equalTo(12L));

    String totalAfterEvalBinary = exec(commandObjects.get("total"));
    assertThat(totalAfterEvalBinary, equalTo("12"));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testEvalReadonlyWithScriptKeysAndArgsListSha() {
    exec(commandObjects.set("readonlyKey1", "readonlyValue1"));
    exec(commandObjects.set("readonlyKey2", "readonlyValue2"));

    // Script to retrieve values for provided keys, concatenated
    String script = "return redis.call('get', KEYS[1]) .. redis.call('get', KEYS[2])";
    String sha1 = exec(commandObjects.scriptLoad(script));
    assertThat(sha1, notNullValue());

    Object eval = exec(commandObjects.evalshaReadonly(
        sha1,
        Arrays.asList("readonlyKey1", "readonlyKey2"),
        Collections.emptyList()));

    assertThat(eval, equalTo("readonlyValue1readonlyValue2"));

    Object evalBinary = exec(commandObjects.evalshaReadonly(
        sha1.getBytes(),
        Arrays.asList("readonlyKey1".getBytes(), "readonlyKey2".getBytes()),
        Collections.emptyList()));

    assertThat(evalBinary, equalTo("readonlyValue1readonlyValue2".getBytes()));
  }

  @Test
  public void testScriptExists() {
    String script = "return 'test script'";
    String sha1 = exec(commandObjects.scriptLoad(script));
    assertThat(sha1, notNullValue());

    List<Boolean> exists = exec(commandObjects.scriptExists(Collections.singletonList(sha1)));

    assertThat(exists, contains(true));

    // Load another script to test with multiple SHA1 hashes
    String anotherScript = "return 'another test script'";
    String anotherSha1 = exec(commandObjects.scriptLoad(anotherScript));
    assertThat(anotherSha1, notNullValue());

    String nonExistingSha1 = "nonexistentsha1";

    List<Boolean> existsMultiple = exec(commandObjects.scriptExists(
        "sampleKey", sha1, anotherSha1, nonExistingSha1));

    assertThat(existsMultiple, contains(true, true, false));

    List<Boolean> existsMultipleBinary = exec(commandObjects.scriptExists(
        "sampleKey".getBytes(), sha1.getBytes(), anotherSha1.getBytes(), nonExistingSha1.getBytes()));

    assertThat(existsMultipleBinary, contains(true, true, false));
  }

  @Test
  public void testScriptLoadAndRun() {
    String script = "return 'Hello, Redis!'";
    String sha1 = exec(commandObjects.scriptLoad(script));
    assertThat(sha1, notNullValue());

    Object scriptResponse1 = exec(commandObjects.evalsha(sha1));
    assertThat(scriptResponse1, equalTo("Hello, Redis!"));
  }

  @Test
  public void testScriptLoadAndRunSampleKey() {
    String anotherScript = "return redis.call('get', 'testKey')";

    String sampleKey = "testKey";
    exec(commandObjects.set(sampleKey, "sampleValue")); // Set a value for the sampleKey

    String anotherSha1 = exec(commandObjects.scriptLoad(anotherScript, sampleKey));
    assertThat(anotherSha1, notNullValue());

    Object scriptResponse2 = exec(commandObjects.evalsha(anotherSha1, sampleKey));
    assertThat(scriptResponse2, equalTo("sampleValue"));
  }

  @Test
  public void testScriptLoadAndRunSampleKeyBinary() {
    String anotherScript = "return redis.call('get', 'testKey')";

    String sampleKey = "testKey";
    exec(commandObjects.set(sampleKey, "sampleValue")); // Set a value for the sampleKey

    byte[] anotherSha1 = exec(commandObjects.scriptLoad(anotherScript.getBytes(), sampleKey.getBytes()));
    assertThat(anotherSha1, notNullValue());

    Object scriptResponse2 = exec(commandObjects.evalsha(anotherSha1, sampleKey.getBytes()));
    assertThat(scriptResponse2, equalTo("sampleValue".getBytes()));
  }

  @Test
  public void testScriptFlush() {
    String script = "return 'test script flush'";
    String sha1 = exec(commandObjects.scriptLoad(script));
    assertThat(sha1, notNullValue());

    List<Boolean> existsBefore = exec(commandObjects.scriptExists(Collections.singletonList(sha1)));
    assertThat(existsBefore, contains(true));

    String flush = exec(commandObjects.scriptFlush());
    assertThat(flush, equalTo("OK"));

    List<Boolean> existsAfter = exec(commandObjects.scriptExists(Collections.singletonList(sha1)));
    assertThat(existsAfter, contains(false));
  }

  @Test
  public void testScriptFlushSampleKeyAndMode() {
    String script = "return 'test script flush'";
    String sha1 = exec(commandObjects.scriptLoad(script));
    assertThat(sha1, notNullValue());

    List<Boolean> existsBefore = exec(commandObjects.scriptExists(Collections.singletonList(sha1)));
    assertThat(existsBefore, contains(true));

    String flush = exec(commandObjects.scriptFlush("anyKey", FlushMode.SYNC));
    assertThat(flush, equalTo("OK"));

    List<Boolean> existsAfter = exec(commandObjects.scriptExists(Collections.singletonList(sha1)));
    assertThat(existsAfter, contains(false));
  }

  @Test
  public void testScriptFlushSampleKey() {
    String script = "return 'test script flush'";
    String sha1 = exec(commandObjects.scriptLoad(script));
    assertThat(sha1, notNullValue());

    List<Boolean> existsBefore = exec(commandObjects.scriptExists(Collections.singletonList(sha1)));
    assertThat(existsBefore, contains(true));

    String flush = exec(commandObjects.scriptFlush("anyKey"));
    assertThat(flush, equalTo("OK"));

    List<Boolean> existsAfter = exec(commandObjects.scriptExists(Collections.singletonList(sha1)));
    assertThat(existsAfter, contains(false));
  }

  @Test
  public void testScriptFlushBinary() {
    String script = "return 'test script flush'";
    String sha1 = exec(commandObjects.scriptLoad(script));
    assertThat(sha1, notNullValue());

    List<Boolean> existsBefore = exec(commandObjects.scriptExists(Collections.singletonList(sha1)));
    assertThat(existsBefore, contains(true));

    String flush = exec(commandObjects.scriptFlush("anyKey".getBytes()));
    assertThat(flush, equalTo("OK"));

    List<Boolean> existsAfter = exec(commandObjects.scriptExists(Collections.singletonList(sha1)));
    assertThat(existsAfter, contains(false));
  }

  @Test
  public void testScriptFlushSampleKeyAndModeBinary() {
    String script = "return 'test script flush'";
    String sha1 = exec(commandObjects.scriptLoad(script));
    assertThat(sha1, notNullValue());

    List<Boolean> existsBefore = exec(commandObjects.scriptExists(Collections.singletonList(sha1)));
    assertThat(existsBefore, contains(true));

    String flush = exec(commandObjects.scriptFlush("anyKey".getBytes(), FlushMode.SYNC));
    assertThat(flush, equalTo("OK"));

    List<Boolean> existsAfter = exec(commandObjects.scriptExists(Collections.singletonList(sha1)));
    assertThat(existsAfter, contains(false));
  }

  @Test
  public void testScriptKill() {
    JedisException e = assertThrows(JedisException.class,
        () -> exec(commandObjects.scriptKill()));
    assertThat(e.getMessage(), containsString("No scripts in execution right now."));

    e = assertThrows(JedisException.class,
        () -> exec(commandObjects.scriptKill("anyKey")));
    assertThat(e.getMessage(), containsString("No scripts in execution right now."));

    e = assertThrows(JedisException.class,
        () -> exec(commandObjects.scriptKill("anyKey".getBytes())));
    assertThat(e.getMessage(), containsString("No scripts in execution right now."));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testSumValuesFunction() {
    String luaScript = "#!lua name=mylib\n" +
        "redis.register_function('sumValues', function(keys, args)\n" +
        "local sum = 0\n" +
        "for _, key in ipairs(keys) do\n" +
        "local val = redis.call('GET', key)\n" +
        "if val then sum = sum + tonumber(val) end\n" +
        "end\n" +
        "redis.call('SET', 'total', sum)\n" +
        "return sum\n" +
        "end)";
    String functionLoad = exec(commandObjects.functionLoad(luaScript));
    assertThat(functionLoad, equalTo("mylib"));

    exec(commandObjects.set("key1", "10"));
    exec(commandObjects.set("key2", "20"));
    exec(commandObjects.set("key3", "30"));

    String initialTotal = exec(commandObjects.get("total"));
    assertThat(initialTotal, nullValue());

    Object fcall = exec(commandObjects.fcall(
        "sumValues",
        Arrays.asList("key1", "key2", "key3"),
        new ArrayList<>()));

    assertThat(fcall, equalTo(60L));

    String totalAfterFcall = exec(commandObjects.get("total"));
    assertThat(totalAfterFcall, equalTo("60"));

    // reset
    exec(commandObjects.del("total"));

    String totalAfterRest = exec(commandObjects.get("total"));
    assertThat(totalAfterRest, nullValue());

    Object fcallBinary = exec(commandObjects.fcall(
        "sumValues".getBytes(),
        Arrays.asList("key1".getBytes(), "key2".getBytes(), "key3".getBytes()),
        new ArrayList<>()));

    assertThat(fcallBinary, equalTo(60L));

    String totalAfterFcallBinary = exec(commandObjects.get("total"));
    assertThat(totalAfterFcallBinary, equalTo("60"));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testSumValuesFunctionReadonly() {
    String luaScript = "#!lua name=mylib\n" +
        "redis.register_function{function_name='sumValues', callback=function(keys, args)\n" +
        "local sum = 0\n" +
        "for _, key in ipairs(keys) do\n" +
        "local val = redis.call('GET', key)\n" +
        "if val then sum = sum + tonumber(val) end\n" +
        "end\n" +
        "return sum\n" +
        "end, flags={'no-writes'}}";
    String functionLoad = exec(commandObjects.functionLoad(luaScript));
    assertThat(functionLoad, equalTo("mylib"));

    exec(commandObjects.set("key1", "10"));
    exec(commandObjects.set("key2", "20"));
    exec(commandObjects.set("key3", "30"));

    Object fcall = exec(commandObjects.fcallReadonly(
        "sumValues",
        Arrays.asList("key1", "key2", "key3"),
        new ArrayList<>()));

    assertThat(fcall, equalTo(60L));

    Object fcallBinary = exec(commandObjects.fcallReadonly(
        "sumValues".getBytes(),
        Arrays.asList("key1".getBytes(), "key2".getBytes(), "key3".getBytes()),
        new ArrayList<>()));

    assertThat(fcallBinary, equalTo(60L));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testFunctionDeletion() {
    String luaScript = "#!lua name=mylib\n" +
        "redis.register_function('sumValues', function(keys, args) return 42 end)";
    exec(commandObjects.functionLoad(luaScript));

    String libraryName = "mylib";

    List<LibraryInfo> listResponse = exec(commandObjects.functionList());

    assertThat(listResponse, hasSize(1));
    assertThat(listResponse.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(listResponse.get(0).getFunctions(), hasSize(1));
    assertThat(listResponse.get(0).getFunctions().get(0), hasEntry("name", "sumValues"));

    String delete = exec(commandObjects.functionDelete(libraryName));
    assertThat(delete, equalTo("OK"));

    listResponse = exec(commandObjects.functionList());
    assertThat(listResponse, empty());
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testFunctionDeletionBinary() {
    String luaScript = "#!lua name=mylib\n" +
        "redis.register_function('sumValues', function(keys, args) return 42 end)";
    exec(commandObjects.functionLoad(luaScript));

    String libraryName = "mylib";

    List<LibraryInfo> listResponse = exec(commandObjects.functionList());

    assertThat(listResponse, hasSize(1));
    assertThat(listResponse.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(listResponse.get(0).getFunctions(), hasSize(1));
    assertThat(listResponse.get(0).getFunctions().get(0), hasEntry("name", "sumValues"));

    String deleteBinary = exec(commandObjects.functionDelete(libraryName.getBytes()));
    assertThat(deleteBinary, equalTo("OK"));

    listResponse = exec(commandObjects.functionList());
    assertThat(listResponse, empty());
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testFunctionListing() {
    String luaScript = "#!lua name=mylib\n" +
        "redis.register_function('sumValues', function(keys, args) return 42 end)";
    exec(commandObjects.functionLoad(luaScript));

    String libraryName = "mylib";

    List<LibraryInfo> list = exec(commandObjects.functionList());

    assertThat(list, hasSize(1));
    assertThat(list.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(list.get(0).getFunctions(), hasSize(1));
    assertThat(list.get(0).getFunctions().get(0), hasEntry("name", "sumValues"));
    assertThat(list.get(0).getLibraryCode(), nullValue());

    List<Object> listBinary = exec(commandObjects.functionListBinary());

    assertThat(listBinary, hasSize(1));

    List<LibraryInfo> listLibrary = exec(commandObjects.functionList(libraryName));

    assertThat(listLibrary, hasSize(1));
    assertThat(listLibrary.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(listLibrary.get(0).getFunctions(), hasSize(1));
    assertThat(listLibrary.get(0).getFunctions().get(0), hasEntry("name", "sumValues"));
    assertThat(listLibrary.get(0).getLibraryCode(), nullValue());

    List<Object> listLibraryBinary = exec(commandObjects.functionList(libraryName.getBytes()));

    assertThat(listLibraryBinary, hasSize(1));

    List<LibraryInfo> listWithCode = exec(commandObjects.functionListWithCode());

    assertThat(listWithCode, hasSize(1));
    assertThat(listWithCode.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(listWithCode.get(0).getFunctions(), hasSize(1));
    assertThat(listWithCode.get(0).getFunctions().get(0), hasEntry("name", "sumValues"));
    assertThat(listWithCode.get(0).getLibraryCode(), notNullValue());

    List<Object> listWithCodeBinary = exec(commandObjects.functionListWithCodeBinary());

    assertThat(listWithCodeBinary, hasSize(1));

    List<LibraryInfo> listWithCodeLibrary = exec(commandObjects.functionListWithCode(libraryName));

    assertThat(listWithCodeLibrary, hasSize(1));
    assertThat(listWithCodeLibrary.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(listWithCodeLibrary.get(0).getFunctions(), hasSize(1));
    assertThat(listWithCodeLibrary.get(0).getFunctions().get(0), hasEntry("name", "sumValues"));
    assertThat(listWithCodeLibrary.get(0).getLibraryCode(), notNullValue());

    List<Object> listWithCodeLibraryBinary = exec(commandObjects.functionListWithCode(libraryName.getBytes()));

    assertThat(listWithCodeLibraryBinary, hasSize(1));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testFunctionReload() {
    String luaScript = "#!lua name=mylib\n" +
        "redis.register_function('dummy', function(keys, args) return 42 end)";
    String loadResult = exec(commandObjects.functionLoad(luaScript));
    assertThat(loadResult, equalTo("mylib"));

    Object result = exec(commandObjects.fcall(
        "dummy".getBytes(), new ArrayList<>(), new ArrayList<>()));
    assertThat(result, equalTo(42L));

    String luaScriptChanged = "#!lua name=mylib\n" +
        "redis.register_function('dummy', function(keys, args) return 52 end)";
    String replaceResult = exec(commandObjects.functionLoadReplace(luaScriptChanged));
    assertThat(replaceResult, equalTo("mylib"));

    Object resultAfter = exec(commandObjects.fcall(
        "dummy".getBytes(), new ArrayList<>(), new ArrayList<>()));
    assertThat(resultAfter, equalTo(52L));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testFunctionReloadBinary() {
    String luaScript = "#!lua name=mylib\n" +
        "redis.register_function('dummy', function(keys, args) return 42 end)";
    String loadResult = exec(commandObjects.functionLoad(luaScript.getBytes()));
    assertThat(loadResult, equalTo("mylib"));

    Object result = exec(commandObjects.fcall((
        "dummy").getBytes(), new ArrayList<>(), new ArrayList<>()));
    assertThat(result, equalTo(42L));

    String luaScriptChanged = "#!lua name=mylib\n" +
        "redis.register_function('dummy', function(keys, args) return 52 end)";
    String replaceResult = exec(commandObjects.functionLoadReplace(luaScriptChanged.getBytes()));
    assertThat(replaceResult, equalTo("mylib"));

    Object resultAfter = exec(commandObjects.fcall(
        "dummy".getBytes(), new ArrayList<>(), new ArrayList<>()));
    assertThat(resultAfter, equalTo(52L));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testFunctionStats() {
    String luaScript = "#!lua name=mylib\n" +
        "redis.register_function('dummy', function(keys, args) return 42 end)";
    String loadResult = exec(commandObjects.functionLoad(luaScript));
    assertThat(loadResult, equalTo("mylib"));

    for (int i = 0; i < 5; i++) {
      Object result = exec(commandObjects.fcall(
          "dummy".getBytes(), new ArrayList<>(), new ArrayList<>()));
      assertThat(result, equalTo(42L));
    }

    FunctionStats stats = exec(commandObjects.functionStats());

    assertThat(stats, notNullValue());
    assertThat(stats.getEngines(), hasKey("LUA"));
    Map<String, Object> luaStats = stats.getEngines().get("LUA");
    assertThat(luaStats, hasEntry("libraries_count", 1L));
    assertThat(luaStats, hasEntry("functions_count", 1L));

    Object statsBinary = exec(commandObjects.functionStatsBinary());

    assertThat(statsBinary, notNullValue());
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testFunctionDumpFlushRestore() {
    String luaScript = "#!lua name=mylib\n" +
        "redis.register_function('sumValues', function(keys, args) return 42 end)";
    exec(commandObjects.functionLoad(luaScript));

    String libraryName = "mylib";

    List<LibraryInfo> list = exec(commandObjects.functionList());
    assertThat(list, hasSize(1));
    assertThat(list.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(list.get(0).getFunctions(), hasSize(1));
    assertThat(list.get(0).getFunctions().get(0), hasEntry("name", "sumValues"));

    byte[] dump = exec(commandObjects.functionDump());
    assertThat(dump, notNullValue());

    String flush = exec(commandObjects.functionFlush());
    assertThat(flush, equalTo("OK"));

    list = exec(commandObjects.functionList());
    assertThat(list, empty());

    String restore = exec(commandObjects.functionRestore(dump));
    assertThat(restore, equalTo("OK"));

    list = exec(commandObjects.functionList());
    assertThat(list, hasSize(1));
    assertThat(list.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(list.get(0).getFunctions(), hasSize(1));
    assertThat(list.get(0).getFunctions().get(0), hasEntry("name", "sumValues"));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testFunctionDumpFlushRestoreWithPolicy() {
    String luaScript = "#!lua name=mylib\n" +
        "redis.register_function('sumValues', function(keys, args) return 42 end)";
    exec(commandObjects.functionLoad(luaScript));

    String libraryName = "mylib";

    List<LibraryInfo> list = exec(commandObjects.functionList());
    assertThat(list, hasSize(1));
    assertThat(list.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(list.get(0).getFunctions(), hasSize(1));
    assertThat(list.get(0).getFunctions().get(0), hasEntry("name", "sumValues"));

    byte[] dump = exec(commandObjects.functionDump());
    assertThat(dump, notNullValue());

    String flush = exec(commandObjects.functionFlush());
    assertThat(flush, equalTo("OK"));

    list = exec(commandObjects.functionList());
    assertThat(list, empty());

    String restore = exec(commandObjects.functionRestore(dump, FunctionRestorePolicy.REPLACE));
    assertThat(restore, equalTo("OK"));

    list = exec(commandObjects.functionList());
    assertThat(list, hasSize(1));
    assertThat(list.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(list.get(0).getFunctions(), hasSize(1));
    assertThat(list.get(0).getFunctions().get(0), hasEntry("name", "sumValues"));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testFunctionFlushWithMode() {
    String luaScript = "#!lua name=mylib\n" +
        "redis.register_function('sumValues', function(keys, args) return 42 end)";
    exec(commandObjects.functionLoad(luaScript));

    String libraryName = "mylib";

    List<LibraryInfo> list = exec(commandObjects.functionList());

    assertThat(list, hasSize(1));
    assertThat(list.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(list.get(0).getFunctions(), hasSize(1));
    assertThat(list.get(0).getFunctions().get(0), hasEntry("name", "sumValues"));

    String flush = exec(commandObjects.functionFlush(FlushMode.SYNC));
    assertThat(flush, equalTo("OK"));

    list = exec(commandObjects.functionList());
    assertThat(list, empty());
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testFunctionKill() {
    JedisException e = assertThrows(JedisException.class,
        () -> exec(commandObjects.functionKill()));
    assertThat(e.getMessage(), containsString("No scripts in execution right now"));
  }
}
