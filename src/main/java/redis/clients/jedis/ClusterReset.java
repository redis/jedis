package redis.clients.jedis;

import redis.clients.jedis.args.ClusterResetType;

/**
 * @deprecated Use {@link ClusterResetType}.
 */
@Deprecated
public enum ClusterReset {
  SOFT, HARD
}
