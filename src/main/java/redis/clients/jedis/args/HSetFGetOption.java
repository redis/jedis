package redis.clients.jedis.args;

import redis.clients.jedis.Protocol.Keyword;

/**
 * GET option for {@code HSETF} command.
 */
public enum HSetFGetOption implements Rawable {

  NEW(Keyword.GETNEW),
  OLD(Keyword.GETOLD);

  private final Keyword keyword;

  private HSetFGetOption(Keyword keyword) {
    this.keyword = keyword;
  }

  @Override
  public byte[] getRaw() {
    return this.keyword.getRaw();
  }
}
