package redis.clients.jedis.mocked.unified;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.AggregationResult;
import redis.clients.jedis.search.schemafields.SchemaField;
import redis.clients.jedis.search.schemafields.TextField;

public class UnifiedJedisSearchAndQueryCommandsTest extends UnifiedJedisMockedTestBase {

  @Test
  public void testFtAggregate() {
    String indexName = "myIndex";
    AggregationBuilder aggr = new AggregationBuilder().groupBy("@field");
    AggregationResult expectedResponse = mock(AggregationResult.class);

    when(commandObjects.ftAggregate(indexName, aggr)).thenReturn(aggregationResultCommandObject);
    when(commandExecutor.executeCommand(aggregationResultCommandObject)).thenReturn(expectedResponse);

    AggregationResult result = jedis.ftAggregate(indexName, aggr);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(aggregationResultCommandObject);
    verify(commandObjects).ftAggregate(indexName, aggr);
  }

  @Test
  public void testFtAliasAdd() {
    String aliasName = "myAlias";
    String indexName = "myIndex";
    String expectedResponse = "OK";

    when(commandObjects.ftAliasAdd(aliasName, indexName)).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.ftAliasAdd(aliasName, indexName);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).ftAliasAdd(aliasName, indexName);
  }

  @Test
  public void testFtAliasDel() {
    String aliasName = "myAlias";
    String expectedResponse = "OK";

    when(commandObjects.ftAliasDel(aliasName)).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.ftAliasDel(aliasName);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).ftAliasDel(aliasName);
  }

  @Test
  public void testFtAliasUpdate() {
    String aliasName = "myAlias";
    String indexName = "myIndex";
    String expectedResponse = "OK";

    when(commandObjects.ftAliasUpdate(aliasName, indexName)).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.ftAliasUpdate(aliasName, indexName);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).ftAliasUpdate(aliasName, indexName);
  }

  @Test
  public void testFtAlterWithSchema() {
    String indexName = "myIndex";
    Schema schema = new Schema().addField(new Schema.Field("myField", Schema.FieldType.TEXT));
    String expectedResponse = "OK";

    when(commandObjects.ftAlter(indexName, schema)).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.ftAlter(indexName, schema);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).ftAlter(indexName, schema);
  }

  @Test
  public void testFtAlterWithSchemaFields() {
    String indexName = "myIndex";
    Iterable<SchemaField> schemaFields = Collections.singletonList(new TextField("newField"));
    String expectedResponse = "OK";

    when(commandObjects.ftAlter(indexName, schemaFields)).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.ftAlter(indexName, schemaFields);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).ftAlter(indexName, schemaFields);
  }

  @Test
  public void testFtConfigGet() {
    String option = "TIMEOUT";
    Map<String, Object> expectedResponse = Collections.singletonMap(option, "1000");

    when(commandObjects.ftConfigGet(option)).thenReturn(mapStringObjectCommandObject);
    when(commandExecutor.executeCommand(mapStringObjectCommandObject)).thenReturn(expectedResponse);

    Map<String, Object> result = jedis.ftConfigGet(option);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(mapStringObjectCommandObject);
    verify(commandObjects).ftConfigGet(option);
  }

  @Test
  public void testFtConfigGetWithIndexName() {
    String indexName = "myIndex";
    String option = "TIMEOUT";
    Map<String, Object> expectedResponse = Collections.singletonMap(option, "1000");

    when(commandObjects.ftConfigGet(indexName, option)).thenReturn(mapStringObjectCommandObject);
    when(commandExecutor.executeCommand(mapStringObjectCommandObject)).thenReturn(expectedResponse);

    Map<String, Object> result = jedis.ftConfigGet(indexName, option);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(mapStringObjectCommandObject);
    verify(commandObjects).ftConfigGet(indexName, option);
  }

  @Test
  public void testFtConfigSet() {
    String option = "TIMEOUT";
    String value = "1000";
    String expectedResponse = "OK";

    when(commandObjects.ftConfigSet(option, value)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.ftConfigSet(option, value);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).ftConfigSet(option, value);
  }

  @Test
  public void testFtConfigSetWithIndexName() {
    String indexName = "myIndex";
    String option = "TIMEOUT";
    String value = "1000";
    String expectedResponse = "OK";

    when(commandObjects.ftConfigSet(indexName, option, value)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.ftConfigSet(indexName, option, value);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).ftConfigSet(indexName, option, value);
  }

  @Test
  public void testFtCreateWithOptionsAndSchema() {
    String indexName = "myIndex";
    IndexOptions indexOptions = IndexOptions.defaultOptions();
    Schema schema = new Schema().addField(new Schema.Field("myField", Schema.FieldType.TEXT));
    String expectedResponse = "OK";

    when(commandObjects.ftCreate(indexName, indexOptions, schema)).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.ftCreate(indexName, indexOptions, schema);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).ftCreate(indexName, indexOptions, schema);
  }

  @Test
  public void testFtCreateWithCreateParamsAndSchemaFields() {
    String indexName = "myIndex";
    FTCreateParams createParams = FTCreateParams.createParams();
    Iterable<SchemaField> schemaFields = Collections.singletonList(new TextField("myField"));
    String expectedResponse = "OK";

    when(commandObjects.ftCreate(indexName, createParams, schemaFields)).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.ftCreate(indexName, createParams, schemaFields);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).ftCreate(indexName, createParams, schemaFields);
  }

  @Test
  public void testFtCursorDel() {
    String indexName = "myIndex";
    long cursorId = 123L;
    String expectedResponse = "OK";

    when(commandObjects.ftCursorDel(indexName, cursorId)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.ftCursorDel(indexName, cursorId);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).ftCursorDel(indexName, cursorId);
  }

  @Test
  public void testFtCursorRead() {
    String indexName = "myIndex";
    long cursorId = 123L;
    int count = 10;
    AggregationResult expectedResponse = mock(AggregationResult.class);

    when(commandObjects.ftCursorRead(indexName, cursorId, count)).thenReturn(aggregationResultCommandObject);
    when(commandExecutor.executeCommand(aggregationResultCommandObject)).thenReturn(expectedResponse);

    AggregationResult result = jedis.ftCursorRead(indexName, cursorId, count);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(aggregationResultCommandObject);
    verify(commandObjects).ftCursorRead(indexName, cursorId, count);
  }

  @Test
  public void testFtDictAdd() {
    String dictionary = "myDict";
    String[] terms = { "term1", "term2" };
    long expectedResponse = 2L;

    when(commandObjects.ftDictAdd(dictionary, terms)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.ftDictAdd(dictionary, terms);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).ftDictAdd(dictionary, terms);
  }

  @Test
  public void testFtDictAddBySampleKey() {
    String indexName = "myIndex";
    String dictionary = "myDict";
    String[] terms = { "term1", "term2" };
    long expectedResponse = 2L;

    when(commandObjects.ftDictAddBySampleKey(indexName, dictionary, terms)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.ftDictAddBySampleKey(indexName, dictionary, terms);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).ftDictAddBySampleKey(indexName, dictionary, terms);
  }

  @Test
  public void testFtDictDel() {
    String dictionary = "myDict";
    String[] terms = { "term1", "term2" };
    long expectedResponse = 1L;

    when(commandObjects.ftDictDel(dictionary, terms)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.ftDictDel(dictionary, terms);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).ftDictDel(dictionary, terms);
  }

  @Test
  public void testFtDictDelBySampleKey() {
    String indexName = "myIndex";
    String dictionary = "myDict";
    String[] terms = { "term1", "term2" };
    long expectedResponse = 1L;

    when(commandObjects.ftDictDelBySampleKey(indexName, dictionary, terms)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.ftDictDelBySampleKey(indexName, dictionary, terms);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).ftDictDelBySampleKey(indexName, dictionary, terms);
  }

  @Test
  public void testFtDictDump() {
    String dictionary = "myDict";
    Set<String> expectedResponse = new HashSet<>(Arrays.asList("term1", "term2"));

    when(commandObjects.ftDictDump(dictionary)).thenReturn(setStringCommandObject);
    when(commandExecutor.executeCommand(setStringCommandObject)).thenReturn(expectedResponse);

    Set<String> result = jedis.ftDictDump(dictionary);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(setStringCommandObject);
    verify(commandObjects).ftDictDump(dictionary);
  }

  @Test
  public void testFtDictDumpBySampleKey() {
    String indexName = "myIndex";
    String dictionary = "myDict";
    Set<String> expectedResponse = new HashSet<>(Arrays.asList("term1", "term2"));

    when(commandObjects.ftDictDumpBySampleKey(indexName, dictionary)).thenReturn(setStringCommandObject);
    when(commandExecutor.executeCommand(setStringCommandObject)).thenReturn(expectedResponse);

    Set<String> result = jedis.ftDictDumpBySampleKey(indexName, dictionary);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(setStringCommandObject);
    verify(commandObjects).ftDictDumpBySampleKey(indexName, dictionary);
  }

  @Test
  public void testFtDropIndex() {
    String indexName = "myIndex";
    String expectedResponse = "OK";

    when(commandObjects.ftDropIndex(indexName)).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.ftDropIndex(indexName);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).ftDropIndex(indexName);
  }

  @Test
  public void testFtDropIndexDD() {
    String indexName = "myIndex";
    String expectedResponse = "OK";

    when(commandObjects.ftDropIndexDD(indexName)).thenReturn(stringCommandObject);
    when(commandExecutor.broadcastCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.ftDropIndexDD(indexName);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).broadcastCommand(stringCommandObject);
    verify(commandObjects).ftDropIndexDD(indexName);
  }

  @Test
  public void testFtExplain() {
    String indexName = "myIndex";
    Query query = new Query("hello world").limit(0, 10);
    String expectedResponse = "QUERY PLAN";

    when(commandObjects.ftExplain(indexName, query)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.ftExplain(indexName, query);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).ftExplain(indexName, query);
  }

  @Test
  public void testFtExplainCLI() {
    String indexName = "myIndex";
    Query query = new Query("hello world").limit(0, 10);
    List<String> expectedResponse = Arrays.asList("QUERY PLAN", "DETAILS");

    when(commandObjects.ftExplainCLI(indexName, query)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedResponse);

    List<String> result = jedis.ftExplainCLI(indexName, query);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).ftExplainCLI(indexName, query);
  }

  @Test
  public void testFtInfo() {
    String indexName = "myIndex";
    Map<String, Object> expectedResponse = Collections.singletonMap("index_definition", Collections.singletonMap("key_type", "HASH"));

    when(commandObjects.ftInfo(indexName)).thenReturn(mapStringObjectCommandObject);
    when(commandExecutor.executeCommand(mapStringObjectCommandObject)).thenReturn(expectedResponse);

    Map<String, Object> result = jedis.ftInfo(indexName);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(mapStringObjectCommandObject);
    verify(commandObjects).ftInfo(indexName);
  }

  @Test
  public void testFtList() {
    Set<String> expectedResponse = new HashSet<>(Arrays.asList("index1", "index2"));

    when(commandObjects.ftList()).thenReturn(setStringCommandObject);
    when(commandExecutor.executeCommand(setStringCommandObject)).thenReturn(expectedResponse);

    Set<String> result = jedis.ftList();

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(setStringCommandObject);
    verify(commandObjects).ftList();
  }

  @Test
  public void testFtSearch() {
    String indexName = "myIndex";
    String query = "hello world";
    SearchResult expectedResponse = mock(SearchResult.class);

    when(commandObjects.ftSearch(indexName, query)).thenReturn(searchResultCommandObject);
    when(commandExecutor.executeCommand(searchResultCommandObject)).thenReturn(expectedResponse);

    SearchResult result = jedis.ftSearch(indexName, query);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(searchResultCommandObject);
    verify(commandObjects).ftSearch(indexName, query);
  }

  @Test
  public void testFtSearchWithParams() {
    String indexName = "myIndex";
    String query = "hello world";
    FTSearchParams params = new FTSearchParams().noContent().limit(0, 10);
    SearchResult expectedResponse = mock(SearchResult.class);

    when(commandObjects.ftSearch(indexName, query, params)).thenReturn(searchResultCommandObject);
    when(commandExecutor.executeCommand(searchResultCommandObject)).thenReturn(expectedResponse);

    SearchResult result = jedis.ftSearch(indexName, query, params);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(searchResultCommandObject);
    verify(commandObjects).ftSearch(indexName, query, params);
  }

  @Test
  public void testFtSearchWithQueryObject() {
    String indexName = "myIndex";
    Query query = new Query("hello world");
    SearchResult expectedResponse = mock(SearchResult.class);

    when(commandObjects.ftSearch(indexName, query)).thenReturn(searchResultCommandObject);
    when(commandExecutor.executeCommand(searchResultCommandObject)).thenReturn(expectedResponse);

    SearchResult result = jedis.ftSearch(indexName, query);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(searchResultCommandObject);
    verify(commandObjects).ftSearch(indexName, query);
  }

  @Test
  public void testFtSearchWithQueryObjectBinary() {
    byte[] indexName = "myIndex".getBytes();
    Query query = new Query("hello world").limit(0, 10);
    SearchResult expectedResponse = mock(SearchResult.class);

    when(commandObjects.ftSearch(indexName, query)).thenReturn(searchResultCommandObject);
    when(commandExecutor.executeCommand(searchResultCommandObject)).thenReturn(expectedResponse);

    SearchResult result = jedis.ftSearch(indexName, query);

    assertThat(result, sameInstance(expectedResponse));

    verify(commandExecutor).executeCommand(searchResultCommandObject);
    verify(commandObjects).ftSearch(indexName, query);
  }

  @Test
  public void testFtSpellCheck() {
    String index = "myIndex";
    String query = "hello world";
    Map<String, Map<String, Double>> expectedResponse = Collections.singletonMap("term1", Collections.singletonMap("suggestion1", 1.0));

    when(commandObjects.ftSpellCheck(index, query)).thenReturn(mapStringMapStringDoubleCommandObject);
    when(commandExecutor.executeCommand(mapStringMapStringDoubleCommandObject)).thenReturn(expectedResponse);

    Map<String, Map<String, Double>> result = jedis.ftSpellCheck(index, query);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(mapStringMapStringDoubleCommandObject);
    verify(commandObjects).ftSpellCheck(index, query);
  }

  @Test
  public void testFtSpellCheckWithParams() {
    String index = "myIndex";
    String query = "hello world";
    FTSpellCheckParams spellCheckParams = new FTSpellCheckParams().distance(1);
    Map<String, Map<String, Double>> expectedResponse = Collections.singletonMap("term1", Collections.singletonMap("suggestion1", 1.0));

    when(commandObjects.ftSpellCheck(index, query, spellCheckParams)).thenReturn(mapStringMapStringDoubleCommandObject);
    when(commandExecutor.executeCommand(mapStringMapStringDoubleCommandObject)).thenReturn(expectedResponse);

    Map<String, Map<String, Double>> result = jedis.ftSpellCheck(index, query, spellCheckParams);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(mapStringMapStringDoubleCommandObject);
    verify(commandObjects).ftSpellCheck(index, query, spellCheckParams);
  }

  @Test
  public void testFtSynDump() {
    String indexName = "myIndex";
    Map<String, List<String>> expectedResponse = Collections.singletonMap("group1", Arrays.asList("term1", "term2"));

    when(commandObjects.ftSynDump(indexName)).thenReturn(mapStringListStringCommandObject);
    when(commandExecutor.executeCommand(mapStringListStringCommandObject)).thenReturn(expectedResponse);

    Map<String, List<String>> result = jedis.ftSynDump(indexName);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(mapStringListStringCommandObject);
    verify(commandObjects).ftSynDump(indexName);
  }

  @Test
  public void testFtSynUpdate() {
    String indexName = "myIndex";
    String synonymGroupId = "group1";
    String[] terms = { "term1", "term2" };
    String expectedResponse = "OK";

    when(commandObjects.ftSynUpdate(indexName, synonymGroupId, terms)).thenReturn(stringCommandObject);
    when(commandExecutor.executeCommand(stringCommandObject)).thenReturn(expectedResponse);

    String result = jedis.ftSynUpdate(indexName, synonymGroupId, terms);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(stringCommandObject);
    verify(commandObjects).ftSynUpdate(indexName, synonymGroupId, terms);
  }

  @Test
  public void testFtTagVals() {
    String indexName = "myIndex";
    String fieldName = "myField";
    Set<String> expectedResponse = new HashSet<>(Arrays.asList("tag1", "tag2"));

    when(commandObjects.ftTagVals(indexName, fieldName)).thenReturn(setStringCommandObject);
    when(commandExecutor.executeCommand(setStringCommandObject)).thenReturn(expectedResponse);

    Set<String> result = jedis.ftTagVals(indexName, fieldName);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(setStringCommandObject);
    verify(commandObjects).ftTagVals(indexName, fieldName);
  }

  @Test
  public void testFtSugAdd() {
    String key = "sugKey";
    String string = "suggestion";
    double score = 1.0;
    long expectedResponse = 1L;

    when(commandObjects.ftSugAdd(key, string, score)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.ftSugAdd(key, string, score);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).ftSugAdd(key, string, score);
  }

  @Test
  public void testFtSugAddIncr() {
    String key = "sugKey";
    String string = "suggestion";
    double score = 1.0;
    long expectedResponse = 2L;

    when(commandObjects.ftSugAddIncr(key, string, score)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.ftSugAddIncr(key, string, score);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).ftSugAddIncr(key, string, score);
  }

  @Test
  public void testFtSugDel() {
    String key = "sugKey";
    String string = "suggestion";
    boolean expectedResponse = true;

    when(commandObjects.ftSugDel(key, string)).thenReturn(booleanCommandObject);
    when(commandExecutor.executeCommand(booleanCommandObject)).thenReturn(expectedResponse);

    boolean result = jedis.ftSugDel(key, string);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(booleanCommandObject);
    verify(commandObjects).ftSugDel(key, string);
  }

  @Test
  public void testFtSugGet() {
    String key = "sugKey";
    String prefix = "sug";
    List<String> expectedResponse = Arrays.asList("suggestion1", "suggestion2");

    when(commandObjects.ftSugGet(key, prefix)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedResponse);

    List<String> result = jedis.ftSugGet(key, prefix);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).ftSugGet(key, prefix);
  }

  @Test
  public void testFtSugGetWithFuzzyAndMax() {
    String key = "sugKey";
    String prefix = "sug";
    boolean fuzzy = true;
    int max = 10;
    List<String> expectedResponse = Arrays.asList("suggestion1", "suggestion2");

    when(commandObjects.ftSugGet(key, prefix, fuzzy, max)).thenReturn(listStringCommandObject);
    when(commandExecutor.executeCommand(listStringCommandObject)).thenReturn(expectedResponse);

    List<String> result = jedis.ftSugGet(key, prefix, fuzzy, max);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(listStringCommandObject);
    verify(commandObjects).ftSugGet(key, prefix, fuzzy, max);
  }

  @Test
  public void testFtSugGetWithScores() {
    String key = "sugKey";
    String prefix = "sug";
    List<Tuple> expectedResponse = Arrays.asList(new Tuple("suggestion1", 1.0), new Tuple("suggestion2", 0.8));

    when(commandObjects.ftSugGetWithScores(key, prefix)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedResponse);

    List<Tuple> result = jedis.ftSugGetWithScores(key, prefix);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).ftSugGetWithScores(key, prefix);
  }

  @Test
  public void testFtSugGetWithScoresAndFuzzyMax() {
    String key = "sugKey";
    String prefix = "sug";
    boolean fuzzy = true;
    int max = 10;
    List<Tuple> expectedResponse = Arrays.asList(new Tuple("suggestion1", 1.0), new Tuple("suggestion2", 0.8));

    when(commandObjects.ftSugGetWithScores(key, prefix, fuzzy, max)).thenReturn(listTupleCommandObject);
    when(commandExecutor.executeCommand(listTupleCommandObject)).thenReturn(expectedResponse);

    List<Tuple> result = jedis.ftSugGetWithScores(key, prefix, fuzzy, max);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(listTupleCommandObject);
    verify(commandObjects).ftSugGetWithScores(key, prefix, fuzzy, max);
  }

  @Test
  public void testFtSugLen() {
    String key = "sugKey";
    long expectedResponse = 42L;

    when(commandObjects.ftSugLen(key)).thenReturn(longCommandObject);
    when(commandExecutor.executeCommand(longCommandObject)).thenReturn(expectedResponse);

    long result = jedis.ftSugLen(key);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(longCommandObject);
    verify(commandObjects).ftSugLen(key);
  }

  @Test
  public void testFtProfileAggregate() {
    String indexName = "myIndex";
    FTProfileParams profileParams = new FTProfileParams();
    AggregationBuilder aggr = new AggregationBuilder().groupBy("@field");
    Map.Entry<AggregationResult, ProfilingInfo> expectedResponse = new AbstractMap.SimpleEntry<>(
        mock(AggregationResult.class), mock(ProfilingInfo.class));

    when(commandObjects.ftProfileAggregate(indexName, profileParams, aggr)).thenReturn(entryAggregationResultMapStringObjectCommandObject);
    when(commandExecutor.executeCommand(entryAggregationResultMapStringObjectCommandObject)).thenReturn(expectedResponse);

    Map.Entry<AggregationResult, ProfilingInfo> result = jedis.ftProfileAggregate(indexName, profileParams, aggr);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(entryAggregationResultMapStringObjectCommandObject);
    verify(commandObjects).ftProfileAggregate(indexName, profileParams, aggr);
  }

  @Test
  public void testFtProfileSearchWithQueryObject() {
    String indexName = "myIndex";
    FTProfileParams profileParams = new FTProfileParams();
    Query query = new Query("hello world").limit(0, 10);
    Map.Entry<SearchResult, ProfilingInfo> expectedResponse = new AbstractMap.SimpleEntry<>(
        mock(SearchResult.class), mock(ProfilingInfo.class));

    when(commandObjects.ftProfileSearch(indexName, profileParams, query)).thenReturn(entrySearchResultMapStringObjectCommandObject);
    when(commandExecutor.executeCommand(entrySearchResultMapStringObjectCommandObject)).thenReturn(expectedResponse);

    Map.Entry<SearchResult, ProfilingInfo> result = jedis.ftProfileSearch(indexName, profileParams, query);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(entrySearchResultMapStringObjectCommandObject);
    verify(commandObjects).ftProfileSearch(indexName, profileParams, query);
  }

  @Test
  public void testFtProfileSearchWithQueryAndSearchParams() {
    String indexName = "myIndex";
    FTProfileParams profileParams = new FTProfileParams();
    String query = "hello world";
    FTSearchParams searchParams = new FTSearchParams().noContent().limit(0, 10);
    Map.Entry<SearchResult, ProfilingInfo> expectedResponse = new AbstractMap.SimpleEntry<>(
        mock(SearchResult.class), mock(ProfilingInfo.class));

    when(commandObjects.ftProfileSearch(indexName, profileParams, query, searchParams)).thenReturn(entrySearchResultMapStringObjectCommandObject);
    when(commandExecutor.executeCommand(entrySearchResultMapStringObjectCommandObject)).thenReturn(expectedResponse);

    Map.Entry<SearchResult, ProfilingInfo> result = jedis.ftProfileSearch(indexName, profileParams, query, searchParams);

    assertThat(result, equalTo(expectedResponse));

    verify(commandExecutor).executeCommand(entrySearchResultMapStringObjectCommandObject);
    verify(commandObjects).ftProfileSearch(indexName, profileParams, query, searchParams);
  }

  @Test
  public void testSetDefaultSearchDialect() {
    int dialect = 1;

    jedis.setDefaultSearchDialect(dialect);

    verify(commandObjects).setDefaultSearchDialect(dialect);
  }

}
