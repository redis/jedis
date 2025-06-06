package redis.clients.jedis.search.aggr;

import java.io.Closeable;
import java.util.Iterator;
import java.util.NoSuchElementException;

import redis.clients.jedis.Connection;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.search.SearchProtocol;
import redis.clients.jedis.util.IOUtils;

/**
 * Iterator for Redis search aggregation results with cursor support. This class manages the
 * connection to a specific Redis node and handles cursor-based pagination for large aggregation
 * results.
 * <p>
 * The iterator supports the {@link #remove()} method which deletes the cursor on the server and
 * terminates the iteration, freeing server resources immediately.
 * <p>
 * Usage example:
 * 
 * <pre>
 * {
 *   &#64;code
 *   AggregationBuilder aggr = new AggregationBuilder().groupBy("@field").cursor(100, 60000); // 100
 *                                                                                            // results
 *                                                                                            // per
 *                                                                                            // batch,
 *                                                                                            // 60
 *                                                                                            // second
 *                                                                                            // TTL
 *                                                                                            // for
 *                                                                                            // the
 *                                                                                            // cursor
 *
 *   try (AggregateIterator iterator = new AggregateIterator(provider, "myindex", aggr)) {
 *     while (iterator.hasNext()) {
 *       AggregationResult batch = iterator.next();
 *
 *       if (batch.isEmpty()) {
 *         break; // FT.AGGREGATE returned empty result set
 *       }
 *
 *       // Process batch - access rows via batch.getRows()
 *
 *       // Optionally terminate early and free server resources
 *       if (someCondition) {
 *         iterator.remove(); // Deletes cursor and stops iteration
 *         break;
 *       }
 *     }
 *   }
 * }
 * </pre>
 */
public class AggregateIterator implements Iterator<AggregationResult>, Closeable {

  private final ConnectionProvider connectionProvider;
  private final String indexName;
  private final Integer batchSize;

  private Connection connection;
  private Long cursorId = -1L;
  private AggregationResult aggrCommandResult;

  /**
   * Creates a new AggregateIterator.
   * @param connectionProvider the connection provider for cluster/standalone Redis
   * @param indexName the search index name
   * @param aggregationBuilder the aggregation query with cursor configuration
   * @throws IllegalArgumentException if aggregation doesn't have cursor configured
   */
  public AggregateIterator(ConnectionProvider connectionProvider, String indexName,
      AggregationBuilder aggregationBuilder) {
    if (!aggregationBuilder.isWithCursor()) {
      throw new IllegalArgumentException("AggregationBuilder must have cursor configured");
    }

    this.connectionProvider = connectionProvider;
    this.indexName = indexName;
    this.batchSize = aggregationBuilder.getCursorCount();

    // Get a dedicated connection for this cursor session
    this.connection = acquireConnection(aggregationBuilder);
  }

  @Override
  public boolean hasNext() {
    return aggrCommandResult != null || cursorId > 0;
  }

  @Override
  public AggregationResult next() {
    if (!hasNext()) {
      throw new NoSuchElementException("No more aggregation results available");
    }

    try {
      if (aggrCommandResult != null) {
        try {
          return aggrCommandResult;
        } finally {
          aggrCommandResult = null;
        }
      } else {
        return doFetch();
      }

    } catch (Exception e) {
      throw new JedisException("Failed to fetch next aggregation batch", e);
    }
  }

  /**
   * Returns the current cursor ID.
   * @return cursor ID, or null if not initialized
   */
  public Long getCursorId() {
    return cursorId;
  }

  @Override
  public void remove() {
    aggrCommandResult = null;

    if (cursorId == null || cursorId <= 0) {
      // Cursor is already closed or not initialized, nothing to do
      return;
    }

    deleteCursor();
    // Mark cursor as deleted to prevent further operations
    cursorId = -1L;
  }

  @Override
  public void close() {
    deleteCursor();
    // Mark cursor as closed to prevent further operations
    cursorId = -1L;
    IOUtils.closeQuietly(connection);
  }

  /**
   * Deletes the cursor on the server to free resources. This method is idempotent and safe to call
   * multiple times.
   */
  private void deleteCursor() {
    if (cursorId != null && cursorId > 0) {
      try {
        // Delete the cursor to free server resources
        connection.executeCommand(
          new redis.clients.jedis.CommandArguments(SearchProtocol.SearchCommand.CURSOR)
              .add(SearchProtocol.SearchKeyword.DEL).add(indexName).add(cursorId));
      } catch (Exception e) {
        // Log but don't throw - cursor will expire naturally
        System.err.println("Warning: Failed to delete cursor " + cursorId + ": " + e.getMessage());
      }
    }
  }

  private AggregationResult doFetch() {
    if (cursorId == null || cursorId <= 0) {
      return null;
    }

    redis.clients.jedis.CommandArguments args = new redis.clients.jedis.ClusterCommandArguments(
        SearchProtocol.SearchCommand.CURSOR).add(SearchProtocol.SearchKeyword.READ).add(indexName)
            .add(cursorId);

    // Only add COUNT argument if a batch size was explicitly specified
    if (batchSize != null) {
      args.add(SearchProtocol.SearchKeyword.COUNT).add(batchSize);
    }

    Object rawReply = connection.executeCommand(args);
    AggregationResult result = AggregationResult.SEARCH_AGGREGATION_RESULT_WITH_CURSOR
        .build(rawReply);

    cursorId = result.getCursorId();
    return result;
  }

  private Connection acquireConnection(AggregationBuilder aggregationBuilder) {
    // Create the initial FT.AGGREGATE command
    redis.clients.jedis.CommandArguments args = new redis.clients.jedis.ClusterCommandArguments(
        SearchProtocol.SearchCommand.AGGREGATE).add(indexName).addParams(aggregationBuilder);

    Connection conn = null;
    try {
      // Get connection and execute initial command
      conn = connectionProvider.getConnection(args);
      Object rawReply = conn.executeCommand(args);
      aggrCommandResult = AggregationResult.SEARCH_AGGREGATION_RESULT_WITH_CURSOR.build(rawReply);

      cursorId = aggrCommandResult.getCursorId();

      return conn;

    } catch (Exception e) {
      IOUtils.closeQuietly(conn);
      throw new JedisException("Failed to initialize aggregation cursor", e);
    }
  }

}
