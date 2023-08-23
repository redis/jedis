package redis.clients.jedis.graph;

import java.util.List;

/**
 * Query response header interface. Represents the response schema (column names and types).
 * @deprecated Redis Graph support is deprecated.
 */
@Deprecated
public interface Header {

  List<ResultSet.ColumnType> getSchemaTypes();

  List<String> getSchemaNames();
}
