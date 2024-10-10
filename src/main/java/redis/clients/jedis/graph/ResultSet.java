package redis.clients.jedis.graph;

/**
 * Hold a query result.
 * @deprecated Redis Graph support is deprecated.
 */
@Deprecated
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
