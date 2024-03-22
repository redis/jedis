package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import org.json.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import redis.clients.jedis.args.BitCountOption;
import redis.clients.jedis.args.BitOP;
import redis.clients.jedis.args.ExpiryOption;
import redis.clients.jedis.args.FlushMode;
import redis.clients.jedis.args.FunctionRestorePolicy;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.args.ListDirection;
import redis.clients.jedis.args.ListPosition;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.args.SortedSetOption;
import redis.clients.jedis.bloom.BFInsertParams;
import redis.clients.jedis.bloom.BFReserveParams;
import redis.clients.jedis.bloom.CFInsertParams;
import redis.clients.jedis.bloom.CFReserveParams;
import redis.clients.jedis.bloom.TDigestMergeParams;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.graph.GraphCommandObjects;
import redis.clients.jedis.graph.ResultSet;
import redis.clients.jedis.json.JsonObjectMapper;
import redis.clients.jedis.json.JsonSetParams;
import redis.clients.jedis.json.Path;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.params.BitPosParams;
import redis.clients.jedis.params.GeoAddParams;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.GeoRadiusStoreParam;
import redis.clients.jedis.params.GeoSearchParam;
import redis.clients.jedis.params.GetExParams;
import redis.clients.jedis.params.LCSParams;
import redis.clients.jedis.params.LPosParams;
import redis.clients.jedis.params.MigrateParams;
import redis.clients.jedis.params.RestoreParams;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.SortingParams;
import redis.clients.jedis.params.XAddParams;
import redis.clients.jedis.params.XAutoClaimParams;
import redis.clients.jedis.params.XClaimParams;
import redis.clients.jedis.params.XPendingParams;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.params.XTrimParams;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;
import redis.clients.jedis.params.ZParams;
import redis.clients.jedis.params.ZRangeParams;
import redis.clients.jedis.resps.FunctionStats;
import redis.clients.jedis.resps.GeoRadiusResponse;
import redis.clients.jedis.resps.LCSMatchResult;
import redis.clients.jedis.resps.LibraryInfo;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.StreamConsumerInfo;
import redis.clients.jedis.resps.StreamConsumersInfo;
import redis.clients.jedis.resps.StreamEntry;
import redis.clients.jedis.resps.StreamFullInfo;
import redis.clients.jedis.resps.StreamGroupInfo;
import redis.clients.jedis.resps.StreamInfo;
import redis.clients.jedis.resps.StreamPendingEntry;
import redis.clients.jedis.resps.StreamPendingSummary;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.search.FTCreateParams;
import redis.clients.jedis.search.FTSearchParams;
import redis.clients.jedis.search.FTSpellCheckParams;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.Schema;
import redis.clients.jedis.search.SearchResult;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;
import redis.clients.jedis.search.schemafields.SchemaField;
import redis.clients.jedis.search.schemafields.TextField;
import redis.clients.jedis.timeseries.AggregationType;
import redis.clients.jedis.timeseries.TSAlterParams;
import redis.clients.jedis.timeseries.TSCreateParams;
import redis.clients.jedis.timeseries.TSElement;
import redis.clients.jedis.timeseries.TSGetParams;
import redis.clients.jedis.timeseries.TSMGetElement;
import redis.clients.jedis.timeseries.TSMGetParams;
import redis.clients.jedis.timeseries.TSMRangeElements;
import redis.clients.jedis.timeseries.TSMRangeParams;
import redis.clients.jedis.timeseries.TSRangeParams;
import redis.clients.jedis.util.KeyValue;

/**
 * Exhaustive unit tests for {@link PipeliningBase}, using Mockito. Given that {@link PipeliningBase}
 * is, essentially, only requesting commands from a {@link CommandObjects} instance and sending them
 * to its subclasses via {@link PipeliningBase#appendCommand(CommandObject)}, and given that it has
 * many methods, using mocks is the most convenient and reliable way to completely test it.
 */
@RunWith(MockitoJUnitRunner.class)
public class PipeliningBaseTest {

  /**
   * Used for JSON tests.
   */
  @SuppressWarnings("unused")
  private static class MyBean {
    String field1;
    String field2;
  }

  /**
   * {@link PipeliningBase} under-test. Given that it is an abstract class, an in-place implementation
   * is used, that collects commands in a list.
   */
  private PipeliningBase pipeliningBase;

  /**
   * Accumulates commands sent by the {@link PipeliningBase} under-test to its subclass.
   */
  private final List<CommandObject<?>> commands = new ArrayList<>();

  /**
   * {@link CommandObjects} instance used by the {@link PipeliningBase} under-test. Depending on
   * the test case, it is trained to return one of the mock {@link CommandObject} instances below.
   */
  @Mock
  private CommandObjects commandObjects;

  /**
   * The {@link GraphCommandObjects} instance used by the {@link PipeliningBase} under-test.
   */
  @Mock
  private GraphCommandObjects graphCommandObjects;

  /**
   * Mock {@link Response} that is returned by {@link PipeliningBase} from the
   * {@link PipeliningBase#appendCommand(CommandObject)} method. Using such a mock makes
   * it easy to assert.
   */
  @Mock
  private Response<?> predefinedResponse;

  // Below follows a list of mocked CommandObjects, one per type. This is the cleanest way to create
  // mocks, given that CommandObject is a generic class. Using {@code Mockito.mock(...)} yields too
  // many warnings related to generics.
  // To make the code more readable, try to keep the list sorted alphabetically, and without automatic
  // reformatting.

  // @formatter:off
  @Mock private CommandObject<AggregationResult> aggregationResultCommandObject;
  @Mock private CommandObject<Boolean> booleanCommandObject;
  @Mock private CommandObject<Class<?>> classCommandObject;
  @Mock private CommandObject<Double> doubleCommandObject;
  @Mock private CommandObject<FunctionStats> functionStatsCommandObject;
  @Mock private CommandObject<KeyValue<Long, Double>> keyValueLongDoubleCommandObject;
  @Mock private CommandObject<KeyValue<Long, Long>> keyValueLongLongCommandObject;
  @Mock private CommandObject<KeyValue<String, List<String>>> keyValueStringListStringCommandObject;
  @Mock private CommandObject<KeyValue<String, List<Tuple>>> keyValueStringListTupleCommandObject;
  @Mock private CommandObject<KeyValue<String, String>> keyValueStringStringCommandObject;
  @Mock private CommandObject<KeyValue<String, Tuple>> keyValueStringTupleCommandObject;
  @Mock private CommandObject<KeyValue<byte[], List<Tuple>>> keyValueBytesListTupleCommandObject;
  @Mock private CommandObject<KeyValue<byte[], List<byte[]>>> keyValueBytesListBytesCommandObject;
  @Mock private CommandObject<KeyValue<byte[], Tuple>> keyValueBytesTupleCommandObject;
  @Mock private CommandObject<KeyValue<byte[], byte[]>> keyValueBytesBytesCommandObject;
  @Mock private CommandObject<LCSMatchResult> lcsMatchResultCommandObject;
  @Mock private CommandObject<List<Boolean>> listBooleanCommandObject;
  @Mock private CommandObject<List<Class<?>>> listClassCommandObject;
  @Mock private CommandObject<List<Double>> listDoubleCommandObject;
  @Mock private CommandObject<List<GeoCoordinate>> listGeoCoordinateCommandObject;
  @Mock private CommandObject<List<GeoRadiusResponse>> listGeoRadiusResponseCommandObject;
  @Mock private CommandObject<List<JSONArray>> listJsonArrayCommandObject;
  @Mock private CommandObject<List<LibraryInfo>> listLibraryInfoCommandObject;
  @Mock private CommandObject<List<Long>> listLongCommandObject;
  @Mock private CommandObject<List<Map.Entry<String, List<StreamEntry>>>> listEntryStringListStreamEntryCommandObject;
  @Mock private CommandObject<List<Map.Entry<String, String>>> listEntryStringStringCommandObject;
  @Mock private CommandObject<List<Map.Entry<byte[], byte[]>>> listEntryBytesBytesCommandObject;
  @Mock private CommandObject<List<MyBean>> listMyBeanCommandObject;
  @Mock private CommandObject<List<Object>> listObjectCommandObject;
  @Mock private CommandObject<List<StreamConsumerInfo>> listStreamConsumerInfoCommandObject;
  @Mock private CommandObject<List<StreamConsumersInfo>> listStreamConsumersInfoCommandObject;
  @Mock private CommandObject<List<StreamEntry>> listStreamEntryCommandObject;
  @Mock private CommandObject<List<StreamEntryID>> listStreamEntryIdCommandObject;
  @Mock private CommandObject<List<StreamGroupInfo>> listStreamGroupInfoCommandObject;
  @Mock private CommandObject<List<StreamPendingEntry>> listStreamPendingEntryCommandObject;
  @Mock private CommandObject<List<String>> listStringCommandObject;
  @Mock private CommandObject<List<TSElement>> listTsElementCommandObject;
  @Mock private CommandObject<List<Tuple>> listTupleCommandObject;
  @Mock private CommandObject<List<byte[]>> listBytesCommandObject;
  @Mock private CommandObject<Long> longCommandObject;
  @Mock private CommandObject<Map.Entry<Long, byte[]>> entryLongBytesCommandObject;
  @Mock private CommandObject<Map.Entry<StreamEntryID, List<StreamEntry>>> entryStreamEntryIdListStreamEntryCommandObject;
  @Mock private CommandObject<Map.Entry<StreamEntryID, List<StreamEntryID>>> entryStreamEntryIdListStreamEntryIdCommandObject;
  @Mock private CommandObject<Map<String, List<String>>> mapStringListStringCommandObject;
  @Mock private CommandObject<Map<String, Long>> mapStringLongCommandObject;
  @Mock private CommandObject<Map<String, Map<String, Double>>> mapStringMapStringDoubleCommandObject;
  @Mock private CommandObject<Map<String, Object>> mapStringObjectCommandObject;
  @Mock private CommandObject<Map<String, String>> mapStringStringCommandObject;
  @Mock private CommandObject<Map<String, TSMGetElement>> mapStringTsmGetElementCommandObject;
  @Mock private CommandObject<Map<String, TSMRangeElements>> mapStringTsmRangeElementsCommandObject;
  @Mock private CommandObject<Map<byte[], byte[]>> mapBytesBytesCommandObject;
  @Mock private CommandObject<MyBean> myBeanCommandObject;
  @Mock private CommandObject<Object> objectCommandObject;
  @Mock private CommandObject<ResultSet> resultSetCommandObject;
  @Mock private CommandObject<ScanResult<Map.Entry<String, String>>> scanResultEntryStringStringCommandObject;
  @Mock private CommandObject<ScanResult<Map.Entry<byte[], byte[]>>> scanResultEntryBytesBytesCommandObject;
  @Mock private CommandObject<ScanResult<String>> scanResultStringCommandObject;
  @Mock private CommandObject<ScanResult<Tuple>> scanResultTupleCommandObject;
  @Mock private CommandObject<ScanResult<byte[]>> scanResultBytesCommandObject;
  @Mock private CommandObject<SearchResult> searchResultCommandObject;
  @Mock private CommandObject<Set<String>> setStringCommandObject;
  @Mock private CommandObject<Set<byte[]>> setBytesCommandObject;
  @Mock private CommandObject<StreamEntryID> streamEntryIdCommandObject;
  @Mock private CommandObject<StreamFullInfo> streamFullInfoCommandObject;
  @Mock private CommandObject<StreamInfo> streamInfoCommandObject;
  @Mock private CommandObject<StreamPendingSummary> streamPendingSummaryCommandObject;
  @Mock private CommandObject<String> stringCommandObject;
  @Mock private CommandObject<TSElement> tsElementCommandObject;
  @Mock private CommandObject<Tuple> tupleCommandObject;
  @Mock private CommandObject<byte[]> bytesCommandObject;
  // @formatter:on

  /**
   * Prepare a concrete implementation of {@link PipeliningBase} that collects all commands
   * in a list, so that asserts can be run on the content of the list.
   * <p>
   * Most of the test methods will only test one specific method of {@link PipeliningBase},
   * so only one command will be collected.
   * <p>
   * At the end of this test class there are tests that check multiple methods of
   * {@link PipeliningBase}.
   */
  @Before
  public void setUp() {
    pipeliningBase = new PipeliningBase(commandObjects) {

      @Override
      @SuppressWarnings("unchecked")
      protected <T> Response<T> appendCommand(CommandObject<T> commandObject) {
        // Collect the command in the list.
        commands.add(commandObject);
        // Return a well known response, that can be asserted in the test cases.
        return (Response<T>) predefinedResponse;
      }
    };

    pipeliningBase.setGraphCommands(graphCommandObjects);
  }

  @Test
  public void testExists() {
    when(commandObjects.exists("key")).thenReturn(booleanCommandObject);

    Response<Boolean> result = pipeliningBase.exists("key");

    assertThat(commands, contains(booleanCommandObject));
    assertThat(result, is(predefinedResponse));
  }

