package redis.clients.jedis.sentinel.listenner;

import redis.clients.jedis.HostAndPort;

/**
 * interface for monitor the master failover under sentinel mode We offer two implementation options
 * @see SentinelSubscribeListener Passive subscription
 * @see SentinelActiveDetectListener Active detection
 */
public interface SentinelListener {
  void start();

  void shutdown();

  void onChange(HostAndPort hostAndPort);
}