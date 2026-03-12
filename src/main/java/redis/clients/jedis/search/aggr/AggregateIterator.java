package redis.clients.jedis.search.aggr;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.search.SearchProtocol;
import redis.clients.jedis.util.Pool;

/**
 * Iterator for Redis search aggregation results with cursor support. This class manages the
 * connection to a specific Redis node and handles cursor-based pagination for large aggregation
 * results.
 * <p>
 * The iterator supports the {@link #remove()} method which deletes the cursor on the server and
 * terminates the iteration, freeing server resources immediately.
 * <p>
 * This implementation uses connection pooling to prevent connection pool exhaustion during
 * long-running aggregation operations. Connections are borrowed from the pool for each batch
 * operation and returned immediately after use.
 * <p>
 * Usage example:
 *
 * <pre>
 * {
 *   &#64;code
 *   // 100 results per batch, 60 second TTL for the cursor
 *   AggregationBuilder aggr = new AggregationBuilder().groupBy("@field").cursor(100, 60000);
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

  private final String indexName;
  private final Integer batchSize;

  // Connection pool entry - can be either Connection or Pool<Connection>
  private final Map.Entry<?, ?> connectionEntry;
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

    this.indexName = indexName;
    this.batchSize = aggregationBuilder.getCursorCount();

    // Get connection pool entry - use getPrimaryNodesConnectionMap() to get pool-based connections
    Map<?, ?> connectionMap = connectionProvider.getPrimaryNodesConnectionMap();
    if (connectionMap.isEmpty()) {
      throw new JedisException("No connections available from connection provider");
    }
    // Get the first (or only) entry from the map
    this.connectionEntry = connectionMap.entrySet().iterator().next();

    // Execute initial aggregation command
    initializeAggregation(aggregationBuilder);
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
    // Note: No connection to close - connections are borrowed and returned per operation
  }

  /**
   * Deletes the cursor on the server to free resources. This method is idempotent and safe to call
   * multiple times.
   */
  private void deleteCursor() {
    if (cursorId != null && cursorId > 0) {
      CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.CURSOR)
          .add(SearchProtocol.SearchKeyword.DEL).add(indexName).add(cursorId);
      try {
        // Delete the cursor to free server resources
        executeCommand(args);
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

    CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.CURSOR)
        .add(SearchProtocol.SearchKeyword.READ).add(indexName).add(cursorId);

    // Only add COUNT argument if a batch size was explicitly specified
    if (batchSize != null) {
      args.add(SearchProtocol.SearchKeyword.COUNT).add(batchSize);
    }

    Object rawReply = executeCommand(args);
    AggregationResult result = AggregationResult.SEARCH_AGGREGATION_RESULT_WITH_CURSOR
        .build(rawReply);

    cursorId = result.getCursorId();
    return result;
  }

  /**
   * Initializes the aggregation by executing the initial FT.AGGREGATE command.
   */
  private void initializeAggregation(AggregationBuilder aggregationBuilder) {
    CommandArguments args = new CommandArguments(SearchProtocol.SearchCommand.AGGREGATE)
        .add(indexName).addParams(aggregationBuilder);

    try {
      Object rawReply = executeCommand(args);
      aggrCommandResult = AggregationResult.SEARCH_AGGREGATION_RESULT_WITH_CURSOR.build(rawReply);
      cursorId = aggrCommandResult.getCursorId();
    } catch (Exception e) {
      throw new JedisException("Failed to initialize aggregation cursor", e);
    }
  }

  /**
   * Executes a command using the connection entry. If the entry value is a Pool, borrows a
   * connection, executes the command, and returns the connection to the pool. This pattern prevents
   * connection pool exhaustion during long-running aggregation operations.
   */
  @SuppressWarnings("unchecked")
  private Object executeCommand(CommandArguments args) {
    Object entryValue = connectionEntry.getValue();

    if (entryValue instanceof Connection) {
      // Direct connection (non-pooled) - use directly
      return ((Connection) entryValue).executeCommand(args);
    } else if (entryValue instanceof Pool) {
      // Pooled connection - borrow, use, and return
      try (Connection conn = ((Pool<Connection>) entryValue).getResource()) {
        return conn.executeCommand(args);
      }
    } else {
      throw new IllegalArgumentException(entryValue.getClass() + " is not supported.");
    }
  }

}
