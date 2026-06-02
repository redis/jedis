package redis.clients.jedis;

import redis.clients.jedis.annots.Experimental;

/**
 * A component whose connections can be temporarily rebound to a new endpoint in response to a
 * server MOVING notification.
 */
@Experimental
public interface RebindAware {

  /**
   * Outcome of a {@link #rebind(long, HostAndPort, long)} call.
   */
  enum RebindResult {
    /** Duplicate or out-of-order event ({@code seq <=} last applied); nothing changed. */
    STALE,
    /** Applied a new target; */
    APPLIED_NEW_TARGET
  }

  /**
   * Binds new connections to {@code newHostAndPort} for the next {@code ttlSeconds}, after which
   * the originally configured endpoint is used again. Applied only if {@code seq} is greater than
   * the last applied sequence number; otherwise ignored as a duplicate or out-of-order event.
   * @param seq sequence number of the MOVING event
   * @param newHostAndPort target for new connections during the grace window
   * @param ttlSeconds grace window in seconds before the rebind expires
   * @return {@link RebindResult#APPLIED_NEW_TARGET} if applied, else {@link RebindResult#STALE}
   */
  RebindResult rebind(long seq, HostAndPort newHostAndPort, long ttlSeconds);
}