package redis.clients.jedis.graph;

/**
 * Hold a query result
 */
public interface ResultSet extends Iterable<Record> {

  public enum ColumnType {
    UNKNOWN,
    SCALAR,
    NODE,
    RELATION
  }

  int size();

  Header getHeader();

  Statistics getStatistics();
}
