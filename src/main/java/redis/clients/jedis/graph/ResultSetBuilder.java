package redis.clients.jedis.graph;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.graph.entities.*;
import redis.clients.jedis.util.SafeEncoder;

class ResultSetBuilder extends Builder<ResultSet> {

  private final GraphCache graphCache;

  ResultSetBuilder(GraphCache cache) {
    this.graphCache = cache;
  }

  @Override
  public ResultSet build(Object data) {
    List<Object> rawResponse = (List<Object>) data;
    // If a run-time error occurred, the last member of the rawResponse will be a
    // JedisDataException.
    if (rawResponse.get(rawResponse.size() - 1) instanceof JedisDataException) {
      throw (JedisDataException) rawResponse.get(rawResponse.size() - 1);
    }
//
//    HeaderImpl header = parseHeader(rawResponse.get(0));
//    List<Record> records = parseRecords(header, rawResponse.get(1));
//    StatisticsImpl statistics = parseStatistics(rawResponse.get(2));
//    return new ResultSetImpl(header, records, statistics);

    final Object headerObject;
    final Object recordsObject;
    final Object statisticsObject;

    if (rawResponse.size() == 1) {
      headerObject = emptyList();
      recordsObject = emptyList();
      statisticsObject = rawResponse.get(0);
    } else if (rawResponse.size() == 3) {
      headerObject = rawResponse.get(0);
      recordsObject = rawResponse.get(1);
      statisticsObject = rawResponse.get(2);
    } else {
      throw new JedisException("Unrecognized graph response format.");
    }

    HeaderImpl header = parseHeader(headerObject);
    List<Record> records = parseRecords(header, recordsObject);
    StatisticsImpl statistics = parseStatistics(statisticsObject);
    return new ResultSetImpl(header, records, statistics);
  }

  private class ResultSetImpl implements ResultSet {

    private final Header header;
    private final List<Record> results;
    private final Statistics statistics;

    private ResultSetImpl(Header header, List<Record> results, Statistics statistics) {
      this.header = header;
      this.results = results;
      this.statistics = statistics;
    }

    @Override
    public Header getHeader() {
      return header;
    }

    @Override
    public Statistics getStatistics() {
      return statistics;
    }

