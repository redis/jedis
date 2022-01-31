package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;

import static redis.clients.jedis.Protocol.Keyword.LEN;
import static redis.clients.jedis.Protocol.Keyword.IDX;
import static redis.clients.jedis.Protocol.Keyword.MINMATCHLEN;
import static redis.clients.jedis.Protocol.Keyword.WITHMATCHLEN;

public class LCSParams implements IParams {

  private boolean idx = false;
  private boolean len = false;
  private boolean withMatchLen = false;
  private long minMatchLen;

  public static LCSParams LCSParams() { return new LCSParams(); }

  /**
   * When IDX is given the command returns an array with the LCS length
   * and all the ranges in both the strings, start and end offset for
   * each string, where there are matches.
   * @return LCSParams
   */
  public LCSParams idx() {
    this.idx = true;
    return this;
  }

  /**
   * When LEN is given the command returns the length of the longest common substring.
   * @return LCSParams
   */
  public LCSParams len() {
    this.len = true;
    return this;
  }

  /**
   * When WITHMATCHLEN is given each array representing a match will also have the length of the match.
   * @return LCSParams
   */
  public LCSParams withMatchLen() {
    this.withMatchLen = true;
    return this;
  }

  /**
   * Specify the minimum match length.
   * @return LCSParams
   */
  public LCSParams minMatchLen(long minMatchLen) {
    this.minMatchLen = minMatchLen;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (len) {
      args.add(LEN);
    }
    if (idx) {
      args.add(IDX);
    }
    if (minMatchLen > 0) {
      args.add(MINMATCHLEN);
      args.add(minMatchLen);
    }
    if (withMatchLen) {
      args.add(WITHMATCHLEN);
    }
  }
}
