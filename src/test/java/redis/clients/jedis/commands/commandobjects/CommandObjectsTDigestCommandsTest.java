package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notANumber;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.bloom.TDigestMergeParams;

/**
 * Tests related to <a href="https://redis.io/commands/?group=tdigest">T-Digest</a> commands.
 */
public class CommandObjectsTDigestCommandsTest extends CommandObjectsModulesTestBase {

  public CommandObjectsTDigestCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testTDigestAddMinMax() {
    String key = "testTDigest";

    String create = exec(commandObjects.tdigestCreate(key));
    assertThat(create, equalTo("OK"));

    String add = exec(commandObjects.tdigestAdd(key, 1.0, 2.0, 3.0, 4.0, 5.0));
    assertThat(add, equalTo("OK"));

    Double minValue = exec(commandObjects.tdigestMin(key));
    assertThat(minValue, equalTo(1.0));

    Double maxValue = exec(commandObjects.tdigestMax(key));
    assertThat(maxValue, equalTo(5.0));
  }

  @Test
  public void testTDigestMerge() {
    String destinationKey = "testTDigestMergeDest";
    String sourceKey1 = "testTDigestSource1";
    String sourceKey2 = "testTDigestSource2";

    String create1 = exec(commandObjects.tdigestCreate(sourceKey1));
    assertThat(create1, equalTo("OK"));

    String create2 = exec(commandObjects.tdigestCreate(sourceKey2));
    assertThat(create2, equalTo("OK"));

    String add1 = exec(commandObjects.tdigestAdd(sourceKey1, 1.0, 2.0));
    assertThat(add1, equalTo("OK"));

    String add2 = exec(commandObjects.tdigestAdd(sourceKey2, 3.0, 4.0));
    assertThat(add2, equalTo("OK"));

    String merge = exec(commandObjects.tdigestMerge(destinationKey, sourceKey1, sourceKey2));
    assertThat(merge, equalTo("OK"));

    Double minAfterMerge = exec(commandObjects.tdigestMin(destinationKey));
    assertThat(minAfterMerge, equalTo(1.0));

    Double maxAfterMerge = exec(commandObjects.tdigestMax(destinationKey));
    assertThat(maxAfterMerge, equalTo(4.0));
  }

  @Test
  public void testTDigestMergeWithParams() {
    String destinationKey = "testTDigestMergeDestParams";
    String sourceKey1 = "testTDigestSource1Params";
    String sourceKey2 = "testTDigestSource2Params";

    TDigestMergeParams mergeParams = new TDigestMergeParams().compression(100);

    String create1 = exec(commandObjects.tdigestCreate(sourceKey1, 100));
    assertThat(create1, equalTo("OK"));

    String create2 = exec(commandObjects.tdigestCreate(sourceKey2, 100));
    assertThat(create2, equalTo("OK"));

    String add1 = exec(commandObjects.tdigestAdd(sourceKey1, 10.0, 20.0));
    assertThat(add1, equalTo("OK"));

    String add2 = exec(commandObjects.tdigestAdd(sourceKey2, 30.0, 40.0));
    assertThat(add2, equalTo("OK"));

    String merge = exec(commandObjects.tdigestMerge(mergeParams, destinationKey, sourceKey1, sourceKey2));
    assertThat(merge, equalTo("OK"));

    Double minAfterMerge = exec(commandObjects.tdigestMin(destinationKey));
    assertThat(minAfterMerge, equalTo(10.0));

    Double maxAfterMerge = exec(commandObjects.tdigestMax(destinationKey));
    assertThat(maxAfterMerge, equalTo(40.0));
  }

  @Test
  public void testTDigestReset() {
    String key = "testTDigest";

    String create = exec(commandObjects.tdigestCreate(key));
    assertThat(create, equalTo("OK"));

    String add = exec(commandObjects.tdigestAdd(key, 5.0, 10.0, 15.0));
    assertThat(add, equalTo("OK"));

    Double minBeforeReset = exec(commandObjects.tdigestMin(key));
    assertThat(minBeforeReset, equalTo(5.0));

    Double maxBeforeReset = exec(commandObjects.tdigestMax(key));
    assertThat(maxBeforeReset, equalTo(15.0));

    String reset = exec(commandObjects.tdigestReset(key));
    assertThat(reset, equalTo("OK"));

    Double minAfterReset = exec(commandObjects.tdigestMin(key));
    assertThat(minAfterReset, notANumber());

    Double maxAfterReset = exec(commandObjects.tdigestMax(key));
    assertThat(maxAfterReset, notANumber());
  }

