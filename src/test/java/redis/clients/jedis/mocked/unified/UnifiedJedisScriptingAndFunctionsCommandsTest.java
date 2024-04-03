package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import redis.clients.jedis.args.FlushMode;
import redis.clients.jedis.args.FunctionRestorePolicy;
import redis.clients.jedis.resps.FunctionStats;
import redis.clients.jedis.resps.LibraryInfo;

public class UnifiedJedisScriptingAndFunctionsCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testEval() {
    String script = "return 1";
    Object expectedEvalResult = 1;

    when(commandObjects.eval(script)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedEvalResult);

    Object result = jedis.eval(script);

    assertThat(result, equalTo(expectedEvalResult));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).eval(script);
  }

  @Test
  public void testEvalBinary() {
    byte[] script = "return 1".getBytes();
    Object expectedEvalResult = 1;

    when(commandObjects.eval(script)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedEvalResult);

    Object result = jedis.eval(script);

    assertThat(result, equalTo(expectedEvalResult));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).eval(script);
  }

  @Test
  public void testEvalWithParams() {
    String script = "return KEYS[1]";
    int keyCount = 1;
    String[] params = { "key1" };
    Object expectedEvalResult = "key1";

    when(commandObjects.eval(script, keyCount, params)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedEvalResult);

    Object result = jedis.eval(script, keyCount, params);

    assertThat(result, equalTo(expectedEvalResult));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).eval(script, keyCount, params);
  }

  @Test
  public void testEvalWithParamsBinary() {
    byte[] script = "return KEYS[1]".getBytes();
    int keyCount = 1;
    byte[][] params = { "key1".getBytes() };
    Object expectedEvalResult = "key1".getBytes();

    when(commandObjects.eval(script, keyCount, params)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedEvalResult);

    Object result = jedis.eval(script, keyCount, params);

    assertThat(result, equalTo(expectedEvalResult));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).eval(script, keyCount, params);
  }

  @Test
  public void testEvalWithLists() {
    String script = "return KEYS[1]";
    List<String> keys = Collections.singletonList("key1");
    List<String> args = Collections.emptyList();
    Object expectedEvalResult = "key1";

    when(commandObjects.eval(script, keys, args)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedEvalResult);

    Object result = jedis.eval(script, keys, args);

    assertThat(result, equalTo(expectedEvalResult));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).eval(script, keys, args);
  }

  @Test
  public void testEvalWithListsBinary() {
    byte[] script = "return KEYS[1]".getBytes();
    List<byte[]> keys = Collections.singletonList("key1".getBytes());
    List<byte[]> args = Collections.emptyList();
    Object expectedEvalResult = "key1".getBytes();

    when(commandObjects.eval(script, keys, args)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedEvalResult);

    Object result = jedis.eval(script, keys, args);

    assertThat(result, equalTo(expectedEvalResult));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).eval(script, keys, args);
  }

  @Test
  public void testEvalWithSampleKey() {
    String script = "return redis.call('get', KEYS[1])";
    String sampleKey = "myKey";
    Object expectedResponse = "value";

    when(commandObjects.eval(script, sampleKey)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedResponse);

    Object result = jedis.eval(script, sampleKey);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).eval(script, sampleKey);
  }

  @Test
  public void testEvalWithSampleKeyBinary() {
    byte[] script = "return redis.call('get', KEYS[1])".getBytes();
    byte[] sampleKey = "myKey".getBytes();
    Object expectedResponse = "value".getBytes();

    when(commandObjects.eval(script, sampleKey)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedResponse);

    Object result = jedis.eval(script, sampleKey);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).eval(script, sampleKey);
  }

  @Test
  public void testEvalReadonly() {
    String script = "return KEYS[1]";
    List<String> keys = Collections.singletonList("key1");
    List<String> args = Collections.emptyList();
    Object expectedEvalResult = "key1";

    when(commandObjects.evalReadonly(script, keys, args)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedEvalResult);

    Object result = jedis.evalReadonly(script, keys, args);

    assertThat(result, equalTo(expectedEvalResult));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).evalReadonly(script, keys, args);
  }

  @Test
  public void testEvalReadonlyBinary() {
    byte[] script = "return KEYS[1]".getBytes();
    List<byte[]> keys = Collections.singletonList("key1".getBytes());
    List<byte[]> args = Collections.emptyList();
    Object expectedEvalResult = "key1".getBytes();

    when(commandObjects.evalReadonly(script, keys, args)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedEvalResult);

    Object result = jedis.evalReadonly(script, keys, args);

    assertThat(result, equalTo(expectedEvalResult));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).evalReadonly(script, keys, args);
  }

  @Test
  public void testEvalsha() {
    String sha1 = "someSha1Hash";
    Object expectedEvalshaResult = 1;

    when(commandObjects.evalsha(sha1)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedEvalshaResult);

    Object result = jedis.evalsha(sha1);

    assertThat(result, equalTo(expectedEvalshaResult));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).evalsha(sha1);
  }

  @Test
  public void testEvalshaBinary() {
    byte[] sha1 = "someSha1Hash".getBytes();
    Object expectedEvalshaResult = 1;

    when(commandObjects.evalsha(sha1)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedEvalshaResult);

    Object result = jedis.evalsha(sha1);

    assertThat(result, equalTo(expectedEvalshaResult));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).evalsha(sha1);
  }

  @Test
  public void testEvalshaWithParams() {
    String sha1 = "someSha1Hash";
    int keyCount = 1;
    String[] params = { "key1" };
    Object expectedEvalshaResult = "key1";

    when(commandObjects.evalsha(sha1, keyCount, params)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedEvalshaResult);

    Object result = jedis.evalsha(sha1, keyCount, params);

    assertThat(result, equalTo(expectedEvalshaResult));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).evalsha(sha1, keyCount, params);
  }

  @Test
  public void testEvalshaWithParamsBinary() {
    byte[] sha1 = "someSha1Hash".getBytes();
    int keyCount = 1;
    byte[][] params = { "key1".getBytes() };
    Object expectedEvalshaResult = "key1".getBytes();

    when(commandObjects.evalsha(sha1, keyCount, params)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedEvalshaResult);

    Object result = jedis.evalsha(sha1, keyCount, params);

    assertThat(result, equalTo(expectedEvalshaResult));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).evalsha(sha1, keyCount, params);
  }

  @Test
  public void testEvalshaWithLists() {
    String sha1 = "someSha1Hash";
    List<String> keys = Collections.singletonList("key1");
    List<String> args = Collections.emptyList();
    Object expectedEvalshaResult = "key1";

    when(commandObjects.evalsha(sha1, keys, args)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedEvalshaResult);

    Object result = jedis.evalsha(sha1, keys, args);

    assertThat(result, equalTo(expectedEvalshaResult));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).evalsha(sha1, keys, args);
  }

  @Test
  public void testEvalshaWithListsBinary() {
    byte[] sha1 = "someSha1Hash".getBytes();
    List<byte[]> keys = Collections.singletonList("key1".getBytes());
    List<byte[]> args = Collections.emptyList();
    Object expectedEvalshaResult = "key1".getBytes();

    when(commandObjects.evalsha(sha1, keys, args)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedEvalshaResult);

    Object result = jedis.evalsha(sha1, keys, args);

    assertThat(result, equalTo(expectedEvalshaResult));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).evalsha(sha1, keys, args);
  }

  @Test
  public void testEvalshaWithSampleKey() {
    String sha1 = "someSha1Hash";
    String sampleKey = "myKey";
    Object expectedResponse = "value";

    when(commandObjects.evalsha(sha1, sampleKey)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedResponse);

    Object result = jedis.evalsha(sha1, sampleKey);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).evalsha(sha1, sampleKey);
  }

  @Test
  public void testEvalshaWithSampleKeyBinary() {
    byte[] sha1 = "someSha1Hash".getBytes();
    byte[] sampleKey = "myKey".getBytes();
    Object expectedResponse = "value".getBytes();

    when(commandObjects.evalsha(sha1, sampleKey)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedResponse);

    Object result = jedis.evalsha(sha1, sampleKey);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).evalsha(sha1, sampleKey);
  }

  @Test
  public void testEvalshaReadonly() {
    String sha1 = "someSha1Hash";
    List<String> keys = Collections.singletonList("key1");
    List<String> args = Collections.emptyList();
    Object expectedEvalshaResult = "key1";

    when(commandObjects.evalshaReadonly(sha1, keys, args)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedEvalshaResult);

    Object result = jedis.evalshaReadonly(sha1, keys, args);

    assertThat(result, equalTo(expectedEvalshaResult));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).evalshaReadonly(sha1, keys, args);
  }

  @Test
  public void testEvalshaReadonlyBinary() {
    byte[] sha1 = "someSha1Hash".getBytes();
    List<byte[]> keys = Collections.singletonList("key1".getBytes());
    List<byte[]> args = Collections.emptyList();
    Object expectedEvalshaResult = "key1".getBytes();

    when(commandObjects.evalshaReadonly(sha1, keys, args)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedEvalshaResult);

    Object result = jedis.evalshaReadonly(sha1, keys, args);

    assertThat(result, equalTo(expectedEvalshaResult));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).evalshaReadonly(sha1, keys, args);
  }

  @Test
  public void testFcall() {
    String name = "myFunction";
    List<String> keys = Collections.singletonList("key1");
    List<String> args = Collections.singletonList("arg1");
    Object expectedFcallResult = "someResult";

    when(commandObjects.fcall(name, keys, args)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedFcallResult);

    Object result = jedis.fcall(name, keys, args);

    assertThat(result, equalTo(expectedFcallResult));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).fcall(name, keys, args);
  }

  @Test
  public void testFcallBinary() {
    byte[] name = "myFunction".getBytes();
    List<byte[]> keys = Collections.singletonList("key1".getBytes());
    List<byte[]> args = Collections.singletonList("arg1".getBytes());
    Object expectedFcallResult = "someResult".getBytes();

    when(commandObjects.fcall(name, keys, args)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedFcallResult);

    Object result = jedis.fcall(name, keys, args);

    assertThat(result, equalTo(expectedFcallResult));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).fcall(name, keys, args);
  }

  @Test
  public void testFcallReadonly() {
    String name = "myFunction";
    List<String> keys = Collections.singletonList("key1");
    List<String> args = Collections.singletonList("arg1");
    Object expectedFcallResult = "someResult";

    when(commandObjects.fcallReadonly(name, keys, args)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedFcallResult);

    Object result = jedis.fcallReadonly(name, keys, args);

    assertThat(result, equalTo(expectedFcallResult));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).fcallReadonly(name, keys, args);
  }

  @Test
  public void testFcallReadonlyBinary() {
    byte[] name = "myFunction".getBytes();
    List<byte[]> keys = Collections.singletonList("key1".getBytes());
    List<byte[]> args = Collections.singletonList("arg1".getBytes());
    Object expectedFcallResult = "someResult".getBytes();

    when(commandObjects.fcallReadonly(name, keys, args)).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedFcallResult);

    Object result = jedis.fcallReadonly(name, keys, args);

    assertThat(result, equalTo(expectedFcallResult));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).fcallReadonly(name, keys, args);
  }

  @Test
  public void testFunctionDelete() {
    String libraryName = "mylib";
    String expectedResponse = "OK";

    when(commandObjects.functionDelete(libraryName)).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.functionDelete(libraryName);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).functionDelete(libraryName);
  }

  @Test
  public void testFunctionDeleteBinary() {
    byte[] libraryName = "mylib".getBytes();
    String expectedResponse = "OK";

    when(commandObjects.functionDelete(libraryName)).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.functionDelete(libraryName);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).functionDelete(libraryName);
  }

  @Test
  public void testFunctionDump() {
    byte[] expectedDump = "someSerializedData".getBytes();

    when(commandObjects.functionDump()).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedDump);

    byte[] result = jedis.functionDump();

    assertThat(result, equalTo(expectedDump));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).functionDump();
  }

  @Test
  public void testFunctionFlush() {
    String expectedResponse = "OK";

    when(commandObjects.functionFlush()).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.functionFlush();

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).functionFlush();
  }

  @Test
  public void testFunctionFlushWithMode() {
    FlushMode mode = FlushMode.ASYNC;
    String expectedResponse = "OK";

    when(commandObjects.functionFlush(mode)).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.functionFlush(mode);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).functionFlush(mode);
  }

  @Test
  public void testFunctionKill() {
    String expectedResponse = "OK";

    when(commandObjects.functionKill()).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.functionKill();

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).functionKill();
  }

  @Test
  public void testFunctionList() {
    List<LibraryInfo> expectedLibraryInfoList = new ArrayList<>();

    when(commandObjects.functionList()).thenReturn(listLibraryInfoCommandObject);
    when(commandExecutor.executeCommand(listLibraryInfoCommandObject)).thenReturn(expectedLibraryInfoList);

    List<LibraryInfo> result = jedis.functionList();

    assertThat(result, equalTo(expectedLibraryInfoList));

    verify(commandExecutor).executeCommand(listLibraryInfoCommandObject);
    verify(commandObjects).functionList();
  }

  @Test
  public void testFunctionListBinary() {
    List<Object> expectedFunctionListBinary = new ArrayList<>();

    when(commandObjects.functionListBinary()).thenReturn(listObjectCommandObject);
    when(commandExecutor.executeCommand(listObjectCommandObject)).thenReturn(expectedFunctionListBinary);

    List<Object> result = jedis.functionListBinary();

    assertThat(result, equalTo(expectedFunctionListBinary));

    verify(commandExecutor).executeCommand(listObjectCommandObject);
    verify(commandObjects).functionListBinary();
  }

  @Test
  public void testFunctionListWithPattern() {
    String libraryNamePattern = "mylib*";
    List<LibraryInfo> expectedLibraryInfoList = new ArrayList<>();

    when(commandObjects.functionList(libraryNamePattern)).thenReturn(listLibraryInfoCommandObject);
    when(commandExecutor.executeCommand(listLibraryInfoCommandObject)).thenReturn(expectedLibraryInfoList);

    List<LibraryInfo> result = jedis.functionList(libraryNamePattern);

    assertThat(result, equalTo(expectedLibraryInfoList));

    verify(commandExecutor).executeCommand(listLibraryInfoCommandObject);
    verify(commandObjects).functionList(libraryNamePattern);
  }

  @Test
  public void testFunctionListWithPatternBinary() {
    byte[] libraryNamePattern = "mylib*".getBytes();
    List<Object> expectedFunctionList = new ArrayList<>();

    when(commandObjects.functionList(libraryNamePattern)).thenReturn(listObjectCommandObject);
    when(commandExecutor.executeCommand(listObjectCommandObject)).thenReturn(expectedFunctionList);

    List<Object> result = jedis.functionList(libraryNamePattern);

    assertThat(result, equalTo(expectedFunctionList));

    verify(commandExecutor).executeCommand(listObjectCommandObject);
    verify(commandObjects).functionList(libraryNamePattern);
  }

  @Test
  public void testFunctionListWithCode() {
    List<LibraryInfo> expectedLibraryInfoList = new ArrayList<>();

    when(commandObjects.functionListWithCode()).thenReturn(listLibraryInfoCommandObject);
    when(commandExecutor.executeCommand(listLibraryInfoCommandObject)).thenReturn(expectedLibraryInfoList);

    List<LibraryInfo> result = jedis.functionListWithCode();

    assertThat(result, equalTo(expectedLibraryInfoList));

    verify(commandExecutor).executeCommand(listLibraryInfoCommandObject);
    verify(commandObjects).functionListWithCode();
  }

  @Test
  public void testFunctionListWithCodeBinary() {
    List<Object> expectedFunctionListWithCodeBinary = new ArrayList<>();

    when(commandObjects.functionListWithCodeBinary()).thenReturn(listObjectCommandObject);
    when(commandExecutor.executeCommand(listObjectCommandObject)).thenReturn(expectedFunctionListWithCodeBinary);

    List<Object> result = jedis.functionListWithCodeBinary();

    assertThat(result, equalTo(expectedFunctionListWithCodeBinary));

    verify(commandExecutor).executeCommand(listObjectCommandObject);
    verify(commandObjects).functionListWithCodeBinary();
  }

  @Test
  public void testFunctionListWithCodeAndPattern() {
    String libraryNamePattern = "mylib*";
    List<LibraryInfo> expectedLibraryInfoList = new ArrayList<>();

    when(commandObjects.functionListWithCode(libraryNamePattern)).thenReturn(listLibraryInfoCommandObject);
    when(commandExecutor.executeCommand(listLibraryInfoCommandObject)).thenReturn(expectedLibraryInfoList);

    List<LibraryInfo> result = jedis.functionListWithCode(libraryNamePattern);

    assertThat(result, equalTo(expectedLibraryInfoList));

    verify(commandExecutor).executeCommand(listLibraryInfoCommandObject);
    verify(commandObjects).functionListWithCode(libraryNamePattern);
  }

  @Test
  public void testFunctionListWithCodeAndPatternBinary() {
    byte[] libraryNamePattern = "mylib*".getBytes();
    List<Object> expectedFunctionListWithCode = new ArrayList<>();

    when(commandObjects.functionListWithCode(libraryNamePattern)).thenReturn(listObjectCommandObject);
    when(commandExecutor.executeCommand(listObjectCommandObject)).thenReturn(expectedFunctionListWithCode);

    List<Object> result = jedis.functionListWithCode(libraryNamePattern);

    assertThat(result, equalTo(expectedFunctionListWithCode));

    verify(commandExecutor).executeCommand(listObjectCommandObject);
    verify(commandObjects).functionListWithCode(libraryNamePattern);
  }

  @Test
  public void testFunctionLoad() {
    String functionCode = "function myfunc() return 'hello' end";
    String expectedResponse = "OK";

    when(commandObjects.functionLoad(functionCode)).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.functionLoad(functionCode);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).functionLoad(functionCode);
  }

  @Test
  public void testFunctionLoadWithBinary() {
    byte[] functionCode = "function myfunc() return 'hello' end".getBytes();
    String expectedResponse = "OK";

    when(commandObjects.functionLoad(functionCode)).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.functionLoad(functionCode);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).functionLoad(functionCode);
  }

  @Test
  public void testFunctionLoadReplace() {
    String functionCode = "function myfunc() return 'hello' end";
    String expectedResponse = "OK";

    when(commandObjects.functionLoadReplace(functionCode)).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.functionLoadReplace(functionCode);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).functionLoadReplace(functionCode);
  }

  @Test
  public void testFunctionLoadReplaceBinary() {
    byte[] functionCode = "function myfunc() return 'hello' end".getBytes();
    String expectedResponse = "OK";

    when(commandObjects.functionLoadReplace(functionCode)).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.functionLoadReplace(functionCode);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).functionLoadReplace(functionCode);
  }

  @Test
  public void testFunctionRestore() {
    byte[] serializedValue = "serializedData".getBytes();
    String expectedResponse = "OK";

    when(commandObjects.functionRestore(serializedValue)).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.functionRestore(serializedValue);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).functionRestore(serializedValue);
  }

  @Test
  public void testFunctionRestoreWithPolicy() {
    byte[] serializedValue = "serializedData".getBytes();
    FunctionRestorePolicy policy = FunctionRestorePolicy.FLUSH;
    String expectedResponse = "OK";

    when(commandObjects.functionRestore(serializedValue, policy)).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.functionRestore(serializedValue, policy);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).functionRestore(serializedValue, policy);
  }

  @Test
  public void testFunctionStats() {
    FunctionStats expectedFunctionStats = mock(FunctionStats.class);

    when(commandObjects.functionStats()).thenReturn(functionStatsCommandObject);
    when(commandExecutor.executeCommand(functionStatsCommandObject)).thenReturn(expectedFunctionStats);

    FunctionStats result = jedis.functionStats();

    assertThat(result, sameInstance(expectedFunctionStats));

    verify(commandExecutor).executeCommand(functionStatsCommandObject);
    verify(commandObjects).functionStats();
  }

  @Test
  public void testFunctionStatsBinary() {
    Object expectedFunctionStatsBinary = new Object();

    when(commandObjects.functionStatsBinary()).thenReturn(objectCommandObject);
    when(commandExecutor.executeCommand(objectCommandObject)).thenReturn(expectedFunctionStatsBinary);

    Object result = jedis.functionStatsBinary();

    assertThat(result, equalTo(expectedFunctionStatsBinary));

    verify(commandExecutor).executeCommand(objectCommandObject);
    verify(commandObjects).functionStatsBinary();
  }

  @Test
  public void testScriptExistsWithSha1s() {
    List<String> sha1s = Arrays.asList("sha1One", "sha1Two");
    List<Boolean> expectedResponse = Arrays.asList(true, false);

    when(commandObjects.scriptExists(sha1s)).thenReturn(listBooleanCommandObject);
    when(commandExecutor.broadcastCommand(listBooleanCommandObject)).thenReturn(expectedResponse);

    List<Boolean> result = jedis.scriptExists(sha1s);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(listBooleanCommandObject);
    verify(commandObjects).scriptExists(sha1s);
  }

  @Test
  public void testScriptExistsWithSha1AndSampleKey() {
    String sha1 = "someSha1Hash";
    String sampleKey = "myKey";
    Boolean expectedResponse = true;

    when(commandObjects.scriptExists(sampleKey, sha1)).thenReturn(listBooleanCommandObject);
    when(commandExecutor.executeCommand(listBooleanCommandObject)).thenReturn(Collections.singletonList(expectedResponse));

    Boolean result = jedis.scriptExists(sha1, sampleKey);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(listBooleanCommandObject);
    verify(commandObjects).scriptExists(sampleKey, sha1);
  }

  @Test
  public void testScriptExistsWithSha1AndSampleKeyBinary() {
    byte[] sha1 = "someSha1Hash".getBytes();
    byte[] sampleKey = "myKey".getBytes();
    Boolean expectedResponse = true;

    when(commandObjects.scriptExists(sampleKey, new byte[][]{ sha1 })).thenReturn(listBooleanCommandObject);
    when(commandExecutor.executeCommand(listBooleanCommandObject)).thenReturn(Collections.singletonList(expectedResponse));

    Boolean result = jedis.scriptExists(sha1, sampleKey);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(listBooleanCommandObject);
    verify(commandObjects).scriptExists(sampleKey, new byte[][]{ sha1 });
  }

  @Test
  public void testScriptExistsWithKeyAndSha1s() {
    String sampleKey = "myKey";
    String[] sha1s = { "sha1One", "sha1Two" };
    List<Boolean> expectedResponse = Arrays.asList(true, false);

    when(commandObjects.scriptExists(sampleKey, sha1s)).thenReturn(listBooleanCommandObject);
    when(commandExecutor.executeCommand(listBooleanCommandObject)).thenReturn(expectedResponse);

    List<Boolean> result = jedis.scriptExists(sampleKey, sha1s);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(listBooleanCommandObject);
    verify(commandObjects).scriptExists(sampleKey, sha1s);
  }

  @Test
  public void testScriptExistsWithKeyAndSha1sBinary() {
    byte[] sampleKey = "myKey".getBytes();
    byte[][] sha1s = { "sha1One".getBytes(), "sha1Two".getBytes() };
    List<Boolean> expectedResponse = Arrays.asList(true, false);

    when(commandObjects.scriptExists(sampleKey, sha1s)).thenReturn(listBooleanCommandObject);
    when(commandExecutor.executeCommand(listBooleanCommandObject)).thenReturn(expectedResponse);

    List<Boolean> result = jedis.scriptExists(sampleKey, sha1s);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(listBooleanCommandObject);
    verify(commandObjects).scriptExists(sampleKey, sha1s);
  }

  @Test
  public void testScriptFlushWithoutKey() {
    String expectedResponse = "OK";

    when(commandObjects.scriptFlush()).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.scriptFlush();

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).scriptFlush();
  }

  @Test
  public void testScriptFlush() {
    String sampleKey = "myKey";
    String expectedResponse = "OK";

    when(commandObjects.scriptFlush(sampleKey)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.scriptFlush(sampleKey);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).scriptFlush(sampleKey);
  }

  @Test
  public void testScriptFlushBinary() {
    byte[] sampleKey = "myKey".getBytes();
    String expectedResponse = "OK";

    when(commandObjects.scriptFlush(sampleKey)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.scriptFlush(sampleKey);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).scriptFlush(sampleKey);
  }

  @Test
  public void testScriptFlushWithMode() {
    String sampleKey = "myKey";
    FlushMode flushMode = FlushMode.SYNC;
    String expectedResponse = "OK";

    when(commandObjects.scriptFlush(sampleKey, flushMode)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.scriptFlush(sampleKey, flushMode);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).scriptFlush(sampleKey, flushMode);
  }

  @Test
  public void testScriptFlushWithModeBinary() {
    byte[] sampleKey = "myKey".getBytes();
    FlushMode flushMode = FlushMode.SYNC;
    String expectedResponse = "OK";

    when(commandObjects.scriptFlush(sampleKey, flushMode)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.scriptFlush(sampleKey, flushMode);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).scriptFlush(sampleKey, flushMode);
  }

  @Test
  public void testScriptKillWithoutKey() {
    String expectedResponse = "OK";

    when(commandObjects.scriptKill()).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.scriptKill();

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).scriptKill();
  }

  @Test
  public void testScriptKill() {
    String sampleKey = "myKey";
    String expectedResponse = "OK";

    when(commandObjects.scriptKill(sampleKey)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.scriptKill(sampleKey);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).scriptKill(sampleKey);
  }

  @Test
  public void testScriptKillBinary() {
    byte[] sampleKey = "myKey".getBytes();
    String expectedResponse = "OK";

    when(commandObjects.scriptKill(sampleKey)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.scriptKill(sampleKey);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).scriptKill(sampleKey);
  }

  @Test
  public void testScriptLoadWithoutKey() {
    String script = "return redis.call('get', 'constantKey')";
    String expectedSha1 = "someSha1Hash";

    when(commandObjects.scriptLoad(script)).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedSha1);

    String result = jedis.scriptLoad(script);

    assertThat(result, equalTo(expectedSha1));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).scriptLoad(script);
  }

  @Test
  public void testScriptLoad() {
    String script = "return redis.call('get', KEYS[1])";
    String sampleKey = "myKey";
    String expectedSha1 = "someSha1Hash";

    when(commandObjects.scriptLoad(script, sampleKey)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedSha1);

    String result = jedis.scriptLoad(script, sampleKey);

    assertThat(result, equalTo(expectedSha1));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).scriptLoad(script, sampleKey);
  }

  @Test
  public void testScriptLoadBinary() {
    byte[] script = "return redis.call('get', KEYS[1])".getBytes();
    byte[] sampleKey = "myKey".getBytes();
    byte[] expectedSha1 = "someSha1Hash".getBytes();

    when(commandObjects.scriptLoad(script, sampleKey)).thenReturn(bytesCommandObject);
    when(commandExecutor.executeCommand(bytesCommandObject)).thenReturn(expectedSha1);

    byte[] result = jedis.scriptLoad(script, sampleKey);

    assertThat(result, equalTo(expectedSha1));

    verify(commandExecutor).executeCommand(bytesCommandObject);
    verify(commandObjects).scriptLoad(script, sampleKey);
  }

}
