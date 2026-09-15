package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArgumentCount;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArguments;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.bloom.RedisBloomProtocol.CountMinSketchCommand;
import redis.clients.jedis.bloom.RedisBloomProtocol.RedisBloomKeyword;

/**
 * Argument construction and client-side validation of the CMS.INITBYDIM / CMS.INITBYPROB cell
 * size overloads; no server needed.
 */
public class CommandObjectsCountMinSketchTest {

  private final CommandObjects commandObjects = new CommandObjects(RedisProtocol.RESP2);

  @ParameterizedTest
  @ValueSource(ints = { 1, 2, 4, 8 })
  public void initByDimEmitsCellSize(int cellSize) {
    CommandArguments args = commandObjects.cmsInitByDim("cms", 1000L, 5L, cellSize).getArguments();

    assertThat(args, hasArgumentCount(6));
    assertThat(args, hasArguments(CountMinSketchCommand.INITBYDIM, RawableFactory.from("cms"),
      RawableFactory.from(1000L), RawableFactory.from(5L), RedisBloomKeyword.CELL_SIZE,
      RawableFactory.from((long) cellSize)));
  }

  @ParameterizedTest
  @ValueSource(ints = { 1, 2, 4, 8 })
  public void initByProbEmitsCellSize(int cellSize) {
    CommandArguments args = commandObjects.cmsInitByProb("cms", 0.001, 0.01, cellSize)
        .getArguments();

    assertThat(args, hasArgumentCount(6));
    assertThat(args, hasArguments(CountMinSketchCommand.INITBYPROB, RawableFactory.from("cms"),
      RawableFactory.from(0.001), RawableFactory.from(0.01), RedisBloomKeyword.CELL_SIZE,
      RawableFactory.from((long) cellSize)));
  }

  @ParameterizedTest
  @ValueSource(ints = { -1, 0, 3, 5, 16 })
  public void rejectsUnsupportedCellSize(int cellSize) {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
      () -> commandObjects.cmsInitByDim("cms", 1000L, 5L, cellSize));
    assertEquals("CMS cell size must be 1, 2, 4 or 8", e.getMessage());

    assertThrows(IllegalArgumentException.class,
      () -> commandObjects.cmsInitByProb("cms", 0.001, 0.01, cellSize));
  }
}
