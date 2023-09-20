package redis.clients.jedis;

/**
 * interface for monitor the master failover under sentinel mode We offer two implementation options
 * @see SentinelMasterSubscribeListener Passive subscription
 * @see SentinelMasterActiveDetectListener Active detection
 */
public interface SentinelMasterListener {
  void start();

  void shutdown();

  void onChange(HostAndPort hostAndPort);
}