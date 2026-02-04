package redis.clients.jedis.search;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.params.IParams;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.LIMIT;

/**
 * LIMIT operation for post-processing in FT.HYBRID command.
 */
@Experimental
public class Limit implements IParams {

  private final int offset;
  private final int count;

  private Limit(int offset, int count) {
    this.offset = offset;
    this.count = count;
  }

  /**
   * Create a LIMIT operation.
   * @param offset the offset
   * @param count the count
   * @return a new Limit instance
   */
  public static Limit of(int offset, int count) {
    return new Limit(offset, count);
  }

  @Override
  public void addParams(CommandArguments args) {
    args.add(LIMIT);
    args.add(offset);
    args.add(count);
  }
}
