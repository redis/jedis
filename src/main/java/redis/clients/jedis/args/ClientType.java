package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

public enum ClientType implements Rawable {

  NORMAL, MASTER, SLAVE, REPLICA, PUBSUB;

  private final byte[] raw;

  private ClientType() {
    raw = SafeEncoder.encode(name().toLowerCase());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
