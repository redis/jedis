package redis.clients.jedis.mocked.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import redis.clients.jedis.Response;
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

public class PipeliningBaseSearchAndQueryCommandsTest extends PipeliningBaseMockedTestBase {

  @Test
  public void testFtAggregate() {
    AggregationBuilder aggr = new AggregationBuilder().groupBy("@field");

    when(commandObjects.ftAggregate("myIndex", aggr)).thenReturn(aggregationResultCommandObject);

    Response<AggregationResult> response = pipeliningBase.ftAggregate("myIndex", aggr);

    assertThat(commands, contains(aggregationResultCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtAliasAdd() {
    when(commandObjects.ftAliasAdd("myAlias", "myIndex")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftAliasAdd("myAlias", "myIndex");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtAliasDel() {
    when(commandObjects.ftAliasDel("myAlias")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftAliasDel("myAlias");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtAliasUpdate() {
    when(commandObjects.ftAliasUpdate("myAlias", "myIndex")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftAliasUpdate("myAlias", "myIndex");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtAlterWithSchema() {
    Schema schema = new Schema().addField(new Schema.Field("newField", Schema.FieldType.TEXT));

    when(commandObjects.ftAlter("myIndex", schema)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftAlter("myIndex", schema);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtAlterWithSchemaFields() {
    Iterable<SchemaField> schemaFields = Collections.singletonList(new TextField("newField"));

    when(commandObjects.ftAlter("myIndex", schemaFields)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftAlter("myIndex", schemaFields);

    assertThat(commands, contains(stringCommandObject));
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
  public void testFtCreateWithOptionsAndSchema() {
    IndexOptions indexOptions = IndexOptions.defaultOptions();
    Schema schema = new Schema().addField(new Schema.Field("myField", Schema.FieldType.TEXT));

    when(commandObjects.ftCreate("myIndex", indexOptions, schema)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftCreate("myIndex", indexOptions, schema);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtCreateWithCreateParamsAndSchemaFields() {
    FTCreateParams createParams = FTCreateParams.createParams();
    Iterable<SchemaField> schemaFields = Collections.singletonList(new TextField("myField"));

    when(commandObjects.ftCreate("myIndex", createParams, schemaFields)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftCreate("myIndex", createParams, schemaFields);

    assertThat(commands, contains(stringCommandObject));
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
  public void testFtDictAddBySampleKey() {
    String[] terms = { "term1", "term2" };

    when(commandObjects.ftDictAddBySampleKey("myIndex", "myDict", terms)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.ftDictAddBySampleKey("myIndex", "myDict", terms);

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
  public void testFtDictDelBySampleKey() {
    String[] terms = { "term1", "term2" };

    when(commandObjects.ftDictDelBySampleKey("myIndex", "myDict", terms)).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.ftDictDelBySampleKey("myIndex", "myDict", terms);

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
  public void testFtDictDumpBySampleKey() {
    when(commandObjects.ftDictDumpBySampleKey("myIndex", "myDict")).thenReturn(setStringCommandObject);

    Response<Set<String>> response = pipeliningBase.ftDictDumpBySampleKey("myIndex", "myDict");

    assertThat(commands, contains(setStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtDropIndex() {
    when(commandObjects.ftDropIndex("myIndex")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftDropIndex("myIndex");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtDropIndexDD() {
    when(commandObjects.ftDropIndexDD("myIndex")).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftDropIndexDD("myIndex");

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtExplain() {
    Query query = new Query("hello world");

    when(commandObjects.ftExplain("myIndex", query)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftExplain("myIndex", query);

    assertThat(commands, contains(stringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtExplainCLI() {
    Query query = new Query("hello world");

    when(commandObjects.ftExplainCLI("myIndex", query)).thenReturn(listStringCommandObject);

    Response<List<String>> response = pipeliningBase.ftExplainCLI("myIndex", query);

    assertThat(commands, contains(listStringCommandObject));
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
  public void testFtSearch() {
    String query = "hello world";

    when(commandObjects.ftSearch("myIndex", query)).thenReturn(searchResultCommandObject);

    Response<SearchResult> response = pipeliningBase.ftSearch("myIndex", query);

    assertThat(commands, contains(searchResultCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtSearchWithParams() {
    String query = "hello world";
    FTSearchParams searchParams = FTSearchParams.searchParams().limit(0, 10);

    when(commandObjects.ftSearch("myIndex", query, searchParams)).thenReturn(searchResultCommandObject);

    Response<SearchResult> response = pipeliningBase.ftSearch("myIndex", query, searchParams);

    assertThat(commands, contains(searchResultCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtSearchWithQueryObject() {
    Query query = new Query("hello world").limit(0, 10);

    when(commandObjects.ftSearch("myIndex", query)).thenReturn(searchResultCommandObject);

    Response<SearchResult> response = pipeliningBase.ftSearch("myIndex", query);

    assertThat(commands, contains(searchResultCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtSearchWithQueryObjectBinary() {
    byte[] indexName = "myIndex".getBytes();
    Query query = new Query("hello world").limit(0, 10);

    when(commandObjects.ftSearch(indexName, query)).thenReturn(searchResultCommandObject);

    Response<SearchResult> response = pipeliningBase.ftSearch(indexName, query);

    assertThat(commands, contains(searchResultCommandObject));
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
  public void testFtSynDump() {
    when(commandObjects.ftSynDump("myIndex")).thenReturn(mapStringListStringCommandObject);

    Response<Map<String, List<String>>> response = pipeliningBase.ftSynDump("myIndex");

    assertThat(commands, contains(mapStringListStringCommandObject));
    assertThat(response, is(predefinedResponse));
  }

  @Test
  public void testFtSynUpdate() {
    String synonymGroupId = "group1";
    String[] terms = { "term1", "term2" };

    when(commandObjects.ftSynUpdate("myIndex", synonymGroupId, terms)).thenReturn(stringCommandObject);

    Response<String> response = pipeliningBase.ftSynUpdate("myIndex", synonymGroupId, terms);

    assertThat(commands, contains(stringCommandObject));
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
  public void testFtSugDel() {
    when(commandObjects.ftSugDel("mySug", "hello")).thenReturn(booleanCommandObject);

    Response<Boolean> response = pipeliningBase.ftSugDel("mySug", "hello");

    assertThat(commands, contains(booleanCommandObject));
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
  public void testFtSugLen() {
    when(commandObjects.ftSugLen("mySug")).thenReturn(longCommandObject);

    Response<Long> response = pipeliningBase.ftSugLen("mySug");

    assertThat(commands, contains(longCommandObject));
    assertThat(response, is(predefinedResponse));
  }

}
