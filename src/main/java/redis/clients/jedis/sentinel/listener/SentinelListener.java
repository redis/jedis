package redis.clients.jedis.sentinel.listener;

import redis.clients.jedis.HostAndPort;

/**
 * interface for monitor the master failover under sentinel mode We offer two implementation options
 * @see SentinelSubscribeListener subscribe failover message from "+switch-master" channel
 * @see SentinelActiveDetectListener active detect master node .in case of the subscribe message lost
 */
public interface SentinelListener {
  void start();

  void shutdown();

  void onChange(HostAndPort hostAndPort);
}