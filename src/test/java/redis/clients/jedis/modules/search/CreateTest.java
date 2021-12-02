//package redis.clients.jedis.modules.search;
//
//import static org.junit.Assert.*;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import org.junit.Test;
//
//import redis.clients.jedis.search.IndexDefinition;
//import redis.clients.jedis.search.IndexOptions;
//
//public class CreateTest {
//
//  @Test
//  public void defaultOptions() throws Exception {
//    IndexOptions defaultOptions = IndexOptions.defaultOptions();
//    List<String> arrayList = new ArrayList<>();
//    defaultOptions.serializeRedisArgs(arrayList);
//
//    assertEquals(Arrays.asList(), arrayList);
//  }
//
//  @Test
//  public void allOptions() throws Exception {
//    IndexOptions defaultOptions = new Client.IndexOptions(0);
//
//    defaultOptions.setStopwords("stop", "run");
//    defaultOptions.setTemporary(1234L);
//    defaultOptions.setDefinition(new IndexDefinition());
//
//    List<String> arrayList = new ArrayList<>();
//    defaultOptions.serializeRedisArgs(arrayList);
//
//    assertEquals(
//        Arrays.asList("NOOFFSETS", "NOFIELDS", "NOFREQS", "TEMPORARY", "1234", "STOPWORDS", "2", "stop", "run"),
//        arrayList);
//  }
//
//  @Test
//  public void allIndexDefinition() throws Exception {
//    IndexDefinition indexRule = new IndexDefinition(IndexDefinition.Type.HASH);
//
//    indexRule.setAsync(true);
//    indexRule.setFilter("@sum<30");
//    indexRule.setLanguage("FR");
//    indexRule.setLanguageField("myLanguage");
//    indexRule.setPayloadField("myPayload");
//    indexRule.setPrefixes("person:");
//    indexRule.setScore(0.818656);
//    indexRule.setScoreFiled("myScore");
//
//    List<String> arrayList = new ArrayList<>();
//    indexRule.serializeRedisArgs(arrayList);
//
//    assertEquals(Arrays.asList("ON", "HASH", "ASYNC", "PREFIX", "1", "person:", "FILTER", "@sum<30", "LANGUAGE_FIELD",
//        "myLanguage", "LANGUAGE", "FR", "SCORE_FIELD", "myScore", "SCORE", "0.818656", "PAYLOAD_FIELD", "myPayload"),
//        arrayList);
//
//    assertEquals(true, indexRule.isAsync());
//    assertEquals("@sum<30", indexRule.getFilter());
//    assertEquals("FR", indexRule.getLanguage());
//    assertEquals("myLanguage", indexRule.getLanguageField());
//    assertEquals("myPayload", indexRule.getPayloadField());
//    assertArrayEquals(new String[]{"person:"}, indexRule.getPrefixes());
//    assertEquals(0.818656, indexRule.getScore(), 0.0);
//    assertEquals("myScore", indexRule.getScoreFiled());
//    assertEquals(IndexDefinition.Type.HASH, indexRule.getType());
//  }
//}