  @Test
  public void testExistsMultipleKeys() {
    when(commandObjects.exists("key1", "key2", "key3")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.exists("key1", "key2", "key3");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPersist() {
    when(commandObjects.persist("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.persist("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testType() {
    when(commandObjects.type("key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.type("key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testDump() {
    when(commandObjects.dump("key")).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.dump("key");

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRestore() {
    byte[] serializedValue = new byte[]{ 1, 2, 3 };
    long ttl = 1000L;

    when(commandObjects.restore("key", ttl, serializedValue)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.restore("key", ttl, serializedValue);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRestoreWithParams() {
    byte[] serializedValue = new byte[]{ 1, 2, 3 };
    long ttl = 1000L;
    RestoreParams params = new RestoreParams();

    when(commandObjects.restore("key", ttl, serializedValue, params)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.restore("key", ttl, serializedValue, params);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExpire() {
    when(commandObjects.expire("key", 60)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.expire("key", 60);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExpireWithExpiryOption() {
    when(commandObjects.expire("key", 60, ExpiryOption.NX)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.expire("key", 60, ExpiryOption.NX);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPexpire() {
    when(commandObjects.pexpire("key", 100000)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pexpire("key", 100000);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPexpireWithExpiryOption() {
    when(commandObjects.pexpire("key", 100000, ExpiryOption.NX)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pexpire("key", 100000, ExpiryOption.NX);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExpireTime() {
    when(commandObjects.expireTime("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.expireTime("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPexpireTime() {
    when(commandObjects.pexpireTime("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pexpireTime("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExpireAt() {
    int unixTime = 1609459200;

    when(commandObjects.expireAt("key", unixTime)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.expireAt("key", unixTime);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExpireAtWithExpiryOption() {
    int unixTime = 1609459200;

    when(commandObjects.expireAt("key", unixTime, ExpiryOption.NX)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.expireAt("key", unixTime, ExpiryOption.NX);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPexpireAt() {
    long millisecondsTimestamp = 1609459200000L;

    when(commandObjects.pexpireAt("key", millisecondsTimestamp)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pexpireAt("key", millisecondsTimestamp);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPexpireAtWithExpiryOption() {
    long millisecondsTimestamp = 1609459200000L;

    when(commandObjects.pexpireAt("key", millisecondsTimestamp, ExpiryOption.NX)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pexpireAt("key", millisecondsTimestamp, ExpiryOption.NX);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTtl() {
    when(commandObjects.ttl("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.ttl("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPttl() {
    when(commandObjects.pttl("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pttl("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTouchSingleKey() {
    when(commandObjects.touch("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.touch("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTouchMultipleKeys() {
    String[] keys = { "key1", "key2", "key3" };

    when(commandObjects.touch(keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.touch(keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSortSingleKey() {
    when(commandObjects.sort("key")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.sort("key");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSortSingleKeyStore() {
    when(commandObjects.sort("key", "dstKey")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sort("key", "dstKey");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSortWithParams() {
    SortingParams sortingParams = new SortingParams();

    when(commandObjects.sort("key", sortingParams)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.sort("key", sortingParams);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSortWithParamsStore() {
    SortingParams sortingParams = new SortingParams();

    when(commandObjects.sort("key", sortingParams, "dstKey")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sort("key", sortingParams, "dstKey");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSortReadonly() {
    SortingParams sortingParams = new SortingParams();

    when(commandObjects.sortReadonly("key", sortingParams)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.sortReadonly("key", sortingParams);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testDelSingleKey() {
    when(commandObjects.del("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.del("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testDelMultipleKeys() {
    String[] keys = { "key1", "key2", "key3" };

    when(commandObjects.del(keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.del(keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testUnlinkSingleKey() {
    when(commandObjects.unlink("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.unlink("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testUnlinkMultipleKeys() {
    String[] keys = { "key1", "key2", "key3" };

    when(commandObjects.unlink(keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.unlink(keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCopy() {
    when(commandObjects.copy("srcKey", "dstKey", true)).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.copy("srcKey", "dstKey", true);

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRename() {
    when(commandObjects.rename("oldkey", "newkey")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.rename("oldkey", "newkey");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRenamenx() {
    when(commandObjects.renamenx("oldkey", "newkey")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.renamenx("oldkey", "newkey");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testMemoryUsageSingleKey() {
    when(commandObjects.memoryUsage("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.memoryUsage("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testMemoryUsageWithSamples() {
    when(commandObjects.memoryUsage("key", 10)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.memoryUsage("key", 10);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testObjectRefcount() {
    when(commandObjects.objectRefcount("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.objectRefcount("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testObjectEncoding() {
    when(commandObjects.objectEncoding("key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.objectEncoding("key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testObjectIdletime() {
    when(commandObjects.objectIdletime("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.objectIdletime("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testObjectFreq() {
    when(commandObjects.objectFreq("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.objectFreq("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testMigrateSingleKey() {
    when(commandObjects.migrate("host", 6379, "key", 5000)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.migrate("host", 6379, "key", 5000);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testMigrateMultipleKeys() {
    MigrateParams params = new MigrateParams();
    String[] keys = { "key1", "key2" };

    when(commandObjects.migrate("host", 6379, 5000, params, keys)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.migrate("host", 6379, 5000, params, keys);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testKeys() {
    when(commandObjects.keys("pattern")).thenReturn(setStringCommandObject);

    Response<Set<String>> response = pipeliningBase.keys("pattern");

    assertThat(commands, contains(setStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScan() {
    when(commandObjects.scan("0")).thenReturn(scanResultStringCommandObject);

    Response<ScanResult<String>> response = pipeliningBase.scan("0");

    assertThat(commands, contains(scanResultStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScanWithParams() {
    ScanParams scanParams = new ScanParams();

    when(commandObjects.scan("0", scanParams)).thenReturn(scanResultStringCommandObject);

    Response<ScanResult<String>> response = pipeliningBase.scan("0", scanParams);

    assertThat(commands, contains(scanResultStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScanWithType() {
    ScanParams scanParams = new ScanParams();

    when(commandObjects.scan("0", scanParams, "type")).thenReturn(scanResultStringCommandObject);

    Response<ScanResult<String>> response = pipeliningBase.scan("0", scanParams, "type");

    assertThat(commands, contains(scanResultStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRandomKey() {
    when(commandObjects.randomKey()).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.randomKey();

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGet() {
    when(commandObjects.get("key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.get("key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSetGet() {
    when(commandObjects.setGet("key", "value")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.setGet("key", "value");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSetGetWithParams() {
    SetParams setParams = new SetParams();

    when(commandObjects.setGet("key", "value", setParams)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.setGet("key", "value", setParams);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGetDel() {
    when(commandObjects.getDel("key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.getDel("key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGetEx() {
    GetExParams params = new GetExParams();

    when(commandObjects.getEx("key", params)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.getEx("key", params);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSetbit() {
    when(commandObjects.setbit("key", 100, true)).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.setbit("key", 100, true);

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGetbit() {
    when(commandObjects.getbit("key", 100)).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.getbit("key", 100);

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSetrange() {
    when(commandObjects.setrange("key", 100, "value")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.setrange("key", 100, "value");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGetrange() {
    when(commandObjects.getrange("key", 0, 100)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.getrange("key", 0, 100);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGetSet() {
    when(commandObjects.getSet("key", "value")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.getSet("key", "value");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSetnx() {
    when(commandObjects.setnx("key", "value")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.setnx("key", "value");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSetex() {
    when(commandObjects.setex("key", 60, "value")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.setex("key", 60, "value");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPsetex() {
    when(commandObjects.psetex("key", 100000, "value")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.psetex("key", 100000, "value");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testMget() {
    String[] keys = { "key1", "key2", "key3" };

    when(commandObjects.mget(keys)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.mget(keys);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testMset() {
    String[] keysvalues = { "key1", "value1", "key2", "value2" };

    when(commandObjects.mset(keysvalues)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.mset(keysvalues);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testMsetnx() {
    String[] keysvalues = { "key1", "value1", "key2", "value2" };

    when(commandObjects.msetnx(keysvalues)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.msetnx(keysvalues);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testIncr() {
    when(commandObjects.incr("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.incr("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testIncrBy() {
    when(commandObjects.incrBy("key", 10L)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.incrBy("key", 10L);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testIncrByFloat() {
    when(commandObjects.incrByFloat("key", 1.5)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.incrByFloat("key", 1.5);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testDecr() {
    when(commandObjects.decr("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.decr("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testDecrBy() {
    when(commandObjects.decrBy("key", 10L)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.decrBy("key", 10L);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testAppend() {
    when(commandObjects.append("key", "value")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.append("key", "value");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSubstr() {
    when(commandObjects.substr("key", 0, 10)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.substr("key", 0, 10);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testStrlen() {
    when(commandObjects.strlen("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.strlen("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBitcount() {
    when(commandObjects.bitcount("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.bitcount("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBitcountRange() {
    when(commandObjects.bitcount("key", 0, 10)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.bitcount("key", 0, 10);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBitcountRangeOption() {
    BitCountOption option = BitCountOption.BYTE;

    when(commandObjects.bitcount("key", 0, 10, option)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.bitcount("key", 0, 10, option);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBitpos() {
    when(commandObjects.bitpos("key", true)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.bitpos("key", true);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBitposParams() {
    BitPosParams params = new BitPosParams(0, -1);

    when(commandObjects.bitpos("key", true, params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.bitpos("key", true, params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBitfield() {
    String[] arguments = { "INCRBY", "mykey", "2", "1" };

    when(commandObjects.bitfield("key", arguments)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.bitfield("key", arguments);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBitfieldReadonly() {
    String[] arguments = { "GET", "u4", "0" };

    when(commandObjects.bitfieldReadonly("key", arguments)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.bitfieldReadonly("key", arguments);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBitop() {
    BitOP op = BitOP.AND;

    when(commandObjects.bitop(op, "destKey", "srckey1", "srckey2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.bitop(op, "destKey", "srckey1", "srckey2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLcs() {
    LCSParams params = new LCSParams();

    when(commandObjects.lcs("keyA", "keyB", params)).thenReturn(lcsMatchResultCommandObject);

    Response<LCSMatchResult> response = pipeliningBase.lcs("keyA", "keyB", params);

    assertThat(commands, contains(lcsMatchResultCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSet() {
    when(commandObjects.set("key", "value")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.set("key", "value");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSetWithParams() {
    SetParams params = new SetParams();

    when(commandObjects.set("key", "value", params)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.set("key", "value", params);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRpush() {
    when(commandObjects.rpush("key", "value1", "value2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.rpush("key", "value1", "value2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLpush() {
    when(commandObjects.lpush("key", "value1", "value2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.lpush("key", "value1", "value2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLlen() {
    when(commandObjects.llen("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.llen("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLrange() {
    when(commandObjects.lrange("key", 0, -1)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.lrange("key", 0, -1);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLtrim() {
    when(commandObjects.ltrim("key", 1, -1)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ltrim("key", 1, -1);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLindex() {
    when(commandObjects.lindex("key", 1)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.lindex("key", 1);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLset() {
    when(commandObjects.lset("key", 1, "value")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.lset("key", 1, "value");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLrem() {
    when(commandObjects.lrem("key", 2, "value")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.lrem("key", 2, "value");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLpop() {
    when(commandObjects.lpop("key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.lpop("key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLpopCount() {
    when(commandObjects.lpop("key", 2)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.lpop("key", 2);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLpos() {
    when(commandObjects.lpos("key", "element")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.lpos("key", "element");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLposWithParams() {
    LPosParams params = new LPosParams();

    when(commandObjects.lpos("key", "element", params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.lpos("key", "element", params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLposWithParamsCount() {
    LPosParams params = new LPosParams();

    when(commandObjects.lpos("key", "element", params, 3)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.lpos("key", "element", params, 3);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRpop() {
    when(commandObjects.rpop("key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.rpop("key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRpopCount() {
    when(commandObjects.rpop("key", 2)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.rpop("key", 2);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLinsert() {
    ListPosition where = ListPosition.BEFORE;

    when(commandObjects.linsert("key", where, "pivot", "value")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.linsert("key", where, "pivot", "value");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLpushx() {
    when(commandObjects.lpushx("key", "value1", "value2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.lpushx("key", "value1", "value2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRpushx() {
    when(commandObjects.rpushx("key", "value1", "value2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.rpushx("key", "value1", "value2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlpop() {
    when(commandObjects.blpop(30, "key")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.blpop(30, "key");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlpopDoubleTimeoutKey() {
    when(commandObjects.blpop(30.0, "key")).thenReturn(keyValueStringStringCommandObject);

    Response<KeyValue<String, String>> response = pipeliningBase.blpop(30.0, "key");

    assertThat(commands, contains(keyValueStringStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBrpop() {
    when(commandObjects.brpop(30, "key")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.brpop(30, "key");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBrpopDoubleTimeoutKey() {
    when(commandObjects.brpop(30.0, "key")).thenReturn(keyValueStringStringCommandObject);

    Response<KeyValue<String, String>> response = pipeliningBase.brpop(30.0, "key");

    assertThat(commands, contains(keyValueStringStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlpopMultipleKeys() {
    when(commandObjects.blpop(30, "key1", "key2")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.blpop(30, "key1", "key2");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlpopDoubleTimeoutKeys() {
    when(commandObjects.blpop(30.0, "key1", "key2")).thenReturn(keyValueStringStringCommandObject);

    Response<KeyValue<String, String>> response = pipeliningBase.blpop(30.0, "key1", "key2");

    assertThat(commands, contains(keyValueStringStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBrpopMultipleKeys() {
    when(commandObjects.brpop(30, "key1", "key2")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.brpop(30, "key1", "key2");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBrpopDoubleTimeoutKeys() {
    when(commandObjects.brpop(30.0, "key1", "key2")).thenReturn(keyValueStringStringCommandObject);

    Response<KeyValue<String, String>> response = pipeliningBase.brpop(30.0, "key1", "key2");

    assertThat(commands, contains(keyValueStringStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRpoplpush() {
    when(commandObjects.rpoplpush("srcKey", "dstKey")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.rpoplpush("srcKey", "dstKey");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBrpoplpush() {
    when(commandObjects.brpoplpush("source", "destination", 30)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.brpoplpush("source", "destination", 30);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLmove() {
    ListDirection from = ListDirection.LEFT;
    ListDirection to = ListDirection.RIGHT;

    when(commandObjects.lmove("srcKey", "dstKey", from, to)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.lmove("srcKey", "dstKey", from, to);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlmove() {
    ListDirection from = ListDirection.LEFT;
    ListDirection to = ListDirection.RIGHT;
    double timeout = 1.0;

    when(commandObjects.blmove("srcKey", "dstKey", from, to, timeout)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.blmove("srcKey", "dstKey", from, to, timeout);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLmpop() {
    ListDirection direction = ListDirection.LEFT;

    when(commandObjects.lmpop(direction, "key1", "key2")).thenReturn(keyValueStringListStringCommandObject);

    Response<KeyValue<String, List<String>>> response = pipeliningBase.lmpop(direction, "key1", "key2");

    assertThat(commands, contains(keyValueStringListStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLmpopCount() {
    ListDirection direction = ListDirection.LEFT;
    int count = 2;

    when(commandObjects.lmpop(direction, count, "key1", "key2")).thenReturn(keyValueStringListStringCommandObject);

    Response<KeyValue<String, List<String>>> response = pipeliningBase.lmpop(direction, count, "key1", "key2");

    assertThat(commands, contains(keyValueStringListStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlmpop() {
    double timeout = 1.0;
    ListDirection direction = ListDirection.LEFT;

    when(commandObjects.blmpop(timeout, direction, "key1", "key2")).thenReturn(keyValueStringListStringCommandObject);

    Response<KeyValue<String, List<String>>> response = pipeliningBase.blmpop(timeout, direction, "key1", "key2");

    assertThat(commands, contains(keyValueStringListStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlmpopCount() {
    double timeout = 1.0;
    ListDirection direction = ListDirection.LEFT;
    int count = 2;

    when(commandObjects.blmpop(timeout, direction, count, "key1", "key2")).thenReturn(keyValueStringListStringCommandObject);

    Response<KeyValue<String, List<String>>> response = pipeliningBase.blmpop(timeout, direction, count, "key1", "key2");

    assertThat(commands, contains(keyValueStringListStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHset() {
    when(commandObjects.hset("key", "field", "value")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hset("key", "field", "value");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHsetMap() {
    Map<String, String> hash = new HashMap<>();
    hash.put("field1", "value1");
    hash.put("field2", "value2");

    when(commandObjects.hset("key", hash)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hset("key", hash);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHget() {
    when(commandObjects.hget("key", "field")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.hget("key", "field");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHsetnx() {
    when(commandObjects.hsetnx("key", "field", "value")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hsetnx("key", "field", "value");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHmset() {
    Map<String, String> hash = new HashMap<>();
    hash.put("field1", "value1");
    hash.put("field2", "value2");

    when(commandObjects.hmset("key", hash)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.hmset("key", hash);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHmget() {
    when(commandObjects.hmget("key", "field1", "field2")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.hmget("key", "field1", "field2");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHincrBy() {
    when(commandObjects.hincrBy("key", "field", 1L)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hincrBy("key", "field", 1L);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHincrByFloat() {
    when(commandObjects.hincrByFloat("key", "field", 1.0)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.hincrByFloat("key", "field", 1.0);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHexists() {
    when(commandObjects.hexists("key", "field")).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.hexists("key", "field");

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHdel() {
    when(commandObjects.hdel("key", "field1", "field2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hdel("key", "field1", "field2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHlen() {
    when(commandObjects.hlen("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hlen("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHkeys() {
    when(commandObjects.hkeys("key")).thenReturn(setStringCommandObject);

    Response<Set<String>> response = pipeliningBase.hkeys("key");

    assertThat(commands, contains(setStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHvals() {
    when(commandObjects.hvals("key")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.hvals("key");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHgetAll() {
    when(commandObjects.hgetAll("key")).thenReturn(mapStringStringCommandObject);

    Response<Map<String, String>> response = pipeliningBase.hgetAll("key");

    assertThat(commands, contains(mapStringStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHrandfield() {
    when(commandObjects.hrandfield("key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.hrandfield("key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHrandfieldCount() {
    long count = 2;

    when(commandObjects.hrandfield("key", count)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.hrandfield("key", count);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHrandfieldWithValues() {
    long count = 2;

    when(commandObjects.hrandfieldWithValues("key", count)).thenReturn(listEntryStringStringCommandObject);

    Response<List<Map.Entry<String, String>>> response = pipeliningBase.hrandfieldWithValues("key", count);

    assertThat(commands, contains(listEntryStringStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHscan() {
    String cursor = "0";
    ScanParams params = new ScanParams();

    when(commandObjects.hscan("key", cursor, params)).thenReturn(scanResultEntryStringStringCommandObject);

    Response<ScanResult<Map.Entry<String, String>>> response = pipeliningBase.hscan("key", cursor, params);

    assertThat(commands, contains(scanResultEntryStringStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHscanNoValues() {
    String cursor = "0";
    ScanParams params = new ScanParams();

    when(commandObjects.hscanNoValues("key", cursor, params)).thenReturn(scanResultStringCommandObject);

    Response<ScanResult<String>> response = pipeliningBase.hscanNoValues("key", cursor, params);

    assertThat(commands, contains(scanResultStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHstrlen() {
    when(commandObjects.hstrlen("key", "field")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hstrlen("key", "field");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSadd() {
    when(commandObjects.sadd("key", "member1", "member2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sadd("key", "member1", "member2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSmembers() {
    when(commandObjects.smembers("key")).thenReturn(setStringCommandObject);

    Response<Set<String>> response = pipeliningBase.smembers("key");

    assertThat(commands, contains(setStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSrem() {
    when(commandObjects.srem("key", "member1", "member2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.srem("key", "member1", "member2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSpop() {
    when(commandObjects.spop("key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.spop("key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSpopCount() {
    long count = 2;

    when(commandObjects.spop("key", count)).thenReturn(setStringCommandObject);

    Response<Set<String>> response = pipeliningBase.spop("key", count);

    assertThat(commands, contains(setStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScard() {
    when(commandObjects.scard("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.scard("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSismember() {
    when(commandObjects.sismember("key", "member")).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.sismember("key", "member");

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSmismember() {
    when(commandObjects.smismember("key", "member1", "member2")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.smismember("key", "member1", "member2");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSrandmember() {
    when(commandObjects.srandmember("key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.srandmember("key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSrandmemberCount() {
    int count = 2;

    when(commandObjects.srandmember("key", count)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.srandmember("key", count);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSscan() {
    String cursor = "0";
    ScanParams params = new ScanParams();

    when(commandObjects.sscan("key", cursor, params)).thenReturn(scanResultStringCommandObject);

    Response<ScanResult<String>> response = pipeliningBase.sscan("key", cursor, params);

    assertThat(commands, contains(scanResultStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSdiff() {
    when(commandObjects.sdiff("key1", "key2")).thenReturn(setStringCommandObject);

    Response<Set<String>> response = pipeliningBase.sdiff("key1", "key2");

    assertThat(commands, contains(setStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSdiffstore() {
    when(commandObjects.sdiffstore("dstKey", "key1", "key2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sdiffstore("dstKey", "key1", "key2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSinter() {
    when(commandObjects.sinter("key1", "key2")).thenReturn(setStringCommandObject);

    Response<Set<String>> response = pipeliningBase.sinter("key1", "key2");

    assertThat(commands, contains(setStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSinterstore() {
    when(commandObjects.sinterstore("dstKey", "key1", "key2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sinterstore("dstKey", "key1", "key2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSintercard() {
    when(commandObjects.sintercard("key1", "key2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sintercard("key1", "key2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSintercardWithLimit() {
    int limit = 1;

    when(commandObjects.sintercard(limit, "key1", "key2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sintercard(limit, "key1", "key2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSunion() {
    when(commandObjects.sunion("key1", "key2")).thenReturn(setStringCommandObject);

    Response<Set<String>> response = pipeliningBase.sunion("key1", "key2");

    assertThat(commands, contains(setStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSunionstore() {
    when(commandObjects.sunionstore("dstKey", "key1", "key2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sunionstore("dstKey", "key1", "key2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSmove() {
    when(commandObjects.smove("srcKey", "dstKey", "member")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.smove("srcKey", "dstKey", "member");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZaddSingle() {
    when(commandObjects.zadd("key", 1.0, "member")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zadd("key", 1.0, "member");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZaddSingleWithParams() {
    ZAddParams params = new ZAddParams();

    when(commandObjects.zadd("key", 1.0, "member", params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zadd("key", 1.0, "member", params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZaddMultiple() {
    Map<String, Double> scoreMembers = new HashMap<>();
    scoreMembers.put("member1", 1.0);
    scoreMembers.put("member2", 2.0);

    when(commandObjects.zadd("key", scoreMembers)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zadd("key", scoreMembers);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZaddMultipleWithParams() {
    Map<String, Double> scoreMembers = new HashMap<>();
    scoreMembers.put("member1", 1.0);
    scoreMembers.put("member2", 2.0);

    ZAddParams params = new ZAddParams();

    when(commandObjects.zadd("key", scoreMembers, params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zadd("key", scoreMembers, params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZaddIncr() {
    ZAddParams params = new ZAddParams();

    when(commandObjects.zaddIncr("key", 1.0, "member", params)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.zaddIncr("key", 1.0, "member", params);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrem() {
    when(commandObjects.zrem("key", "member1", "member2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zrem("key", "member1", "member2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZincrby() {
    when(commandObjects.zincrby("key", 1.0, "member")).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.zincrby("key", 1.0, "member");

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZincrbyWithParams() {
    ZIncrByParams params = new ZIncrByParams();

    when(commandObjects.zincrby("key", 1.0, "member", params)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.zincrby("key", 1.0, "member", params);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrank() {
    when(commandObjects.zrank("key", "member")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zrank("key", "member");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrank() {
    when(commandObjects.zrevrank("key", "member")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zrevrank("key", "member");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrankWithScore() {
    when(commandObjects.zrankWithScore("key", "member")).thenReturn(keyValueLongDoubleCommandObject);

    Response<KeyValue<Long, Double>> response = pipeliningBase.zrankWithScore("key", "member");

    assertThat(commands, contains(keyValueLongDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrankWithScore() {
    when(commandObjects.zrevrankWithScore("key", "member")).thenReturn(keyValueLongDoubleCommandObject);

    Response<KeyValue<Long, Double>> response = pipeliningBase.zrevrankWithScore("key", "member");

    assertThat(commands, contains(keyValueLongDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrange() {
    when(commandObjects.zrange("key", 0, -1)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrange("key", 0, -1);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrange() {
    when(commandObjects.zrevrange("key", 0, -1)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrevrange("key", 0, -1);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeWithScores() {
    when(commandObjects.zrangeWithScores("key", 0, -1)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeWithScores("key", 0, -1);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeWithScores() {
    when(commandObjects.zrevrangeWithScores("key", 0, -1)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrevrangeWithScores("key", 0, -1);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrandmember() {
    when(commandObjects.zrandmember("key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.zrandmember("key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrandmemberCount() {
    long count = 2;

    when(commandObjects.zrandmember("key", count)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrandmember("key", count);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrandmemberWithScores() {
    long count = 2;

    when(commandObjects.zrandmemberWithScores("key", count)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrandmemberWithScores("key", count);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZcard() {
    when(commandObjects.zcard("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zcard("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZscore() {
    when(commandObjects.zscore("key", "member")).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.zscore("key", "member");

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZmscore() {
    when(commandObjects.zmscore("key", "member1", "member2")).thenReturn(listDoubleCommandObject);

    Response<List<Double>> response = pipeliningBase.zmscore("key", "member1", "member2");

    assertThat(commands, contains(listDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZpopmax() {
    when(commandObjects.zpopmax("key")).thenReturn(tupleCommandObject);

    Response<Tuple> response = pipeliningBase.zpopmax("key");

    assertThat(commands, contains(tupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZpopmaxCount() {
    int count = 2;

    when(commandObjects.zpopmax("key", count)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zpopmax("key", count);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZpopmin() {
    when(commandObjects.zpopmin("key")).thenReturn(tupleCommandObject);

    Response<Tuple> response = pipeliningBase.zpopmin("key");

    assertThat(commands, contains(tupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZpopminCount() {
    int count = 2;

    when(commandObjects.zpopmin("key", count)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zpopmin("key", count);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZcountDouble() {
    when(commandObjects.zcount("key", 1.0, 2.0)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zcount("key", 1.0, 2.0);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZcountString() {
    when(commandObjects.zcount("key", "1", "2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zcount("key", "1", "2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreDouble() {
    when(commandObjects.zrangeByScore("key", 1.0, 2.0)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrangeByScore("key", 1.0, 2.0);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreString() {
    when(commandObjects.zrangeByScore("key", "1", "2")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrangeByScore("key", "1", "2");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreDouble() {
    when(commandObjects.zrevrangeByScore("key", 2.0, 1.0)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrevrangeByScore("key", 2.0, 1.0);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreDoubleWithLimit() {
    when(commandObjects.zrangeByScore("key", 1.0, 2.0, 0, 1)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrangeByScore("key", 1.0, 2.0, 0, 1);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreString() {
    when(commandObjects.zrevrangeByScore("key", "2", "1")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrevrangeByScore("key", "2", "1");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreStringWithLimit() {
    when(commandObjects.zrangeByScore("key", "1", "2", 0, 1)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrangeByScore("key", "1", "2", 0, 1);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreDoubleWithLimit() {
    when(commandObjects.zrevrangeByScore("key", 2.0, 1.0, 0, 1)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrevrangeByScore("key", 2.0, 1.0, 0, 1);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithScores() {
    when(commandObjects.zrangeByScoreWithScores("key", 1.0, 2.0)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeByScoreWithScores("key", 1.0, 2.0);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreWithScoresDouble() {
    when(commandObjects.zrevrangeByScoreWithScores("key", 2.0, 1.0)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrevrangeByScoreWithScores("key", 2.0, 1.0);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithScoresDoubleWithLimit() {
    when(commandObjects.zrangeByScoreWithScores("key", 1.0, 2.0, 0, 1)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeByScoreWithScores("key", 1.0, 2.0, 0, 1);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreStringWithLimit() {
    when(commandObjects.zrevrangeByScore("key", "2", "1", 0, 1)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrevrangeByScore("key", "2", "1", 0, 1);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithScoresString() {
    when(commandObjects.zrangeByScoreWithScores("key", "1", "2")).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeByScoreWithScores("key", "1", "2");

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreWithScoresString() {
    when(commandObjects.zrevrangeByScoreWithScores("key", "2", "1")).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrevrangeByScoreWithScores("key", "2", "1");

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithScoresStringWithLimit() {
    when(commandObjects.zrangeByScoreWithScores("key", "1", "2", 0, 1)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeByScoreWithScores("key", "1", "2", 0, 1);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreWithScoresDoubleWithLimit() {
    when(commandObjects.zrevrangeByScoreWithScores("key", 2.0, 1.0, 0, 1)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrevrangeByScoreWithScores("key", 2.0, 1.0, 0, 1);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreWithScoresStringWithLimit() {
    when(commandObjects.zrevrangeByScoreWithScores("key", "2", "1", 0, 1)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrevrangeByScoreWithScores("key", "2", "1", 0, 1);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeZRangeParams() {
    ZRangeParams zRangeParams = new ZRangeParams(1, 2);

    when(commandObjects.zrange("key", zRangeParams)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrange("key", zRangeParams);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeWithScoresZRangeParams() {
    ZRangeParams zRangeParams = new ZRangeParams(1, 2);

    when(commandObjects.zrangeWithScores("key", zRangeParams)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeWithScores("key", zRangeParams);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangestore() {
    ZRangeParams zRangeParams = new ZRangeParams(1, 2);

    when(commandObjects.zrangestore("dest", "src", zRangeParams)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zrangestore("dest", "src", zRangeParams);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZremrangeByRank() {
    when(commandObjects.zremrangeByRank("key", 0, 1)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zremrangeByRank("key", 0, 1);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZremrangeByScoreDouble() {
    when(commandObjects.zremrangeByScore("key", 1.0, 2.0)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zremrangeByScore("key", 1.0, 2.0);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZremrangeByScoreString() {
    when(commandObjects.zremrangeByScore("key", "1", "2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zremrangeByScore("key", "1", "2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZlexcount() {
    when(commandObjects.zlexcount("key", "[a", "[z")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zlexcount("key", "[a", "[z");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByLex() {
    when(commandObjects.zrangeByLex("key", "[a", "[z")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrangeByLex("key", "[a", "[z");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByLexWithLimit() {
    when(commandObjects.zrangeByLex("key", "[a", "[z", 0, 10)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrangeByLex("key", "[a", "[z", 0, 10);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByLex() {
    when(commandObjects.zrevrangeByLex("key", "[z", "[a")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrevrangeByLex("key", "[z", "[a");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByLexWithLimit() {
    when(commandObjects.zrevrangeByLex("key", "[z", "[a", 0, 10)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zrevrangeByLex("key", "[z", "[a", 0, 10);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZremrangeByLex() {
    when(commandObjects.zremrangeByLex("key", "[a", "[z")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zremrangeByLex("key", "[a", "[z");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZscan() {
    ScanParams params = new ScanParams();

    when(commandObjects.zscan("key", "0", params)).thenReturn(scanResultTupleCommandObject);

    Response<ScanResult<Tuple>> response = pipeliningBase.zscan("key", "0", params);

    assertThat(commands, contains(scanResultTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBzpopmax() {
    when(commandObjects.bzpopmax(1.0, "key1", "key2")).thenReturn(keyValueStringTupleCommandObject);

    Response<KeyValue<String, Tuple>> response = pipeliningBase.bzpopmax(1.0, "key1", "key2");

    assertThat(commands, contains(keyValueStringTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBzpopmin() {
    when(commandObjects.bzpopmin(1.0, "key1", "key2")).thenReturn(keyValueStringTupleCommandObject);

    Response<KeyValue<String, Tuple>> response = pipeliningBase.bzpopmin(1.0, "key1", "key2");

    assertThat(commands, contains(keyValueStringTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZmpop() {
    SortedSetOption option = SortedSetOption.MAX;

    when(commandObjects.zmpop(option, "key1", "key2")).thenReturn(keyValueStringListTupleCommandObject);

    Response<KeyValue<String, List<Tuple>>> response = pipeliningBase.zmpop(option, "key1", "key2");

    assertThat(commands, contains(keyValueStringListTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZmpopWithCount() {
    SortedSetOption option = SortedSetOption.MAX;
    int count = 2;

    when(commandObjects.zmpop(option, count, "key1", "key2")).thenReturn(keyValueStringListTupleCommandObject);

    Response<KeyValue<String, List<Tuple>>> response = pipeliningBase.zmpop(option, count, "key1", "key2");

    assertThat(commands, contains(keyValueStringListTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBzmpop() {
    SortedSetOption option = SortedSetOption.MAX;

    when(commandObjects.bzmpop(1.0, option, "key1", "key2")).thenReturn(keyValueStringListTupleCommandObject);

    Response<KeyValue<String, List<Tuple>>> response = pipeliningBase.bzmpop(1.0, option, "key1", "key2");

    assertThat(commands, contains(keyValueStringListTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBzmpopWithCount() {
    SortedSetOption option = SortedSetOption.MAX;
    int count = 2;

    when(commandObjects.bzmpop(1.0, option, count, "key1", "key2")).thenReturn(keyValueStringListTupleCommandObject);

    Response<KeyValue<String, List<Tuple>>> response = pipeliningBase.bzmpop(1.0, option, count, "key1", "key2");

    assertThat(commands, contains(keyValueStringListTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZdiff() {
    when(commandObjects.zdiff("key1", "key2")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zdiff("key1", "key2");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZdiffWithScores() {
    when(commandObjects.zdiffWithScores("key1", "key2")).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zdiffWithScores("key1", "key2");

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZdiffStore() {
    when(commandObjects.zdiffStore("dstKey", "key1", "key2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zdiffStore("dstKey", "key1", "key2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZdiffstore() {
    when(commandObjects.zdiffstore("dstKey", "key1", "key2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zdiffstore("dstKey", "key1", "key2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZinterstore() {
    when(commandObjects.zinterstore("dstKey", "set1", "set2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zinterstore("dstKey", "set1", "set2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZinterstoreWithParams() {
    ZParams params = new ZParams();

    when(commandObjects.zinterstore("dstKey", params, "set1", "set2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zinterstore("dstKey", params, "set1", "set2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZinter() {
    ZParams params = new ZParams();

    when(commandObjects.zinter(params, "key1", "key2")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zinter(params, "key1", "key2");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZinterWithScores() {
    ZParams params = new ZParams();

    when(commandObjects.zinterWithScores(params, "key1", "key2")).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zinterWithScores(params, "key1", "key2");

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZintercard() {
    when(commandObjects.zintercard("key1", "key2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zintercard("key1", "key2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZintercardWithLimit() {
    long limit = 2;

    when(commandObjects.zintercard(limit, "key1", "key2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zintercard(limit, "key1", "key2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZunion() {
    ZParams params = new ZParams();

    when(commandObjects.zunion(params, "key1", "key2")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.zunion(params, "key1", "key2");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZunionWithScores() {
    ZParams params = new ZParams();

    when(commandObjects.zunionWithScores(params, "key1", "key2")).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zunionWithScores(params, "key1", "key2");

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZunionstore() {
    when(commandObjects.zunionstore("dstKey", "set1", "set2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zunionstore("dstKey", "set1", "set2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZunionstoreWithParams() {
    ZParams params = new ZParams();

    when(commandObjects.zunionstore("dstKey", params, "set1", "set2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zunionstore("dstKey", params, "set1", "set2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoaddSingle() {
    when(commandObjects.geoadd("key", 13.361389, 38.115556, "member")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geoadd("key", 13.361389, 38.115556, "member");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoaddMap() {
    Map<String, GeoCoordinate> memberCoordinateMap = new HashMap<>();
    memberCoordinateMap.put("member", new GeoCoordinate(13.361389, 38.115556));

    when(commandObjects.geoadd("key", memberCoordinateMap)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geoadd("key", memberCoordinateMap);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoaddMapWithParams() {
    GeoAddParams params = new GeoAddParams();

    Map<String, GeoCoordinate> memberCoordinateMap = new HashMap<>();
    memberCoordinateMap.put("member", new GeoCoordinate(13.361389, 38.115556));

    when(commandObjects.geoadd("key", params, memberCoordinateMap)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geoadd("key", params, memberCoordinateMap);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeodist() {
    when(commandObjects.geodist("key", "member1", "member2")).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.geodist("key", "member1", "member2");

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeodistWithUnit() {
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.geodist("key", "member1", "member2", unit)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.geodist("key", "member1", "member2", unit);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeohash() {
    when(commandObjects.geohash("key", "member1", "member2")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.geohash("key", "member1", "member2");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeopos() {
    when(commandObjects.geopos("key", "member1", "member2")).thenReturn(listGeoCoordinateCommandObject);

    Response<List<GeoCoordinate>> response = pipeliningBase.geopos("key", "member1", "member2");

    assertThat(commands, contains(listGeoCoordinateCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradius() {
    when(commandObjects.georadius("key", 15.0, 37.0, 100.0, GeoUnit.KM))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response =
        pipeliningBase.georadius("key", 15.0, 37.0, 100.0, GeoUnit.KM);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusReadonly() {
    when(commandObjects.georadiusReadonly("key", 15.0, 37.0, 100.0, GeoUnit.KM))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response =
        pipeliningBase.georadiusReadonly("key", 15.0, 37.0, 100.0, GeoUnit.KM);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusWithParam() {
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();

    when(commandObjects.georadius("key", 15.0, 37.0, 100.0, GeoUnit.KM, param))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response =
        pipeliningBase.georadius("key", 15.0, 37.0, 100.0, GeoUnit.KM, param);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusReadonlyWithParam() {
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();

    when(commandObjects.georadiusReadonly("key", 15.0, 37.0, 100.0, GeoUnit.KM, param))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response =
        pipeliningBase.georadiusReadonly("key", 15.0, 37.0, 100.0, GeoUnit.KM, param);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByMember() {
    when(commandObjects.georadiusByMember("key", "member", 100.0, GeoUnit.KM))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response =
        pipeliningBase.georadiusByMember("key", "member", 100.0, GeoUnit.KM);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByMemberReadonly() {
    when(commandObjects.georadiusByMemberReadonly("key", "member", 100.0, GeoUnit.KM))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response =
        pipeliningBase.georadiusByMemberReadonly("key", "member", 100.0, GeoUnit.KM);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByMemberWithParam() {
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();

    when(commandObjects.georadiusByMember("key", "member", 100.0, GeoUnit.KM, param))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase
        .georadiusByMember("key", "member", 100.0, GeoUnit.KM, param);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByMemberReadonlyWithParam() {
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();

    when(commandObjects.georadiusByMemberReadonly("key", "member", 100.0, GeoUnit.KM, param))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase
        .georadiusByMemberReadonly("key", "member", 100.0, GeoUnit.KM, param);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusStore() {
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();
    GeoRadiusStoreParam storeParam = GeoRadiusStoreParam.geoRadiusStoreParam().store("storeKey");

    when(commandObjects.georadiusStore("key", 15.0, 37.0, 100.0, GeoUnit.KM, param, storeParam))
        .thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase
        .georadiusStore("key", 15.0, 37.0, 100.0, GeoUnit.KM, param, storeParam);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByMemberStore() {
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();
    GeoRadiusStoreParam storeParam = GeoRadiusStoreParam.geoRadiusStoreParam().store("storeKey");

    when(commandObjects.georadiusByMemberStore("key", "member", 100.0, GeoUnit.KM, param, storeParam))
        .thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase
        .georadiusByMemberStore("key", "member", 100.0, GeoUnit.KM, param, storeParam);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchByMemberRadius() {
    when(commandObjects.geosearch("key", "member", 100.0, GeoUnit.KM))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase
        .geosearch("key", "member", 100.0, GeoUnit.KM);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchByCoordRadius() {
    GeoCoordinate coord = new GeoCoordinate(15.0, 37.0);

    when(commandObjects.geosearch("key", coord, 100.0, GeoUnit.KM))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.geosearch("key", coord, 100.0, GeoUnit.KM);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchByMemberBox() {
    when(commandObjects.geosearch("key", "member", 50.0, 50.0, GeoUnit.KM))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase
        .geosearch("key", "member", 50.0, 50.0, GeoUnit.KM);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchByCoordBox() {
    GeoCoordinate coord = new GeoCoordinate(15.0, 37.0);

    when(commandObjects.geosearch("key", coord, 50.0, 50.0, GeoUnit.KM))
        .thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase
        .geosearch("key", coord, 50.0, 50.0, GeoUnit.KM);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchWithParams() {
    GeoSearchParam params = new GeoSearchParam();

    when(commandObjects.geosearch("key", params)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.geosearch("key", params);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreByMemberRadius() {
    when(commandObjects.geosearchStore("dest", "src", "member", 100.0, GeoUnit.KM))
        .thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase
        .geosearchStore("dest", "src", "member", 100.0, GeoUnit.KM);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreByCoordRadius() {
    GeoCoordinate coord = new GeoCoordinate(15.0, 37.0);

    when(commandObjects.geosearchStore("dest", "src", coord, 100.0, GeoUnit.KM))
        .thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geosearchStore("dest", "src", coord, 100.0, GeoUnit.KM);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreByMemberBox() {
    when(commandObjects.geosearchStore("dest", "src", "member", 50.0, 50.0, GeoUnit.KM))
        .thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase
        .geosearchStore("dest", "src", "member", 50.0, 50.0, GeoUnit.KM);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreByCoordBox() {
    GeoCoordinate coord = new GeoCoordinate(15.0, 37.0);

    when(commandObjects.geosearchStore("dest", "src", coord, 50.0, 50.0, GeoUnit.KM))
        .thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase
        .geosearchStore("dest", "src", coord, 50.0, 50.0, GeoUnit.KM);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreWithParams() {
    GeoSearchParam params = new GeoSearchParam();

    when(commandObjects.geosearchStore("dest", "src", params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geosearchStore("dest", "src", params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreStoreDist() {
    GeoSearchParam params = new GeoSearchParam();

    when(commandObjects.geosearchStoreStoreDist("dest", "src", params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geosearchStoreStoreDist("dest", "src", params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPfadd() {
    when(commandObjects.pfadd("key", "element1", "element2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pfadd("key", "element1", "element2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPfmerge() {
    when(commandObjects.pfmerge("destkey", "sourcekey1", "sourcekey2")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.pfmerge("destkey", "sourcekey1", "sourcekey2");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPfcountSingleKey() {
    when(commandObjects.pfcount("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pfcount("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPfcountMultipleKeys() {
    when(commandObjects.pfcount("key1", "key2")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pfcount("key1", "key2");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXadd() {
    StreamEntryID id = new StreamEntryID();

    Map<String, String> hash = new HashMap<>();
    hash.put("field1", "value1");

    when(commandObjects.xadd("key", id, hash)).thenReturn(streamEntryIdCommandObject);

    Response<StreamEntryID> response = pipeliningBase.xadd("key", id, hash);

    assertThat(commands, contains(streamEntryIdCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXaddWithParams() {
    XAddParams params = new XAddParams();

    Map<String, String> hash = new HashMap<>();
    hash.put("field1", "value1");

    when(commandObjects.xadd("key", params, hash)).thenReturn(streamEntryIdCommandObject);

    Response<StreamEntryID> response = pipeliningBase.xadd("key", params, hash);

    assertThat(commands, contains(streamEntryIdCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXlen() {
    when(commandObjects.xlen("key")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.xlen("key");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXrange() {
    StreamEntryID start = new StreamEntryID("0-0");
    StreamEntryID end = new StreamEntryID("9999999999999-0");

    when(commandObjects.xrange("key", start, end)).thenReturn(listStreamEntryCommandObject);

    Response<List<StreamEntry>> response = pipeliningBase.xrange("key", start, end);

    assertThat(commands, contains(listStreamEntryCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXrangeWithCount() {
    StreamEntryID start = new StreamEntryID("0-0");
    StreamEntryID end = new StreamEntryID("9999999999999-0");
    int count = 10;

    when(commandObjects.xrange("key", start, end, count)).thenReturn(listStreamEntryCommandObject);

    Response<List<StreamEntry>> response = pipeliningBase.xrange("key", start, end, count);

    assertThat(commands, contains(listStreamEntryCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXrevrange() {
    StreamEntryID end = new StreamEntryID("9999999999999-0");
    StreamEntryID start = new StreamEntryID("0-0");

    when(commandObjects.xrevrange("key", end, start)).thenReturn(listStreamEntryCommandObject);

    Response<List<StreamEntry>> response = pipeliningBase.xrevrange("key", end, start);

    assertThat(commands, contains(listStreamEntryCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXrevrangeWithCount() {
    StreamEntryID end = new StreamEntryID("9999999999999-0");
    StreamEntryID start = new StreamEntryID("0-0");
    int count = 10;

    when(commandObjects.xrevrange("key", end, start, count)).thenReturn(listStreamEntryCommandObject);

    Response<List<StreamEntry>> response = pipeliningBase.xrevrange("key", end, start, count);

    assertThat(commands, contains(listStreamEntryCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXrangeWithStringIDs() {
    String start = "-";
    String end = "+";

    when(commandObjects.xrange("key", start, end)).thenReturn(listStreamEntryCommandObject);

    Response<List<StreamEntry>> response = pipeliningBase.xrange("key", start, end);

    assertThat(commands, contains(listStreamEntryCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXrangeWithStringIDsAndCount() {
    String start = "-";
    String end = "+";
    int count = 10;

    when(commandObjects.xrange("key", start, end, count)).thenReturn(listStreamEntryCommandObject);

    Response<List<StreamEntry>> response = pipeliningBase.xrange("key", start, end, count);

    assertThat(commands, contains(listStreamEntryCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXrevrangeWithStringIDs() {
    String end = "+";
    String start = "-";

    when(commandObjects.xrevrange("key", end, start)).thenReturn(listStreamEntryCommandObject);

    Response<List<StreamEntry>> response = pipeliningBase.xrevrange("key", end, start);

    assertThat(commands, contains(listStreamEntryCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXrevrangeWithStringIDsAndCount() {
    String end = "+";
    String start = "-";
    int count = 10;

    when(commandObjects.xrevrange("key", end, start, count)).thenReturn(listStreamEntryCommandObject);

    Response<List<StreamEntry>> response = pipeliningBase.xrevrange("key", end, start, count);

    assertThat(commands, contains(listStreamEntryCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXack() {
    StreamEntryID[] ids = { new StreamEntryID("1526999352406-0"), new StreamEntryID("1526999352406-1") };

    when(commandObjects.xack("key", "group", ids)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.xack("key", "group", ids);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXgroupCreate() {
    StreamEntryID id = new StreamEntryID("0-0");

    when(commandObjects.xgroupCreate("key", "groupName", id, true)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.xgroupCreate("key", "groupName", id, true);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXgroupSetID() {
    StreamEntryID id = new StreamEntryID("0-0");

    when(commandObjects.xgroupSetID("key", "groupName", id)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.xgroupSetID("key", "groupName", id);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXgroupDestroy() {
    when(commandObjects.xgroupDestroy("key", "groupName")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.xgroupDestroy("key", "groupName");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXgroupCreateConsumer() {
    when(commandObjects.xgroupCreateConsumer("key", "groupName", "consumerName"))
        .thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.xgroupCreateConsumer("key", "groupName", "consumerName");

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXgroupDelConsumer() {
    when(commandObjects.xgroupDelConsumer("key", "groupName", "consumerName"))
        .thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.xgroupDelConsumer("key", "groupName", "consumerName");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXpendingSummary() {
    when(commandObjects.xpending("key", "groupName")).thenReturn(streamPendingSummaryCommandObject);

    Response<StreamPendingSummary> response = pipeliningBase.xpending("key", "groupName");

    assertThat(commands, contains(streamPendingSummaryCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXpendingRange() {
    XPendingParams params = new XPendingParams();

    when(commandObjects.xpending("key", "groupName", params)).thenReturn(listStreamPendingEntryCommandObject);

    Response<List<StreamPendingEntry>> response = pipeliningBase.xpending("key", "groupName", params);

    assertThat(commands, contains(listStreamPendingEntryCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXdel() {
    StreamEntryID[] ids = { new StreamEntryID("1526999352406-0"), new StreamEntryID("1526999352406-1") };

    when(commandObjects.xdel("key", ids)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.xdel("key", ids);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXtrim() {
    when(commandObjects.xtrim("key", 1000L, true)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.xtrim("key", 1000L, true);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXtrimWithParams() {
    XTrimParams params = new XTrimParams().maxLen(1000L);
    when(commandObjects.xtrim("key", params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.xtrim("key", params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXclaim() {
    StreamEntryID[] ids = { new StreamEntryID("1526999352406-0"), new StreamEntryID("1526999352406-1") };
    XClaimParams params = new XClaimParams().idle(10000L);

    when(commandObjects.xclaim("key", "group", "consumerName", 10000L, params, ids))
        .thenReturn(listStreamEntryCommandObject);

    Response<List<StreamEntry>> response = pipeliningBase
        .xclaim("key", "group", "consumerName", 10000L, params, ids);

    assertThat(commands, contains(listStreamEntryCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXclaimJustId() {
    StreamEntryID[] ids = { new StreamEntryID("1526999352406-0"), new StreamEntryID("1526999352406-1") };
    XClaimParams params = new XClaimParams().idle(10000L);

    when(commandObjects.xclaimJustId("key", "group", "consumerName", 10000L, params, ids))
        .thenReturn(listStreamEntryIdCommandObject);

    Response<List<StreamEntryID>> response = pipeliningBase
        .xclaimJustId("key", "group", "consumerName", 10000L, params, ids);

    assertThat(commands, contains(listStreamEntryIdCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXautoclaim() {
    StreamEntryID start = new StreamEntryID("0-0");
    XAutoClaimParams params = new XAutoClaimParams();

    when(commandObjects.xautoclaim("key", "group", "consumerName", 10000L, start, params))
        .thenReturn(entryStreamEntryIdListStreamEntryCommandObject);

    Response<Map.Entry<StreamEntryID, List<StreamEntry>>> response = pipeliningBase
        .xautoclaim("key", "group", "consumerName", 10000L, start, params);

    assertThat(commands, contains(entryStreamEntryIdListStreamEntryCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXautoclaimJustId() {
    StreamEntryID start = new StreamEntryID("0-0");
    XAutoClaimParams params = new XAutoClaimParams();

    when(commandObjects.xautoclaimJustId("key", "group", "consumerName", 10000L, start, params))
        .thenReturn(entryStreamEntryIdListStreamEntryIdCommandObject);

    Response<Map.Entry<StreamEntryID, List<StreamEntryID>>> response = pipeliningBase
        .xautoclaimJustId("key", "group", "consumerName", 10000L, start, params);

    assertThat(commands, contains(entryStreamEntryIdListStreamEntryIdCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXinfoStream() {
    when(commandObjects.xinfoStream("key")).thenReturn(streamInfoCommandObject);

    Response<StreamInfo> response = pipeliningBase.xinfoStream("key");

    assertThat(commands, contains(streamInfoCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXinfoStreamFull() {
    when(commandObjects.xinfoStreamFull("key")).thenReturn(streamFullInfoCommandObject);

    Response<StreamFullInfo> response = pipeliningBase.xinfoStreamFull("key");

    assertThat(commands, contains(streamFullInfoCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXinfoStreamFullWithCount() {
    int count = 10;
    when(commandObjects.xinfoStreamFull("key", count)).thenReturn(streamFullInfoCommandObject);

    Response<StreamFullInfo> response = pipeliningBase.xinfoStreamFull("key", count);

    assertThat(commands, contains(streamFullInfoCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXinfoGroups() {
    when(commandObjects.xinfoGroups("key")).thenReturn(listStreamGroupInfoCommandObject);

    Response<List<StreamGroupInfo>> response = pipeliningBase.xinfoGroups("key");

    assertThat(commands, contains(listStreamGroupInfoCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXinfoConsumers() {
    when(commandObjects.xinfoConsumers("key", "group")).thenReturn(listStreamConsumersInfoCommandObject);

    Response<List<StreamConsumersInfo>> response = pipeliningBase.xinfoConsumers("key", "group");

    assertThat(commands, contains(listStreamConsumersInfoCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXinfoConsumers2() {
    when(commandObjects.xinfoConsumers2("key", "group")).thenReturn(listStreamConsumerInfoCommandObject);

    Response<List<StreamConsumerInfo>> response = pipeliningBase.xinfoConsumers2("key", "group");

    assertThat(commands, contains(listStreamConsumerInfoCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXread() {
    XReadParams xReadParams = new XReadParams();

    Map<String, StreamEntryID> streams = new HashMap<>();
    streams.put("key1", new StreamEntryID("0-0"));
    streams.put("key2", new StreamEntryID("0-0"));

    when(commandObjects.xread(xReadParams, streams)).thenReturn(listEntryStringListStreamEntryCommandObject);

    Response<List<Map.Entry<String, List<StreamEntry>>>> response = pipeliningBase.xread(xReadParams, streams);

    assertThat(commands, contains(listEntryStringListStreamEntryCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXreadGroup() {
    XReadGroupParams xReadGroupParams = new XReadGroupParams();

    Map<String, StreamEntryID> streams = new HashMap<>();
    streams.put("stream1", new StreamEntryID("0-0"));

    when(commandObjects.xreadGroup("groupName", "consumer", xReadGroupParams, streams))
        .thenReturn(listEntryStringListStreamEntryCommandObject);

    Response<List<Map.Entry<String, List<StreamEntry>>>> response = pipeliningBase
        .xreadGroup("groupName", "consumer", xReadGroupParams, streams);

    assertThat(commands, contains(listEntryStringListStreamEntryCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testEval() {
    String script = "return 'Hello, world!'";
    when(commandObjects.eval(script)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.eval(script);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testEvalWithKeysAndParams() {
    String script = "return KEYS[1] .. ARGV[1]";
    int keyCount = 1;

    when(commandObjects.eval(script, keyCount, "key", "arg")).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.eval(script, keyCount, "key", "arg");

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testEvalWithLists() {
    String script = "return KEYS[1] .. ARGV[1]";
    List<String> keys = Collections.singletonList("key");
    List<String> args = Collections.singletonList("arg");

    when(commandObjects.eval(script, keys, args)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.eval(script, keys, args);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testEvalReadonlyWithLists() {
    String script = "return KEYS[1] .. ARGV[1]";
    List<String> keys = Collections.singletonList("key");
    List<String> args = Collections.singletonList("arg");

    when(commandObjects.evalReadonly(script, keys, args)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.evalReadonly(script, keys, args);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testEvalsha() {
    String sha1 = "somehash";

    when(commandObjects.evalsha(sha1)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.evalsha(sha1);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testEvalshaWithKeysAndParams() {
    String sha1 = "somehash";
    int keyCount = 1;

    when(commandObjects.evalsha(sha1, keyCount, "key", "arg")).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.evalsha(sha1, keyCount, "key", "arg");

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testEvalshaWithLists() {
    String sha1 = "somehash";
    List<String> keys = Collections.singletonList("key");
    List<String> args = Collections.singletonList("arg");

    when(commandObjects.evalsha(sha1, keys, args)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.evalsha(sha1, keys, args);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testEvalshaReadonlyWithLists() {
    String sha1 = "somehash";
    List<String> keys = Collections.singletonList("key");
    List<String> args = Collections.singletonList("arg");

    when(commandObjects.evalshaReadonly(sha1, keys, args)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.evalshaReadonly(sha1, keys, args);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testWaitReplicas() {
    int replicas = 2;
    long timeout = 1000L;

    when(commandObjects.waitReplicas("key", replicas, timeout)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.waitReplicas("key", replicas, timeout);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testWaitAOF() {
    long numLocal = 1L;
    long numReplicas = 1L;
    long timeout = 1000L;

    when(commandObjects.waitAOF("key", numLocal, numReplicas, timeout)).thenReturn(keyValueLongLongCommandObject);

    Response<KeyValue<Long, Long>> response = pipeliningBase.waitAOF("key", numLocal, numReplicas, timeout);

    assertThat(commands, contains(keyValueLongLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testEvalWithSampleKey() {
    String script = "return 'Hello, world!'";

    when(commandObjects.eval(script, "key")).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.eval(script, "key");

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testEvalshaWithSampleKey() {
    String sha1 = "somehash";

    when(commandObjects.evalsha(sha1, "key")).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.evalsha(sha1, "key");

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScriptExists() {
    String[] sha1 = { "somehash1", "somehash2" };

    when(commandObjects.scriptExists("key", sha1)).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.scriptExists("key", sha1);

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScriptLoad() {
    String script = "return 'Hello, world!'";

    when(commandObjects.scriptLoad(script, "key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.scriptLoad(script, "key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScriptFlush() {
    when(commandObjects.scriptFlush("key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.scriptFlush("key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScriptFlushWithFlushMode() {
    FlushMode flushMode = FlushMode.SYNC;

    when(commandObjects.scriptFlush("key", flushMode)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.scriptFlush("key", flushMode);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScriptKill() {
    when(commandObjects.scriptKill("key")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.scriptKill("key");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFcallBytes() {
    byte[] name = "functionName".getBytes();
    List<byte[]> keys = Collections.singletonList("key".getBytes());
    List<byte[]> args = Collections.singletonList("arg".getBytes());

    when(commandObjects.fcall(name, keys, args)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.fcall(name, keys, args);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFcallStrings() {
    String name = "functionName";
    List<String> keys = Collections.singletonList("key");
    List<String> args = Collections.singletonList("arg");

    when(commandObjects.fcall(name, keys, args)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.fcall(name, keys, args);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFcallReadonlyBytes() {
    byte[] name = "functionName".getBytes();
    List<byte[]> keys = Collections.singletonList("key".getBytes());
    List<byte[]> args = Collections.singletonList("arg".getBytes());

    when(commandObjects.fcallReadonly(name, keys, args)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.fcallReadonly(name, keys, args);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFcallReadonlyStrings() {
    String name = "functionName";
    List<String> keys = Collections.singletonList("key");
    List<String> args = Collections.singletonList("arg");

    when(commandObjects.fcallReadonly(name, keys, args)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.fcallReadonly(name, keys, args);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionDeleteBytes() {
    byte[] libraryName = "libraryName".getBytes();

    when(commandObjects.functionDelete(libraryName)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.functionDelete(libraryName);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionDeleteStrings() {
    String libraryName = "libraryName";

    when(commandObjects.functionDelete(libraryName)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.functionDelete(libraryName);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionDump() {
    when(commandObjects.functionDump()).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.functionDump();

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionListWithPattern() {
    String libraryNamePattern = "lib*";

    when(commandObjects.functionList(libraryNamePattern)).thenReturn(listLibraryInfoCommandObject);

    Response<List<LibraryInfo>> response = pipeliningBase.functionList(libraryNamePattern);

    assertThat(commands, contains(listLibraryInfoCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionList() {
    when(commandObjects.functionList()).thenReturn(listLibraryInfoCommandObject);

    Response<List<LibraryInfo>> response = pipeliningBase.functionList();

    assertThat(commands, contains(listLibraryInfoCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionListWithCodeWithPattern() {
    String libraryNamePattern = "lib*";

    when(commandObjects.functionListWithCode(libraryNamePattern)).thenReturn(listLibraryInfoCommandObject);

    Response<List<LibraryInfo>> response = pipeliningBase.functionListWithCode(libraryNamePattern);

    assertThat(commands, contains(listLibraryInfoCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionListWithCode() {
    when(commandObjects.functionListWithCode()).thenReturn(listLibraryInfoCommandObject);

    Response<List<LibraryInfo>> response = pipeliningBase.functionListWithCode();

    assertThat(commands, contains(listLibraryInfoCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionListByteArray() {
    when(commandObjects.functionListBinary()).thenReturn(listObjectCommandObject);

    Response<List<Object>> response = pipeliningBase.functionListBinary();

    assertThat(commands, contains(listObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionListBytes() {
    byte[] libraryNamePattern = "lib*".getBytes();

    when(commandObjects.functionList(libraryNamePattern)).thenReturn(listObjectCommandObject);

    Response<List<Object>> response = pipeliningBase.functionList(libraryNamePattern);

    assertThat(commands, contains(listObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionListWithCodeByteArray() {
    when(commandObjects.functionListWithCodeBinary()).thenReturn(listObjectCommandObject);

    Response<List<Object>> response = pipeliningBase.functionListWithCodeBinary();

    assertThat(commands, contains(listObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionListWithCodeBytes() {
    byte[] libraryNamePattern = "lib*".getBytes();

    when(commandObjects.functionListWithCode(libraryNamePattern)).thenReturn(listObjectCommandObject);

    Response<List<Object>> response = pipeliningBase.functionListWithCode(libraryNamePattern);

    assertThat(commands, contains(listObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionLoadBytes() {
    byte[] functionCode = "return 'Hello, world!'".getBytes();

    when(commandObjects.functionLoad(functionCode)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.functionLoad(functionCode);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionLoadStrings() {
    String functionCode = "return 'Hello, world!'";

    when(commandObjects.functionLoad(functionCode)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.functionLoad(functionCode);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionLoadReplaceBytes() {
    byte[] functionCode = "return 'Hello, world!'".getBytes();

    when(commandObjects.functionLoadReplace(functionCode)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.functionLoadReplace(functionCode);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionLoadReplaceStrings() {
    String functionCode = "return 'Hello, world!'";

    when(commandObjects.functionLoadReplace(functionCode)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.functionLoadReplace(functionCode);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionRestoreBytes() {
    byte[] serializedValue = "serialized".getBytes();

    when(commandObjects.functionRestore(serializedValue)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.functionRestore(serializedValue);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionRestoreBytesWithPolicy() {
    byte[] serializedValue = "serialized".getBytes();
    FunctionRestorePolicy policy = FunctionRestorePolicy.FLUSH;

    when(commandObjects.functionRestore(serializedValue, policy)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.functionRestore(serializedValue, policy);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionFlush() {
    when(commandObjects.functionFlush()).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.functionFlush();

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionFlushWithMode() {
    FlushMode mode = FlushMode.SYNC;

    when(commandObjects.functionFlush(mode)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.functionFlush(mode);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionKill() {
    when(commandObjects.functionKill()).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.functionKill();

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionStats() {
    when(commandObjects.functionStats()).thenReturn(functionStatsCommandObject);

    Response<FunctionStats> response = pipeliningBase.functionStats();

    assertThat(commands, contains(functionStatsCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFunctionStatsByteArray() {
    when(commandObjects.functionStatsBinary()).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.functionStatsBinary();

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoaddSingleMember() {
    byte[] key = "location".getBytes();
    double longitude = 13.361389;
    double latitude = 38.115556;
    byte[] member = "Sicily".getBytes();

    when(commandObjects.geoadd(key, longitude, latitude, member)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geoadd(key, longitude, latitude, member);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoaddMemberCoordinateMap() {
    byte[] key = "location".getBytes();

    Map<byte[], GeoCoordinate> memberCoordinateMap = new HashMap<>();
    memberCoordinateMap.put("Palermo".getBytes(), new GeoCoordinate(13.361389, 38.115556));

    when(commandObjects.geoadd(key, memberCoordinateMap)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geoadd(key, memberCoordinateMap);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoaddWithParams() {
    byte[] key = "location".getBytes();
    GeoAddParams params = GeoAddParams.geoAddParams();

    Map<byte[], GeoCoordinate> memberCoordinateMap = new HashMap<>();
    memberCoordinateMap.put("Palermo".getBytes(), new GeoCoordinate(13.361389, 38.115556));

    when(commandObjects.geoadd(key, params, memberCoordinateMap)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geoadd(key, params, memberCoordinateMap);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeodistByteArray() {
    byte[] key = "location".getBytes();
    byte[] member1 = "Palermo".getBytes();
    byte[] member2 = "Catania".getBytes();

    when(commandObjects.geodist(key, member1, member2)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.geodist(key, member1, member2);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeodistWithUnitByteArray() {
    byte[] key = "location".getBytes();
    byte[] member1 = "Palermo".getBytes();
    byte[] member2 = "Catania".getBytes();
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.geodist(key, member1, member2, unit)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.geodist(key, member1, member2, unit);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeohashByteArray() {
    byte[] key = "location".getBytes();
    byte[] member = "Palermo".getBytes();

    when(commandObjects.geohash(key, member)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.geohash(key, member);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoposByteArray() {
    byte[] key = "location".getBytes();
    byte[] member = "Palermo".getBytes();

    when(commandObjects.geopos(key, member)).thenReturn(listGeoCoordinateCommandObject);

    Response<List<GeoCoordinate>> response = pipeliningBase.geopos(key, member);

    assertThat(commands, contains(listGeoCoordinateCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByteArray() {
    byte[] key = "location".getBytes();
    double longitude = 13.361389;
    double latitude = 38.115556;
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.georadius(key, longitude, latitude, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.georadius(key, longitude, latitude, radius, unit);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusReadonlyByteArray() {
    byte[] key = "location".getBytes();
    double longitude = 13.361389;
    double latitude = 38.115556;
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.georadiusReadonly(key, longitude, latitude, radius, unit);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusWithParamByteArray() {
    byte[] key = "location".getBytes();
    double longitude = 13.361389;
    double latitude = 38.115556;
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();

    when(commandObjects.georadius(key, longitude, latitude, radius, unit, param)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.georadius(key, longitude, latitude, radius, unit, param);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusReadonlyWithParamByteArray() {
    byte[] key = "location".getBytes();
    double longitude = 13.361389;
    double latitude = 38.115556;
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();

    when(commandObjects.georadiusReadonly(key, longitude, latitude, radius, unit, param)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.georadiusReadonly(key, longitude, latitude, radius, unit, param);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByMemberByteArray() {
    byte[] key = "location".getBytes();
    byte[] member = "Palermo".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.georadiusByMember(key, member, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.georadiusByMember(key, member, radius, unit);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByMemberReadonlyByteArray() {
    byte[] key = "location".getBytes();
    byte[] member = "Palermo".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.georadiusByMemberReadonly(key, member, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.georadiusByMemberReadonly(key, member, radius, unit);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByMemberWithParamByteArray() {
    byte[] key = "location".getBytes();
    byte[] member = "Palermo".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();

    when(commandObjects.georadiusByMember(key, member, radius, unit, param)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.georadiusByMember(key, member, radius, unit, param);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByMemberReadonlyWithParamByteArray() {
    byte[] key = "location".getBytes();
    byte[] member = "Palermo".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();

    when(commandObjects.georadiusByMemberReadonly(key, member, radius, unit, param)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.georadiusByMemberReadonly(key, member, radius, unit, param);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusStoreByteArray() {
    byte[] key = "location".getBytes();
    double longitude = 13.361389;
    double latitude = 38.115556;
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();
    GeoRadiusStoreParam storeParam = GeoRadiusStoreParam.geoRadiusStoreParam().store("storeKey");

    when(commandObjects.georadiusStore(key, longitude, latitude, radius, unit, param, storeParam)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.georadiusStore(key, longitude, latitude, radius, unit, param, storeParam);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeoradiusByMemberStoreByteArray() {
    byte[] key = "location".getBytes();
    byte[] member = "Palermo".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;
    GeoRadiusParam param = GeoRadiusParam.geoRadiusParam();
    GeoRadiusStoreParam storeParam = GeoRadiusStoreParam.geoRadiusStoreParam().store("storeKey");

    when(commandObjects.georadiusByMemberStore(key, member, radius, unit, param, storeParam)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.georadiusByMemberStore(key, member, radius, unit, param, storeParam);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchByMemberRadiusByteArray() {
    byte[] key = "location".getBytes();
    byte[] member = "Palermo".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.geosearch(key, member, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.geosearch(key, member, radius, unit);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchByCoordinateRadiusByteArray() {
    byte[] key = "location".getBytes();
    GeoCoordinate coord = new GeoCoordinate(13.361389, 38.115556);
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.geosearch(key, coord, radius, unit)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.geosearch(key, coord, radius, unit);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchByMemberBoxByteArray() {
    byte[] key = "location".getBytes();
    byte[] member = "Palermo".getBytes();
    double width = 200;
    double height = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.geosearch(key, member, width, height, unit)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.geosearch(key, member, width, height, unit);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchByCoordinateBoxByteArray() {
    byte[] key = "location".getBytes();
    GeoCoordinate coord = new GeoCoordinate(13.361389, 38.115556);
    double width = 200;
    double height = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.geosearch(key, coord, width, height, unit)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.geosearch(key, coord, width, height, unit);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchWithParamsByteArray() {
    byte[] key = "location".getBytes();
    GeoSearchParam params = GeoSearchParam.geoSearchParam().byRadius(100, GeoUnit.KM);

    when(commandObjects.geosearch(key, params)).thenReturn(listGeoRadiusResponseCommandObject);

    Response<List<GeoRadiusResponse>> response = pipeliningBase.geosearch(key, params);

    assertThat(commands, contains(listGeoRadiusResponseCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStore() {
    byte[] dest = "destination".getBytes();
    byte[] src = "location".getBytes();
    byte[] member = "Palermo".getBytes();
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.geosearchStore(dest, src, member, radius, unit)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geosearchStore(dest, src, member, radius, unit);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreByCoordinateRadius() {
    byte[] dest = "destination".getBytes();
    byte[] src = "location".getBytes();
    GeoCoordinate coord = new GeoCoordinate(13.361389, 38.115556);
    double radius = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.geosearchStore(dest, src, coord, radius, unit)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geosearchStore(dest, src, coord, radius, unit);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreByMemberBoxByteArray() {
    byte[] dest = "destination".getBytes();
    byte[] src = "location".getBytes();
    byte[] member = "Palermo".getBytes();
    double width = 200;
    double height = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.geosearchStore(dest, src, member, width, height, unit)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geosearchStore(dest, src, member, width, height, unit);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreByCoordinateBox() {
    byte[] dest = "destination".getBytes();
    byte[] src = "location".getBytes();
    GeoCoordinate coord = new GeoCoordinate(13.361389, 38.115556);
    double width = 200;
    double height = 100;
    GeoUnit unit = GeoUnit.KM;

    when(commandObjects.geosearchStore(dest, src, coord, width, height, unit)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geosearchStore(dest, src, coord, width, height, unit);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreWithParamsByteArray() {
    byte[] dest = "destination".getBytes();
    byte[] src = "location".getBytes();
    GeoSearchParam params = GeoSearchParam.geoSearchParam().byRadius(100, GeoUnit.KM);

    when(commandObjects.geosearchStore(dest, src, params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geosearchStore(dest, src, params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGeosearchStoreStoreDistByteArray() {
    byte[] dest = "destination".getBytes();
    byte[] src = "location".getBytes();
    GeoSearchParam params = GeoSearchParam.geoSearchParam().byRadius(100, GeoUnit.KM);

    when(commandObjects.geosearchStoreStoreDist(dest, src, params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.geosearchStoreStoreDist(dest, src, params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHsetKeyValue() {
    byte[] key = "hash".getBytes();
    byte[] field = "field1".getBytes();
    byte[] value = "value1".getBytes();

    when(commandObjects.hset(key, field, value)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hset(key, field, value);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHsetMapByteArray() {
    byte[] key = "hash".getBytes();

    Map<byte[], byte[]> hash = new HashMap<>();
    hash.put("field1".getBytes(), "value1".getBytes());

    when(commandObjects.hset(key, hash)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hset(key, hash);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHgetByteArray() {
    byte[] key = "hash".getBytes();
    byte[] field = "field1".getBytes();

    when(commandObjects.hget(key, field)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.hget(key, field);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHsetnxByteArray() {
    byte[] key = "hash".getBytes();
    byte[] field = "field1".getBytes();
    byte[] value = "value1".getBytes();

    when(commandObjects.hsetnx(key, field, value)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hsetnx(key, field, value);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHmsetByteArray() {
    byte[] key = "hash".getBytes();

    Map<byte[], byte[]> hash = new HashMap<>();
    hash.put("field1".getBytes(), "value1".getBytes());

    when(commandObjects.hmset(key, hash)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.hmset(key, hash);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHmgetByteArray() {
    byte[] key = "hash".getBytes();
    byte[] field1 = "field1".getBytes();
    byte[] field2 = "field2".getBytes();

    when(commandObjects.hmget(key, field1, field2)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.hmget(key, field1, field2);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHincrByByteArray() {
    byte[] key = "hash".getBytes();
    byte[] field = "field1".getBytes();
    long increment = 2L;

    when(commandObjects.hincrBy(key, field, increment)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hincrBy(key, field, increment);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHincrByFloatByteArray() {
    byte[] key = "hash".getBytes();
    byte[] field = "field1".getBytes();
    double increment = 2.5;

    when(commandObjects.hincrByFloat(key, field, increment)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.hincrByFloat(key, field, increment);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHexistsByteArray() {
    byte[] key = "hash".getBytes();
    byte[] field = "field1".getBytes();

    when(commandObjects.hexists(key, field)).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.hexists(key, field);

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHdelByteArray() {
    byte[] key = "hash".getBytes();
    byte[] field1 = "field1".getBytes();
    byte[] field2 = "field2".getBytes();

    when(commandObjects.hdel(key, field1, field2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hdel(key, field1, field2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHlenByteArray() {
    byte[] key = "hash".getBytes();

    when(commandObjects.hlen(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hlen(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHkeysByteArray() {
    byte[] key = "hash".getBytes();

    when(commandObjects.hkeys(key)).thenReturn(setBytesCommandObject);

    Response<Set<byte[]>> response = pipeliningBase.hkeys(key);

    assertThat(commands, contains(setBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHvalsByteArray() {
    byte[] key = "hash".getBytes();

    when(commandObjects.hvals(key)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.hvals(key);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHgetAllByteArray() {
    byte[] key = "hash".getBytes();

    when(commandObjects.hgetAll(key)).thenReturn(mapBytesBytesCommandObject);

    Response<Map<byte[], byte[]>> response = pipeliningBase.hgetAll(key);

    assertThat(commands, contains(mapBytesBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHrandfieldByteArray() {
    byte[] key = "hash".getBytes();

    when(commandObjects.hrandfield(key)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.hrandfield(key);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHrandfieldWithCountByteArray() {
    byte[] key = "hash".getBytes();
    long count = 2;

    when(commandObjects.hrandfield(key, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.hrandfield(key, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHrandfieldWithValuesByteArray() {
    byte[] key = "hash".getBytes();
    long count = 2;

    when(commandObjects.hrandfieldWithValues(key, count)).thenReturn(listEntryBytesBytesCommandObject);

    Response<List<Map.Entry<byte[], byte[]>>> response = pipeliningBase.hrandfieldWithValues(key, count);

    assertThat(commands, contains(listEntryBytesBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHscanByteArray() {
    byte[] key = "hash".getBytes();
    byte[] cursor = "0".getBytes();
    ScanParams params = new ScanParams().match("*").count(10);

    when(commandObjects.hscan(key, cursor, params)).thenReturn(scanResultEntryBytesBytesCommandObject);

    Response<ScanResult<Map.Entry<byte[], byte[]>>> response = pipeliningBase.hscan(key, cursor, params);

    assertThat(commands, contains(scanResultEntryBytesBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHscanNoValuesByteArray() {
    byte[] key = "hash".getBytes();
    byte[] cursor = "0".getBytes();
    ScanParams params = new ScanParams().match("*").count(10);

    when(commandObjects.hscanNoValues(key, cursor, params)).thenReturn(scanResultBytesCommandObject);

    Response<ScanResult<byte[]>> response = pipeliningBase.hscanNoValues(key, cursor, params);

    assertThat(commands, contains(scanResultBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testHstrlenByteArray() {
    byte[] key = "hash".getBytes();
    byte[] field = "field1".getBytes();

    when(commandObjects.hstrlen(key, field)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.hstrlen(key, field);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPfaddByteArray() {
    byte[] key = "hll".getBytes();
    byte[] element1 = "element1".getBytes();
    byte[] element2 = "element2".getBytes();

    when(commandObjects.pfadd(key, element1, element2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pfadd(key, element1, element2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPfmergeByteArray() {
    byte[] destkey = "hll_dest".getBytes();
    byte[] sourcekey1 = "hll1".getBytes();
    byte[] sourcekey2 = "hll2".getBytes();

    when(commandObjects.pfmerge(destkey, sourcekey1, sourcekey2)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.pfmerge(destkey, sourcekey1, sourcekey2);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPfcountSingleKeyByteArray() {
    byte[] key = "hll".getBytes();

    when(commandObjects.pfcount(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pfcount(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPfcountMultipleKeysByteArray() {
    byte[] key1 = "hll1".getBytes();
    byte[] key2 = "hll2".getBytes();

    when(commandObjects.pfcount(key1, key2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pfcount(key1, key2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExistsByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.exists(key)).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.exists(key);

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExistsMultipleKeysByteArray() {
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.exists(key1, key2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.exists(key1, key2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPersistByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.persist(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.persist(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTypeByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.type(key)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.type(key);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testDumpByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.dump(key)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.dump(key);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRestoreByteArray() {
    byte[] key = "key".getBytes();
    long ttl = 0L;
    byte[] serializedValue = "serialized".getBytes();

    when(commandObjects.restore(key, ttl, serializedValue)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.restore(key, ttl, serializedValue);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRestoreWithParamsByteArray() {
    byte[] key = "key".getBytes();
    long ttl = 0L;
    byte[] serializedValue = "serialized".getBytes();
    RestoreParams params = RestoreParams.restoreParams().replace();

    when(commandObjects.restore(key, ttl, serializedValue, params)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.restore(key, ttl, serializedValue, params);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExpireByteArray() {
    byte[] key = "key".getBytes();
    long seconds = 60L;

    when(commandObjects.expire(key, seconds)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.expire(key, seconds);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExpireWithOptionByteArray() {
    byte[] key = "key".getBytes();
    long seconds = 60L;
    ExpiryOption expiryOption = ExpiryOption.NX;

    when(commandObjects.expire(key, seconds, expiryOption)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.expire(key, seconds, expiryOption);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPexpireByteArray() {
    byte[] key = "key".getBytes();
    long milliseconds = 60000L;

    when(commandObjects.pexpire(key, milliseconds)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pexpire(key, milliseconds);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPexpireWithOptionByteArray() {
    byte[] key = "key".getBytes();
    long milliseconds = 60000L;
    ExpiryOption expiryOption = ExpiryOption.NX;

    when(commandObjects.pexpire(key, milliseconds, expiryOption)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pexpire(key, milliseconds, expiryOption);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExpireTimeByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.expireTime(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.expireTime(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPexpireTimeByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.pexpireTime(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pexpireTime(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExpireAtByteArray() {
    byte[] key = "key".getBytes();
    long unixTime = 1625097600L;

    when(commandObjects.expireAt(key, unixTime)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.expireAt(key, unixTime);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExpireAtWithOptionByteArray() {
    byte[] key = "key".getBytes();
    long unixTime = 1625097600L;
    ExpiryOption expiryOption = ExpiryOption.NX;

    when(commandObjects.expireAt(key, unixTime, expiryOption)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.expireAt(key, unixTime, expiryOption);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPexpireAtByteArray() {
    byte[] key = "key".getBytes();
    long millisecondsTimestamp = 1625097600000L;

    when(commandObjects.pexpireAt(key, millisecondsTimestamp)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pexpireAt(key, millisecondsTimestamp);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPexpireAtWithOptionByteArray() {
    byte[] key = "key".getBytes();
    long millisecondsTimestamp = 1625097600000L;
    ExpiryOption expiryOption = ExpiryOption.NX;

    when(commandObjects.pexpireAt(key, millisecondsTimestamp, expiryOption)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pexpireAt(key, millisecondsTimestamp, expiryOption);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTtlByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.ttl(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.ttl(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPttlByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.pttl(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.pttl(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTouchSingleKeyByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.touch(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.touch(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTouchMultipleKeysByteArray() {
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.touch(key1, key2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.touch(key1, key2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSortDefaultByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.sort(key)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.sort(key);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSortWithParamsByteArray() {
    byte[] key = "key".getBytes();
    SortingParams sortingParams = new SortingParams().alpha().limit(0, 10);

    when(commandObjects.sort(key, sortingParams)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.sort(key, sortingParams);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSortReadonlyByteArray() {
    byte[] key = "key".getBytes();
    SortingParams sortingParams = new SortingParams().alpha().limit(0, 10);

    when(commandObjects.sortReadonly(key, sortingParams)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.sortReadonly(key, sortingParams);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testDelSingleKeyByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.del(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.del(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testDelMultipleKeysByteArray() {
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.del(key1, key2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.del(key1, key2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testUnlinkSingleKeyByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.unlink(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.unlink(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testUnlinkMultipleKeysByteArray() {
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.unlink(key1, key2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.unlink(key1, key2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCopyByteArray() {
    byte[] srcKey = "sourceKey".getBytes();
    byte[] dstKey = "destinationKey".getBytes();
    boolean replace = true;

    when(commandObjects.copy(srcKey, dstKey, replace)).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.copy(srcKey, dstKey, replace);

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRenameByteArray() {
    byte[] oldkey = "oldKey".getBytes();
    byte[] newkey = "newKey".getBytes();

    when(commandObjects.rename(oldkey, newkey)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.rename(oldkey, newkey);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRenamenxByteArray() {
    byte[] oldkey = "oldKey".getBytes();
    byte[] newkey = "newKey".getBytes();

    when(commandObjects.renamenx(oldkey, newkey)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.renamenx(oldkey, newkey);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSortToDstKeyWithParamsByteArray() {
    byte[] key = "key".getBytes();
    byte[] dstkey = "dstkey".getBytes();
    SortingParams sortingParams = new SortingParams().alpha().limit(0, 10);

    when(commandObjects.sort(key, sortingParams, dstkey)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sort(key, sortingParams, dstkey);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSortToDstKeyByteArray() {
    byte[] key = "key".getBytes();
    byte[] dstkey = "dstkey".getBytes();

    when(commandObjects.sort(key, dstkey)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sort(key, dstkey);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testMemoryUsageByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.memoryUsage(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.memoryUsage(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testMemoryUsageWithSamplesByteArray() {
    byte[] key = "key".getBytes();
    int samples = 5;

    when(commandObjects.memoryUsage(key, samples)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.memoryUsage(key, samples);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testObjectRefcountByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.objectRefcount(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.objectRefcount(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testObjectEncodingByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.objectEncoding(key)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.objectEncoding(key);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testObjectIdletimeByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.objectIdletime(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.objectIdletime(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testObjectFreqByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.objectFreq(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.objectFreq(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testMigrateSingleKeyByteArray() {
    String host = "localhost";
    int port = 6379;
    byte[] key = "key".getBytes();
    int timeout = 1000;

    when(commandObjects.migrate(host, port, key, timeout)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.migrate(host, port, key, timeout);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testMigrateMultipleKeysByteArray() {
    String host = "localhost";
    int port = 6379;
    int timeout = 1000;
    MigrateParams params = MigrateParams.migrateParams().copy().replace();
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.migrate(host, port, timeout, params, key1, key2)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.migrate(host, port, timeout, params, key1, key2);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testKeysByteArray() {
    byte[] pattern = "*".getBytes();

    when(commandObjects.keys(pattern)).thenReturn(setBytesCommandObject);

    Response<Set<byte[]>> response = pipeliningBase.keys(pattern);

    assertThat(commands, contains(setBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScanByteArray() {
    byte[] cursor = "0".getBytes();

    when(commandObjects.scan(cursor)).thenReturn(scanResultBytesCommandObject);

    Response<ScanResult<byte[]>> response = pipeliningBase.scan(cursor);

    assertThat(commands, contains(scanResultBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScanWithParamsByteArray() {
    byte[] cursor = "0".getBytes();
    ScanParams params = new ScanParams().match("*").count(10);

    when(commandObjects.scan(cursor, params)).thenReturn(scanResultBytesCommandObject);

    Response<ScanResult<byte[]>> response = pipeliningBase.scan(cursor, params);

    assertThat(commands, contains(scanResultBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScanWithTypeByteArray() {
    byte[] cursor = "0".getBytes();
    ScanParams params = new ScanParams().match("*").count(10);
    byte[] type = "string".getBytes();

    when(commandObjects.scan(cursor, params, type)).thenReturn(scanResultBytesCommandObject);

    Response<ScanResult<byte[]>> response = pipeliningBase.scan(cursor, params, type);

    assertThat(commands, contains(scanResultBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRandomBinaryKeyByteArray() {
    when(commandObjects.randomBinaryKey()).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.randomBinaryKey();

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRpushByteArray() {
    byte[] key = "key".getBytes();
    byte[] arg1 = "value1".getBytes();
    byte[] arg2 = "value2".getBytes();

    when(commandObjects.rpush(key, arg1, arg2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.rpush(key, arg1, arg2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLpushByteArray() {
    byte[] key = "key".getBytes();
    byte[] arg1 = "value1".getBytes();
    byte[] arg2 = "value2".getBytes();

    when(commandObjects.lpush(key, arg1, arg2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.lpush(key, arg1, arg2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLlenByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.llen(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.llen(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLrangeByteArray() {
    byte[] key = "key".getBytes();
    long start = 0;
    long stop = -1;

    when(commandObjects.lrange(key, start, stop)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.lrange(key, start, stop);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLtrimByteArray() {
    byte[] key = "key".getBytes();
    long start = 1;
    long stop = -1;

    when(commandObjects.ltrim(key, start, stop)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ltrim(key, start, stop);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLindexByteArray() {
    byte[] key = "key".getBytes();
    long index = 0;

    when(commandObjects.lindex(key, index)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.lindex(key, index);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLsetByteArray() {
    byte[] key = "key".getBytes();
    long index = 0;
    byte[] value = "value".getBytes();

    when(commandObjects.lset(key, index, value)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.lset(key, index, value);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLremByteArray() {
    byte[] key = "key".getBytes();
    long count = 1;
    byte[] value = "value".getBytes();

    when(commandObjects.lrem(key, count, value)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.lrem(key, count, value);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLpopByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.lpop(key)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.lpop(key);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLpopWithCountByteArray() {
    byte[] key = "key".getBytes();
    int count = 2;

    when(commandObjects.lpop(key, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.lpop(key, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLposSingleByteArray() {
    byte[] key = "key".getBytes();
    byte[] element = "element".getBytes();

    when(commandObjects.lpos(key, element)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.lpos(key, element);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLposWithParamsByteArray() {
    byte[] key = "key".getBytes();
    byte[] element = "element".getBytes();
    LPosParams params = new LPosParams().rank(1);

    when(commandObjects.lpos(key, element, params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.lpos(key, element, params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLposWithParamsAndCountByteArray() {
    byte[] key = "key".getBytes();
    byte[] element = "element".getBytes();
    LPosParams params = new LPosParams().rank(1);
    long count = 2;

    when(commandObjects.lpos(key, element, params, count)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.lpos(key, element, params, count);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRpopByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.rpop(key)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.rpop(key);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRpopWithCountByteArray() {
    byte[] key = "key".getBytes();
    int count = 2;

    when(commandObjects.rpop(key, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.rpop(key, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLinsertByteArray() {
    byte[] key = "key".getBytes();
    ListPosition where = ListPosition.BEFORE;
    byte[] pivot = "pivot".getBytes();
    byte[] value = "value".getBytes();

    when(commandObjects.linsert(key, where, pivot, value)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.linsert(key, where, pivot, value);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLpushxByteArray() {
    byte[] key = "key".getBytes();
    byte[] arg = "value".getBytes();

    when(commandObjects.lpushx(key, arg)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.lpushx(key, arg);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRpushxByteArray() {
    byte[] key = "key".getBytes();
    byte[] arg = "value".getBytes();

    when(commandObjects.rpushx(key, arg)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.rpushx(key, arg);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlpopWithTimeoutByteArray() {
    int timeout = 10;
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.blpop(timeout, key1, key2)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.blpop(timeout, key1, key2);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlpopWithDoubleTimeoutByteArray() {
    double timeout = 10.5;
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.blpop(timeout, key1, key2)).thenReturn(keyValueBytesBytesCommandObject);

    Response<KeyValue<byte[], byte[]>> response = pipeliningBase.blpop(timeout, key1, key2);

    assertThat(commands, contains(keyValueBytesBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBrpopWithTimeoutByteArray() {
    int timeout = 10;
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.brpop(timeout, key1, key2)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.brpop(timeout, key1, key2);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBrpopWithDoubleTimeoutByteArray() {
    double timeout = 10.5;
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.brpop(timeout, key1, key2)).thenReturn(keyValueBytesBytesCommandObject);

    Response<KeyValue<byte[], byte[]>> response = pipeliningBase.brpop(timeout, key1, key2);

    assertThat(commands, contains(keyValueBytesBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testRpoplpushByteArray() {
    byte[] srckey = "srckey".getBytes();
    byte[] dstkey = "dstkey".getBytes();

    when(commandObjects.rpoplpush(srckey, dstkey)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.rpoplpush(srckey, dstkey);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBrpoplpushByteArray() {
    byte[] source = "source".getBytes();
    byte[] destination = "destination".getBytes();
    int timeout = 10;

    when(commandObjects.brpoplpush(source, destination, timeout)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.brpoplpush(source, destination, timeout);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLmoveByteArray() {
    byte[] srcKey = "srcKey".getBytes();
    byte[] dstKey = "dstKey".getBytes();
    ListDirection from = ListDirection.LEFT;
    ListDirection to = ListDirection.RIGHT;

    when(commandObjects.lmove(srcKey, dstKey, from, to)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.lmove(srcKey, dstKey, from, to);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlmoveByteArray() {
    byte[] srcKey = "srcKey".getBytes();
    byte[] dstKey = "dstKey".getBytes();
    ListDirection from = ListDirection.LEFT;
    ListDirection to = ListDirection.RIGHT;
    double timeout = 10.5;

    when(commandObjects.blmove(srcKey, dstKey, from, to, timeout)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.blmove(srcKey, dstKey, from, to, timeout);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLmpopByteArray() {
    ListDirection direction = ListDirection.LEFT;
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.lmpop(direction, key1, key2)).thenReturn(keyValueBytesListBytesCommandObject);

    Response<KeyValue<byte[], List<byte[]>>> response = pipeliningBase.lmpop(direction, key1, key2);

    assertThat(commands, contains(keyValueBytesListBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLmpopWithCountByteArray() {
    ListDirection direction = ListDirection.LEFT;
    int count = 2;
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.lmpop(direction, count, key1, key2)).thenReturn(keyValueBytesListBytesCommandObject);

    Response<KeyValue<byte[], List<byte[]>>> response = pipeliningBase.lmpop(direction, count, key1, key2);

    assertThat(commands, contains(keyValueBytesListBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlmpopByteArray() {
    double timeout = 10.5;
    ListDirection direction = ListDirection.LEFT;
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.blmpop(timeout, direction, key1, key2)).thenReturn(keyValueBytesListBytesCommandObject);

    Response<KeyValue<byte[], List<byte[]>>> response = pipeliningBase.blmpop(timeout, direction, key1, key2);

    assertThat(commands, contains(keyValueBytesListBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBlmpopWithCountByteArray() {
    double timeout = 10.5;
    ListDirection direction = ListDirection.LEFT;
    int count = 2;
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.blmpop(timeout, direction, count, key1, key2)).thenReturn(keyValueBytesListBytesCommandObject);

    Response<KeyValue<byte[], List<byte[]>>> response = pipeliningBase.blmpop(timeout, direction, count, key1, key2);

    assertThat(commands, contains(keyValueBytesListBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testWaitReplicasByteArray() {
    byte[] sampleKey = "sampleKey".getBytes();
    int replicas = 1;
    long timeout = 1000;

    when(commandObjects.waitReplicas(sampleKey, replicas, timeout)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.waitReplicas(sampleKey, replicas, timeout);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testWaitAOFByteArray() {
    byte[] sampleKey = "sampleKey".getBytes();
    long numLocal = 1;
    long numReplicas = 1;
    long timeout = 1000;

    when(commandObjects.waitAOF(sampleKey, numLocal, numReplicas, timeout)).thenReturn(keyValueLongLongCommandObject);

    Response<KeyValue<Long, Long>> response = pipeliningBase.waitAOF(sampleKey, numLocal, numReplicas, timeout);

    assertThat(commands, contains(keyValueLongLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testEvalByteArray() {
    byte[] script = "return 'Hello, world!'".getBytes();
    byte[] sampleKey = "sampleKey".getBytes();

    when(commandObjects.eval(script, sampleKey)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.eval(script, sampleKey);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testEvalshaByteArray() {
    byte[] sha1 = "abcdef1234567890".getBytes();
    byte[] sampleKey = "sampleKey".getBytes();

    when(commandObjects.evalsha(sha1, sampleKey)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.evalsha(sha1, sampleKey);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScriptExistsByteArray() {
    byte[] sampleKey = "sampleKey".getBytes();
    byte[] sha1 = "abcdef1234567890".getBytes();

    when(commandObjects.scriptExists(sampleKey, sha1)).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.scriptExists(sampleKey, sha1);

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScriptLoadByteArray() {
    byte[] script = "return 'Hello, world!'".getBytes();
    byte[] sampleKey = "sampleKey".getBytes();

    when(commandObjects.scriptLoad(script, sampleKey)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.scriptLoad(script, sampleKey);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScriptFlushByteArray() {
    byte[] sampleKey = "sampleKey".getBytes();

    when(commandObjects.scriptFlush(sampleKey)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.scriptFlush(sampleKey);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScriptFlushWithFlushModeByteArray() {
    byte[] sampleKey = "sampleKey".getBytes();
    FlushMode flushMode = FlushMode.SYNC;

    when(commandObjects.scriptFlush(sampleKey, flushMode)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.scriptFlush(sampleKey, flushMode);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScriptKillByteArray() {
    byte[] sampleKey = "sampleKey".getBytes();

    when(commandObjects.scriptKill(sampleKey)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.scriptKill(sampleKey);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testEvalNoKeysByteArray() {
    byte[] script = "return 'Hello, world!'".getBytes();

    when(commandObjects.eval(script)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.eval(script);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testEvalWithKeyCountAndParamsByteArray() {
    byte[] script = "return KEYS[1]".getBytes();
    int keyCount = 1;
    byte[] param1 = "key1".getBytes();

    when(commandObjects.eval(script, keyCount, param1)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.eval(script, keyCount, param1);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testEvalWithKeysAndArgsByteArray() {
    byte[] script = "return {KEYS[1], ARGV[1]}".getBytes();
    List<byte[]> keys = Collections.singletonList("key1".getBytes());
    List<byte[]> args = Collections.singletonList("arg1".getBytes());

    when(commandObjects.eval(script, keys, args)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.eval(script, keys, args);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testEvalReadonlyWithKeysAndArgsByteArray() {
    byte[] script = "return {KEYS[1], ARGV[1]}".getBytes();
    List<byte[]> keys = Collections.singletonList("key1".getBytes());
    List<byte[]> args = Collections.singletonList("arg1".getBytes());

    when(commandObjects.evalReadonly(script, keys, args)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.evalReadonly(script, keys, args);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testEvalshaNoKeysByteArray() {
    byte[] sha1 = "abcdef1234567890".getBytes();

    when(commandObjects.evalsha(sha1)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.evalsha(sha1);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testEvalshaWithKeyCountAndParamsByteArray() {
    byte[] sha1 = "abcdef1234567890".getBytes();
    int keyCount = 1;
    byte[] param1 = "key1".getBytes();

    when(commandObjects.evalsha(sha1, keyCount, param1)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.evalsha(sha1, keyCount, param1);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testEvalshaWithKeysAndArgsByteArray() {
    byte[] sha1 = "abcdef1234567890".getBytes();
    List<byte[]> keys = Collections.singletonList("key1".getBytes());
    List<byte[]> args = Collections.singletonList("arg1".getBytes());

    when(commandObjects.evalsha(sha1, keys, args)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.evalsha(sha1, keys, args);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testEvalshaReadonlyWithKeysAndArgsByteArray() {
    byte[] sha1 = "abcdef1234567890".getBytes();
    List<byte[]> keys = Collections.singletonList("key1".getBytes());
    List<byte[]> args = Collections.singletonList("arg1".getBytes());

    when(commandObjects.evalshaReadonly(sha1, keys, args)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.evalshaReadonly(sha1, keys, args);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSaddByteArray() {
    byte[] key = "key".getBytes();
    byte[] member1 = "member1".getBytes();
    byte[] member2 = "member2".getBytes();

    when(commandObjects.sadd(key, member1, member2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sadd(key, member1, member2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSmembersByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.smembers(key)).thenReturn(setBytesCommandObject);

    Response<Set<byte[]>> response = pipeliningBase.smembers(key);

    assertThat(commands, contains(setBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSremByteArray() {
    byte[] key = "key".getBytes();
    byte[] member1 = "member1".getBytes();
    byte[] member2 = "member2".getBytes();

    when(commandObjects.srem(key, member1, member2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.srem(key, member1, member2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSpopSingleByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.spop(key)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.spop(key);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSpopMultipleByteArray() {
    byte[] key = "key".getBytes();
    long count = 2;

    when(commandObjects.spop(key, count)).thenReturn(setBytesCommandObject);

    Response<Set<byte[]>> response = pipeliningBase.spop(key, count);

    assertThat(commands, contains(setBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testScardByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.scard(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.scard(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSismemberByteArray() {
    byte[] key = "key".getBytes();
    byte[] member = "member".getBytes();

    when(commandObjects.sismember(key, member)).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.sismember(key, member);

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSmismemberByteArray() {
    byte[] key = "key".getBytes();
    byte[] member1 = "member1".getBytes();
    byte[] member2 = "member2".getBytes();

    when(commandObjects.smismember(key, member1, member2)).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.smismember(key, member1, member2);

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSrandmemberSingleByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.srandmember(key)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.srandmember(key);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSrandmemberMultipleByteArray() {
    byte[] key = "key".getBytes();
    int count = 2;

    when(commandObjects.srandmember(key, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.srandmember(key, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSscanByteArray() {
    byte[] key = "key".getBytes();
    byte[] cursor = "0".getBytes();
    ScanParams params = new ScanParams().match("pattern*").count(10);

    when(commandObjects.sscan(key, cursor, params)).thenReturn(scanResultBytesCommandObject);

    Response<ScanResult<byte[]>> response = pipeliningBase.sscan(key, cursor, params);

    assertThat(commands, contains(scanResultBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSdiffByteArray() {
    byte[][] keys = { "key1".getBytes(), "key2".getBytes(), "key3".getBytes() };

    when(commandObjects.sdiff(keys)).thenReturn(setBytesCommandObject);

    Response<Set<byte[]>> response = pipeliningBase.sdiff(keys);

    assertThat(commands, contains(setBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSdiffstoreByteArray() {
    byte[] dstkey = "destination".getBytes();
    byte[][] keys = { "key1".getBytes(), "key2".getBytes() };

    when(commandObjects.sdiffstore(dstkey, keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sdiffstore(dstkey, keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSinterByteArray() {
    byte[][] keys = { "key1".getBytes(), "key2".getBytes() };

    when(commandObjects.sinter(keys)).thenReturn(setBytesCommandObject);

    Response<Set<byte[]>> response = pipeliningBase.sinter(keys);

    assertThat(commands, contains(setBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSinterstoreByteArray() {
    byte[] dstkey = "destination".getBytes();
    byte[][] keys = { "key1".getBytes(), "key2".getBytes() };

    when(commandObjects.sinterstore(dstkey, keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sinterstore(dstkey, keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSintercardByteArray() {
    byte[][] keys = { "key1".getBytes(), "key2".getBytes() };

    when(commandObjects.sintercard(keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sintercard(keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSintercardWithLimitByteArray() {
    int limit = 2;
    byte[][] keys = { "key1".getBytes(), "key2".getBytes() };

    when(commandObjects.sintercard(limit, keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sintercard(limit, keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSunionByteArray() {
    byte[][] keys = { "key1".getBytes(), "key2".getBytes() };

    when(commandObjects.sunion(keys)).thenReturn(setBytesCommandObject);

    Response<Set<byte[]>> response = pipeliningBase.sunion(keys);

    assertThat(commands, contains(setBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSunionstoreByteArray() {
    byte[] dstkey = "destination".getBytes();
    byte[][] keys = { "key1".getBytes(), "key2".getBytes() };

    when(commandObjects.sunionstore(dstkey, keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.sunionstore(dstkey, keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSmoveByteArray() {
    byte[] srckey = "source".getBytes();
    byte[] dstkey = "destination".getBytes();
    byte[] member = "member".getBytes();

    when(commandObjects.smove(srckey, dstkey, member)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.smove(srckey, dstkey, member);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZaddByteArray() {
    byte[] key = "zset".getBytes();
    double score = 1.0;
    byte[] member = "member".getBytes();

    when(commandObjects.zadd(key, score, member)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zadd(key, score, member);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZaddWithParamsByteArray() {
    byte[] key = "zset".getBytes();
    double score = 1.0;
    byte[] member = "member".getBytes();
    ZAddParams params = ZAddParams.zAddParams().nx();

    when(commandObjects.zadd(key, score, member, params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zadd(key, score, member, params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZaddWithScoreMembersByteArray() {
    byte[] key = "zset".getBytes();

    Map<byte[], Double> scoreMembers = new HashMap<>();
    scoreMembers.put("member1".getBytes(), 1.0);
    scoreMembers.put("member2".getBytes(), 2.0);

    when(commandObjects.zadd(key, scoreMembers)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zadd(key, scoreMembers);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZaddWithScoreMembersAndParamsByteArray() {
    byte[] key = "zset".getBytes();

    Map<byte[], Double> scoreMembers = new HashMap<>();
    scoreMembers.put("member1".getBytes(), 1.0);
    scoreMembers.put("member2".getBytes(), 2.0);

    ZAddParams params = ZAddParams.zAddParams().nx();

    when(commandObjects.zadd(key, scoreMembers, params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zadd(key, scoreMembers, params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZaddIncrByteArray() {
    byte[] key = "zset".getBytes();
    double score = 1.0;
    byte[] member = "member".getBytes();
    ZAddParams params = ZAddParams.zAddParams().xx();

    when(commandObjects.zaddIncr(key, score, member, params)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.zaddIncr(key, score, member, params);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZremByteArray() {
    byte[] key = "zset".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };

    when(commandObjects.zrem(key, members)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zrem(key, members);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZincrbyByteArray() {
    byte[] key = "zset".getBytes();
    double increment = 2.0;
    byte[] member = "member".getBytes();

    when(commandObjects.zincrby(key, increment, member)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.zincrby(key, increment, member);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZincrbyWithParamsByteArray() {
    byte[] key = "zset".getBytes();
    double increment = 2.0;
    byte[] member = "member".getBytes();
    ZIncrByParams params = ZIncrByParams.zIncrByParams().xx();

    when(commandObjects.zincrby(key, increment, member, params)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.zincrby(key, increment, member, params);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrankByteArray() {
    byte[] key = "zset".getBytes();
    byte[] member = "member".getBytes();

    when(commandObjects.zrank(key, member)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zrank(key, member);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrankByteArray() {
    byte[] key = "zset".getBytes();
    byte[] member = "member".getBytes();

    when(commandObjects.zrevrank(key, member)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zrevrank(key, member);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrankWithScoreByteArray() {
    byte[] key = "zset".getBytes();
    byte[] member = "member".getBytes();

    when(commandObjects.zrankWithScore(key, member)).thenReturn(keyValueLongDoubleCommandObject);

    Response<KeyValue<Long, Double>> response = pipeliningBase.zrankWithScore(key, member);

    assertThat(commands, contains(keyValueLongDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrankWithScoreByteArray() {
    byte[] key = "zset".getBytes();
    byte[] member = "member".getBytes();

    when(commandObjects.zrevrankWithScore(key, member)).thenReturn(keyValueLongDoubleCommandObject);

    Response<KeyValue<Long, Double>> response = pipeliningBase.zrevrankWithScore(key, member);

    assertThat(commands, contains(keyValueLongDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByteArray() {
    byte[] key = "zset".getBytes();
    long start = 0;
    long stop = 1;

    when(commandObjects.zrange(key, start, stop)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrange(key, start, stop);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByteArray() {
    byte[] key = "zset".getBytes();
    long start = 0;
    long stop = 1;

    when(commandObjects.zrevrange(key, start, stop)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrevrange(key, start, stop);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeWithScoresByteArray() {
    byte[] key = "zset".getBytes();
    long start = 0;
    long stop = 1;

    when(commandObjects.zrangeWithScores(key, start, stop)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeWithScores(key, start, stop);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeWithScoresByteArray() {
    byte[] key = "zset".getBytes();
    long start = 0;
    long stop = 1;

    when(commandObjects.zrevrangeWithScores(key, start, stop)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrevrangeWithScores(key, start, stop);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrandmemberByteArray() {
    byte[] key = "zset".getBytes();

    when(commandObjects.zrandmember(key)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.zrandmember(key);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrandmemberWithCountByteArray() {
    byte[] key = "zset".getBytes();
    long count = 2;

    when(commandObjects.zrandmember(key, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrandmember(key, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrandmemberWithScoresByteArray() {
    byte[] key = "zset".getBytes();
    long count = 2;

    when(commandObjects.zrandmemberWithScores(key, count)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrandmemberWithScores(key, count);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZcardByteArray() {
    byte[] key = "zset".getBytes();

    when(commandObjects.zcard(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zcard(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZscoreByteArray() {
    byte[] key = "zset".getBytes();
    byte[] member = "member".getBytes();

    when(commandObjects.zscore(key, member)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.zscore(key, member);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZmscoreByteArray() {
    byte[] key = "zset".getBytes();
    byte[][] members = { "member1".getBytes(), "member2".getBytes() };

    when(commandObjects.zmscore(key, members)).thenReturn(listDoubleCommandObject);

    Response<List<Double>> response = pipeliningBase.zmscore(key, members);

    assertThat(commands, contains(listDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZpopmaxByteArray() {
    byte[] key = "zset".getBytes();

    when(commandObjects.zpopmax(key)).thenReturn(tupleCommandObject);

    Response<Tuple> response = pipeliningBase.zpopmax(key);

    assertThat(commands, contains(tupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZpopmaxWithCountByteArray() {
    byte[] key = "zset".getBytes();
    int count = 2;

    when(commandObjects.zpopmax(key, count)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zpopmax(key, count);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZpopminByteArray() {
    byte[] key = "zset".getBytes();

    when(commandObjects.zpopmin(key)).thenReturn(tupleCommandObject);

    Response<Tuple> response = pipeliningBase.zpopmin(key);

    assertThat(commands, contains(tupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZpopminWithCountByteArray() {
    byte[] key = "zset".getBytes();
    int count = 2;

    when(commandObjects.zpopmin(key, count)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zpopmin(key, count);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZcountWithScoreRangeByteArray() {
    byte[] key = "zset".getBytes();
    double min = 1.0;
    double max = 2.0;

    when(commandObjects.zcount(key, min, max)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zcount(key, min, max);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZcountWithLexRangeByteArray() {
    byte[] key = "zset".getBytes();
    byte[] min = "min".getBytes();
    byte[] max = "max".getBytes();

    when(commandObjects.zcount(key, min, max)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zcount(key, min, max);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreByteArray() {
    byte[] key = "zset".getBytes();
    double min = 1.0;
    double max = 2.0;

    when(commandObjects.zrangeByScore(key, min, max)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrangeByScore(key, min, max);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithMinMaxBytesByteArray() {
    byte[] key = "zset".getBytes();
    byte[] min = "1".getBytes();
    byte[] max = "2".getBytes();

    when(commandObjects.zrangeByScore(key, min, max)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrangeByScore(key, min, max);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreByteArray() {
    byte[] key = "zset".getBytes();
    double max = 2.0;
    double min = 1.0;

    when(commandObjects.zrevrangeByScore(key, max, min)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrevrangeByScore(key, max, min);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithOffsetCountByteArray() {
    byte[] key = "zset".getBytes();
    double min = 1.0;
    double max = 2.0;
    int offset = 0;
    int count = 2;

    when(commandObjects.zrangeByScore(key, min, max, offset, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrangeByScore(key, min, max, offset, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreWithMinMaxBytesByteArray() {
    byte[] key = "zset".getBytes();
    byte[] max = "2".getBytes();
    byte[] min = "1".getBytes();

    when(commandObjects.zrevrangeByScore(key, max, min)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrevrangeByScore(key, max, min);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithMinMaxBytesOffsetCountByteArray() {
    byte[] key = "zset".getBytes();
    byte[] min = "1".getBytes();
    byte[] max = "2".getBytes();
    int offset = 0;
    int count = 2;

    when(commandObjects.zrangeByScore(key, min, max, offset, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrangeByScore(key, min, max, offset, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreWithOffsetCountByteArray() {
    byte[] key = "zset".getBytes();
    double max = 2.0;
    double min = 1.0;
    int offset = 0;
    int count = 2;

    when(commandObjects.zrevrangeByScore(key, max, min, offset, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrevrangeByScore(key, max, min, offset, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithScoresByteArray() {
    byte[] key = "zset".getBytes();
    double min = 1.0;
    double max = 2.0;

    when(commandObjects.zrangeByScoreWithScores(key, min, max)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeByScoreWithScores(key, min, max);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreWithScoresByteArray() {
    byte[] key = "zset".getBytes();
    double max = 2.0;
    double min = 1.0;

    when(commandObjects.zrevrangeByScoreWithScores(key, max, min)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrevrangeByScoreWithScores(key, max, min);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithScoresOffsetCountByteArray() {
    byte[] key = "zset".getBytes();
    double min = 1.0;
    double max = 2.0;
    int offset = 0;
    int count = 2;

    when(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeByScoreWithScores(key, min, max, offset, count);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreBytesOffsetCountByteArray() {
    byte[] key = "zset".getBytes();
    byte[] max = "2".getBytes();
    byte[] min = "1".getBytes();
    int offset = 0;
    int count = 2;

    when(commandObjects.zrevrangeByScore(key, max, min, offset, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrevrangeByScore(key, max, min, offset, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithScoresBytesByteArray() {
    byte[] key = "zset".getBytes();
    byte[] min = "1".getBytes();
    byte[] max = "2".getBytes();

    when(commandObjects.zrangeByScoreWithScores(key, min, max)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeByScoreWithScores(key, min, max);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreWithScoresBytesByteArray() {
    byte[] key = "zset".getBytes();
    byte[] max = "2".getBytes();
    byte[] min = "1".getBytes();

    when(commandObjects.zrevrangeByScoreWithScores(key, max, min)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrevrangeByScoreWithScores(key, max, min);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByScoreWithScoresBytesOffsetCountByteArray() {
    byte[] key = "zset".getBytes();
    byte[] min = "1".getBytes();
    byte[] max = "2".getBytes();
    int offset = 0;
    int count = 2;

    when(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeByScoreWithScores(key, min, max, offset, count);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreWithScoresOffsetCountByteArray() {
    byte[] key = "zset".getBytes();
    double max = 2.0;
    double min = 1.0;
    int offset = 0;
    int count = 2;

    when(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrevrangeByScoreWithScores(key, max, min, offset, count);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByScoreWithScoresBytesOffsetCountByteArray() {
    byte[] key = "zset".getBytes();
    byte[] max = "2".getBytes();
    byte[] min = "1".getBytes();
    int offset = 0;
    int count = 2;

    when(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrevrangeByScoreWithScores(key, max, min, offset, count);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZremrangeByRankByteArray() {
    byte[] key = "zset".getBytes();
    long start = 0;
    long stop = 1;

    when(commandObjects.zremrangeByRank(key, start, stop)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zremrangeByRank(key, start, stop);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZremrangeByScoreByteArray() {
    byte[] key = "zset".getBytes();
    double min = 1.0;
    double max = 2.0;

    when(commandObjects.zremrangeByScore(key, min, max)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zremrangeByScore(key, min, max);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZremrangeByScoreBytesByteArray() {
    byte[] key = "zset".getBytes();
    byte[] min = "1".getBytes();
    byte[] max = "2".getBytes();

    when(commandObjects.zremrangeByScore(key, min, max)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zremrangeByScore(key, min, max);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZlexcountByteArray() {
    byte[] key = "zset".getBytes();
    byte[] min = "[a".getBytes();
    byte[] max = "[z".getBytes();

    when(commandObjects.zlexcount(key, min, max)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zlexcount(key, min, max);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByLexByteArray() {
    byte[] key = "zset".getBytes();
    byte[] min = "[a".getBytes();
    byte[] max = "[z".getBytes();

    when(commandObjects.zrangeByLex(key, min, max)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrangeByLex(key, min, max);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeByLexWithLimitByteArray() {
    byte[] key = "zset".getBytes();
    byte[] min = "[a".getBytes();
    byte[] max = "[z".getBytes();
    int offset = 0;
    int count = 10;

    when(commandObjects.zrangeByLex(key, min, max, offset, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrangeByLex(key, min, max, offset, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByLexByteArray() {
    byte[] key = "zset".getBytes();
    byte[] max = "[z".getBytes();
    byte[] min = "[a".getBytes();

    when(commandObjects.zrevrangeByLex(key, max, min)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrevrangeByLex(key, max, min);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrevrangeByLexWithLimitByteArray() {
    byte[] key = "zset".getBytes();
    byte[] max = "[z".getBytes();
    byte[] min = "[a".getBytes();
    int offset = 0;
    int count = 10;

    when(commandObjects.zrevrangeByLex(key, max, min, offset, count)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrevrangeByLex(key, max, min, offset, count);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeWithZRangeParamsByteArray() {
    byte[] key = "zset".getBytes();
    ZRangeParams zRangeParams = ZRangeParams.zrangeParams(0, 1);

    when(commandObjects.zrange(key, zRangeParams)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zrange(key, zRangeParams);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangeWithScoresWithZRangeParamsByteArray() {
    byte[] key = "zset".getBytes();
    ZRangeParams zRangeParams = ZRangeParams.zrangeParams(0, 1);

    when(commandObjects.zrangeWithScores(key, zRangeParams)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zrangeWithScores(key, zRangeParams);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZrangestoreByteArray() {
    byte[] dest = "destZset".getBytes();
    byte[] src = "srcZset".getBytes();
    ZRangeParams zRangeParams = ZRangeParams.zrangeParams(0, 1);

    when(commandObjects.zrangestore(dest, src, zRangeParams)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zrangestore(dest, src, zRangeParams);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZremrangeByLexByteArray() {
    byte[] key = "zset".getBytes();
    byte[] min = "[a".getBytes();
    byte[] max = "[z".getBytes();

    when(commandObjects.zremrangeByLex(key, min, max)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zremrangeByLex(key, min, max);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZscanByteArray() {
    byte[] key = "zset".getBytes();
    byte[] cursor = "0".getBytes();
    ScanParams params = new ScanParams();

    when(commandObjects.zscan(key, cursor, params)).thenReturn(scanResultTupleCommandObject);

    Response<ScanResult<Tuple>> response = pipeliningBase.zscan(key, cursor, params);

    assertThat(commands, contains(scanResultTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBzpopmaxByteArray() {
    double timeout = 1.0;
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.bzpopmax(timeout, keys)).thenReturn(keyValueBytesTupleCommandObject);

    Response<KeyValue<byte[], Tuple>> response = pipeliningBase.bzpopmax(timeout, keys);

    assertThat(commands, contains(keyValueBytesTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBzpopminByteArray() {
    double timeout = 1.0;
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.bzpopmin(timeout, keys)).thenReturn(keyValueBytesTupleCommandObject);

    Response<KeyValue<byte[], Tuple>> response = pipeliningBase.bzpopmin(timeout, keys);

    assertThat(commands, contains(keyValueBytesTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZmpopByteArray() {
    SortedSetOption option = SortedSetOption.MAX;
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zmpop(option, keys)).thenReturn(keyValueBytesListTupleCommandObject);

    Response<KeyValue<byte[], List<Tuple>>> response = pipeliningBase.zmpop(option, keys);

    assertThat(commands, contains(keyValueBytesListTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZmpopWithCountByteArray() {
    SortedSetOption option = SortedSetOption.MAX;
    int count = 2;
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zmpop(option, count, keys)).thenReturn(keyValueBytesListTupleCommandObject);

    Response<KeyValue<byte[], List<Tuple>>> response = pipeliningBase.zmpop(option, count, keys);

    assertThat(commands, contains(keyValueBytesListTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBzmpopByteArray() {
    double timeout = 1.0;
    SortedSetOption option = SortedSetOption.MAX;
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.bzmpop(timeout, option, keys)).thenReturn(keyValueBytesListTupleCommandObject);

    Response<KeyValue<byte[], List<Tuple>>> response = pipeliningBase.bzmpop(timeout, option, keys);

    assertThat(commands, contains(keyValueBytesListTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBzmpopWithCountByteArray() {
    double timeout = 1.0;
    SortedSetOption option = SortedSetOption.MAX;
    int count = 2;
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.bzmpop(timeout, option, count, keys)).thenReturn(keyValueBytesListTupleCommandObject);

    Response<KeyValue<byte[], List<Tuple>>> response = pipeliningBase.bzmpop(timeout, option, count, keys);

    assertThat(commands, contains(keyValueBytesListTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZdiffByteArray() {
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zdiff(keys)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zdiff(keys);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZdiffWithScoresByteArray() {
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zdiffWithScores(keys)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zdiffWithScores(keys);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZdiffStoreByteArray() {
    byte[] dstkey = "destZset".getBytes();
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zdiffStore(dstkey, keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zdiffStore(dstkey, keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZdifftoreByteArray() {
    byte[] dstkey = "destZset".getBytes();
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zdiffstore(dstkey, keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zdiffstore(dstkey, keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZinterByteArray() {
    ZParams params = new ZParams();
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zinter(params, keys)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zinter(params, keys);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZinterWithScoresByteArray() {
    ZParams params = new ZParams();
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zinterWithScores(params, keys)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zinterWithScores(params, keys);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZinterstoreByteArray() {
    byte[] dstkey = "destZset".getBytes();
    byte[][] sets = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zinterstore(dstkey, sets)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zinterstore(dstkey, sets);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZinterstoreWithParamsByteArray() {
    byte[] dstkey = "destZset".getBytes();
    ZParams params = new ZParams();
    byte[][] sets = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zinterstore(dstkey, params, sets)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zinterstore(dstkey, params, sets);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZintercardByteArray() {
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zintercard(keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zintercard(keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZintercardWithLimitByteArray() {
    long limit = 2;
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zintercard(limit, keys)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zintercard(limit, keys);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZunionByteArray() {
    ZParams params = new ZParams();
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zunion(params, keys)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.zunion(params, keys);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZunionWithScoresByteArray() {
    ZParams params = new ZParams();
    byte[][] keys = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zunionWithScores(params, keys)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.zunionWithScores(params, keys);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZunionstoreByteArray() {
    byte[] dstkey = "destZset".getBytes();
    byte[][] sets = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zunionstore(dstkey, sets)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zunionstore(dstkey, sets);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testZunionstoreWithParamsByteArray() {
    byte[] dstkey = "destZset".getBytes();
    ZParams params = new ZParams();
    byte[][] sets = { "zset1".getBytes(), "zset2".getBytes() };

    when(commandObjects.zunionstore(dstkey, params, sets)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.zunionstore(dstkey, params, sets);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXaddByteArray() {
    byte[] key = "stream".getBytes();
    XAddParams params = new XAddParams();

    Map<byte[], byte[]> hash = new HashMap<>();
    hash.put("field1".getBytes(), "value1".getBytes());

    when(commandObjects.xadd(key, params, hash)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.xadd(key, params, hash);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXlenByteArray() {
    byte[] key = "stream".getBytes();

    when(commandObjects.xlen(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.xlen(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXrangeByteArray() {
    byte[] key = "stream".getBytes();
    byte[] start = "startId".getBytes();
    byte[] end = "endId".getBytes();

    when(commandObjects.xrange(key, start, end)).thenReturn(listObjectCommandObject);

    Response<List<Object>> response = pipeliningBase.xrange(key, start, end);

    assertThat(commands, contains(listObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXrangeWithCountByteArray() {
    byte[] key = "stream".getBytes();
    byte[] start = "startId".getBytes();
    byte[] end = "endId".getBytes();
    int count = 10;

    when(commandObjects.xrange(key, start, end, count)).thenReturn(listObjectCommandObject);

    Response<List<Object>> response = pipeliningBase.xrange(key, start, end, count);

    assertThat(commands, contains(listObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXrevrangeByteArray() {
    byte[] key = "stream".getBytes();
    byte[] end = "endId".getBytes();
    byte[] start = "startId".getBytes();

    when(commandObjects.xrevrange(key, end, start)).thenReturn(listObjectCommandObject);

    Response<List<Object>> response = pipeliningBase.xrevrange(key, end, start);

    assertThat(commands, contains(listObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXrevrangeWithCountByteArray() {
    byte[] key = "stream".getBytes();
    byte[] end = "endId".getBytes();
    byte[] start = "startId".getBytes();
    int count = 10;

    when(commandObjects.xrevrange(key, end, start, count)).thenReturn(listObjectCommandObject);

    Response<List<Object>> response = pipeliningBase.xrevrange(key, end, start, count);

    assertThat(commands, contains(listObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXackByteArray() {
    byte[] key = "stream".getBytes();
    byte[] group = "group".getBytes();
    byte[] id1 = "id1".getBytes();
    byte[] id2 = "id2".getBytes();

    when(commandObjects.xack(key, group, id1, id2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.xack(key, group, id1, id2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXgroupCreateByteArray() {
    byte[] key = "stream".getBytes();
    byte[] groupName = "group".getBytes();
    byte[] id = "id".getBytes();
    boolean makeStream = true;

    when(commandObjects.xgroupCreate(key, groupName, id, makeStream)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.xgroupCreate(key, groupName, id, makeStream);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXgroupSetIDByteArray() {
    byte[] key = "stream".getBytes();
    byte[] groupName = "group".getBytes();
    byte[] id = "id".getBytes();

    when(commandObjects.xgroupSetID(key, groupName, id)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.xgroupSetID(key, groupName, id);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXgroupDestroyByteArray() {
    byte[] key = "stream".getBytes();
    byte[] groupName = "group".getBytes();

    when(commandObjects.xgroupDestroy(key, groupName)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.xgroupDestroy(key, groupName);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXgroupCreateConsumerByteArray() {
    byte[] key = "stream".getBytes();
    byte[] groupName = "group".getBytes();
    byte[] consumerName = "consumer".getBytes();

    when(commandObjects.xgroupCreateConsumer(key, groupName, consumerName)).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.xgroupCreateConsumer(key, groupName, consumerName);

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXgroupDelConsumerByteArray() {
    byte[] key = "stream".getBytes();
    byte[] groupName = "group".getBytes();
    byte[] consumerName = "consumer".getBytes();

    when(commandObjects.xgroupDelConsumer(key, groupName, consumerName)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.xgroupDelConsumer(key, groupName, consumerName);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXdelByteArray() {
    byte[] key = "stream".getBytes();
    byte[] id1 = "id1".getBytes();
    byte[] id2 = "id2".getBytes();

    when(commandObjects.xdel(key, id1, id2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.xdel(key, id1, id2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXtrimByteArray() {
    byte[] key = "stream".getBytes();
    long maxLen = 1000L;
    boolean approximateLength = true;

    when(commandObjects.xtrim(key, maxLen, approximateLength)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.xtrim(key, maxLen, approximateLength);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXtrimWithParamsByteArray() {
    byte[] key = "stream".getBytes();
    XTrimParams params = new XTrimParams().maxLen(1000L);

    when(commandObjects.xtrim(key, params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.xtrim(key, params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXpendingByteArray() {
    byte[] key = "stream".getBytes();
    byte[] groupName = "group".getBytes();

    when(commandObjects.xpending(key, groupName)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.xpending(key, groupName);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXpendingWithParamsByteArray() {
    byte[] key = "stream".getBytes();
    byte[] groupName = "group".getBytes();
    XPendingParams params = new XPendingParams().count(10);

    when(commandObjects.xpending(key, groupName, params)).thenReturn(listObjectCommandObject);

    Response<List<Object>> response = pipeliningBase.xpending(key, groupName, params);

    assertThat(commands, contains(listObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXclaimByteArray() {
    byte[] key = "stream".getBytes();
    byte[] group = "group".getBytes();
    byte[] consumerName = "consumer".getBytes();
    long minIdleTime = 10000L;
    XClaimParams params = new XClaimParams();
    byte[] id1 = "id1".getBytes();
    byte[] id2 = "id2".getBytes();

    when(commandObjects.xclaim(key, group, consumerName, minIdleTime, params, id1, id2)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.xclaim(key, group, consumerName, minIdleTime, params, id1, id2);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXclaimJustIdByteArray() {
    byte[] key = "stream".getBytes();
    byte[] group = "group".getBytes();
    byte[] consumerName = "consumer".getBytes();
    long minIdleTime = 10000L;
    XClaimParams params = new XClaimParams();
    byte[] id1 = "id1".getBytes();
    byte[] id2 = "id2".getBytes();

    when(commandObjects.xclaimJustId(key, group, consumerName, minIdleTime, params, id1, id2)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.xclaimJustId(key, group, consumerName, minIdleTime, params, id1, id2);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXautoclaimByteArray() {
    byte[] key = "stream".getBytes();
    byte[] groupName = "group".getBytes();
    byte[] consumerName = "consumer".getBytes();
    long minIdleTime = 10000L;
    byte[] start = "startId".getBytes();
    XAutoClaimParams params = new XAutoClaimParams();

    when(commandObjects.xautoclaim(key, groupName, consumerName, minIdleTime, start, params)).thenReturn(listObjectCommandObject);

    Response<List<Object>> response = pipeliningBase.xautoclaim(key, groupName, consumerName, minIdleTime, start, params);

    assertThat(commands, contains(listObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXautoclaimJustIdByteArray() {
    byte[] key = "stream".getBytes();
    byte[] groupName = "group".getBytes();
    byte[] consumerName = "consumer".getBytes();
    long minIdleTime = 10000L;
    byte[] start = "startId".getBytes();
    XAutoClaimParams params = new XAutoClaimParams();

    when(commandObjects.xautoclaimJustId(key, groupName, consumerName, minIdleTime, start, params)).thenReturn(listObjectCommandObject);

    Response<List<Object>> response = pipeliningBase.xautoclaimJustId(key, groupName, consumerName, minIdleTime, start, params);

    assertThat(commands, contains(listObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXinfoStreamByteArray() {
    byte[] key = "stream".getBytes();

    when(commandObjects.xinfoStream(key)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.xinfoStream(key);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXinfoStreamFullByteArray() {
    byte[] key = "stream".getBytes();

    when(commandObjects.xinfoStreamFull(key)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.xinfoStreamFull(key);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXinfoStreamFullWithCountByteArray() {
    byte[] key = "stream".getBytes();
    int count = 10;

    when(commandObjects.xinfoStreamFull(key, count)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.xinfoStreamFull(key, count);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXinfoGroupsByteArray() {
    byte[] key = "stream".getBytes();

    when(commandObjects.xinfoGroups(key)).thenReturn(listObjectCommandObject);

    Response<List<Object>> response = pipeliningBase.xinfoGroups(key);

    assertThat(commands, contains(listObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXinfoConsumersByteArray() {
    byte[] key = "stream".getBytes();
    byte[] group = "group".getBytes();

    when(commandObjects.xinfoConsumers(key, group)).thenReturn(listObjectCommandObject);

    Response<List<Object>> response = pipeliningBase.xinfoConsumers(key, group);

    assertThat(commands, contains(listObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXreadByteArray() {
    XReadParams xReadParams = new XReadParams();
    Map.Entry<byte[], byte[]> stream1 = new AbstractMap.SimpleImmutableEntry<>("stream1".getBytes(), "id1".getBytes());

    when(commandObjects.xread(xReadParams, stream1)).thenReturn(listObjectCommandObject);

    Response<List<Object>> response = pipeliningBase.xread(xReadParams, stream1);

    assertThat(commands, contains(listObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testXreadGroupByteArray() {
    byte[] groupName = "group".getBytes();
    byte[] consumer = "consumer".getBytes();
    XReadGroupParams xReadGroupParams = new XReadGroupParams();
    Map.Entry<byte[], byte[]> stream1 = new AbstractMap.SimpleImmutableEntry<>("stream1".getBytes(), "id1".getBytes());

    when(commandObjects.xreadGroup(groupName, consumer, xReadGroupParams, stream1)).thenReturn(listObjectCommandObject);

    Response<List<Object>> response = pipeliningBase.xreadGroup(groupName, consumer, xReadGroupParams, stream1);

    assertThat(commands, contains(listObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSetByteArray() {
    byte[] key = "key".getBytes();
    byte[] value = "value".getBytes();

    when(commandObjects.set(key, value)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.set(key, value);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSetWithParamsByteArray() {
    byte[] key = "key".getBytes();
    byte[] value = "value".getBytes();
    SetParams params = new SetParams().nx().ex(10);

    when(commandObjects.set(key, value, params)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.set(key, value, params);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGetByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.get(key)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.get(key);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSetGetByteArray() {
    byte[] key = "key".getBytes();
    byte[] value = "value".getBytes();

    when(commandObjects.setGet(key, value)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.setGet(key, value);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSetGetWithParamsByteArray() {
    byte[] key = "key".getBytes();
    byte[] value = "value".getBytes();
    SetParams params = new SetParams().nx().ex(10);

    when(commandObjects.setGet(key, value, params)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.setGet(key, value, params);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGetDelByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.getDel(key)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.getDel(key);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGetExByteArray() {
    byte[] key = "key".getBytes();
    GetExParams params = new GetExParams().ex(10);

    when(commandObjects.getEx(key, params)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.getEx(key, params);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSetbitByteArray() {
    byte[] key = "key".getBytes();
    long offset = 10L;
    boolean value = true;

    when(commandObjects.setbit(key, offset, value)).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.setbit(key, offset, value);

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGetbitByteArray() {
    byte[] key = "key".getBytes();
    long offset = 10L;

    when(commandObjects.getbit(key, offset)).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.getbit(key, offset);

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSetrangeByteArray() {
    byte[] key = "key".getBytes();
    long offset = 10L;
    byte[] value = "value".getBytes();

    when(commandObjects.setrange(key, offset, value)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.setrange(key, offset, value);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGetrangeByteArray() {
    byte[] key = "key".getBytes();
    long startOffset = 0L;
    long endOffset = 10L;

    when(commandObjects.getrange(key, startOffset, endOffset)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.getrange(key, startOffset, endOffset);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGetSetByteArray() {
    byte[] key = "key".getBytes();
    byte[] value = "value".getBytes();

    when(commandObjects.getSet(key, value)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.getSet(key, value);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSetnxByteArray() {
    byte[] key = "key".getBytes();
    byte[] value = "value".getBytes();

    when(commandObjects.setnx(key, value)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.setnx(key, value);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSetexByteArray() {
    byte[] key = "key".getBytes();
    long seconds = 60L;
    byte[] value = "value".getBytes();

    when(commandObjects.setex(key, seconds, value)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.setex(key, seconds, value);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testPsetexByteArray() {
    byte[] key = "key".getBytes();
    long milliseconds = 5000L;
    byte[] value = "value".getBytes();

    when(commandObjects.psetex(key, milliseconds, value)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.psetex(key, milliseconds, value);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testMgetByteArray() {
    byte[] key1 = "key1".getBytes();
    byte[] key2 = "key2".getBytes();

    when(commandObjects.mget(key1, key2)).thenReturn(listBytesCommandObject);

    Response<List<byte[]>> response = pipeliningBase.mget(key1, key2);

    assertThat(commands, contains(listBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testMsetByteArray() {
    byte[] key1 = "key1".getBytes();
    byte[] value1 = "value1".getBytes();
    byte[] key2 = "key2".getBytes();
    byte[] value2 = "value2".getBytes();

    when(commandObjects.mset(key1, value1, key2, value2)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.mset(key1, value1, key2, value2);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testMsetnxByteArray() {
    byte[] key1 = "key1".getBytes();
    byte[] value1 = "value1".getBytes();
    byte[] key2 = "key2".getBytes();
    byte[] value2 = "value2".getBytes();

    when(commandObjects.msetnx(key1, value1, key2, value2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.msetnx(key1, value1, key2, value2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testIncrByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.incr(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.incr(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testIncrByByteArray() {
    byte[] key = "key".getBytes();
    long increment = 2L;

    when(commandObjects.incrBy(key, increment)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.incrBy(key, increment);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testIncrByFloatByteArray() {
    byte[] key = "key".getBytes();
    double increment = 2.5;

    when(commandObjects.incrByFloat(key, increment)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.incrByFloat(key, increment);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testDecrByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.decr(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.decr(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testDecrByByteArray() {
    byte[] key = "key".getBytes();
    long decrement = 2L;

    when(commandObjects.decrBy(key, decrement)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.decrBy(key, decrement);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testAppendByteArray() {
    byte[] key = "key".getBytes();
    byte[] value = "value".getBytes();

    when(commandObjects.append(key, value)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.append(key, value);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSubstrByteArray() {
    byte[] key = "key".getBytes();
    int start = 0;
    int end = 5;

    when(commandObjects.substr(key, start, end)).thenReturn(bytesCommandObject);

    Response<byte[]> response = pipeliningBase.substr(key, start, end);

    assertThat(commands, contains(bytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testStrlenByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.strlen(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.strlen(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBitcountByteArray() {
    byte[] key = "key".getBytes();

    when(commandObjects.bitcount(key)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.bitcount(key);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBitcountWithRangeByteArray() {
    byte[] key = "key".getBytes();
    long start = 0L;
    long end = 10L;

    when(commandObjects.bitcount(key, start, end)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.bitcount(key, start, end);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBitcountWithRangeAndOptionByteArray() {
    byte[] key = "key".getBytes();
    long start = 0L;
    long end = 10L;
    BitCountOption option = BitCountOption.BYTE;

    when(commandObjects.bitcount(key, start, end, option)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.bitcount(key, start, end, option);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBitposByteArray() {
    byte[] key = "key".getBytes();
    boolean value = true;

    when(commandObjects.bitpos(key, value)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.bitpos(key, value);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBitposWithParamsByteArray() {
    byte[] key = "key".getBytes();
    boolean value = true;
    BitPosParams params = new BitPosParams(0);

    when(commandObjects.bitpos(key, value, params)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.bitpos(key, value, params);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBitfieldByteArray() {
    byte[] key = "key".getBytes();
    byte[] arguments = "INCRBY i5 100 1".getBytes();

    when(commandObjects.bitfield(key, arguments)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.bitfield(key, arguments);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBitfieldReadonlyByteArray() {
    byte[] key = "key".getBytes();
    byte[] arguments = "GET i5 100".getBytes();

    when(commandObjects.bitfieldReadonly(key, arguments)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.bitfieldReadonly(key, arguments);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBitopByteArray() {
    BitOP op = BitOP.AND;
    byte[] destKey = "destKey".getBytes();
    byte[] srcKey1 = "srcKey1".getBytes();
    byte[] srcKey2 = "srcKey2".getBytes();

    when(commandObjects.bitop(op, destKey, srcKey1, srcKey2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.bitop(op, destKey, srcKey1, srcKey2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtCreateByteArray() {
    IndexOptions indexOptions = IndexOptions.defaultOptions();
    Schema schema = new Schema().addField(new Schema.Field("myField", Schema.FieldType.TEXT));

    when(commandObjects.ftCreate("myIndex", indexOptions, schema)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftCreate("myIndex", indexOptions, schema);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtCreateWithParamsByteArray() {
    FTCreateParams createParams = FTCreateParams.createParams();
    Iterable<SchemaField> schemaFields = Collections.singletonList(new TextField("myField"));

    when(commandObjects.ftCreate("myIndex", createParams, schemaFields)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftCreate("myIndex", createParams, schemaFields);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtAlterByteArray() {
    Schema schema = new Schema().addField(new Schema.Field("newField", Schema.FieldType.TEXT));

    when(commandObjects.ftAlter("myIndex", schema)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftAlter("myIndex", schema);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtAlterWithFieldsByteArray() {
    Iterable<SchemaField> schemaFields = Collections.singletonList(new TextField("newField"));

    when(commandObjects.ftAlter("myIndex", schemaFields)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftAlter("myIndex", schemaFields);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtAliasAddByteArray() {
    when(commandObjects.ftAliasAdd("myAlias", "myIndex")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftAliasAdd("myAlias", "myIndex");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtAliasUpdateByteArray() {
    when(commandObjects.ftAliasUpdate("myAlias", "myIndex")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftAliasUpdate("myAlias", "myIndex");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtAliasDelByteArray() {
    when(commandObjects.ftAliasDel("myAlias")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftAliasDel("myAlias");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtDropIndexByteArray() {
    when(commandObjects.ftDropIndex("myIndex")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftDropIndex("myIndex");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtDropIndexDDByteArray() {
    when(commandObjects.ftDropIndexDD("myIndex")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftDropIndexDD("myIndex");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtSearchByteArray() {
    String query = "hello world";

    when(commandObjects.ftSearch("myIndex", query)).thenReturn(searchResultCommandObject);

    Response<SearchResult> response = pipeliningBase.ftSearch("myIndex", query);

    assertThat(commands, contains(searchResultCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtSearchWithParamsByteArray() {
    String query = "hello world";
    FTSearchParams searchParams = FTSearchParams.searchParams().limit(0, 10);

    when(commandObjects.ftSearch("myIndex", query, searchParams)).thenReturn(searchResultCommandObject);

    Response<SearchResult> response = pipeliningBase.ftSearch("myIndex", query, searchParams);

    assertThat(commands, contains(searchResultCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtSearchWithQueryByteArray() {
    Query query = new Query("hello world").limit(0, 10);

    when(commandObjects.ftSearch("myIndex", query)).thenReturn(searchResultCommandObject);

    Response<SearchResult> response = pipeliningBase.ftSearch("myIndex", query);

    assertThat(commands, contains(searchResultCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtSearchWithQueryBytesByteArray() {
    byte[] indexName = "myIndex".getBytes();
    Query query = new Query("hello world").limit(0, 10);

    when(commandObjects.ftSearch(indexName, query)).thenReturn(searchResultCommandObject);

    Response<SearchResult> response = pipeliningBase.ftSearch(indexName, query);

    assertThat(commands, contains(searchResultCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtExplainByteArray() {
    Query query = new Query("hello world");

    when(commandObjects.ftExplain("myIndex", query)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftExplain("myIndex", query);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtExplainCLIByteArray() {
    Query query = new Query("hello world");

    when(commandObjects.ftExplainCLI("myIndex", query)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.ftExplainCLI("myIndex", query);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtAggregateByteArray() {
    AggregationBuilder aggr = new AggregationBuilder().groupBy("@field");

    when(commandObjects.ftAggregate("myIndex", aggr)).thenReturn(aggregationResultCommandObject);

    Response<AggregationResult> response = pipeliningBase.ftAggregate("myIndex", aggr);

    assertThat(commands, contains(aggregationResultCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtSynUpdateByteArray() {
    String synonymGroupId = "group1";
    String[] terms = { "term1", "term2" };

    when(commandObjects.ftSynUpdate("myIndex", synonymGroupId, terms)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftSynUpdate("myIndex", synonymGroupId, terms);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtSynDump() {
    when(commandObjects.ftSynDump("myIndex")).thenReturn(mapStringListStringCommandObject);

    Response<Map<String, List<String>>> response = pipeliningBase.ftSynDump("myIndex");

    assertThat(commands, contains(mapStringListStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtDictAdd() {
    String[] terms = { "term1", "term2" };

    when(commandObjects.ftDictAdd("myDict", terms)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.ftDictAdd("myDict", terms);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtDictDel() {
    String[] terms = { "term1", "term2" };

    when(commandObjects.ftDictDel("myDict", terms)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.ftDictDel("myDict", terms);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtDictDump() {
    when(commandObjects.ftDictDump("myDict")).thenReturn(setStringCommandObject);

    Response<Set<String>> response = pipeliningBase.ftDictDump("myDict");

    assertThat(commands, contains(setStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtDictAddBySampleKey() {
    String[] terms = { "term1", "term2" };

    when(commandObjects.ftDictAddBySampleKey("myIndex", "myDict", terms)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.ftDictAddBySampleKey("myIndex", "myDict", terms);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtDictDelBySampleKey() {
    String[] terms = { "term1", "term2" };

    when(commandObjects.ftDictDelBySampleKey("myIndex", "myDict", terms)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.ftDictDelBySampleKey("myIndex", "myDict", terms);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtDictDumpBySampleKey() {
    when(commandObjects.ftDictDumpBySampleKey("myIndex", "myDict")).thenReturn(setStringCommandObject);

    Response<Set<String>> response = pipeliningBase.ftDictDumpBySampleKey("myIndex", "myDict");

    assertThat(commands, contains(setStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtSpellCheck() {
    String query = "hello world";

    when(commandObjects.ftSpellCheck("myIndex", query)).thenReturn(mapStringMapStringDoubleCommandObject);

    Response<Map<String, Map<String, Double>>> response = pipeliningBase.ftSpellCheck("myIndex", query);

    assertThat(commands, contains(mapStringMapStringDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtSpellCheckWithParams() {
    String query = "hello world";
    FTSpellCheckParams spellCheckParams = new FTSpellCheckParams().distance(1);

    when(commandObjects.ftSpellCheck("myIndex", query, spellCheckParams)).thenReturn(mapStringMapStringDoubleCommandObject);

    Response<Map<String, Map<String, Double>>> response = pipeliningBase.ftSpellCheck("myIndex", query, spellCheckParams);

    assertThat(commands, contains(mapStringMapStringDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtInfo() {
    when(commandObjects.ftInfo("myIndex")).thenReturn(mapStringObjectCommandObject);

    Response<Map<String, Object>> response = pipeliningBase.ftInfo("myIndex");

    assertThat(commands, contains(mapStringObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtTagVals() {
    when(commandObjects.ftTagVals("myIndex", "myField")).thenReturn(setStringCommandObject);

    Response<Set<String>> response = pipeliningBase.ftTagVals("myIndex", "myField");

    assertThat(commands, contains(setStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtConfigGet() {
    when(commandObjects.ftConfigGet("TIMEOUT")).thenReturn(mapStringObjectCommandObject);

    Response<Map<String, Object>> response = pipeliningBase.ftConfigGet("TIMEOUT");

    assertThat(commands, contains(mapStringObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtConfigGetWithIndexName() {
    when(commandObjects.ftConfigGet("myIndex", "TIMEOUT")).thenReturn(mapStringObjectCommandObject);

    Response<Map<String, Object>> response = pipeliningBase.ftConfigGet("myIndex", "TIMEOUT");

    assertThat(commands, contains(mapStringObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtConfigSet() {
    when(commandObjects.ftConfigSet("TIMEOUT", "100")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftConfigSet("TIMEOUT", "100");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtConfigSetWithIndexName() {
    when(commandObjects.ftConfigSet("myIndex", "TIMEOUT", "100")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftConfigSet("myIndex", "TIMEOUT", "100");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtSugAdd() {
    when(commandObjects.ftSugAdd("mySug", "hello", 1.0)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.ftSugAdd("mySug", "hello", 1.0);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtSugAddIncr() {
    when(commandObjects.ftSugAddIncr("mySug", "hello", 1.0)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.ftSugAddIncr("mySug", "hello", 1.0);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtSugGet() {
    when(commandObjects.ftSugGet("mySug", "he")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.ftSugGet("mySug", "he");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtSugGetWithFuzzyAndMax() {
    when(commandObjects.ftSugGet("mySug", "he", true, 10)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.ftSugGet("mySug", "he", true, 10);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtSugGetWithScores() {
    when(commandObjects.ftSugGetWithScores("mySug", "he")).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.ftSugGetWithScores("mySug", "he");

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtSugGetWithScoresFuzzyMax() {
    when(commandObjects.ftSugGetWithScores("mySug", "he", true, 10)).thenReturn(listTupleCommandObject);

    Response<List<Tuple>> response = pipeliningBase.ftSugGetWithScores("mySug", "he", true, 10);

    assertThat(commands, contains(listTupleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtSugDel() {
    when(commandObjects.ftSugDel("mySug", "hello")).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.ftSugDel("mySug", "hello");

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtSugLen() {
    when(commandObjects.ftSugLen("mySug")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.ftSugLen("mySug");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testLcsByteArray() {
    byte[] keyA = "keyA".getBytes();
    byte[] keyB = "keyB".getBytes();
    LCSParams params = new LCSParams().withMatchLen();

    when(commandObjects.lcs(keyA, keyB, params)).thenReturn(lcsMatchResultCommandObject);

    Response<LCSMatchResult> response = pipeliningBase.lcs(keyA, keyB, params);

    assertThat(commands, contains(lcsMatchResultCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonSet() {
    Path2 path = Path2.of("$.field");
    Object object = new JsonObject();

    when(commandObjects.jsonSet("myJson", path, object)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.jsonSet("myJson", path, object);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonSetWithEscape() {
    Path2 path = Path2.of("$.field");
    Object object = new JsonObject();

    when(commandObjects.jsonSetWithEscape("myJson", path, object)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.jsonSetWithEscape("myJson", path, object);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonSetOldPath() {
    Path path = Path.of("$.field");
    Object object = new JsonObject();

    when(commandObjects.jsonSet("myJson", path, object)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.jsonSet("myJson", path, object);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonSetWithParams() {
    Path2 path = Path2.of("$.field");
    Object object = new JsonObject();
    JsonSetParams params = new JsonSetParams().nx();

    when(commandObjects.jsonSet("myJson", path, object, params)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.jsonSet("myJson", path, object, params);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonSetWithEscapeAndParams() {
    Path2 path = Path2.of("$.field");
    Object object = new JsonObject();
    JsonSetParams params = new JsonSetParams().nx();

    when(commandObjects.jsonSetWithEscape("myJson", path, object, params)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.jsonSetWithEscape("myJson", path, object, params);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonSetWithPathAndParams() {
    Path path = new Path("$.field");
    Object object = new JsonObject();
    JsonSetParams params = new JsonSetParams().nx();

    when(commandObjects.jsonSet("myJson", path, object, params)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.jsonSet("myJson", path, object, params);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonMergeWithPath2() {
    Path2 path = Path2.of("$.field");
    Object object = new JsonObject();

    when(commandObjects.jsonMerge("myJson", path, object)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.jsonMerge("myJson", path, object);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonMergeWithPath() {
    Path path = new Path("$.field");
    Object object = new JsonObject();

    when(commandObjects.jsonMerge("myJson", path, object)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.jsonMerge("myJson", path, object);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonGet() {
    when(commandObjects.jsonGet("myJson")).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.jsonGet("myJson");

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonGetWithClass() {
    when(commandObjects.jsonGet("myJson", MyBean.class)).thenReturn(myBeanCommandObject);

    Response<MyBean> response = pipeliningBase.jsonGet("myJson", MyBean.class);

    assertThat(commands, contains(myBeanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonGetWithPaths2() {
    Path2[] paths = { Path2.of("$.field1"), Path2.of("$.field2") };

    when(commandObjects.jsonGet("myJson", paths)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.jsonGet("myJson", paths);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonGetWithPaths() {
    Path[] paths = { new Path("$.field1"), new Path("$.field2") };

    when(commandObjects.jsonGet("myJson", paths)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.jsonGet("myJson", paths);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonGetWithClassAndPaths() {
    Path[] paths = { new Path("$.field1"), new Path("$.field2") };

    when(commandObjects.jsonGet("myJson", MyBean.class, paths)).thenReturn(myBeanCommandObject);

    Response<MyBean> response = pipeliningBase.jsonGet("myJson", MyBean.class, paths);

    assertThat(commands, contains(myBeanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonMGetWithPath2() {
    Path2 path = Path2.of("$.field");

    when(commandObjects.jsonMGet(path, "key1", "key2")).thenReturn(listJsonArrayCommandObject);

    Response<List<JSONArray>> response = pipeliningBase.jsonMGet(path, "key1", "key2");

    assertThat(commands, contains(listJsonArrayCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonMGetWithPathAndClass() {
    Path path = new Path("$.field");

    when(commandObjects.jsonMGet(path, MyBean.class, "key1", "key2")).thenReturn(listMyBeanCommandObject);

    Response<List<MyBean>> response = pipeliningBase.jsonMGet(path, MyBean.class, "key1", "key2");

    assertThat(commands, contains(listMyBeanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonDelWithKey() {
    when(commandObjects.jsonDel("myJson")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonDel("myJson");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonDelWithKeyAndPath2() {
    Path2 path = Path2.of("$.field");

    when(commandObjects.jsonDel("myJson", path)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonDel("myJson", path);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonDelWithKeyAndPath() {
    Path path = new Path("$.field");

    when(commandObjects.jsonDel("myJson", path)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonDel("myJson", path);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonClearWithKey() {
    when(commandObjects.jsonClear("myJson")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonClear("myJson");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonClearWithKeyAndPath2() {
    Path2 path = Path2.of("$.field");

    when(commandObjects.jsonClear("myJson", path)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonClear("myJson", path);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonClearWithKeyAndPath() {
    Path path = new Path("$.field");

    when(commandObjects.jsonClear("myJson", path)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonClear("myJson", path);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonToggleWithPath2() {
    Path2 path = Path2.of("$.field");

    when(commandObjects.jsonToggle("myJson", path)).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.jsonToggle("myJson", path);

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonToggleWithPath() {
    Path path = new Path("$.field");

    when(commandObjects.jsonToggle("myJson", path)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.jsonToggle("myJson", path);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonTypeWithKey() {
    when(commandObjects.jsonType("myJson")).thenReturn(classCommandObject);

    Response<Class<?>> response = pipeliningBase.jsonType("myJson");

    assertThat(commands, contains(classCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonTypeWithKeyAndPath2() {
    Path2 path = Path2.of("$.field");

    when(commandObjects.jsonType("myJson", path)).thenReturn(listClassCommandObject);

    Response<List<Class<?>>> response = pipeliningBase.jsonType("myJson", path);

    assertThat(commands, contains(listClassCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonTypeWithKeyAndPath() {
    Path path = new Path("$.field");

    when(commandObjects.jsonType("myJson", path)).thenReturn(classCommandObject);

    Response<Class<?>> response = pipeliningBase.jsonType("myJson", path);

    assertThat(commands, contains(classCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonStrAppendWithKey() {
    when(commandObjects.jsonStrAppend("myJson", "append")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonStrAppend("myJson", "append");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonStrAppendWithKeyAndPath2() {
    Path2 path = Path2.of("$.field");

    when(commandObjects.jsonStrAppend("myJson", path, "append")).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.jsonStrAppend("myJson", path, "append");

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonStrAppendWithKeyAndPath() {
    Path path = new Path("$.field");

    when(commandObjects.jsonStrAppend("myJson", path, "append")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonStrAppend("myJson", path, "append");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonStrLenWithKey() {
    when(commandObjects.jsonStrLen("myJson")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonStrLen("myJson");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonStrLenWithKeyAndPath2() {
    Path2 path = Path2.of("$.field");

    when(commandObjects.jsonStrLen("myJson", path)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.jsonStrLen("myJson", path);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonStrLenWithKeyAndPath() {
    Path path = new Path("$.field");

    when(commandObjects.jsonStrLen("myJson", path)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonStrLen("myJson", path);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonNumIncrByWithPath2() {
    Path2 path = Path2.of("$.number");

    when(commandObjects.jsonNumIncrBy("myJson", path, 42.0)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.jsonNumIncrBy("myJson", path, 42.0);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonNumIncrByWithPath() {
    Path path = new Path("$.number");

    when(commandObjects.jsonNumIncrBy("myJson", path, 42.0)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.jsonNumIncrBy("myJson", path, 42.0);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrAppendWithPath2() {
    Path2 path = Path2.of("$.array");
    Object[] objects = { "one", "two", "three" };

    when(commandObjects.jsonArrAppend("myJson", path, objects)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.jsonArrAppend("myJson", path, objects);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrAppendWithPath2WithEscape() {
    Path2 path = Path2.of("$.array");
    Object[] objects = { "one", "two", "three" };

    when(commandObjects.jsonArrAppendWithEscape("myJson", path, objects)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.jsonArrAppendWithEscape("myJson", path, objects);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrAppendWithPath() {
    Path path = new Path("$.array");
    Object[] objects = { "one", "two", "three" };

    when(commandObjects.jsonArrAppend("myJson", path, objects)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonArrAppend("myJson", path, objects);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrIndexWithPath2() {
    Path2 path = Path2.of("$.array");
    Object scalar = "two";

    when(commandObjects.jsonArrIndex("myJson", path, scalar)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.jsonArrIndex("myJson", path, scalar);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrIndexWithPath2WithEscape() {
    Path2 path = Path2.of("$.array");
    Object scalar = "two";

    when(commandObjects.jsonArrIndexWithEscape("myJson", path, scalar)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.jsonArrIndexWithEscape("myJson", path, scalar);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrIndexWithPath() {
    Path path = new Path("$.array");
    Object scalar = "two";

    when(commandObjects.jsonArrIndex("myJson", path, scalar)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonArrIndex("myJson", path, scalar);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrInsertWithPath2() {
    Path2 path = Path2.of("$.array");
    Object[] objects = { "one", "two", "three" };

    when(commandObjects.jsonArrInsert("myJson", path, 1, objects)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.jsonArrInsert("myJson", path, 1, objects);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrInsertWithPath2WithEscape() {
    Path2 path = Path2.of("$.array");
    Object[] objects = { "one", "two", "three" };

    when(commandObjects.jsonArrInsertWithEscape("myJson", path, 1, objects)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.jsonArrInsertWithEscape("myJson", path, 1, objects);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrInsertWithPath() {
    Path path = new Path("$.array");
    Object[] pojos = { "one", "two", "three" };

    when(commandObjects.jsonArrInsert("myJson", path, 1, pojos)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonArrInsert("myJson", path, 1, pojos);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrPopWithKey() {
    when(commandObjects.jsonArrPop("myJson")).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.jsonArrPop("myJson");

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrLenWithKeyAndPath() {
    Path path = new Path("$.array");

    when(commandObjects.jsonArrLen("myJson", path)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonArrLen("myJson", path);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrTrimWithPath2() {
    Path2 path = Path2.of("$.array");

    when(commandObjects.jsonArrTrim("myJson", path, 1, 2)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.jsonArrTrim("myJson", path, 1, 2);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrTrimWithPath() {
    Path path = new Path("$.array");

    when(commandObjects.jsonArrTrim("myJson", path, 1, 2)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonArrTrim("myJson", path, 1, 2);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrPopWithKeyClassAndPath() {
    Path path = new Path("$.array");

    when(commandObjects.jsonArrPop("myJson", MyBean.class, path)).thenReturn(myBeanCommandObject);

    Response<MyBean> response = pipeliningBase.jsonArrPop("myJson", MyBean.class, path);

    assertThat(commands, contains(myBeanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrPopWithKeyPath2AndIndex() {
    Path2 path = Path2.of("$.array");

    when(commandObjects.jsonArrPop("myJson", path, 1)).thenReturn(listObjectCommandObject);

    Response<List<Object>> response = pipeliningBase.jsonArrPop("myJson", path, 1);

    assertThat(commands, contains(listObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrPopWithKeyPathAndIndex() {
    Path path = new Path("$.array");

    when(commandObjects.jsonArrPop("myJson", path, 1)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.jsonArrPop("myJson", path, 1);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrPopWithKeyClassPathAndIndex() {
    Path path = new Path("$.array");

    when(commandObjects.jsonArrPop("myJson", MyBean.class, path, 1)).thenReturn(myBeanCommandObject);

    Response<MyBean> response = pipeliningBase.jsonArrPop("myJson", MyBean.class, path, 1);

    assertThat(commands, contains(myBeanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrLenWithKey() {
    when(commandObjects.jsonArrLen("myJson")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.jsonArrLen("myJson");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrLenWithKeyAndPath2() {
    Path2 path = Path2.of("$.array");

    when(commandObjects.jsonArrLen("myJson", path)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.jsonArrLen("myJson", path);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrPopWithKeyAndClass() {
    when(commandObjects.jsonArrPop("myJson", MyBean.class)).thenReturn(myBeanCommandObject);

    Response<MyBean> response = pipeliningBase.jsonArrPop("myJson", MyBean.class);

    assertThat(commands, contains(myBeanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrPopWithKeyAndPath2() {
    Path2 path = Path2.of("$.array");

    when(commandObjects.jsonArrPop("myJson", path)).thenReturn(listObjectCommandObject);

    Response<List<Object>> response = pipeliningBase.jsonArrPop("myJson", path);

    assertThat(commands, contains(listObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testJsonArrPopWithKeyAndPath() {
    Path path = new Path("$.array");

    when(commandObjects.jsonArrPop("myJson", path)).thenReturn(objectCommandObject);

    Response<Object> response = pipeliningBase.jsonArrPop("myJson", path);

    assertThat(commands, contains(objectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsCreate() {
    when(commandObjects.tsCreate("myTimeSeries")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.tsCreate("myTimeSeries");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsCreateWithParams() {
    TSCreateParams createParams = TSCreateParams.createParams();

    when(commandObjects.tsCreate("myTimeSeries", createParams)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.tsCreate("myTimeSeries", createParams);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsDel() {
    when(commandObjects.tsDel("myTimeSeries", 1000L, 2000L)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.tsDel("myTimeSeries", 1000L, 2000L);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsAlter() {
    TSAlterParams alterParams = TSAlterParams.alterParams();

    when(commandObjects.tsAlter("myTimeSeries", alterParams)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.tsAlter("myTimeSeries", alterParams);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsAdd() {
    when(commandObjects.tsAdd("myTimeSeries", 42.0)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.tsAdd("myTimeSeries", 42.0);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsAddWithTimestamp() {
    when(commandObjects.tsAdd("myTimeSeries", 1000L, 42.0)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.tsAdd("myTimeSeries", 1000L, 42.0);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsAddWithTimestampAndParams() {
    TSCreateParams createParams = TSCreateParams.createParams();

    when(commandObjects.tsAdd("myTimeSeries", 1000L, 42.0, createParams)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.tsAdd("myTimeSeries", 1000L, 42.0, createParams);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsMAdd() {
    Map.Entry<String, TSElement> entry1 = new AbstractMap.SimpleEntry<>("ts1", new TSElement(1000L, 1.0));
    Map.Entry<String, TSElement> entry2 = new AbstractMap.SimpleEntry<>("ts2", new TSElement(2000L, 2.0));

    when(commandObjects.tsMAdd(entry1, entry2)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.tsMAdd(entry1, entry2);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsIncrBy() {
    when(commandObjects.tsIncrBy("myTimeSeries", 1.0)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.tsIncrBy("myTimeSeries", 1.0);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsIncrByWithTimestamp() {
    when(commandObjects.tsIncrBy("myTimeSeries", 1.0, 1000L)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.tsIncrBy("myTimeSeries", 1.0, 1000L);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsDecrBy() {
    when(commandObjects.tsDecrBy("myTimeSeries", 1.0)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.tsDecrBy("myTimeSeries", 1.0);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsDecrByWithTimestamp() {
    when(commandObjects.tsDecrBy("myTimeSeries", 1.0, 1000L)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.tsDecrBy("myTimeSeries", 1.0, 1000L);

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsRange() {
    when(commandObjects.tsRange("myTimeSeries", 1000L, 2000L)).thenReturn(listTsElementCommandObject);

    Response<List<TSElement>> response = pipeliningBase.tsRange("myTimeSeries", 1000L, 2000L);

    assertThat(commands, contains(listTsElementCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsRangeWithParams() {
    TSRangeParams rangeParams = TSRangeParams.rangeParams();

    when(commandObjects.tsRange("myTimeSeries", rangeParams)).thenReturn(listTsElementCommandObject);

    Response<List<TSElement>> response = pipeliningBase.tsRange("myTimeSeries", rangeParams);

    assertThat(commands, contains(listTsElementCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsRevRange() {
    when(commandObjects.tsRevRange("myTimeSeries", 1000L, 2000L)).thenReturn(listTsElementCommandObject);

    Response<List<TSElement>> response = pipeliningBase.tsRevRange("myTimeSeries", 1000L, 2000L);

    assertThat(commands, contains(listTsElementCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsRevRangeWithParams() {
    TSRangeParams rangeParams = TSRangeParams.rangeParams();

    when(commandObjects.tsRevRange("myTimeSeries", rangeParams)).thenReturn(listTsElementCommandObject);

    Response<List<TSElement>> response = pipeliningBase.tsRevRange("myTimeSeries", rangeParams);

    assertThat(commands, contains(listTsElementCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsMRange() {
    String[] filters = { "sensor_id=123" };

    when(commandObjects.tsMRange(1000L, 2000L, filters)).thenReturn(mapStringTsmRangeElementsCommandObject);

    Response<Map<String, TSMRangeElements>> response = pipeliningBase.tsMRange(1000L, 2000L, filters);

    assertThat(commands, contains(mapStringTsmRangeElementsCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsMRangeWithParams() {
    TSMRangeParams multiRangeParams = TSMRangeParams.multiRangeParams();

    when(commandObjects.tsMRange(multiRangeParams)).thenReturn(mapStringTsmRangeElementsCommandObject);

    Response<Map<String, TSMRangeElements>> response = pipeliningBase.tsMRange(multiRangeParams);

    assertThat(commands, contains(mapStringTsmRangeElementsCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsMRevRange() {
    String[] filters = { "sensor_id=123" };

    when(commandObjects.tsMRevRange(1000L, 2000L, filters)).thenReturn(mapStringTsmRangeElementsCommandObject);

    Response<Map<String, TSMRangeElements>> response = pipeliningBase.tsMRevRange(1000L, 2000L, filters);

    assertThat(commands, contains(mapStringTsmRangeElementsCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsMRevRangeWithParams() {
    TSMRangeParams multiRangeParams = TSMRangeParams.multiRangeParams();

    when(commandObjects.tsMRevRange(multiRangeParams)).thenReturn(mapStringTsmRangeElementsCommandObject);

    Response<Map<String, TSMRangeElements>> response = pipeliningBase.tsMRevRange(multiRangeParams);

    assertThat(commands, contains(mapStringTsmRangeElementsCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsGet() {
    when(commandObjects.tsGet("myTimeSeries")).thenReturn(tsElementCommandObject);

    Response<TSElement> response = pipeliningBase.tsGet("myTimeSeries");

    assertThat(commands, contains(tsElementCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsGetWithParams() {
    TSGetParams getParams = TSGetParams.getParams();

    when(commandObjects.tsGet("myTimeSeries", getParams)).thenReturn(tsElementCommandObject);

    Response<TSElement> response = pipeliningBase.tsGet("myTimeSeries", getParams);

    assertThat(commands, contains(tsElementCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsMGet() {
    TSMGetParams multiGetParams = TSMGetParams.multiGetParams();
    String[] filters = { "sensor_id=123" };

    when(commandObjects.tsMGet(multiGetParams, filters)).thenReturn(mapStringTsmGetElementCommandObject);

    Response<Map<String, TSMGetElement>> response = pipeliningBase.tsMGet(multiGetParams, filters);

    assertThat(commands, contains(mapStringTsmGetElementCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsCreateRule() {
    AggregationType aggregationType = AggregationType.AVG;
    long timeBucket = 60;

    when(commandObjects.tsCreateRule("sourceTimeSeries", "destTimeSeries", aggregationType, timeBucket)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.tsCreateRule("sourceTimeSeries", "destTimeSeries", aggregationType, timeBucket);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsCreateRuleWithAlignTimestamp() {
    AggregationType aggregationType = AggregationType.AVG;
    long bucketDuration = 60;
    long alignTimestamp = 0;

    when(commandObjects.tsCreateRule("sourceTimeSeries", "destTimeSeries", aggregationType, bucketDuration, alignTimestamp)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.tsCreateRule("sourceTimeSeries", "destTimeSeries", aggregationType, bucketDuration, alignTimestamp);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsDeleteRule() {
    when(commandObjects.tsDeleteRule("sourceTimeSeries", "destTimeSeries")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.tsDeleteRule("sourceTimeSeries", "destTimeSeries");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTsQueryIndex() {
    String[] filters = { "sensor_id=123" };

    when(commandObjects.tsQueryIndex(filters)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.tsQueryIndex(filters);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfReserve() {
    double errorRate = 0.01;
    long capacity = 10000L;

    when(commandObjects.bfReserve("myBloomFilter", errorRate, capacity)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.bfReserve("myBloomFilter", errorRate, capacity);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfReserveWithParams() {
    double errorRate = 0.01;
    long capacity = 10000L;

    BFReserveParams reserveParams = new BFReserveParams().expansion(2);
    when(commandObjects.bfReserve("myBloomFilter", errorRate, capacity, reserveParams)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.bfReserve("myBloomFilter", errorRate, capacity, reserveParams);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfAdd() {
    when(commandObjects.bfAdd("myBloomFilter", "item1")).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.bfAdd("myBloomFilter", "item1");

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfMAdd() {
    when(commandObjects.bfMAdd("myBloomFilter", "item1", "item2")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.bfMAdd("myBloomFilter", "item1", "item2");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfInsert() {
    when(commandObjects.bfInsert("myBloomFilter", "item1", "item2")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.bfInsert("myBloomFilter", "item1", "item2");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfInsertWithParams() {
    BFInsertParams insertParams = new BFInsertParams().capacity(10000L).error(0.01);

    when(commandObjects.bfInsert("myBloomFilter", insertParams, "item1", "item2")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.bfInsert("myBloomFilter", insertParams, "item1", "item2");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfExists() {
    when(commandObjects.bfExists("myBloomFilter", "item1")).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.bfExists("myBloomFilter", "item1");

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfMExists() {
    when(commandObjects.bfMExists("myBloomFilter", "item1", "item2")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.bfMExists("myBloomFilter", "item1", "item2");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfScanDump() {
    when(commandObjects.bfScanDump("myBloomFilter", 0L)).thenReturn(entryLongBytesCommandObject);

    Response<Map.Entry<Long, byte[]>> response = pipeliningBase.bfScanDump("myBloomFilter", 0L);

    assertThat(commands, contains(entryLongBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfLoadChunk() {
    byte[] data = { 1, 2, 3, 4 };

    when(commandObjects.bfLoadChunk("myBloomFilter", 0L, data)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.bfLoadChunk("myBloomFilter", 0L, data);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfCard() {
    when(commandObjects.bfCard("myBloomFilter")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.bfCard("myBloomFilter");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testBfInfo() {
    when(commandObjects.bfInfo("myBloomFilter")).thenReturn(mapStringObjectCommandObject);

    Response<Map<String, Object>> response = pipeliningBase.bfInfo("myBloomFilter");

    assertThat(commands, contains(mapStringObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfReserve() {
    when(commandObjects.cfReserve("myCuckooFilter", 10000L)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.cfReserve("myCuckooFilter", 10000L);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfReserveWithParams() {
    CFReserveParams reserveParams = new CFReserveParams().bucketSize(2).maxIterations(500).expansion(2);

    when(commandObjects.cfReserve("myCuckooFilter", 10000L, reserveParams)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.cfReserve("myCuckooFilter", 10000L, reserveParams);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfAdd() {
    when(commandObjects.cfAdd("myCuckooFilter", "item1")).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.cfAdd("myCuckooFilter", "item1");

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfAddNx() {
    when(commandObjects.cfAddNx("myCuckooFilter", "item1")).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.cfAddNx("myCuckooFilter", "item1");

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfInsert() {
    when(commandObjects.cfInsert("myCuckooFilter", "item1", "item2")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.cfInsert("myCuckooFilter", "item1", "item2");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfInsertWithParams() {
    CFInsertParams insertParams = new CFInsertParams().capacity(10000L).noCreate();

    when(commandObjects.cfInsert("myCuckooFilter", insertParams, "item1", "item2")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.cfInsert("myCuckooFilter", insertParams, "item1", "item2");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfInsertNx() {
    when(commandObjects.cfInsertNx("myCuckooFilter", "item1", "item2")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.cfInsertNx("myCuckooFilter", "item1", "item2");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfInsertNxWithParams() {
    CFInsertParams insertParams = new CFInsertParams().capacity(10000L).noCreate();

    when(commandObjects.cfInsertNx("myCuckooFilter", insertParams, "item1", "item2")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.cfInsertNx("myCuckooFilter", insertParams, "item1", "item2");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfExists() {
    when(commandObjects.cfExists("myCuckooFilter", "item1")).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.cfExists("myCuckooFilter", "item1");

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfDel() {
    when(commandObjects.cfDel("myCuckooFilter", "item1")).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.cfDel("myCuckooFilter", "item1");

    assertThat(commands, contains(booleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfCount() {
    when(commandObjects.cfCount("myCuckooFilter", "item1")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.cfCount("myCuckooFilter", "item1");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfScanDump() {
    when(commandObjects.cfScanDump("myCuckooFilter", 0L)).thenReturn(entryLongBytesCommandObject);

    Response<Map.Entry<Long, byte[]>> response = pipeliningBase.cfScanDump("myCuckooFilter", 0L);

    assertThat(commands, contains(entryLongBytesCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfLoadChunk() {
    byte[] data = { 1, 2, 3, 4 };

    when(commandObjects.cfLoadChunk("myCuckooFilter", 0L, data)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.cfLoadChunk("myCuckooFilter", 0L, data);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCfInfo() {
    when(commandObjects.cfInfo("myCuckooFilter")).thenReturn(mapStringObjectCommandObject);

    Response<Map<String, Object>> response = pipeliningBase.cfInfo("myCuckooFilter");

    assertThat(commands, contains(mapStringObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCmsInitByDim() {
    when(commandObjects.cmsInitByDim("myCountMinSketch", 1000L, 5L)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.cmsInitByDim("myCountMinSketch", 1000L, 5L);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCmsInitByProb() {
    double error = 0.01;
    double probability = 0.99;

    when(commandObjects.cmsInitByProb("myCountMinSketch", error, probability)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.cmsInitByProb("myCountMinSketch", error, probability);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCmsIncrBy() {
    Map<String, Long> itemIncrements = new HashMap<>();
    itemIncrements.put("item1", 1L);
    itemIncrements.put("item2", 2L);

    when(commandObjects.cmsIncrBy("myCountMinSketch", itemIncrements)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.cmsIncrBy("myCountMinSketch", itemIncrements);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCmsQuery() {
    when(commandObjects.cmsQuery("myCountMinSketch", "item1", "item2")).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.cmsQuery("myCountMinSketch", "item1", "item2");

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCmsMerge() {
    when(commandObjects.cmsMerge("mergedCountMinSketch", "cms1", "cms2")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.cmsMerge("mergedCountMinSketch", "cms1", "cms2");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCmsMergeWithWeights() {
    Map<String, Long> keysAndWeights = new HashMap<>();
    keysAndWeights.put("cms1", 1L);
    keysAndWeights.put("cms2", 2L);

    when(commandObjects.cmsMerge("mergedCountMinSketch", keysAndWeights)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.cmsMerge("mergedCountMinSketch", keysAndWeights);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testCmsInfo() {
    when(commandObjects.cmsInfo("myCountMinSketch")).thenReturn(mapStringObjectCommandObject);

    Response<Map<String, Object>> response = pipeliningBase.cmsInfo("myCountMinSketch");

    assertThat(commands, contains(mapStringObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTopkReserve() {
    when(commandObjects.topkReserve("myTopK", 3L)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.topkReserve("myTopK", 3L);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTopkReserveWithParams() {
    long width = 50L;
    long depth = 5L;
    double decay = 0.9;

    when(commandObjects.topkReserve("myTopK", 3L, width, depth, decay)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.topkReserve("myTopK", 3L, width, depth, decay);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTopkAdd() {
    when(commandObjects.topkAdd("myTopK", "item1", "item2")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.topkAdd("myTopK", "item1", "item2");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTopkIncrBy() {
    Map<String, Long> itemIncrements = new HashMap<>();
    itemIncrements.put("item1", 1L);
    itemIncrements.put("item2", 2L);

    when(commandObjects.topkIncrBy("myTopK", itemIncrements)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.topkIncrBy("myTopK", itemIncrements);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTopkQuery() {
    when(commandObjects.topkQuery("myTopK", "item1", "item2")).thenReturn(listBooleanCommandObject);

    Response<List<Boolean>> response = pipeliningBase.topkQuery("myTopK", "item1", "item2");

    assertThat(commands, contains(listBooleanCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTopkList() {
    when(commandObjects.topkList("myTopK")).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.topkList("myTopK");

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTopkListWithCount() {
    when(commandObjects.topkListWithCount("myTopK")).thenReturn(mapStringLongCommandObject);

    Response<Map<String, Long>> response = pipeliningBase.topkListWithCount("myTopK");

    assertThat(commands, contains(mapStringLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTopkInfo() {
    when(commandObjects.topkInfo("myTopK")).thenReturn(mapStringObjectCommandObject);

    Response<Map<String, Object>> response = pipeliningBase.topkInfo("myTopK");

    assertThat(commands, contains(mapStringObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestCreate() {
    when(commandObjects.tdigestCreate("myTDigest")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.tdigestCreate("myTDigest");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestCreateWithCompression() {
    when(commandObjects.tdigestCreate("myTDigest", 100)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.tdigestCreate("myTDigest", 100);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestReset() {
    when(commandObjects.tdigestReset("myTDigest")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.tdigestReset("myTDigest");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestMerge() {
    when(commandObjects.tdigestMerge("destinationTDigest", "sourceTDigest1", "sourceTDigest2"))
        .thenReturn(stringCommandObject);

    Response<String> response =
        pipeliningBase.tdigestMerge("destinationTDigest", "sourceTDigest1", "sourceTDigest2");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestMergeWithParams() {
    TDigestMergeParams mergeParams = new TDigestMergeParams().compression(100);

    when(commandObjects.tdigestMerge(mergeParams, "destinationTDigest", "sourceTDigest1", "sourceTDigest2"))
        .thenReturn(stringCommandObject);

    Response<String> response =
        pipeliningBase.tdigestMerge(mergeParams, "destinationTDigest", "sourceTDigest1", "sourceTDigest2");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestInfo() {
    when(commandObjects.tdigestInfo("myTDigest")).thenReturn(mapStringObjectCommandObject);

    Response<Map<String, Object>> response = pipeliningBase.tdigestInfo("myTDigest");

    assertThat(commands, contains(mapStringObjectCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestAdd() {
    when(commandObjects.tdigestAdd("myTDigest", 1.0, 2.0, 3.0)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.tdigestAdd("myTDigest", 1.0, 2.0, 3.0);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestCDF() {
    when(commandObjects.tdigestCDF("myTDigest", 1.0, 2.0)).thenReturn(listDoubleCommandObject);

    Response<List<Double>> response = pipeliningBase.tdigestCDF("myTDigest", 1.0, 2.0);

    assertThat(commands, contains(listDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestQuantile() {
    when(commandObjects.tdigestQuantile("myTDigest", 0.5, 0.9)).thenReturn(listDoubleCommandObject);

    Response<List<Double>> response = pipeliningBase.tdigestQuantile("myTDigest", 0.5, 0.9);

    assertThat(commands, contains(listDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestMin() {
    when(commandObjects.tdigestMin("myTDigest")).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.tdigestMin("myTDigest");

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestMax() {
    when(commandObjects.tdigestMax("myTDigest")).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.tdigestMax("myTDigest");

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestTrimmedMean() {
    when(commandObjects.tdigestTrimmedMean("myTDigest", 0.1, 0.9)).thenReturn(doubleCommandObject);

    Response<Double> response = pipeliningBase.tdigestTrimmedMean("myTDigest", 0.1, 0.9);

    assertThat(commands, contains(doubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestRank() {
    when(commandObjects.tdigestRank("myTDigest", 1.0, 2.0)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.tdigestRank("myTDigest", 1.0, 2.0);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestRevRank() {
    when(commandObjects.tdigestRevRank("myTDigest", 1.0, 2.0)).thenReturn(listLongCommandObject);

    Response<List<Long>> response = pipeliningBase.tdigestRevRank("myTDigest", 1.0, 2.0);

    assertThat(commands, contains(listLongCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestByRank() {
    when(commandObjects.tdigestByRank("myTDigest", 1, 2)).thenReturn(listDoubleCommandObject);

    Response<List<Double>> response = pipeliningBase.tdigestByRank("myTDigest", 1, 2);

    assertThat(commands, contains(listDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testTdigestByRevRank() {
    when(commandObjects.tdigestByRevRank("myTDigest", 1, 2)).thenReturn(listDoubleCommandObject);

    Response<List<Double>> response = pipeliningBase.tdigestByRevRank("myTDigest", 1, 2);

    assertThat(commands, contains(listDoubleCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGraphQuery() {
    String query = "MATCH (n) RETURN n";

    when(graphCommandObjects.graphQuery("myGraph", query)).thenReturn(resultSetCommandObject);

    Response<ResultSet> response = pipeliningBase.graphQuery("myGraph", query);

    assertThat(commands, contains(resultSetCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGraphReadonlyQuery() {
    String query = "MATCH (n) RETURN n";

    when(graphCommandObjects.graphReadonlyQuery("myGraph", query)).thenReturn(resultSetCommandObject);

    Response<ResultSet> response = pipeliningBase.graphReadonlyQuery("myGraph", query);

    assertThat(commands, contains(resultSetCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGraphQueryWithTimeout() {
    String query = "MATCH (n) RETURN n";

    when(graphCommandObjects.graphQuery("myGraph", query, 1000L)).thenReturn(resultSetCommandObject);

    Response<ResultSet> response = pipeliningBase.graphQuery("myGraph", query, 1000L);

    assertThat(commands, contains(resultSetCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGraphReadonlyQueryWithTimeout() {
    String query = "MATCH (n) RETURN n";

    when(graphCommandObjects.graphReadonlyQuery("myGraph", query, 1000L)).thenReturn(resultSetCommandObject);

    Response<ResultSet> response = pipeliningBase.graphReadonlyQuery("myGraph", query, 1000L);

    assertThat(commands, contains(resultSetCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGraphQueryWithParams() {
    String query = "MATCH (n) WHERE n.name = $name RETURN n";
    Map<String, Object> params = Collections.singletonMap("name", "Alice");

    when(graphCommandObjects.graphQuery("myGraph", query, params)).thenReturn(resultSetCommandObject);

    Response<ResultSet> response = pipeliningBase.graphQuery("myGraph", query, params);

    assertThat(commands, contains(resultSetCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGraphReadonlyQueryWithParams() {
    String query = "MATCH (n) WHERE n.name = $name RETURN n";
    Map<String, Object> params = Collections.singletonMap("name", "Alice");

    when(graphCommandObjects.graphReadonlyQuery("myGraph", query, params)).thenReturn(resultSetCommandObject);

    Response<ResultSet> response = pipeliningBase.graphReadonlyQuery("myGraph", query, params);

    assertThat(commands, contains(resultSetCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGraphQueryWithParamsAndTimeout() {
    String query = "MATCH (n) WHERE n.name = $name RETURN n";
    Map<String, Object> params = Collections.singletonMap("name", "Alice");

    when(graphCommandObjects.graphQuery("myGraph", query, params, 1000L)).thenReturn(resultSetCommandObject);

    Response<ResultSet> response = pipeliningBase.graphQuery("myGraph", query, params, 1000L);

    assertThat(commands, contains(resultSetCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGraphReadonlyQueryWithParamsAndTimeout() {
    String query = "MATCH (n) WHERE n.name = $name RETURN n";
    Map<String, Object> params = Collections.singletonMap("name", "Alice");

    when(graphCommandObjects.graphReadonlyQuery("myGraph", query, params, 1000L)).thenReturn(resultSetCommandObject);

    Response<ResultSet> response = pipeliningBase.graphReadonlyQuery("myGraph", query, params, 1000L);

    assertThat(commands, contains(resultSetCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGraphDelete() {
    when(graphCommandObjects.graphDelete("myGraph")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.graphDelete("myGraph");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testGraphProfile() {
    String query = "PROFILE MATCH (n) RETURN n";

    when(commandObjects.graphProfile("myGraph", query)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.graphProfile("myGraph", query);

    assertThat(commands, contains(listStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSendCommandWithStringArgs() {
    ProtocolCommand cmd = Protocol.Command.GET;
    String arg1 = "key1";
    String arg2 = "key2";

    Response<Object> response = pipeliningBase.sendCommand(cmd, arg1, arg2);

    assertThat(commands, hasSize(1));

    List<Rawable> arguments = new ArrayList<>();
    commands.get(0).getArguments().forEach(arguments::add);

    assertThat(arguments.stream().map(Rawable::getRaw).collect(Collectors.toList()), contains(
        Protocol.Command.GET.getRaw(),
        arg1.getBytes(),
        arg2.getBytes()
    ));

    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSendCommandWithByteArgs() {
    ProtocolCommand cmd = Protocol.Command.SET;
    byte[] arg1 = "key1".getBytes();
    byte[] arg2 = "value1".getBytes();

    Response<Object> response = pipeliningBase.sendCommand(cmd, arg1, arg2);

    assertThat(commands, hasSize(1));

    List<Rawable> arguments = new ArrayList<>();
    commands.get(0).getArguments().forEach(arguments::add);

    assertThat(arguments.stream().map(Rawable::getRaw).collect(Collectors.toList()), contains(
        Protocol.Command.SET.getRaw(),
        arg1,
        arg2
    ));

    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testExecuteCommand() {
    CommandArguments commandArguments = new CommandArguments(Protocol.Command.GET).key("key1");
    CommandObject<String> commandObject = new CommandObject<>(commandArguments, BuilderFactory.STRING);

    Response<String> response = pipeliningBase.executeCommand(commandObject);

    assertThat(commands, contains(commandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testSetJsonObjectMapper() {
    JsonObjectMapper jsonObjectMapper = mock(JsonObjectMapper.class);
    doNothing().when(commandObjects).setJsonObjectMapper(jsonObjectMapper);

    pipeliningBase.setJsonObjectMapper(jsonObjectMapper);

    verify(commandObjects).setJsonObjectMapper(jsonObjectMapper);
  }

  @Test
  public void testMultipleCommands() {
    when(commandObjects.exists("key1")).thenReturn(booleanCommandObject);
    when(commandObjects.exists("key2")).thenReturn(booleanCommandObject);

    Response<Boolean> result1 = pipeliningBase.exists("key1");
    Response<Boolean> result2 = pipeliningBase.exists("key2");

    assertThat(commands, contains(
        booleanCommandObject,
        booleanCommandObject
    ));

    assertThat(result1, is(predefinedResponse));
    assertThat(result2, is(predefinedResponse));
  }

}