  @Test
  public void testTDigestCdf() {
    String key = "testTDigest";

    String create = exec(commandObjects.tdigestCreate(key));
    assertThat(create, equalTo("OK"));

    String add = exec(commandObjects.tdigestAdd(key, 1.0, 2.0, 3.0, 4.0, 5.0));
    assertThat(add, equalTo("OK"));

    List<Double> cdf = exec(commandObjects.tdigestCDF(key, 1.0, 3.0, 5.0));

    assertThat(cdf, notNullValue());
    assertThat(cdf.size(), equalTo(3));
    assertThat(cdf.get(0), lessThanOrEqualTo(0.2));
    assertThat(cdf.get(1), lessThanOrEqualTo(0.6));
    assertThat(cdf.get(2), lessThanOrEqualTo(1.0));
  }

  @Test
  public void testTDigestQuantile() {
    String key = "testTDigest";

    String create = exec(commandObjects.tdigestCreate(key));
    assertThat(create, equalTo("OK"));

    String add = exec(commandObjects.tdigestAdd(key, 1.0, 2.0, 3.0, 4.0, 5.0));
    assertThat(add, equalTo("OK"));

    List<Double> quantile = exec(commandObjects.tdigestQuantile(key, 0.25, 0.5, 0.75));

    assertThat(quantile, notNullValue());
    assertThat(quantile.size(), equalTo(3));
    assertThat(quantile.get(0), lessThanOrEqualTo(2.0));
    assertThat(quantile.get(1), lessThanOrEqualTo(3.0));
    assertThat(quantile.get(2), lessThanOrEqualTo(4.0));
  }

  @Test
  public void testTDigestTrimmedMean() {
    String key = "testTDigest";

    String create = exec(commandObjects.tdigestCreate(key));
    assertThat(create, equalTo("OK"));

    String add = exec(commandObjects.tdigestAdd(key, 1.0, 2.0, 3.0, 4.0, 5.0));
    assertThat(add, equalTo("OK"));

    Double trimmedMean = exec(commandObjects.tdigestTrimmedMean(key, 0.1, 0.9));

    assertThat(trimmedMean, notNullValue());
    assertThat(trimmedMean, lessThanOrEqualTo(4.0));
    assertThat(trimmedMean, greaterThanOrEqualTo(2.0));
  }

  @Test
  public void testTDigestRankAndRevRank() {
    String key = "testTDigest";

    String create = exec(commandObjects.tdigestCreate(key));
    assertThat(create, equalTo("OK"));

    String add = exec(commandObjects.tdigestAdd(key, 1.0, 2.0, 3.0, 4.0, 5.0));
    assertThat(add, equalTo("OK"));

    List<Long> rank = exec(commandObjects.tdigestRank(key, 1.0, 3.0, 5.0));

    assertThat(rank, notNullValue());
    assertThat(rank.size(), equalTo(3));
    assertThat(rank.get(0), lessThanOrEqualTo(1L));
    assertThat(rank.get(1), lessThanOrEqualTo(3L));
    assertThat(rank.get(2), lessThanOrEqualTo(5L));

    List<Long> revRank = exec(commandObjects.tdigestRevRank(key, 1.0, 3.0, 5.0));

    assertThat(revRank, notNullValue());
    assertThat(revRank.size(), equalTo(3));
    assertThat(revRank.get(0), greaterThanOrEqualTo(4L));
    assertThat(revRank.get(1), greaterThanOrEqualTo(2L));
    assertThat(revRank.get(2), greaterThanOrEqualTo(0L));
  }

  @Test
  public void testTDigestByRankAndByRevRank() {
    String key = "testTDigest";

    String create = exec(commandObjects.tdigestCreate(key));
    assertThat(create, equalTo("OK"));

    String add = exec(commandObjects.tdigestAdd(key, 0.5, 1.5, 2.5, 3.5, 4.5));
    assertThat(add, equalTo("OK"));

    List<Double> byRank = exec(commandObjects.tdigestByRank(key, 0, 2, 4));

    assertThat(byRank, notNullValue());
    assertThat(byRank.size(), equalTo(3));
    assertThat(byRank.get(0), closeTo(0.5, 0.1));
    assertThat(byRank.get(1), closeTo(2.5, 0.1));
    assertThat(byRank.get(2), closeTo(4.5, 0.1));

    List<Double> byRevRank = exec(commandObjects.tdigestByRevRank(key, 0, 2, 4));

    assertThat(byRevRank, notNullValue());
    assertThat(byRevRank.size(), equalTo(3));
    assertThat(byRevRank.get(0), closeTo(4.5, 0.1));
    assertThat(byRevRank.get(1), closeTo(2.5, 0.1));
    assertThat(byRevRank.get(2), closeTo(0.5, 0.1));
  }

  @Test
  public void testTDigestInfo() {
    String key = "testTDigest";

    String create = exec(commandObjects.tdigestCreate(key));
    assertThat(create, equalTo("OK"));

    String add = exec(commandObjects.tdigestAdd(key, 1.0, 2.0, 3.0));
    assertThat(add, equalTo("OK"));

    Map<String, Object> info = exec(commandObjects.tdigestInfo(key));

    assertThat(info, notNullValue());
    assertThat(info, hasKey("Compression"));
    assertThat(info, hasEntry("Observations", 3L));
  }
}