    @Override
    public int size() {
      return results.size();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ResultSetImpl)) {
        return false;
      }
      ResultSetImpl resultSet = (ResultSetImpl) o;
      return Objects.equals(getHeader(), resultSet.getHeader())
          && Objects.equals(getStatistics(), resultSet.getStatistics())
          && Objects.equals(results, resultSet.results);
    }

    @Override
    public int hashCode() {
      return Objects.hash(getHeader(), getStatistics(), results);
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("ResultSetImpl{");
      sb.append("header=").append(header);
      sb.append(", statistics=").append(statistics);
      sb.append(", results=").append(results);
      sb.append('}');
      return sb.toString();
    }

    @Override
    public Iterator<Record> iterator() {
      return results.iterator();
    }
  }

  @SuppressWarnings("unchecked")
  private List<Record> parseRecords(Header header, Object data) {
    List<List<Object>> rawResultSet = (List<List<Object>>) data;

    if (rawResultSet == null || rawResultSet.isEmpty()) {
      return new ArrayList<>(0);
    }

    List<Record> results = new ArrayList<>(rawResultSet.size());
    // go over each raw result
    for (List<Object> row : rawResultSet) {

      List<Object> parsedRow = new ArrayList<>(row.size());
      // go over each object in the result
      for (int i = 0; i < row.size(); i++) {
        // get raw representation of the object
        List<Object> obj = (List<Object>) row.get(i);
        // get object type
        ResultSet.ColumnType objType = header.getSchemaTypes().get(i);
        // deserialize according to type and
        switch (objType) {
          case NODE:
            parsedRow.add(deserializeNode(obj));
            break;
          case RELATION:
            parsedRow.add(deserializeEdge(obj));
            break;
          case SCALAR:
            parsedRow.add(deserializeScalar(obj));
            break;
          default:
            parsedRow.add(null);
            break;
        }
      }

      // create new record from deserialized objects
      Record record = new RecordImpl(header.getSchemaNames(), parsedRow);
      results.add(record);
    }

    return results;
  }

  /**
   * @param rawNodeData - raw node object in the form of list of object rawNodeData.get(0) - id
   * (long) rawNodeData.get(1) - a list y which contains the labels of this node. Each entry is a
   * label id from the type of long rawNodeData.get(2) - a list which contains the properties of the
   * node.
   * @return Node object
   */
  @SuppressWarnings("unchecked")
  private Node deserializeNode(List<Object> rawNodeData) {

    List<Long> labelsIndices = (List<Long>) rawNodeData.get(1);
    List<List<Object>> rawProperties = (List<List<Object>>) rawNodeData.get(2);

    Node node = new Node(labelsIndices.size(), rawProperties.size());
    deserializeGraphEntityId(node, (Long) rawNodeData.get(0));

    for (Long labelIndex : labelsIndices) {
      String label = graphCache.getLabel(labelIndex.intValue());
      node.addLabel(label);
    }

    deserializeGraphEntityProperties(node, rawProperties);

    return node;
  }

  /**
   * @param graphEntity graph entity
   * @param id entity id to be set to the graph entity
   */
  private void deserializeGraphEntityId(GraphEntity graphEntity, long id) {
    graphEntity.setId(id);
  }

  /**
   * @param rawEdgeData - a list of objects rawEdgeData[0] - edge id rawEdgeData[1] - edge
   * relationship type rawEdgeData[2] - edge source rawEdgeData[3] - edge destination rawEdgeData[4]
   * - edge properties
   * @return Edge object
   */
  @SuppressWarnings("unchecked")
  private Edge deserializeEdge(List<Object> rawEdgeData) {

    List<List<Object>> rawProperties = (List<List<Object>>) rawEdgeData.get(4);

    Edge edge = new Edge(rawProperties.size());
    deserializeGraphEntityId(edge, (Long) rawEdgeData.get(0));

    String relationshipType = graphCache.getRelationshipType(((Long) rawEdgeData.get(1)).intValue());
    edge.setRelationshipType(relationshipType);

    edge.setSource((long) rawEdgeData.get(2));
    edge.setDestination((long) rawEdgeData.get(3));

    deserializeGraphEntityProperties(edge, rawProperties);

    return edge;
  }

  /**
   * @param entity graph entity for adding the properties to
   * @param rawProperties raw representation of a list of graph entity properties. Each entry is a
   * list (rawProperty) is a raw representation of property, as follows: rawProperty.get(0) -
   * property key rawProperty.get(1) - property type rawProperty.get(2) - property value
   */
  private void deserializeGraphEntityProperties(GraphEntity entity, List<List<Object>> rawProperties) {

    for (List<Object> rawProperty : rawProperties) {
      String name = graphCache.getPropertyName(((Long) rawProperty.get(0)).intValue());

      // trimmed for getting to value using deserializeScalar
      List<Object> propertyScalar = rawProperty.subList(1, rawProperty.size());

      entity.addProperty(name, deserializeScalar(propertyScalar));
    }
  }

  /**
   * @param rawScalarData - a list of object. list[0] is the scalar type, list[1] is the scalar
   * value
   * @return value of the specific scalar type
   */
  @SuppressWarnings("unchecked")
  private Object deserializeScalar(List<Object> rawScalarData) {
    ScalarType type = getValueTypeFromObject(rawScalarData.get(0));

    Object obj = rawScalarData.get(1);
    switch (type) {
      case NULL:
        return null;
      case BOOLEAN:
        return Boolean.parseBoolean(SafeEncoder.encode((byte[]) obj));
      case DOUBLE:
        return BuilderFactory.DOUBLE.build(obj);
      case INTEGER:
        return (Long) obj;
      case STRING:
        return SafeEncoder.encode((byte[]) obj);
      case ARRAY:
        return deserializeArray(obj);
      case NODE:
        return deserializeNode((List<Object>) obj);
      case EDGE:
        return deserializeEdge((List<Object>) obj);
      case PATH:
        return deserializePath(obj);
      case MAP:
        return deserializeMap(obj);
      case POINT:
        return deserializePoint(obj);
      case UNKNOWN:
      default:
        return obj;
    }
  }

  private Object deserializePoint(Object rawScalarData) {
    return new Point(BuilderFactory.DOUBLE_LIST.build(rawScalarData));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> deserializeMap(Object rawScalarData) {
    List<Object> keyTypeValueEntries = (List<Object>) rawScalarData;

    int size = keyTypeValueEntries.size();
    Map<String, Object> map = new HashMap<>(size >> 1); // set the capacity to half of the list

    for (int i = 0; i < size; i += 2) {
      String key = SafeEncoder.encode((byte[]) keyTypeValueEntries.get(i));
      Object value = deserializeScalar((List<Object>) keyTypeValueEntries.get(i + 1));
      map.put(key, value);
    }
    return map;
  }

  @SuppressWarnings("unchecked")
  private Path deserializePath(Object rawScalarData) {
    List<List<Object>> array = (List<List<Object>>) rawScalarData;
    List<Node> nodes = (List<Node>) deserializeScalar(array.get(0));
    List<Edge> edges = (List<Edge>) deserializeScalar(array.get(1));
    return new Path(nodes, edges);
  }

  @SuppressWarnings("unchecked")
  private List<Object> deserializeArray(Object rawScalarData) {
    List<List<Object>> array = (List<List<Object>>) rawScalarData;
    List<Object> res = new ArrayList<>(array.size());
    for (List<Object> arrayValue : array) {
      res.add(deserializeScalar(arrayValue));
    }
    return res;
  }

  /**
   * Auxiliary function to retrieve scalar types
   *
   * @param rawScalarType
   * @return scalar type
   */
  private ScalarType getValueTypeFromObject(Object rawScalarType) {
    return getScalarType(((Long) rawScalarType).intValue());
  }

  private static enum ScalarType {
    UNKNOWN,
    NULL,
    STRING,
    INTEGER, // 64 bit long.
    BOOLEAN,
    DOUBLE,
    ARRAY,
    EDGE,
    NODE,
    PATH,
    MAP,
    POINT;
  }

  private static final ScalarType[] SCALAR_TYPES = ScalarType.values();

  private static ScalarType getScalarType(int index) {
    try {
      return SCALAR_TYPES[index];
    } catch (IndexOutOfBoundsException e) {
      throw new JedisException("Unrecognized response type");
    }
  }

  private class RecordImpl implements Record {

    private final List<String> header;
    private final List<Object> values;

    public RecordImpl(List<String> header, List<Object> values) {
      this.header = header;
      this.values = values;
    }

    @Override
    public <T> T getValue(int index) {
      return (T) this.values.get(index);
    }

    @Override
    public <T> T getValue(String key) {
      return getValue(this.header.indexOf(key));
    }

    @Override
    public String getString(int index) {
      return this.values.get(index).toString();
    }

    @Override
    public String getString(String key) {
      return getString(this.header.indexOf(key));
    }

    @Override
    public List<String> keys() {
      return header;
    }

    @Override
    public List<Object> values() {
      return this.values;
    }

    @Override
    public boolean containsKey(String key) {
      return this.header.contains(key);
    }

    @Override
    public int size() {
      return this.header.size();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof RecordImpl)) {
        return false;
      }
      RecordImpl record = (RecordImpl) o;
      return Objects.equals(header, record.header)
          && Objects.equals(values, record.values);
    }

    @Override
    public int hashCode() {
      return Objects.hash(header, values);
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("Record{");
      sb.append("values=").append(values);
      sb.append('}');
      return sb.toString();
    }
  }

  private static final ResultSet.ColumnType[] COLUMN_TYPES = ResultSet.ColumnType.values();

  private class HeaderImpl implements Header {

    private final List<ResultSet.ColumnType> schemaTypes;
    private final List<String> schemaNames;

    private HeaderImpl() {
      this.schemaTypes = emptyList();
      this.schemaNames = emptyList();
    }

    private HeaderImpl(List<ResultSet.ColumnType> schemaTypes, List<String> schemaNames) {
      this.schemaTypes = schemaTypes;
      this.schemaNames = schemaNames;
    }

    /**
     * @return a list of column names, ordered by they appearance in the query
     */
    @Override
    public List<String> getSchemaNames() {
      return schemaNames;
    }

    /**
     * @return a list of column types, ordered by they appearance in the query
     */
    @Override
    public List<ResultSet.ColumnType> getSchemaTypes() {
      return schemaTypes;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof HeaderImpl)) {
        return false;
      }
      HeaderImpl header = (HeaderImpl) o;
      return Objects.equals(getSchemaTypes(), header.getSchemaTypes())
          && Objects.equals(getSchemaNames(), header.getSchemaNames());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getSchemaTypes(), getSchemaNames());
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("HeaderImpl{");
      sb.append("schemaTypes=").append(schemaTypes);
      sb.append(", schemaNames=").append(schemaNames);
      sb.append('}');
      return sb.toString();
    }
  }

  private HeaderImpl parseHeader(Object data) {
    if (data == null) {
      return new HeaderImpl();
    }

    List<List<Object>> list = (List<List<Object>>) data;
    List<ResultSet.ColumnType> types = new ArrayList<>(list.size());
    List<String> texts = new ArrayList<>(list.size());
    for (List<Object> tuple : list) {
      types.add(COLUMN_TYPES[((Long) tuple.get(0)).intValue()]);
      texts.add(SafeEncoder.encode((byte[]) tuple.get(1)));
    }
    return new HeaderImpl(types, texts);
  }

  private class StatisticsImpl implements Statistics {

    private final Map<String, String> statistics;

    private StatisticsImpl(Map<String, String> statistics) {
      this.statistics = statistics;
    }

    /**
     *
     * @param label the requested statistic label as key
     * @return a string with the value, if key exists, null otherwise
     */
    public String getStringValue(String label) {
      return statistics.get(label);
    }

    /**
     *
     * @param label the requested statistic label as key
     * @return a string with the value, if key exists, 0 otherwise
     */
    private int getIntValue(String label) {
      String value = getStringValue(label);
      return value == null ? 0 : Integer.parseInt(value);
    }

    /**
     *
     * @return number of nodes created after query execution
     */
    @Override
    public int nodesCreated() {
      return getIntValue("Nodes created");
    }

    /**
     *
     * @return number of nodes deleted after query execution
     */
    @Override
    public int nodesDeleted() {
      return getIntValue("Nodes deleted");
    }

    /**
     *
     * @return number of indices added after query execution
     */
    @Override
    public int indicesCreated() {
      return getIntValue("Indices created");
    }

    @Override
    public int indicesDeleted() {
      return getIntValue("Indices deleted");
    }

    /**
     *
     * @return number of labels added after query execution
     */
    @Override
    public int labelsAdded() {
      return getIntValue("Labels added");
    }

    /**
     *
     * @return number of relationship deleted after query execution
     */
    @Override
    public int relationshipsDeleted() {
      return getIntValue("Relationships deleted");
    }

    /**
     *
     * @return number of relationship created after query execution
     */
    @Override
    public int relationshipsCreated() {
      return getIntValue("Relationships created");
    }

    /**
     *
     * @return number of properties set after query execution
     */
    @Override
    public int propertiesSet() {
      return getIntValue("Properties set");
    }

    /**
     *
     * @return The execution plan was cached on RedisGraph.
     */
    @Override
    public boolean cachedExecution() {
      return "1".equals(getStringValue("Cached execution"));
    }

    @Override
    public String queryIntervalExecutionTime() {
      return getStringValue("Query internal execution time");
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof StatisticsImpl)) {
        return false;
      }
      StatisticsImpl that = (StatisticsImpl) o;
      return Objects.equals(statistics, that.statistics);
    }

    @Override
    public int hashCode() {
      return Objects.hash(statistics);
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("Statistics{");
      sb.append(statistics);
      sb.append('}');
      return sb.toString();
    }
  }

  private StatisticsImpl parseStatistics(Object data) {
    Map<String, String> map = ((List<byte[]>) data).stream()
        .map(SafeEncoder::encode).map(s -> s.split(": "))
        .collect(Collectors.toMap(sa -> sa[0], sa -> sa[1]));
    return new StatisticsImpl(map);
  }

}
