package redis.clients.jedis.mcf;

public enum SwitchReason {
  HEALTH_CHECK, CIRCUIT_BREAKER, FAILBACK, FORCED
}
