package redis.clients.jedis;

public enum ReadFrom {
    // read from the upstream only.
    UPSTREAM,
    // read from the replica only.
    REPLICA,
    // read preferred from the upstream and fall back to a replica if the upstream is not available.
    UPSTREAM_PREFERRED,
    // read preferred from replica and fall back to upstream if no replica is not available.
    REPLICA_PREFERRED
}
