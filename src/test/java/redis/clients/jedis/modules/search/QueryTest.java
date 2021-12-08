//package redis.clients.jedis.modules.search;
//
//import static org.junit.Assert.*;
//
//import java.util.ArrayList;
//import org.junit.Before;
//import org.junit.Test;
//import redis.clients.jedis.search.Query;
//
//public class QueryTest {
//
//  Query query;
//
//  @Before
//  public void setUp() throws Exception {
//    query = new Query("hello world");
//  }
//
//  @Test
//  public void getNoContent() throws Exception {
//    assertFalse(query.getNoContent());
//    assertEquals(query, query.setNoContent());
//    assertTrue(query.getNoContent());
//  }
//
//  @Test
//  public void getWithScores() throws Exception {
//    assertFalse(query.getWithScores());
//    assertEquals(query, query.setWithScores());
//    assertTrue(query.getWithScores());
//  }
//
//  @Test
//  public void serializeRedisArgs() throws Exception {
//    query.setNoContent().setLanguage("xx").setNoStopwords().setVerbatim().setWithPayload().setWithScores().setScorer("My.Scorer");
//
//    ArrayList<byte[]> args = new ArrayList<>(1);
//    query.serializeRedisArgs(args);
//
//    assertEquals(10, args.size());
//    assertEquals(query._queryString, new String(args.get(0)));
////    assertTrue(args.contains("xx".getBytes()));
////    assertTrue(args.contains("NOSTOPWORDS".getBytes()));
////    assertTrue(args.contains("VERBATIM".getBytes()));
////    assertTrue(args.contains("PAYLOADS".getBytes()));
////    assertTrue(args.contains("WITHSCORES".getBytes()));
//  }
//
//  @Test
//  public void limit() throws Exception {
//    assertEquals(0, query._paging.offset);
//    assertEquals(10, query._paging.num);
//    assertEquals(query, query.limit(1, 30));
//    assertEquals(1, query._paging.offset);
//    assertEquals(30, query._paging.num);
//
//  }
//
//  @Test
//  public void addFilter() throws Exception {
//    assertEquals(0, query._filters.size());
//    Query.NumericFilter f = new Query.NumericFilter("foo", 0, 100);
//    assertEquals(query, query.addFilter(f));
//    assertEquals(f, query._filters.get(0));
//  }
//
//  @Test
//  public void setVerbatim() throws Exception {
//    assertFalse(query._verbatim);
//    assertEquals(query, query.setVerbatim());
//    assertTrue(query._verbatim);
//  }
//
//  @Test
//  public void setNoStopwords() throws Exception {
//    assertFalse(query._noStopwords);
//    assertEquals(query, query.setNoStopwords());
//    assertTrue(query._noStopwords);
//
//  }
//
//  @Test
//  public void setLanguage() throws Exception {
//    assertEquals(null, query._language);
//    assertEquals(query, query.setLanguage("chinese"));
//    assertEquals("chinese", query._language);
//  }
//
//  @Test
//  public void setScorer() throws Exception {
//    assertEquals(null, query._scorer);
//    assertEquals(query, query.setScorer("the.scroer"));
//    assertEquals("the.scroer", query._scorer);
//  }
//
//  @Test
//  public void limitFields() throws Exception {
//    assertNull(query._fields);
//    assertEquals(query, query.limitFields("foo", "bar"));
//    assertEquals(2, query._fields.length);
//  }
//
//  @Test
//  public void returnFields() throws Exception {
//    assertNull(query._returnFields);
//    assertEquals(query, query.returnFields("foo", "bar"));
//    assertEquals(2, query._returnFields.length);
//  }
//
//  @Test
//  public void highlightFields() throws Exception {
//    assertEquals(false, query.wantsHighlight);
//    assertNull(query.highlightFields);
//
//    query = new Query("Hello");
//    assertEquals(query, query.highlightFields("foo", "bar"));
//    assertEquals(2, query.highlightFields.length);
//    assertNull(query.highlightTags);
//    assertEquals(true, query.wantsHighlight);
//
//    query = new Query("Hello").highlightFields();
//    assertNull(query.highlightFields);
//    assertNull(query.highlightTags);
//    assertEquals(true, query.wantsHighlight);
//
//    assertEquals(query, query.highlightFields(new Query.HighlightTags("<b>", "</b>")));
//    assertNull(query.highlightFields);
//    assertEquals(2, query.highlightTags.length);
//    assertEquals("<b>", query.highlightTags[0]);
//    assertEquals("</b>", query.highlightTags[1]);
//  }
//
//  @Test
//  public void summarizeFields() throws Exception {
//    assertEquals(false, query.wantsSummarize);
//    assertNull(query.summarizeFields);
//
//    query = new Query("Hello");
//    assertEquals(query, query.summarizeFields());
//    assertEquals(true, query.wantsSummarize);
//    assertNull(query.summarizeFields);
//    assertEquals(-1, query.summarizeFragmentLen);
//    assertEquals(-1, query.summarizeNumFragments);
//
//    query = new Query("Hello");
//    assertEquals(query, query.summarizeFields("someField"));
//    assertEquals(true, query.wantsSummarize);
//    assertEquals(1, query.summarizeFields.length);
//    assertEquals(-1, query.summarizeFragmentLen);
//    assertEquals(-1, query.summarizeNumFragments);
//  }
//
//}
