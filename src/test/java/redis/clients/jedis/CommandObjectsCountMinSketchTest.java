package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArgumentCount;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArguments;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.bloom.CmsCellSize;
import redis.clients.jedis.bloom.RedisBloomProtocol.CountMinSketchCommand;
import redis.clients.jedis.bloom.RedisBloomProtocol.RedisBloomKeyword;

/**
 * Argument construction of the CMS.INITBYDIM / CMS.INITBYPROB cell size overloads; no server
 * needed.
 */
public class CommandObjectsCountMinSketchTest {

  private final CommandObjects commandObjects = new CommandObjects(RedisProtocol.RESP2);

  @Test
  public void initByDimEmitsCellSize() {
    CommandArguments args = commandObjects.cmsInitByDim("cms", 1000L, 5L, CmsCellSize.ONE_BYTE)
        .getArguments();

    assertThat(args, hasArgumentCount(6));
    assertThat(args,
      hasArguments(CountMinSketchCommand.INITBYDIM, RawableFactory.from("cms"),
        RawableFactory.from(1000L), RawableFactory.from(5L), RedisBloomKeyword.CELL_SIZE,
        RawableFactory.from(1L)));
  }

  @Test
  public void initByProbEmitsCellSize() {
    CommandArguments args = commandObjects
        .cmsInitByProb("cms", 0.001, 0.01, CmsCellSize.EIGHT_BYTES).getArguments();

    assertThat(args, hasArgumentCount(6));
    assertThat(args,
      hasArguments(CountMinSketchCommand.INITBYPROB, RawableFactory.from("cms"),
        RawableFactory.from(0.001), RawableFactory.from(0.01), RedisBloomKeyword.CELL_SIZE,
        RawableFactory.from(8L)));
  }
}
