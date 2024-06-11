package redis.clients.jedis.modules.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.Connection;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.graph.Header;
import redis.clients.jedis.graph.Record;
import redis.clients.jedis.graph.ResultSet;
import redis.clients.jedis.graph.entities.Node;
import redis.clients.jedis.graph.entities.Property;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;

@org.junit.Ignore
@RunWith(Parameterized.class)
public class GraphPipelineTest extends RedisModuleCommandsTestBase {

//  private Connection c;

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }

  public GraphPipelineTest(RedisProtocol protocol) {
    super(protocol);
  }

//
//  @Before
//  public void createApi() {
//    api = new RedisGraph();
//  }
//
//  @After
//  public void deleteGraph() {
//    api.deleteGraph("social");
//    api.close();
//  }
//
//  @Before
//  public void createApi() {
//    c = createConnection();
//  }
//
//  @After
//  public void deleteGraph() {
//    c.close();
//  }

  @Test
  public void testSync() {
//    Pipeline pipeline = new Pipeline(c);
    Pipeline pipeline = (Pipeline) client.pipelined();

    pipeline.set("x", "1");
    pipeline.graphQuery("social", "CREATE (:Person {name:'a'})");
    pipeline.graphQuery("g", "CREATE (:Person {name:'a'})");
    pipeline.incr("x");
    pipeline.get("x");
    pipeline.graphQuery("social", "MATCH (n:Person) RETURN n");
    pipeline.graphDelete("g");
//    pipeline.callProcedure("social", "db.labels");
    pipeline.graphQuery("social", "CALL db.labels()");
    List<Object> results = pipeline.syncAndReturnAll();

    // Redis set command
    assertEquals(String.class, results.get(0).getClass());
    assertEquals("OK", results.get(0));

    // Redis graph command
//    assertEquals(ResultSetImpl.class, results.get(1).getClass());
    ResultSet resultSet = (ResultSet) results.get(1);
    assertEquals(1, resultSet.getStatistics().nodesCreated());
    assertEquals(1, resultSet.getStatistics().propertiesSet());

//    assertEquals(ResultSetImpl.class, results.get(2).getClass());
    resultSet = (ResultSet) results.get(2);
    assertEquals(1, resultSet.getStatistics().nodesCreated());
    assertEquals(1, resultSet.getStatistics().propertiesSet());

    // Redis incr command
    assertEquals(Long.class, results.get(3).getClass());
    assertEquals(2L, results.get(3));

    // Redis get command
    assertEquals(String.class, results.get(4).getClass());
    assertEquals("2", results.get(4));

    // Graph query result
//    assertEquals(ResultSetImpl.class, results.get(5).getClass());
    resultSet = (ResultSet) results.get(5);

    assertNotNull(resultSet.getHeader());
    Header header = resultSet.getHeader();

    List<String> schemaNames = header.getSchemaNames();
    assertNotNull(schemaNames);
    assertEquals(1, schemaNames.size());
    assertEquals("n", schemaNames.get(0));

    Property<String> nameProperty = new Property<>("name", "a");

    Node expectedNode = new Node();
    expectedNode.setId(0);
    expectedNode.addLabel("Person");
    expectedNode.addProperty(nameProperty);
    // see that the result were pulled from the right graph
    assertEquals(1, resultSet.size());
    Iterator<Record> iterator = resultSet.iterator();
    assertTrue(iterator.hasNext());
    Record record = iterator.next();
    assertFalse(iterator.hasNext());
    assertEquals(Arrays.asList("n"), record.keys());
    assertEquals(expectedNode, record.getValue("n"));

//    assertEquals(ResultSetImpl.class, results.get(7).getClass());
    resultSet = (ResultSet) results.get(7);

    assertNotNull(resultSet.getHeader());
    header = resultSet.getHeader();

    schemaNames = header.getSchemaNames();
    assertNotNull(schemaNames);
    assertEquals(1, schemaNames.size());
    assertEquals("label", schemaNames.get(0));

    assertEquals(1, resultSet.size());
    iterator = resultSet.iterator();
    assertTrue(iterator.hasNext());
    record = iterator.next();
    assertFalse(iterator.hasNext());
    assertEquals(Arrays.asList("label"), record.keys());
    assertEquals("Person", record.getValue("label"));
  }

  @Test
  public void testReadOnlyQueries() {
//    Pipeline pipeline = new Pipeline(c);
    Pipeline pipeline = (Pipeline) client.pipelined();

    pipeline.set("x", "1");
    pipeline.graphQuery("social", "CREATE (:Person {name:'a'})");
    pipeline.graphQuery("g", "CREATE (:Person {name:'a'})");
    pipeline.graphReadonlyQuery("social", "MATCH (n:Person) RETURN n");
    pipeline.graphDelete("g");
//    pipeline.callProcedure("social", "db.labels");
    pipeline.graphQuery("social", "CALL db.labels()");
    List<Object> results = pipeline.syncAndReturnAll();

    // Redis set command
    assertEquals(String.class, results.get(0).getClass());
    assertEquals("OK", results.get(0));

    // Redis graph command
//    assertEquals(ResultSetImpl.class, results.get(1).getClass());
    ResultSet resultSet = (ResultSet) results.get(1);
    assertEquals(1, resultSet.getStatistics().nodesCreated());
    assertEquals(1, resultSet.getStatistics().propertiesSet());

//    assertEquals(ResultSetImpl.class, results.get(2).getClass());
    resultSet = (ResultSet) results.get(2);
    assertEquals(1, resultSet.getStatistics().nodesCreated());
    assertEquals(1, resultSet.getStatistics().propertiesSet());

    // Graph read-only query result
//    assertEquals(ResultSetImpl.class, results.get(5).getClass());
    resultSet = (ResultSet) results.get(3);

    assertNotNull(resultSet.getHeader());
    Header header = resultSet.getHeader();

    List<String> schemaNames = header.getSchemaNames();
    assertNotNull(schemaNames);
    assertEquals(1, schemaNames.size());
    assertEquals("n", schemaNames.get(0));

    Property<String> nameProperty = new Property<>("name", "a");

    Node expectedNode = new Node();
    expectedNode.setId(0);
    expectedNode.addLabel("Person");
    expectedNode.addProperty(nameProperty);
    // see that the result were pulled from the right graph
    assertEquals(1, resultSet.size());
    Iterator<Record> iterator = resultSet.iterator();
    assertTrue(iterator.hasNext());
    Record record = iterator.next();
    assertFalse(iterator.hasNext());
    assertEquals(Arrays.asList("n"), record.keys());
    assertEquals(expectedNode, record.getValue("n"));

//    assertEquals(ResultSetImpl.class, results.get(5).getClass());
    resultSet = (ResultSet) results.get(5);

    assertNotNull(resultSet.getHeader());
    header = resultSet.getHeader();

    schemaNames = header.getSchemaNames();
    assertNotNull(schemaNames);
    assertEquals(1, schemaNames.size());
    assertEquals("label", schemaNames.get(0));

    assertEquals(1, resultSet.size());
    iterator = resultSet.iterator();
    assertTrue(iterator.hasNext());
    record = iterator.next();
    assertFalse(iterator.hasNext());
    assertEquals(Arrays.asList("label"), record.keys());
    assertEquals("Person", record.getValue("label"));
  }

  @Test
  public void testWaitReplicas() {
//    Pipeline pipeline = new Pipeline(c);
    Pipeline pipeline = (Pipeline) client.pipelined();
    pipeline.set("x", "1");
    pipeline.graphProfile("social", "CREATE (:Person {name:'a'})");
    pipeline.graphProfile("g", "CREATE (:Person {name:'a'})");
    pipeline.waitReplicas(0, 100L);
    List<Object> results = pipeline.syncAndReturnAll();
    assertEquals(Long.valueOf(0), results.get(3));
  }

  @Test
  @org.junit.Ignore
  public void testWaitAof() {
//    Pipeline pipeline = new Pipeline(c);
    Pipeline pipeline = (Pipeline) client.pipelined();
    pipeline.set("x", "1");
    pipeline.graphProfile("social", "CREATE (:Person {name:'a'})");
    pipeline.graphProfile("g", "CREATE (:Person {name:'a'})");
    pipeline.waitAOF(1L, 0L, 100L);
    List<Object> results = pipeline.syncAndReturnAll();
    assertEquals(0L, results.get(3));
  }
}
