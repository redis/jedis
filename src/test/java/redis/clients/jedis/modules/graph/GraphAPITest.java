package redis.clients.jedis.modules.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.*;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.graph.Header;
import redis.clients.jedis.graph.Record;
import redis.clients.jedis.graph.ResultSet;
import redis.clients.jedis.graph.Statistics;
import redis.clients.jedis.graph.entities.*;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;

@org.junit.Ignore
@RunWith(Parameterized.class)
public class GraphAPITest extends RedisModuleCommandsTestBase {

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }

  public GraphAPITest(RedisProtocol protocol) {
    super(protocol);
  }

  @After
  @Override
  public void tearDown() throws Exception {
    client.graphDelete("social");
    super.tearDown();
  }

    @Test
    public void testCreateNode() {
        // Create a node
        ResultSet resultSet = client.graphQuery("social", "CREATE ({name:'roi',age:32})");

        Statistics stats = resultSet.getStatistics();
        assertEquals(1, stats.nodesCreated());
        assertEquals(0, stats.nodesDeleted());
        assertEquals(0, stats.relationshipsCreated());
        assertEquals(0, stats.relationshipsDeleted());
        assertEquals(2, stats.propertiesSet());
        assertNotNull(stats.queryIntervalExecutionTime());

        assertEquals(0, resultSet.size());

        assertFalse(resultSet.iterator().hasNext());

        try {
            resultSet.iterator().next();
            fail();
        } catch (NoSuchElementException ignored) {
        }
    }

    @Test
    public void testCreateLabeledNode() {
        // Create a node with a label
        ResultSet resultSet = client.graphQuery("social", "CREATE (:human{name:'danny',age:12})");

        Statistics stats = resultSet.getStatistics();
//        assertEquals("1", stats.getStringValue(Label.NODES_CREATED));
        assertEquals(1, stats.nodesCreated());
//        assertEquals("2", stats.getStringValue(Label.PROPERTIES_SET));
        assertEquals(2, stats.propertiesSet());
//        assertNotNull(stats.getStringValue(Label.QUERY_INTERNAL_EXECUTION_TIME));
        assertNotNull(stats.queryIntervalExecutionTime());

        assertEquals(0, resultSet.size());
        assertFalse(resultSet.iterator().hasNext());
    }

    @Test
    public void testConnectNodes() {
        // Create both source and destination nodes
        assertNotNull(client.graphQuery("social", "CREATE (:person{name:'roi',age:32})"));
        assertNotNull(client.graphQuery("social", "CREATE (:person{name:'amit',age:30})"));

        // Connect source and destination nodes.
        ResultSet resultSet = client.graphQuery("social",
                "MATCH (a:person), (b:person) WHERE (a.name = 'roi' AND b.name='amit')  CREATE (a)-[:knows]->(b)");

        Statistics stats = resultSet.getStatistics();
//        assertNull(stats.getStringValue(Label.NODES_CREATED));
        assertEquals(0, stats.nodesCreated());
        assertEquals(1, stats.relationshipsCreated());
        assertEquals(0, stats.relationshipsDeleted());
//        assertNull(stats.getStringValue(Label.PROPERTIES_SET));
        assertEquals(0, stats.propertiesSet());
//        assertNotNull(stats.getStringValue(Label.QUERY_INTERNAL_EXECUTION_TIME));
        assertNotNull(stats.queryIntervalExecutionTime());

        assertEquals(0, resultSet.size());
        assertFalse(resultSet.iterator().hasNext());
    }

    @Test
    public void testDeleteNodes() {
        assertNotNull(client.graphQuery("social", "CREATE (:person{name:'roi',age:32})"));
        assertNotNull(client.graphQuery("social", "CREATE (:person{name:'amit',age:30})"));

        ResultSet deleteResult = client.graphQuery("social", "MATCH (a:person) WHERE (a.name = 'roi') DELETE a");

        Statistics delStats = deleteResult.getStatistics();
//        assertNull(delStats.getStringValue(Label.NODES_CREATED));
        assertEquals(0, delStats.nodesCreated());
        assertEquals(1, delStats.nodesDeleted());
//        assertNull(delStats.getStringValue(Label.RELATIONSHIPS_CREATED));
        assertEquals(0, delStats.relationshipsCreated());
//        assertNull(delStats.getStringValue(Label.RELATIONSHIPS_DELETED));
        assertEquals(0, delStats.relationshipsDeleted());
//        assertNull(delStats.getStringValue(Label.PROPERTIES_SET));
        assertEquals(0, delStats.propertiesSet());
//        assertNotNull(delStats.getStringValue(Label.QUERY_INTERNAL_EXECUTION_TIME));
        assertNotNull(delStats.queryIntervalExecutionTime());
        assertEquals(0, deleteResult.size());
        assertFalse(deleteResult.iterator().hasNext());

        assertNotNull(client.graphQuery("social", "CREATE (:person{name:'roi',age:32})"));
        assertNotNull(client.graphQuery("social",
                "MATCH (a:person), (b:person) WHERE (a.name = 'roi' AND b.name='amit')  CREATE (a)-[:knows]->(a)"));

        deleteResult = client.graphQuery("social", "MATCH (a:person) WHERE (a.name = 'roi') DELETE a");

//        assertNull(delStats.getStringValue(Label.NODES_CREATED));
        assertEquals(0, delStats.nodesCreated());
        assertEquals(1, delStats.nodesDeleted());
//        assertNull(delStats.getStringValue(Label.RELATIONSHIPS_CREATED));
        assertEquals(0, delStats.relationshipsCreated());
        // assertEquals(1, delStats.relationshipsDeleted());
        assertEquals(0, delStats.relationshipsDeleted());
//        assertNull(delStats.getStringValue(Label.PROPERTIES_SET));
        assertEquals(0, delStats.propertiesSet());
//        assertNotNull(delStats.getStringValue(Label.QUERY_INTERNAL_EXECUTION_TIME));
        assertNotNull(delStats.queryIntervalExecutionTime());
        assertEquals(0, deleteResult.size());
        assertFalse(deleteResult.iterator().hasNext());
    }

    @Test
    public void testDeleteRelationship() {
        assertNotNull(client.graphQuery("social", "CREATE (:person{name:'roi',age:32})"));
        assertNotNull(client.graphQuery("social", "CREATE (:person{name:'amit',age:30})"));
        assertNotNull(client.graphQuery("social",
                "MATCH (a:person), (b:person) WHERE (a.name = 'roi' AND b.name='amit')  CREATE (a)-[:knows]->(a)"));

        ResultSet deleteResult = client.graphQuery("social",
                "MATCH (a:person)-[e]->() WHERE (a.name = 'roi') DELETE e");

        Statistics delStats = deleteResult.getStatistics();
//        assertNull(delStats.getStringValue(Label.NODES_CREATED));
        assertEquals(0, delStats.nodesCreated());
//        assertNull(delStats.getStringValue(Label.NODES_DELETED));
        assertEquals(0, delStats.nodesDeleted());
//        assertNull(delStats.getStringValue(Label.RELATIONSHIPS_CREATED));
        assertEquals(0, delStats.relationshipsCreated());
        assertEquals(1, delStats.relationshipsDeleted());
//        assertNull(delStats.getStringValue(Label.PROPERTIES_SET));
        assertEquals(0, delStats.propertiesSet());
//        assertNotNull(delStats.getStringValue(Label.QUERY_INTERNAL_EXECUTION_TIME));
        assertNotNull(delStats.queryIntervalExecutionTime());
        assertEquals(0, deleteResult.size());
        assertFalse(deleteResult.iterator().hasNext());
    }

    @Test
    public void testIndex() {
        // Create both source and destination nodes
        assertNotNull(client.graphQuery("social", "CREATE (:person{name:'roi',age:32})"));

        ResultSet createIndexResult = client.graphQuery("social", "CREATE INDEX ON :person(age)");
        assertFalse(createIndexResult.iterator().hasNext());
        assertEquals(1, createIndexResult.getStatistics().indicesCreated());

        // since RediSearch as index, those action are allowed
        ResultSet createNonExistingIndexResult = client.graphQuery("social", "CREATE INDEX ON :person(age1)");
        assertFalse(createNonExistingIndexResult.iterator().hasNext());
        assertEquals(1, createNonExistingIndexResult.getStatistics().indicesCreated());

        ResultSet createExistingIndexResult = client.graphQuery("social", "CREATE INDEX ON :person(age)");
        assertFalse(createExistingIndexResult.iterator().hasNext());
        assertEquals(0, createExistingIndexResult.getStatistics().indicesCreated());

        ResultSet deleteExistingIndexResult = client.graphQuery("social", "DROP INDEX ON :person(age)");
        assertFalse(deleteExistingIndexResult.iterator().hasNext());
        assertEquals(1, deleteExistingIndexResult.getStatistics().indicesDeleted());
    }

    @Test
    public void testHeader() {

        assertNotNull(client.graphQuery("social", "CREATE (:person{name:'roi',age:32})"));
        assertNotNull(client.graphQuery("social", "CREATE (:person{name:'amit',age:30})"));
        assertNotNull(client.graphQuery("social",
                "MATCH (a:person), (b:person) WHERE (a.name = 'roi' AND b.name='amit')  CREATE (a)-[:knows]->(a)"));

        ResultSet queryResult = client.graphQuery("social", "MATCH (a:person)-[r:knows]->(b:person) RETURN a,r, a.age");

        Header header = queryResult.getHeader();
        assertNotNull(header);
        assertEquals("HeaderImpl{"
//                + "schemaTypes=[COLUMN_SCALAR, COLUMN_SCALAR, COLUMN_SCALAR], "
                + "schemaTypes=[SCALAR, SCALAR, SCALAR], "
                + "schemaNames=[a, r, a.age]}", header.toString());
        // Assert.assertEquals(-1901778507, header.hashCode());

        List<String> schemaNames = header.getSchemaNames();

        assertNotNull(schemaNames);
        assertEquals(3, schemaNames.size());
        assertEquals("a", schemaNames.get(0));
        assertEquals("r", schemaNames.get(1));
        assertEquals("a.age", schemaNames.get(2));
    }

    @Test
    public void testRecord() {
        String name = "roi";
        int age = 32;
        double doubleValue = 3.14;
        boolean boolValue = true;

        String place = "TLV";
        int since = 2000;

        Property<String> nameProperty = new Property<>("name", name);
        Property<Integer> ageProperty = new Property<>("age", age);
        Property<Double> doubleProperty = new Property<>("doubleValue", doubleValue);
        Property<Boolean> trueBooleanProperty = new Property<>("boolValue", true);
        Property<Boolean> falseBooleanProperty = new Property<>("boolValue", false);

        Property<String> placeProperty = new Property<>("place", place);
        Property<Integer> sinceProperty = new Property<>("since", since);

        Node expectedNode = new Node();
        expectedNode.setId(0);
        expectedNode.addLabel("person");
        expectedNode.addProperty(nameProperty);
        expectedNode.addProperty(ageProperty);
        expectedNode.addProperty(doubleProperty);
        expectedNode.addProperty(trueBooleanProperty);
        assertEquals(
                "Node{labels=[person], id=0, "
                        + "propertyMap={name=Property{name='name', value=roi}, "
                        + "boolValue=Property{name='boolValue', value=true}, "
                        + "doubleValue=Property{name='doubleValue', value=3.14}, "
                        + "age=Property{name='age', value=32}}}",
                expectedNode.toString());

        Edge expectedEdge = new Edge();
        expectedEdge.setId(0);
        expectedEdge.setSource(0);
        expectedEdge.setDestination(1);
        expectedEdge.setRelationshipType("knows");
        expectedEdge.addProperty(placeProperty);
        expectedEdge.addProperty(sinceProperty);
        expectedEdge.addProperty(doubleProperty);
        expectedEdge.addProperty(falseBooleanProperty);
        assertEquals("Edge{relationshipType='knows', source=0, destination=1, id=0, "
                + "propertyMap={boolValue=Property{name='boolValue', value=false}, "
                + "place=Property{name='place', value=TLV}, "
                + "doubleValue=Property{name='doubleValue', value=3.14}, "
                + "since=Property{name='since', value=2000}}}", expectedEdge.toString());

        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        params.put("age", age);
        params.put("boolValue", boolValue);
        params.put("doubleValue", doubleValue);

        assertNotNull(client.graphQuery("social",
                "CREATE (:person{name:$name,age:$age, doubleValue:$doubleValue, boolValue:$boolValue})", params));
        assertNotNull(client.graphQuery("social", "CREATE (:person{name:'amit',age:30})"));
        assertNotNull(
                client.graphQuery("social", "MATCH (a:person), (b:person) WHERE (a.name = 'roi' AND b.name='amit')  " +
                        "CREATE (a)-[:knows{place:'TLV', since:2000,doubleValue:3.14, boolValue:false}]->(b)"));

        ResultSet resultSet = client.graphQuery("social", "MATCH (a:person)-[r:knows]->(b:person) RETURN a,r, " +
                "a.name, a.age, a.doubleValue, a.boolValue, " +
                "r.place, r.since, r.doubleValue, r.boolValue");
        assertNotNull(resultSet);

        Statistics stats = resultSet.getStatistics();
        assertEquals(0, stats.nodesCreated());
        assertEquals(0, stats.nodesDeleted());
        assertEquals(0, stats.labelsAdded());
        assertEquals(0, stats.propertiesSet());
        assertEquals(0, stats.relationshipsCreated());
        assertEquals(0, stats.relationshipsDeleted());
        assertNotNull(stats.queryIntervalExecutionTime());
        assertFalse(stats.queryIntervalExecutionTime().isEmpty());

        assertEquals(1, resultSet.size());
        Iterator<Record> iterator = resultSet.iterator();
        assertTrue(iterator.hasNext());
        Record record = iterator.next();
        assertFalse(iterator.hasNext());

        Node node = record.getValue(0);
        assertNotNull(node);

        assertEquals(expectedNode, node);

        node = record.getValue("a");
        assertEquals(expectedNode, node);

        Edge edge = record.getValue(1);
        assertNotNull(edge);
        assertEquals(expectedEdge, edge);

        edge = record.getValue("r");
        assertEquals(expectedEdge, edge);

        assertEquals(Arrays.asList("a", "r", "a.name", "a.age", "a.doubleValue", "a.boolValue",
                "r.place", "r.since", "r.doubleValue", "r.boolValue"), record.keys());

        assertEquals(Arrays.asList(expectedNode, expectedEdge,
                name, (long) age, doubleValue, true,
                place, (long) since, doubleValue, false),
                record.values());

        Node a = record.getValue("a");
        for (String propertyName : expectedNode.getEntityPropertyNames()) {
            assertEquals(expectedNode.getProperty(propertyName), a.getProperty(propertyName));
        }

        assertEquals("roi", record.getString(2));
        assertEquals("32", record.getString(3));
        assertEquals(32L, ((Long) record.getValue(3)).longValue());
        assertEquals(32L, ((Long) record.getValue("a.age")).longValue());
        assertEquals("roi", record.getString("a.name"));
        assertEquals("32", record.getString("a.age"));

    }

    @Test
    public void testAdditionToProcedures() {

        assertNotNull(client.graphQuery("social", "CREATE (:person{name:'roi',age:32})"));
        assertNotNull(client.graphQuery("social", "CREATE (:person{name:'amit',age:30})"));
        assertNotNull(client.graphQuery("social",
                "MATCH (a:person), (b:person) WHERE (a.name = 'roi' AND b.name='amit')  CREATE (a)-[:knows]->(b)"));

        // expected objects init
        Property<String> nameProperty = new Property<>("name", "roi");
        Property<Integer> ageProperty = new Property<>("age", 32);
        Property<String> lastNameProperty = new Property<>("lastName", "a");

        Node expectedNode = new Node();
        expectedNode.setId(0);
        expectedNode.addLabel("person");
        expectedNode.addProperty(nameProperty);
        expectedNode.addProperty(ageProperty);

        Edge expectedEdge = new Edge();
        expectedEdge.setId(0);
        expectedEdge.setSource(0);
        expectedEdge.setDestination(1);
        expectedEdge.setRelationshipType("knows");

        ResultSet resultSet = client.graphQuery("social", "MATCH (a:person)-[r:knows]->(b:person) RETURN a,r");
        assertNotNull(resultSet.getHeader());
        Header header = resultSet.getHeader();
        List<String> schemaNames = header.getSchemaNames();
        assertNotNull(schemaNames);
        assertEquals(2, schemaNames.size());
        assertEquals("a", schemaNames.get(0));
        assertEquals("r", schemaNames.get(1));
        assertEquals(1, resultSet.size());
        Iterator<Record> iterator = resultSet.iterator();
        assertTrue(iterator.hasNext());
        Record record = iterator.next();
        assertFalse(iterator.hasNext());
        assertEquals(Arrays.asList("a", "r"), record.keys());
        assertEquals(Arrays.asList(expectedNode, expectedEdge), record.values());

        // test for local cache updates

        expectedNode.removeProperty("name");
        expectedNode.removeProperty("age");
        expectedNode.addProperty(lastNameProperty);
        expectedNode.removeLabel("person");
        expectedNode.addLabel("worker");
        expectedNode.setId(2);
        expectedEdge.setRelationshipType("worksWith");
        expectedEdge.setSource(2);
        expectedEdge.setDestination(3);
        expectedEdge.setId(1);
        assertNotNull(client.graphQuery("social", "CREATE (:worker{lastName:'a'})"));
        assertNotNull(client.graphQuery("social", "CREATE (:worker{lastName:'b'})"));
        assertNotNull(client.graphQuery("social",
                "MATCH (a:worker), (b:worker) WHERE (a.lastName = 'a' AND b.lastName='b')  CREATE (a)-[:worksWith]->(b)"));
        resultSet = client.graphQuery("social", "MATCH (a:worker)-[r:worksWith]->(b:worker) RETURN a,r");
        assertNotNull(resultSet.getHeader());
        header = resultSet.getHeader();
        schemaNames = header.getSchemaNames();
        assertNotNull(schemaNames);
        assertEquals(2, schemaNames.size());
        assertEquals("a", schemaNames.get(0));
        assertEquals("r", schemaNames.get(1));
        assertEquals(1, resultSet.size());
        iterator = resultSet.iterator();
        assertTrue(iterator.hasNext());
        record = iterator.next();
        assertFalse(iterator.hasNext());
        assertEquals(Arrays.asList("a", "r"), record.keys());
        assertEquals(Arrays.asList(expectedNode, expectedEdge), record.values());
    }

    @Test
    public void testEscapedQuery() {
        Map<String, Object> params1 = new HashMap<String, Object>();
        params1.put("s1", "S\"'");
        params1.put("s2", "S'\"");
        assertNotNull(client.graphQuery("social", "CREATE (:escaped{s1:$s1,s2:$s2})", params1));

        Map<String, Object> params2 = new HashMap<String, Object>();
        params2.put("s1", "S\"'");
        params2.put("s2", "S'\"");
        assertNotNull(client.graphQuery("social", "MATCH (n) where n.s1=$s1 and n.s2=$s2 RETURN n", params2));

        assertNotNull(client.graphQuery("social", "MATCH (n) where n.s1='S\"' RETURN n"));
    }

    @Test
    public void testArraySupport() {

        Node expectedANode = new Node();
        expectedANode.setId(0);
        expectedANode.addLabel("person");
        Property<String> aNameProperty = new Property<>("name", "a");
        Property<Integer> aAgeProperty = new Property<>("age", 32);
        Property<List<Long>> aListProperty = new Property<>("array", Arrays.asList(0L, 1L, 2L));
        expectedANode.addProperty(aNameProperty);
        expectedANode.addProperty(aAgeProperty);
        expectedANode.addProperty(aListProperty);

        Node expectedBNode = new Node();
        expectedBNode.setId(1);
        expectedBNode.addLabel("person");
        Property<String> bNameProperty = new Property<>("name", "b");
        Property<Integer> bAgeProperty = new Property<>("age", 30);
        Property<List<Long>> bListProperty = new Property<>("array", Arrays.asList(3L, 4L, 5L));
        expectedBNode.addProperty(bNameProperty);
        expectedBNode.addProperty(bAgeProperty);
        expectedBNode.addProperty(bListProperty);

        assertNotNull(client.graphQuery("social", "CREATE (:person{name:'a',age:32,array:[0,1,2]})"));
        assertNotNull(client.graphQuery("social", "CREATE (:person{name:'b',age:30,array:[3,4,5]})"));

        // test array

        ResultSet resultSet = client.graphQuery("social", "WITH [0,1,2] as x return x");

        // check header
        assertNotNull(resultSet.getHeader());
        Header header = resultSet.getHeader();

        List<String> schemaNames = header.getSchemaNames();
        assertNotNull(schemaNames);
        assertEquals(1, schemaNames.size());
        assertEquals("x", schemaNames.get(0));

        // check record
        assertEquals(1, resultSet.size());
        Iterator<Record> iterator = resultSet.iterator();
        assertTrue(iterator.hasNext());
        Record record = iterator.next();
        assertFalse(iterator.hasNext());
        assertEquals(Arrays.asList("x"), record.keys());

        List<Long> x = record.getValue("x");
        assertEquals(Arrays.asList(0L, 1L, 2L), x);

        // test collect
        resultSet = client.graphQuery("social", "MATCH(n) return collect(n) as x");

        assertNotNull(resultSet.getHeader());
        header = resultSet.getHeader();

        schemaNames = header.getSchemaNames();
        assertNotNull(schemaNames);
        assertEquals(1, schemaNames.size());
        assertEquals("x", schemaNames.get(0));

        // check record
        assertEquals(1, resultSet.size());
        iterator = resultSet.iterator();
        assertTrue(iterator.hasNext());
        record = iterator.next();
        assertFalse(iterator.hasNext());
        assertEquals(Arrays.asList("x"), record.keys());
        x = record.getValue("x");
        assertEquals(Arrays.asList(expectedANode, expectedBNode), x);

        // test unwind
        resultSet = client.graphQuery("social", "unwind([0,1,2]) as x return x");

        assertNotNull(resultSet.getHeader());
        header = resultSet.getHeader();

        schemaNames = header.getSchemaNames();
        assertNotNull(schemaNames);
        assertEquals(1, schemaNames.size());
        assertEquals("x", schemaNames.get(0));

        // check record
        assertEquals(3, resultSet.size());
        iterator = resultSet.iterator();
        for (long i = 0; i < 3; i++) {
            assertTrue(iterator.hasNext());
            record = iterator.next();
            assertEquals(Arrays.asList("x"), record.keys());
            assertEquals(i, (long) record.getValue("x"));
        }
    }

    @Test
    public void testPath() {
        List<Node> nodes = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            Node node = new Node();
            node.setId(i);
            node.addLabel("L1");
            nodes.add(node);
        }

        List<Edge> edges = new ArrayList<>(2);
        for (int i = 0; i < 2; i++) {
            Edge edge = new Edge();
            edge.setId(i);
            edge.setRelationshipType("R1");
            edge.setSource(i);
            edge.setDestination(i + 1);
            edges.add(edge);
        }

        Set<Path> expectedPaths = new HashSet<>();

        Path path01 = new PathBuilder(2).append(nodes.get(0)).append(edges.get(0)).append(nodes.get(1)).build();
        Path path12 = new PathBuilder(2).append(nodes.get(1)).append(edges.get(1)).append(nodes.get(2)).build();
        Path path02 = new PathBuilder(3).append(nodes.get(0)).append(edges.get(0)).append(nodes.get(1))
                .append(edges.get(1)).append(nodes.get(2)).build();

        expectedPaths.add(path01);
        expectedPaths.add(path12);
        expectedPaths.add(path02);

        client.graphQuery("social", "CREATE (:L1)-[:R1]->(:L1)-[:R1]->(:L1)");

        ResultSet resultSet = client.graphQuery("social", "MATCH p = (:L1)-[:R1*]->(:L1) RETURN p");

        assertEquals(expectedPaths.size(), resultSet.size());
        Iterator<Record> iterator = resultSet.iterator();
        for (int i = 0; i < resultSet.size(); i++) {
            Path p = iterator.next().getValue("p");
            assertTrue(expectedPaths.contains(p));
            expectedPaths.remove(p);
        }

    }

    @Test
    public void testNullGraphEntities() {
        // Create two nodes connected by a single outgoing edge.
        assertNotNull(client.graphQuery("social", "CREATE (:L)-[:E]->(:L2)"));
        // Test a query that produces 1 record with 3 null values.
        ResultSet resultSet = client.graphQuery("social", "OPTIONAL MATCH (a:NONEXISTENT)-[e]->(b) RETURN a, e, b");
        assertEquals(1, resultSet.size());
        Iterator<Record> iterator = resultSet.iterator();
        assertTrue(iterator.hasNext());
        Record record = iterator.next();
        assertFalse(iterator.hasNext());
        assertEquals(Arrays.asList(null, null, null), record.values());

        // Test a query that produces 2 records, with 2 null values in the second.
        resultSet = client.graphQuery("social", "MATCH (a) OPTIONAL MATCH (a)-[e]->(b) RETURN a, e, b ORDER BY ID(a)");
        assertEquals(2, resultSet.size());
        iterator = resultSet.iterator();
        record = iterator.next();
        assertEquals(3, record.size());

        assertNotNull(record.getValue(0));
        assertNotNull(record.getValue(1));
        assertNotNull(record.getValue(2));

        record = iterator.next();
        assertEquals(3, record.size());

        assertNotNull(record.getValue(0));
        assertNull(record.getValue(1));
        assertNull(record.getValue(2));

        // Test a query that produces 2 records, the first containing a path and the
        // second containing a null value.
        resultSet = client.graphQuery("social", "MATCH (a) OPTIONAL MATCH p = (a)-[e]->(b) RETURN p");
        assertEquals(2, resultSet.size());
        iterator = resultSet.iterator();

        record = iterator.next();
        assertEquals(1, record.size());
        assertNotNull(record.getValue(0));

        record = iterator.next();
        assertEquals(1, record.size());
        assertNull(record.getValue(0));
    }

    @Test
    public void test64bitnumber() {
        long value = 1L << 40;
        Map<String, Object> params = new HashMap<>();
        params.put("val", value);
        ResultSet resultSet = client.graphQuery("social", "CREATE (n {val:$val}) RETURN n.val", params);
        assertEquals(1, resultSet.size());
        Record r = resultSet.iterator().next();
        assertEquals(Long.valueOf(value), r.getValue(0));
    }

    @Test
    public void testCachedExecution() {
        client.graphQuery("social", "CREATE (:N {val:1}), (:N {val:2})");

        // First time should not be loaded from execution cache
        Map<String, Object> params = new HashMap<>();
        params.put("val", 1L);
        ResultSet resultSet = client.graphQuery("social", "MATCH (n:N {val:$val}) RETURN n.val", params);
        assertEquals(1, resultSet.size());
        Record r = resultSet.iterator().next();
        assertEquals(params.get("val"), r.getValue(0));
        assertFalse(resultSet.getStatistics().cachedExecution());

        // Run in loop many times to make sure the query will be loaded
        // from cache at least once
        for (int i = 0; i < 64; i++) {
            resultSet = client.graphQuery("social", "MATCH (n:N {val:$val}) RETURN n.val", params);
        }
        assertEquals(1, resultSet.size());
        r = resultSet.iterator().next();
        assertEquals(params.get("val"), r.getValue(0));
        assertTrue(resultSet.getStatistics().cachedExecution());
    }

    @Test
    public void testMapDataType() {
        Map<String, Object> expected = new HashMap<>();
        expected.put("a", (long) 1);
        expected.put("b", "str");
        expected.put("c", null);
        List<Object> d = new ArrayList<>();
        d.add((long) 1);
        d.add((long) 2);
        d.add((long) 3);
        expected.put("d", d);
        expected.put("e", true);
        Map<String, Object> f = new HashMap<>();
        f.put("x", (long) 1);
        f.put("y", (long) 2);
        expected.put("f", f);
        ResultSet res = client.graphQuery("social", "RETURN {a:1, b:'str', c:NULL, d:[1,2,3], e:True, f:{x:1, y:2}}");
        assertEquals(1, res.size());
        Record r = res.iterator().next();
        Map<String, Object> actual = r.getValue(0);
        assertEquals(expected, actual);
    }

    @Test
    public void testGeoPointLatLon() {
        ResultSet rs = client.graphQuery("social", "CREATE (:restaurant"
                + " {location: point({latitude:30.27822306, longitude:-97.75134723})})");
        assertEquals(1, rs.getStatistics().nodesCreated());
        assertEquals(1, rs.getStatistics().propertiesSet());

        assertTestGeoPoint();
    }

    @Test
    public void testGeoPointLonLat() {
        ResultSet rs = client.graphQuery("social", "CREATE (:restaurant"
                + " {location: point({longitude:-97.75134723, latitude:30.27822306})})");
        assertEquals(1, rs.getStatistics().nodesCreated());
        assertEquals(1, rs.getStatistics().propertiesSet());

        assertTestGeoPoint();
    }

    private void assertTestGeoPoint() {
        ResultSet results = client.graphQuery("social", "MATCH (restaurant) RETURN restaurant");
        assertEquals(1, results.size());
        Record record = results.iterator().next();
        assertEquals(1, record.size());
        assertEquals(Collections.singletonList("restaurant"), record.keys());
        Node node = record.getValue(0);
        Property<?> property = node.getProperty("location");
        assertEquals(new Point(30.27822306, -97.75134723), property.getValue());
    }

    @Test
    public void timeoutArgument() {
        ResultSet rs = client.graphQuery("social", "UNWIND range(0,100) AS x WITH x AS x WHERE x = 100 RETURN x", 1L);
        assertEquals(1, rs.size());
        Record r = rs.iterator().next();
        assertEquals(Long.valueOf(100), r.getValue(0));
    }

    @Test
    public void testCachedExecutionReadOnly() {
        client.graphQuery("social", "CREATE (:N {val:1}), (:N {val:2})");

        // First time should not be loaded from execution cache
        Map<String, Object> params = new HashMap<>();
        params.put("val", 1L);
        ResultSet resultSet =   client.graphReadonlyQuery("social", "MATCH (n:N {val:$val}) RETURN n.val", params);
        assertEquals(1, resultSet.size());
        Record r = resultSet.iterator().next();
        assertEquals(params.get("val"), r.getValue(0));
        assertFalse(resultSet.getStatistics().cachedExecution());

        // Run in loop many times to make sure the query will be loaded
        // from cache at least once
        for (int i = 0; i < 64; i++) {
            resultSet =   client.graphReadonlyQuery("social", "MATCH (n:N {val:$val}) RETURN n.val", params);
        }
        assertEquals(1, resultSet.size());
        r = resultSet.iterator().next();
        assertEquals(params.get("val"), r.getValue(0));
        assertTrue(resultSet.getStatistics().cachedExecution());
    }

    @Test
    public void testSimpleReadOnly() {
        client.graphQuery("social", "CREATE (:person{name:'filipe',age:30})");
        ResultSet rsRo = client.graphReadonlyQuery("social", "MATCH (a:person) WHERE (a.name = 'filipe') RETURN a.age");
        assertEquals(1, rsRo.size());
        Record r = rsRo.iterator().next();
        assertEquals(Long.valueOf(30), r.getValue(0));
    }

  @Test
  public void profile() {
    assertNotNull(client.graphQuery("social", "CREATE (:person{name:'roi',age:32})"));
    assertNotNull(client.graphQuery("social", "CREATE (:person{name:'amit',age:30})"));

    List<String> profile = client.graphProfile("social",
        "MATCH (a:person), (b:person) WHERE (a.name = 'roi' AND b.name='amit')  CREATE (a)-[:knows]->(b)");
    assertFalse(profile.isEmpty());
    profile.forEach(Assert::assertNotNull);
  }

  @Test
  public void explain() {
    assertNotNull(client.graphProfile("social", "CREATE (:person{name:'roi',age:32})"));
    assertNotNull(client.graphProfile("social", "CREATE (:person{name:'amit',age:30})"));

    List<String> explain = client.graphExplain("social",
        "MATCH (a:person), (b:person) WHERE (a.name = 'roi' AND b.name='amit')  CREATE (a)-[:knows]->(b)");
    assertFalse(explain.isEmpty());
    explain.forEach(Assert::assertNotNull);
  }

  @Test
  public void slowlog() {
    assertNotNull(client.graphProfile("social", "CREATE (:person{name:'roi',age:32})"));
    assertNotNull(client.graphProfile("social", "CREATE (:person{name:'amit',age:30})"));

    List<List<Object>> slowlogs = client.graphSlowlog("social");
    assertEquals(2, slowlogs.size());
    slowlogs.forEach(sl -> assertFalse(sl.isEmpty()));
    slowlogs.forEach(sl -> sl.forEach(Assert::assertNotNull));
  }

  @Test
  public void list() {
    assertEquals(Collections.emptyList(), client.graphList());

    client.graphQuery("social", "CREATE (:person{name:'filipe',age:30})");

    assertEquals(Collections.singletonList("social"), client.graphList());
  }

  @Test
  public void config() {
    client.graphQuery("social", "CREATE (:person{name:'filipe',age:30})");

    final String name = "RESULTSET_SIZE";
    final Object existingValue = client.graphConfigGet(name).get(name);

    assertEquals("OK", client.graphConfigSet(name, 250L));
    assertEquals(Collections.singletonMap(name, 250L), client.graphConfigGet(name));

    client.graphConfigSet(name, existingValue != null ? existingValue : -1);
  }
}
