package redis.clients.jedis.modules.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.AbstractTransaction;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.graph.Header;
import redis.clients.jedis.graph.Record;
import redis.clients.jedis.graph.ResultSet;
import redis.clients.jedis.graph.entities.Node;
import redis.clients.jedis.graph.entities.Property;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;

@org.junit.Ignore
@RunWith(Parameterized.class)
public class GraphTransactionTest extends RedisModuleCommandsTestBase {

//  private Connection c;

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }

  public GraphTransactionTest(RedisProtocol protocol) {
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
  public void testMultiExec() {
//    Transaction transaction = new Transaction(c);
    AbstractTransaction transaction = client.multi();

    transaction.set("x", "1");
    transaction.graphQuery("social", "CREATE (:Person {name:'a'})");
    transaction.graphQuery("g", "CREATE (:Person {name:'a'})");
    transaction.incr("x");
    transaction.get("x");
    transaction.graphQuery("social", "MATCH (n:Person) RETURN n");
    transaction.graphDelete("g");
//    transaction.callProcedure("social", "db.labels");
    transaction.graphQuery("social", "CALL db.labels()");
    List<Object> results = transaction.exec();

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
//
//  @Test
//  public void testWriteTransactionWatch() {
//
//    RedisGraphContext c1 = api.getContext();
//    RedisGraphContext c2 = api.getContext();
//
//    c1.watch("social");
//    RedisGraphTransaction t1 = c1.multi();
//
//    t1.graphQuery("social", "CREATE (:Person {name:'a'})");
//    c2.graphQuery("social", "CREATE (:Person {name:'b'})");
//    List<Object> returnValue = t1.exec();
//    assertNull(returnValue);
//    c1.close();
//    c2.close();
//  }
//
//  @Test
//  public void testReadTransactionWatch() {
//
//    RedisGraphContext c1 = api.getContext();
//    RedisGraphContext c2 = api.getContext();
//    assertNotEquals(c1.getConnectionContext(), c2.getConnectionContext());
//    c1.graphQuery("social", "CREATE (:Person {name:'a'})");
//    c1.watch("social");
//    RedisGraphTransaction t1 = c1.multi();
//
//    Map<String, Object> params = new HashMap<>();
//    params.put("name", 'b');
//    t1.graphQuery("social", "CREATE (:Person {name:$name})", params);
//    c2.graphQuery("social", "MATCH (n) return n");
//    List<Object> returnValue = t1.exec();
//
//    assertNotNull(returnValue);
//    c1.close();
//    c2.close();
//  }

  @Test
  public void testMultiExecWithReadOnlyQueries() {
//    Transaction transaction = new Transaction(c);
    AbstractTransaction transaction = client.multi();

    transaction.set("x", "1");
    transaction.graphQuery("social", "CREATE (:Person {name:'a'})");
    transaction.graphQuery("g", "CREATE (:Person {name:'a'})");
    transaction.graphReadonlyQuery("social", "MATCH (n:Person) RETURN n");
    transaction.graphDelete("g");
//    transaction.callProcedure("social", "db.labels");
    transaction.graphQuery("social", "CALL db.labels()");
    List<Object> results = transaction.exec();

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
}
