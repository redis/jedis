package redis.clients.jedis;

/**
 * Pluggable per-operation {@code SO_TIMEOUT} override for a {@link Connection}. Consulted before
 * each socket read; returns the timeout to apply, or {@link JedisClientConfig#UNSET_TIMEOUT_MS} to
 * defer to the connection's own calculation. Suppliers may chain by wrapping another supplier and
 * returning {@code UNSET_TIMEOUT_MS} when they have no opinion.
 */
@FunctionalInterface
interface SoTimeoutSupplier {

  /**
   * @param blocking whether the in-flight command is a blocking command
   * @return {@code SO_TIMEOUT} in milliseconds ({@code 0} = infinite), or
   *         {@link JedisClientConfig#UNSET_TIMEOUT_MS} to defer to the connection default
   */
  int getSoTimeout(boolean blocking);
}
