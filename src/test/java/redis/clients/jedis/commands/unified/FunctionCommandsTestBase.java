package redis.clients.jedis.commands.unified;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.FlushMode;
import redis.clients.jedis.args.FunctionRestorePolicy;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.resps.FunctionStats;
import redis.clients.jedis.resps.LibraryInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public abstract class FunctionCommandsTestBase extends UnifiedJedisCommandsTestBase {
  final String libraryName = "mylib";
  final String TEST_LUA_SCRIPT_TMPL = "#!lua name=%s\n"
      + "redis.register_function('%s', function(keys, args) return %s end)";

  private String functionName;

  public FunctionCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  protected void setUpFunctions(TestInfo info) {
    functionName = info.getDisplayName().replaceAll("[^a-zA-Z0-9]", "_");
    jedis.functionLoad(String.format(TEST_LUA_SCRIPT_TMPL, libraryName, functionName, "42"));
  }

  protected void cleanUpFunctions() {
    try {
      jedis.functionDelete(libraryName);
    } catch (JedisException e) {
      // ignore
    }
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testFunctionDeletion() {
    List<LibraryInfo> listResponse = jedis.functionList();

    assertThat(listResponse, hasSize(1));
    assertThat(listResponse.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(listResponse.get(0).getFunctions(), hasSize(1));
    assertThat(listResponse.get(0).getFunctions().get(0), hasEntry("name", functionName));

    String delete = jedis.functionDelete(libraryName);
    assertThat(delete, equalTo("OK"));

    listResponse = jedis.functionList();
    assertThat(listResponse, empty());
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testFunctionDeletionBinary() {
    List<LibraryInfo> listResponse = jedis.functionList();

    assertThat(listResponse, hasSize(1));
    assertThat(listResponse.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(listResponse.get(0).getFunctions(), hasSize(1));
    assertThat(listResponse.get(0).getFunctions().get(0), hasEntry("name", functionName));

    String deleteBinary = jedis.functionDelete(libraryName.getBytes());
    assertThat(deleteBinary, equalTo("OK"));

    listResponse = jedis.functionList();
    assertThat(listResponse, empty());
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testFunctionListing() {

    List<LibraryInfo> list = jedis.functionList();

    assertThat(list, hasSize(1));
    assertThat(list.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(list.get(0).getFunctions(), hasSize(1));
    assertThat(list.get(0).getFunctions().get(0), hasEntry("name", functionName));
    assertThat(list.get(0).getLibraryCode(), nullValue());

    List<Object> listBinary = jedis.functionListBinary();

    assertThat(listBinary, hasSize(1));

    List<LibraryInfo> listLibrary = jedis.functionList(libraryName);

    assertThat(listLibrary, hasSize(1));
    assertThat(listLibrary.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(listLibrary.get(0).getFunctions(), hasSize(1));
    assertThat(listLibrary.get(0).getFunctions().get(0), hasEntry("name", functionName));
    assertThat(listLibrary.get(0).getLibraryCode(), nullValue());

    List<Object> listLibraryBinary = jedis.functionList(libraryName.getBytes());

    assertThat(listLibraryBinary, hasSize(1));

    List<LibraryInfo> listWithCode = jedis.functionListWithCode();

    assertThat(listWithCode, hasSize(1));
    assertThat(listWithCode.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(listWithCode.get(0).getFunctions(), hasSize(1));
    assertThat(listWithCode.get(0).getFunctions().get(0), hasEntry("name", functionName));
    assertThat(listWithCode.get(0).getLibraryCode(), notNullValue());

    List<Object> listWithCodeBinary = jedis.functionListWithCodeBinary();

    assertThat(listWithCodeBinary, hasSize(1));

    List<LibraryInfo> listWithCodeLibrary = jedis.functionListWithCode(libraryName);

    assertThat(listWithCodeLibrary, hasSize(1));
    assertThat(listWithCodeLibrary.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(listWithCodeLibrary.get(0).getFunctions(), hasSize(1));
    assertThat(listWithCodeLibrary.get(0).getFunctions().get(0), hasEntry("name", functionName));
    assertThat(listWithCodeLibrary.get(0).getLibraryCode(), notNullValue());

    List<Object> listWithCodeLibraryBinary = jedis.functionListWithCode(libraryName.getBytes());

    assertThat(listWithCodeLibraryBinary, hasSize(1));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testFunctionReload() {
    Object result = jedis.fcall(functionName.getBytes(), new ArrayList<>(), new ArrayList<>());
    assertThat(result, equalTo(42L));

    String luaScriptChanged = String.format(TEST_LUA_SCRIPT_TMPL, libraryName, functionName, "52");
    String replaceResult = jedis.functionLoadReplace(luaScriptChanged);
    assertThat(replaceResult, equalTo("mylib"));

    Object resultAfter = jedis.fcall(functionName.getBytes(), new ArrayList<>(), new ArrayList<>());
    assertThat(resultAfter, equalTo(52L));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testFunctionReloadBinary() {
    Object result = jedis.fcall(functionName.getBytes(), new ArrayList<>(), new ArrayList<>());
    assertThat(result, equalTo(42L));

    String luaScriptChanged = String.format(TEST_LUA_SCRIPT_TMPL, libraryName, functionName, "52");
    String replaceResult = jedis.functionLoadReplace(luaScriptChanged.getBytes());
    assertThat(replaceResult, equalTo("mylib"));

    Object resultAfter = jedis.fcall(functionName.getBytes(), new ArrayList<>(), new ArrayList<>());
    assertThat(resultAfter, equalTo(52L));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testFunctionStats() {

    for (int i = 0; i < 5; i++) {
      Object result = jedis.fcall(functionName.getBytes(), new ArrayList<>(), new ArrayList<>());
      assertThat(result, equalTo(42L));
    }

    FunctionStats stats = jedis.functionStats();

    assertThat(stats, notNullValue());
    assertThat(stats.getEngines(), hasKey("LUA"));
    Map<String, Object> luaStats = stats.getEngines().get("LUA");
    assertThat(luaStats, hasEntry("libraries_count", 1L));
    assertThat(luaStats, hasEntry("functions_count", 1L));

    Object statsBinary = jedis.functionStatsBinary();

    assertThat(statsBinary, notNullValue());
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testFunctionDumpFlushRestore() {

    List<LibraryInfo> list = jedis.functionList();
    assertThat(list, hasSize(1));
    assertThat(list.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(list.get(0).getFunctions(), hasSize(1));
    assertThat(list.get(0).getFunctions().get(0), hasEntry("name", functionName));

    byte[] dump = jedis.functionDump();
    assertThat(dump, notNullValue());

    String flush = jedis.functionFlush();
    assertThat(flush, equalTo("OK"));

    list = jedis.functionList();
    assertThat(list, empty());

    String restore = jedis.functionRestore(dump);
    assertThat(restore, equalTo("OK"));

    list = jedis.functionList();
    assertThat(list, hasSize(1));
    assertThat(list.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(list.get(0).getFunctions(), hasSize(1));
    assertThat(list.get(0).getFunctions().get(0), hasEntry("name", functionName));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testFunctionDumpFlushRestoreWithPolicy() {

    List<LibraryInfo> list = jedis.functionList();
    assertThat(list, hasSize(1));
    assertThat(list.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(list.get(0).getFunctions(), hasSize(1));
    assertThat(list.get(0).getFunctions().get(0), hasEntry("name", functionName));

    byte[] dump = jedis.functionDump();
    assertThat(dump, notNullValue());

    String flush = jedis.functionFlush();
    assertThat(flush, equalTo("OK"));

    list = jedis.functionList();
    assertThat(list, empty());

    String restore = jedis.functionRestore(dump, FunctionRestorePolicy.REPLACE);
    assertThat(restore, equalTo("OK"));

    list = jedis.functionList();
    assertThat(list, hasSize(1));
    assertThat(list.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(list.get(0).getFunctions(), hasSize(1));
    assertThat(list.get(0).getFunctions().get(0), hasEntry("name", functionName));
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testFunctionFlushWithMode() {

    List<LibraryInfo> list = jedis.functionList();

    assertThat(list, hasSize(1));
    assertThat(list.get(0).getLibraryName(), equalTo(libraryName));
    assertThat(list.get(0).getFunctions(), hasSize(1));
    assertThat(list.get(0).getFunctions().get(0), hasEntry("name", functionName));

    String flush = jedis.functionFlush(FlushMode.SYNC);
    assertThat(flush, equalTo("OK"));

    list = jedis.functionList();
    assertThat(list, empty());
  }

  @Test
  @SinceRedisVersion(value = "7.0.0")
  public void testFunctionKill() {
    JedisException e = assertThrows(JedisException.class, () -> jedis.functionKill());
    assertThat(e.getMessage(), containsString("No scripts in execution right now"));
  }
}
