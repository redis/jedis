package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

import java.util.Objects;

public class LCSParams implements IParams {

  private boolean len = false;
  private boolean idx = false;
  private Long minMatchLen;
  private boolean withMatchLen = false;

  public static LCSParams LCSParams() { return new LCSParams(); }

  /**
   * When LEN is given the command returns the length of the longest common substring.
   * @return LCSParams
   */
  public LCSParams len() {
    this.len = true;
    return this;
  }

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
   * Specify the minimum match length.
   * @return LCSParams
   */
  public LCSParams minMatchLen(long minMatchLen) {
    this.minMatchLen = minMatchLen;
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

  @Override
  public void addParams(CommandArguments args) {
    if (len) {
      args.add(Keyword.LEN);
    }
    if (idx) {
      args.add(Keyword.IDX);
    }
    if (minMatchLen != null) {
      args.add(Keyword.MINMATCHLEN).add(minMatchLen);
    }
    if (withMatchLen) {
      args.add(Keyword.WITHMATCHLEN);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LCSParams lcsParams = (LCSParams) o;
    return len == lcsParams.len && idx == lcsParams.idx && withMatchLen == lcsParams.withMatchLen && Objects.equals(minMatchLen, lcsParams.minMatchLen);
  }

  @Override
  public int hashCode() {
    return Objects.hash(len, idx, minMatchLen, withMatchLen);
  }
}
