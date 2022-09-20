package redis.clients.jedis.search;

import redis.clients.jedis.args.Rawable;

class LazyRawable implements Rawable {

  private byte[] raw = null;

  public void setRaw(byte[] raw) {
    this.raw = raw;
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
