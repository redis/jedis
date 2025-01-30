package redis.clients.jedis.mocked;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.resps.*;
import redis.clients.jedis.search.ProfilingInfo;
import redis.clients.jedis.search.SearchResult;
import redis.clients.jedis.search.aggr.AggregationResult;
import redis.clients.jedis.timeseries.*;
import redis.clients.jedis.util.KeyValue;

/**
 * Provides an exhaustive list of mocked {@link redis.clients.jedis.CommandObject}s for use in unit tests.
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class MockedCommandObjectsTestBase {

  /**
   * Used for JSON related tests. The fields are not used actually, given that tests are mocked.
   */
  @SuppressWarnings("unused")
  public final static class MyBean {
    String field1;
    String field2;
  }


  // Below follows a list of mocked CommandObjects, one per type. This is the cleanest way to create
  // mocks, given that CommandObject is a generic class. Using {@code Mockito.mock(...)} yields too
  // many warnings related to generics.
  // To make the code more readable, try to keep the list sorted alphabetically, and without automatic
  // reformatting.

  // @formatter:off
  @Mock protected CommandObject<AggregationResult> aggregationResultCommandObject;
  @Mock protected CommandObject<Boolean> booleanCommandObject;
  @Mock protected CommandObject<Class<?>> classCommandObject;
  @Mock protected CommandObject<Double> doubleCommandObject;
  @Mock protected CommandObject<FunctionStats> functionStatsCommandObject;
  @Mock protected CommandObject<KeyValue<Long, Double>> keyValueLongDoubleCommandObject;
  @Mock protected CommandObject<KeyValue<Long, Long>> keyValueLongLongCommandObject;
  @Mock protected CommandObject<KeyValue<String, List<String>>> keyValueStringListStringCommandObject;
  @Mock protected CommandObject<KeyValue<String, List<Tuple>>> keyValueStringListTupleCommandObject;
  @Mock protected CommandObject<KeyValue<String, String>> keyValueStringStringCommandObject;
  @Mock protected CommandObject<KeyValue<String, Tuple>> keyValueStringTupleCommandObject;
  @Mock protected CommandObject<KeyValue<byte[], List<Tuple>>> keyValueBytesListTupleCommandObject;
  @Mock protected CommandObject<KeyValue<byte[], List<byte[]>>> keyValueBytesListBytesCommandObject;
  @Mock protected CommandObject<KeyValue<byte[], Tuple>> keyValueBytesTupleCommandObject;
  @Mock protected CommandObject<KeyValue<byte[], byte[]>> keyValueBytesBytesCommandObject;
  @Mock protected CommandObject<LCSMatchResult> lcsMatchResultCommandObject;
  @Mock protected CommandObject<List<Boolean>> listBooleanCommandObject;
  @Mock protected CommandObject<List<Class<?>>> listClassCommandObject;
  @Mock protected CommandObject<List<Double>> listDoubleCommandObject;
  @Mock protected CommandObject<List<GeoCoordinate>> listGeoCoordinateCommandObject;
  @Mock protected CommandObject<List<GeoRadiusResponse>> listGeoRadiusResponseCommandObject;
  @Mock protected CommandObject<List<JSONArray>> listJsonArrayCommandObject;
  @Mock protected CommandObject<List<LibraryInfo>> listLibraryInfoCommandObject;
  @Mock protected CommandObject<List<List<Object>>> listListObjectCommandObject;
  @Mock protected CommandObject<List<List<String>>> listListStringCommandObject;
  @Mock protected CommandObject<List<Long>> listLongCommandObject;
  @Mock protected CommandObject<List<Map.Entry<String, List<StreamEntry>>>> listEntryStringListStreamEntryCommandObject;
  @Mock protected CommandObject<List<Map.Entry<String, String>>> listEntryStringStringCommandObject;
  @Mock protected CommandObject<List<Map.Entry<byte[], byte[]>>> listEntryBytesBytesCommandObject;
  @Mock protected CommandObject<List<MyBean>> listMyBeanCommandObject;
  @Mock protected CommandObject<List<Object>> listObjectCommandObject;
  @Mock protected CommandObject<List<StreamConsumerInfo>> listStreamConsumerInfoCommandObject;
  @Mock protected CommandObject<List<StreamConsumersInfo>> listStreamConsumersInfoCommandObject;
  @Mock protected CommandObject<List<StreamEntry>> listStreamEntryCommandObject;
  @Mock protected CommandObject<List<StreamEntryID>> listStreamEntryIdCommandObject;
  @Mock protected CommandObject<List<StreamGroupInfo>> listStreamGroupInfoCommandObject;
  @Mock protected CommandObject<List<StreamPendingEntry>> listStreamPendingEntryCommandObject;
  @Mock protected CommandObject<List<String>> listStringCommandObject;
  @Mock protected CommandObject<List<TSElement>> listTsElementCommandObject;
  @Mock protected CommandObject<List<Tuple>> listTupleCommandObject;
  @Mock protected CommandObject<List<byte[]>> listBytesCommandObject;
  @Mock protected CommandObject<Long> longCommandObject;
  @Mock protected CommandObject<Map.Entry<AggregationResult, ProfilingInfo>> entryAggregationResultMapStringObjectCommandObject;
  @Mock protected CommandObject<Map.Entry<Long, byte[]>> entryLongBytesCommandObject;
  @Mock protected CommandObject<Map.Entry<SearchResult, ProfilingInfo>> entrySearchResultMapStringObjectCommandObject;
  @Mock protected CommandObject<Map.Entry<StreamEntryID, List<StreamEntry>>> entryStreamEntryIdListStreamEntryCommandObject;
  @Mock protected CommandObject<Map.Entry<StreamEntryID, List<StreamEntryID>>> entryStreamEntryIdListStreamEntryIdCommandObject;
  @Mock protected CommandObject<Map<String, List<String>>> mapStringListStringCommandObject;
  @Mock protected CommandObject<Map<String, List<StreamEntry>>> mapStringListStreamEntryCommandObject;
  @Mock protected CommandObject<Map<String, Long>> mapStringLongCommandObject;
  @Mock protected CommandObject<Map<String, Map<String, Double>>> mapStringMapStringDoubleCommandObject;
  @Mock protected CommandObject<Map<String, Object>> mapStringObjectCommandObject;
  @Mock protected CommandObject<Map<String, String>> mapStringStringCommandObject;
  @Mock protected CommandObject<Map<String, TSMGetElement>> mapStringTsmGetElementCommandObject;
  @Mock protected CommandObject<Map<String, TSMRangeElements>> mapStringTsmRangeElementsCommandObject;
  @Mock protected CommandObject<Map<byte[], byte[]>> mapBytesBytesCommandObject;
  @Mock protected CommandObject<MyBean> myBeanCommandObject;
  @Mock protected CommandObject<Object> objectCommandObject;
  @Mock protected CommandObject<ScanResult<Map.Entry<String, String>>> scanResultEntryStringStringCommandObject;
  @Mock protected CommandObject<ScanResult<Map.Entry<byte[], byte[]>>> scanResultEntryBytesBytesCommandObject;
  @Mock protected CommandObject<ScanResult<String>> scanResultStringCommandObject;
  @Mock protected CommandObject<ScanResult<Tuple>> scanResultTupleCommandObject;
  @Mock protected CommandObject<ScanResult<byte[]>> scanResultBytesCommandObject;
  @Mock protected CommandObject<SearchResult> searchResultCommandObject;
  @Mock protected CommandObject<Set<String>> setStringCommandObject;
  @Mock protected CommandObject<Set<byte[]>> setBytesCommandObject;
  @Mock protected CommandObject<StreamEntryID> streamEntryIdCommandObject;
  @Mock protected CommandObject<StreamFullInfo> streamFullInfoCommandObject;
  @Mock protected CommandObject<StreamInfo> streamInfoCommandObject;
  @Mock protected CommandObject<StreamPendingSummary> streamPendingSummaryCommandObject;
  @Mock protected CommandObject<String> stringCommandObject;
  @Mock protected CommandObject<TSElement> tsElementCommandObject;
  @Mock protected CommandObject<TSInfo> tsInfoCommandObject;
  @Mock protected CommandObject<Tuple> tupleCommandObject;
  @Mock protected CommandObject<byte[]> bytesCommandObject;
  // @formatter:on

}
