package redis.clients.jedis.commands.unified.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START_BINARY;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.Response;
import redis.clients.jedis.args.SortedSetOption;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;
import redis.clients.jedis.params.ZParams;
import redis.clients.jedis.params.ZRangeParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.util.AssertUtil;
import redis.clients.jedis.util.KeyValue;
import redis.clients.jedis.util.SafeEncoder;

@RunWith(Parameterized.class)
public class SortedSetPipelineCommandsTest extends PipelineCommandsTestBase {

  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
  final byte[] bcar = { 0x09, 0x0A, 0x0B, 0x0C };
  final byte[] ba = { 0x0A };
  final byte[] bb = { 0x0B };
  final byte[] bc = { 0x0C };
  final byte[] bInclusiveB = { 0x5B, 0x0B };
  final byte[] bExclusiveC = { 0x28, 0x0C };
  final byte[] bLexMinusInf = { 0x2D };
  final byte[] bLexPlusInf = { 0x2B };

  final byte[] bbar1 = { 0x05, 0x06, 0x07, 0x08, 0x0A };
  final byte[] bbar2 = { 0x05, 0x06, 0x07, 0x08, 0x0B };
  final byte[] bbar3 = { 0x05, 0x06, 0x07, 0x08, 0x0C };
  final byte[] bbarstar = { 0x05, 0x06, 0x07, 0x08, '*' };

  public SortedSetPipelineCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void zadd() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 10d, "b");
    pipe.zadd("foo", 0.1d, "c");
    pipe.zadd("foo", 2d, "a");

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bfoo, 0.1d, bc);
    pipe.zadd(bfoo, 2d, ba);

    assertThat(pipe.syncAndReturnAll(), contains(
        1L,
        1L,
        1L,
        0L,
        1L,
        1L,
        1L,
        0L
    ));
  }

  @Test
  public void zaddWithParams() {
    pipe.del("foo");

    // xx: never add new member
    pipe.zadd("foo", 1d, "a", ZAddParams.zAddParams().xx());

    pipe.zadd("foo", 1d, "a");

    // nx: never update current member
    pipe.zadd("foo", 2d, "a", ZAddParams.zAddParams().nx());
    pipe.zscore("foo", "a");

    Map<String, Double> scoreMembers = new HashMap<String, Double>();
    scoreMembers.put("a", 2d);
    scoreMembers.put("b", 1d);
    // ch: return count of members not only added, but also updated
    pipe.zadd("foo", scoreMembers, ZAddParams.zAddParams().ch());

    assertThat(pipe.syncAndReturnAll(), contains(
        0L,
        0L,
        1L,
        0L,
        1d,
        2L
    ));

    // lt: only update existing elements if the new score is less than the current score.
    pipe.zadd("foo", 3d, "a", ZAddParams.zAddParams().lt());
    pipe.zscore("foo", "a");
    pipe.zadd("foo", 1d, "a", ZAddParams.zAddParams().lt());
    pipe.zscore("foo", "a");

    assertThat(pipe.syncAndReturnAll(), contains(
        0L,
        2d,
        0L,
        1d
    ));

    // gt: only update existing elements if the new score is greater than the current score.
    pipe.zadd("foo", 0d, "b", ZAddParams.zAddParams().gt());
    pipe.zscore("foo", "b");
    pipe.zadd("foo", 2d, "b", ZAddParams.zAddParams().gt());
    pipe.zscore("foo", "b");

    assertThat(pipe.syncAndReturnAll(), contains(
        0L,
        1d,
        0L,
        2d
    ));

    // incr: don't update already existing elements.
    pipe.zaddIncr("foo", 1d, "b", ZAddParams.zAddParams().nx());
    pipe.zscore("foo", "b");
    // incr: update elements that already exist.
    pipe.zaddIncr("foo", 1d, "b", ZAddParams.zAddParams().xx());
    pipe.zscore("foo", "b");

    assertThat(pipe.syncAndReturnAll(), contains(
        nullValue(),
        equalTo(2d),
        equalTo(3d),
        equalTo(3d)
    ));

    // binary
    pipe.del(bfoo);

    // xx: never add new member
    pipe.zadd(bfoo, 1d, ba, ZAddParams.zAddParams().xx());

    pipe.zadd(bfoo, 1d, ba);

    // nx: never update current member
    pipe.zadd(bfoo, 2d, ba, ZAddParams.zAddParams().nx());
    pipe.zscore(bfoo, ba);

    Map<byte[], Double> binaryScoreMembers = new HashMap<>();
    binaryScoreMembers.put(ba, 2d);
    binaryScoreMembers.put(bb, 1d);
    // ch: return count of members not only added, but also updated
    pipe.zadd(bfoo, binaryScoreMembers, ZAddParams.zAddParams().ch());

    assertThat(pipe.syncAndReturnAll(), contains(
        0L,
        0L,
        1L,
        0L,
        1d,
        2L
    ));

    // lt: only update existing elements if the new score is less than the current score.
    pipe.zadd(bfoo, 3d, ba, ZAddParams.zAddParams().lt());
    pipe.zscore(bfoo, ba);
    pipe.zadd(bfoo, 1d, ba, ZAddParams.zAddParams().lt());
    pipe.zscore(bfoo, ba);

    assertThat(pipe.syncAndReturnAll(), contains(
        0L,
        2d,
        0L,
        1d
    ));

    // gt: only update existing elements if the new score is greater than the current score.
    pipe.zadd(bfoo, 0d, bb, ZAddParams.zAddParams().gt());
    pipe.zscore(bfoo, bb);
    pipe.zadd(bfoo, 2d, bb, ZAddParams.zAddParams().gt());
    pipe.zscore(bfoo, bb);

    assertThat(pipe.syncAndReturnAll(), contains(
        0L,
        1d,
        0L,
        2d
    ));

    // incr: don't update already existing elements.
    pipe.zaddIncr(bfoo, 1d, bb, ZAddParams.zAddParams().nx());
    pipe.zscore(bfoo, bb);
    // incr: update elements that already exist.
    pipe.zaddIncr(bfoo, 1d, bb, ZAddParams.zAddParams().xx());
    pipe.zscore(bfoo, bb);

    assertThat(pipe.syncAndReturnAll(), contains(
        nullValue(),
        equalTo(2d),
        equalTo(3d),
        equalTo(3d)
    ));
  }

  @Test
  public void zrange() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 10d, "b");
    pipe.zadd("foo", 0.1d, "c");
    pipe.zadd("foo", 2d, "a");

    Response<List<String>> range1 = pipe.zrange("foo", 0, 1);
    Response<List<String>> range2 = pipe.zrange("foo", 0, 100);

    pipe.sync();

    assertThat(range1.get(), contains("c", "a"));
    assertThat(range2.get(), contains("c", "a", "b"));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bfoo, 0.1d, bc);
    pipe.zadd(bfoo, 2d, ba);

    Response<List<byte[]>> brange1 = pipe.zrange(bfoo, 0, 1);
    Response<List<byte[]>> brange2 = pipe.zrange(bfoo, 0, 100);

    pipe.sync();

    assertThat(brange1.get(), contains(bc, ba));
    assertThat(brange2.get(), contains(bc, ba, bb));
  }

  @Test
  public void zrangeByLex() {
    pipe.zadd("foo", 1, "aa");
    pipe.zadd("foo", 1, "c");
    pipe.zadd("foo", 1, "bb");
    pipe.zadd("foo", 1, "d");

    // exclusive aa ~ inclusive c
    Response<List<String>> range1 = pipe.zrangeByLex("foo", "(aa", "[c");

    // with LIMIT
    Response<List<String>> range2 = pipe.zrangeByLex("foo", "-", "+", 1, 2);

    pipe.sync();

    assertThat(range1.get(), contains("bb", "c"));
    assertThat(range2.get(), contains("bb", "c"));
  }

  @Test
  public void zrangeByLexBinary() {
    pipe.zadd(bfoo, 1, ba);
    pipe.zadd(bfoo, 1, bc);
    pipe.zadd(bfoo, 1, bb);

    Response<List<byte[]>> brange1 = pipe.zrangeByLex(bfoo, bInclusiveB, bExclusiveC);

    // with LIMIT
    Response<List<byte[]>> brange2 = pipe.zrangeByLex(bfoo, bLexMinusInf, bLexPlusInf, 0, 2);

    pipe.sync();

    assertThat(brange1.get(), contains(bb));
    assertThat(brange2.get(), contains(ba, bb));
  }

  @Test
  public void zrevrangeByLex() {
    pipe.zadd("foo", 1, "aa");
    pipe.zadd("foo", 1, "c");
    pipe.zadd("foo", 1, "bb");
    pipe.zadd("foo", 1, "d");

    // exclusive aa ~ inclusive c
    Response<List<String>> range1 = pipe.zrevrangeByLex("foo", "[c", "(aa");

    // with LIMIT
    Response<List<String>> range2 = pipe.zrevrangeByLex("foo", "+", "-", 1, 2);

    pipe.sync();

    assertThat(range1.get(), contains("c", "bb"));
    assertThat(range2.get(), contains("c", "bb"));
  }

  @Test
  public void zrevrangeByLexBinary() {
    // binary
    pipe.zadd(bfoo, 1, ba);
    pipe.zadd(bfoo, 1, bc);
    pipe.zadd(bfoo, 1, bb);

    Response<List<byte[]>> brange1 = pipe.zrevrangeByLex(bfoo, bExclusiveC, bInclusiveB);

    // with LIMIT
    Response<List<byte[]>> brange2 = pipe.zrevrangeByLex(bfoo, bLexPlusInf, bLexMinusInf, 0, 2);

    pipe.sync();

    assertThat(brange1.get(), contains(bb));
    assertThat(brange2.get(), contains(bc, bb));
  }

  @Test
  public void zrevrange() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 10d, "b");
    pipe.zadd("foo", 0.1d, "c");
    pipe.zadd("foo", 2d, "a");

    Response<List<String>> range1 = pipe.zrevrange("foo", 0, 1);
    Response<List<String>> range2 = pipe.zrevrange("foo", 0, 100);

    pipe.sync();

    assertThat(range1.get(), contains("b", "a"));
    assertThat(range2.get(), contains("b", "a", "c"));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bfoo, 0.1d, bc);
    pipe.zadd(bfoo, 2d, ba);

    Response<List<byte[]>> brange1 = pipe.zrevrange(bfoo, 0, 1);
    Response<List<byte[]>> brange2 = pipe.zrevrange(bfoo, 0, 100);

    pipe.sync();

    assertThat(brange1.get(), contains(bb, ba));
    assertThat(brange2.get(), contains(bb, ba, bc));
  }

  @Test
  public void zrangeParams() {
    pipe.zadd("foo", 1, "aa");
    pipe.zadd("foo", 1, "c");
    pipe.zadd("foo", 1, "bb");
    pipe.zadd("foo", 1, "d");

    Response<List<String>> range1 = pipe.zrange("foo", ZRangeParams.zrangeByLexParams("[c", "(aa").rev());
    Response<List<Tuple>> range2 = pipe.zrangeWithScores("foo", ZRangeParams.zrangeByScoreParams(0, 1));

    pipe.sync();

    assertThat(range1.get(), contains("c", "bb"));
    assertThat(range2.get().stream().map(Tuple::getElement).collect(Collectors.toList()), contains("aa", "bb", "c", "d"));

    // Binary
    pipe.zadd(bfoo, 1, ba);
    pipe.zadd(bfoo, 1, bc);
    pipe.zadd(bfoo, 1, bb);

    Response<List<byte[]>> brange1 = pipe.zrange(bfoo, ZRangeParams.zrangeByLexParams(bExclusiveC, bInclusiveB).rev());
    Response<List<Tuple>> brange2 = pipe.zrangeWithScores(bfoo, ZRangeParams.zrangeByScoreParams(0, 1).limit(0, 3));

    pipe.sync();

    assertThat(brange1.get(), contains(bb));
    assertThat(brange2.get().stream().map(Tuple::getBinaryElement).collect(Collectors.toList()), contains(ba, bb, bc));
  }

  @Test
  public void zrangestore() {
    pipe.zadd("foo", 1, "aa");
    pipe.zadd("foo", 2, "c");
    pipe.zadd("foo", 3, "bb");

    Response<Long> stored = pipe.zrangestore("bar", "foo", ZRangeParams.zrangeByScoreParams(1, 2));
    Response<List<String>> range = pipe.zrange("bar", 0, -1);

    pipe.sync();

    assertThat(stored.get(), equalTo(2L));
    assertThat(range.get(), contains("aa", "c"));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bfoo, 0.1d, bc);
    pipe.zadd(bfoo, 2d, ba);

    Response<Long> bstored = pipe.zrangestore(bbar, bfoo, ZRangeParams.zrangeParams(0, 1).rev());
    Response<List<byte[]>> brange = pipe.zrevrange(bbar, 0, 1);

    pipe.sync();

    assertThat(bstored.get(), equalTo(2L));
    assertThat(brange.get(), contains(bb, ba));
  }

  @Test
  public void zrem() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 2d, "b");

    Response<Long> result1 = pipe.zrem("foo", "a");
    Response<List<String>> range = pipe.zrange("foo", 0, 100);
    Response<Long> result2 = pipe.zrem("foo", "bar");

    pipe.sync();

    assertThat(result1.get(), equalTo(1L));
    assertThat(range.get(), contains("b"));
    assertThat(result2.get(), equalTo(0L));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 2d, bb);

    Response<Long> bresult1 = pipe.zrem(bfoo, ba);
    Response<List<byte[]>> brange = pipe.zrange(bfoo, 0, 100);
    Response<Long> bresult2 = pipe.zrem(bfoo, bbar);

    pipe.sync();

    assertThat(bresult1.get(), equalTo(1L));
    assertThat(brange.get(), contains(bb));
    assertThat(bresult2.get(), equalTo(0L));
  }

  @Test
  public void zincrby() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 2d, "b");

    Response<Double> result = pipe.zincrby("foo", 2d, "a");
    Response<List<String>> range = pipe.zrange("foo", 0, 100);

    pipe.sync();

    assertThat(result.get(), closeTo(3d, 0.001));
    assertThat(range.get(), contains("b", "a"));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 2d, bb);

    Response<Double> bresult = pipe.zincrby(bfoo, 2d, ba);
    Response<List<byte[]>> brange = pipe.zrange(bfoo, 0, 100);

    pipe.sync();

    assertThat(bresult.get(), closeTo(3d, 0.001));
    assertThat(brange.get(), contains(bb, ba));
  }

  @Test
  public void zincrbyWithParams() {
    pipe.del("foo");

    // xx: never add new member
    Response<Double> result1 = pipe.zincrby("foo", 2d, "a", ZIncrByParams.zIncrByParams().xx());

    pipe.zadd("foo", 2d, "a");

    // nx: never update current member
    Response<Double> result2 = pipe.zincrby("foo", 1d, "a", ZIncrByParams.zIncrByParams().nx());
    Response<Double> result3 = pipe.zscore("foo", "a");

    pipe.sync();

    assertThat(result1.get(), nullValue());
    assertThat(result2.get(), nullValue());
    assertThat(result3.get(), closeTo(2d, 0.001));

    // Binary

    pipe.del(bfoo);

    // xx: never add new member
    Response<Double> bresult1 = pipe.zincrby(bfoo, 2d, ba, ZIncrByParams.zIncrByParams().xx());

    pipe.zadd(bfoo, 2d, ba);

    // nx: never update current member
    Response<Double> bresult2 = pipe.zincrby(bfoo, 1d, ba, ZIncrByParams.zIncrByParams().nx());
    Response<Double> bresult3 = pipe.zscore(bfoo, ba);

    pipe.sync();

    assertThat(bresult1.get(), nullValue());
    assertThat(bresult2.get(), nullValue());
    assertThat(bresult3.get(), closeTo(2d, 0.001));
  }

  @Test
  public void zrank() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 2d, "b");

    pipe.zrank("foo", "a");
    pipe.zrank("foo", "b");
    pipe.zrank("car", "b");

    assertThat(pipe.syncAndReturnAll(), contains(
        equalTo(1L),
        equalTo(1L),
        equalTo(0L),
        equalTo(1L),
        nullValue()
    ));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 2d, bb);

    pipe.zrank(bfoo, ba);
    pipe.zrank(bfoo, bb);
    pipe.zrank(bcar, bb);

    assertThat(pipe.syncAndReturnAll(), contains(
        equalTo(1L),
        equalTo(1L),
        equalTo(0L),
        equalTo(1L),
        nullValue()
    ));
  }

  @Test
  @SinceRedisVersion(value="7.2.0", message = "Starting with Redis version 7.2.0: Added the optional WITHSCORE argument.")
  public void zrankWithScore() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 2d, "b");

    Response<KeyValue<Long, Double>> keyValue1 = pipe.zrankWithScore("foo", "a");
    Response<KeyValue<Long, Double>> keyValue2 = pipe.zrankWithScore("foo", "b");
    Response<KeyValue<Long, Double>> keyValue3 = pipe.zrankWithScore("car", "b");

    pipe.sync();

    assertThat(keyValue1.get(), equalTo(new KeyValue<>(0L, 1d)));
    assertThat(keyValue2.get(), equalTo(new KeyValue<>(1L, 2d)));
    assertThat(keyValue3.get(), nullValue());

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 2d, bb);

    Response<KeyValue<Long, Double>> bKeyValue1 = pipe.zrankWithScore(bfoo, ba);
    Response<KeyValue<Long, Double>> bKeyValue2 = pipe.zrankWithScore(bfoo, bb);
    Response<KeyValue<Long, Double>> bKeyValue3 = pipe.zrankWithScore(bcar, bb);

    pipe.sync();

    assertThat(bKeyValue1.get(), equalTo(new KeyValue<>(0L, 1d)));
    assertThat(bKeyValue2.get(), equalTo(new KeyValue<>(1L, 2d)));
    assertThat(bKeyValue3.get(), nullValue());
  }

  @Test
  public void zrevrank() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 2d, "b");

    pipe.zrevrank("foo", "a");
    pipe.zrevrank("foo", "b");

    assertThat(pipe.syncAndReturnAll(), contains(
        equalTo(1L),
        equalTo(1L),
        equalTo(1L),
        equalTo(0L)
    ));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 2d, bb);
    pipe.zrevrank(bfoo, ba);
    pipe.zrevrank(bfoo, bb);

    assertThat(pipe.syncAndReturnAll(), contains(
        equalTo(1L),
        equalTo(1L),
        equalTo(1L),
        equalTo(0L)
    ));
  }

  @Test
  public void zrangeWithScores() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 10d, "b");
    pipe.zadd("foo", 0.1d, "c");
    pipe.zadd("foo", 2d, "a");

    Response<List<Tuple>> range1 = pipe.zrangeWithScores("foo", 0, 1);
    Response<List<Tuple>> range2 = pipe.zrangeWithScores("foo", 0, 100);

    pipe.sync();

    assertThat(range1.get(), contains(
        new Tuple("c", 0.1d),
        new Tuple("a", 2d)
    ));
    assertThat(range2.get(), contains(
        new Tuple("c", 0.1d),
        new Tuple("a", 2d),
        new Tuple("b", 10d)
    ));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bfoo, 0.1d, bc);
    pipe.zadd(bfoo, 2d, ba);

    Response<List<Tuple>> brange1 = pipe.zrangeWithScores(bfoo, 0, 1);
    Response<List<Tuple>> brange2 = pipe.zrangeWithScores(bfoo, 0, 100);

    pipe.sync();

    assertThat(brange1.get(), contains(
        new Tuple(bc, 0.1d),
        new Tuple(ba, 2d)
    ));
    assertThat(brange2.get(), contains(
        new Tuple(bc, 0.1d),
        new Tuple(ba, 2d),
        new Tuple(bb, 10d)
    ));
  }

  @Test
  public void zrevrangeWithScores() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 10d, "b");
    pipe.zadd("foo", 0.1d, "c");
    pipe.zadd("foo", 2d, "a");

    Response<List<Tuple>> range1 = pipe.zrevrangeWithScores("foo", 0, 1);
    Response<List<Tuple>> range2 = pipe.zrevrangeWithScores("foo", 0, 100);

    pipe.sync();

    assertThat(range1.get(), contains(
        new Tuple("b", 10d),
        new Tuple("a", 2d)
    ));
    assertThat(range2.get(), contains(
        new Tuple("b", 10d),
        new Tuple("a", 2d),
        new Tuple("c", 0.1d)
    ));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bfoo, 0.1d, bc);
    pipe.zadd(bfoo, 2d, ba);

    Response<List<Tuple>> brange1 = pipe.zrevrangeWithScores(bfoo, 0, 1);
    Response<List<Tuple>> brange2 = pipe.zrevrangeWithScores(bfoo, 0, 100);

    pipe.sync();

    assertThat(brange1.get(), contains(
        new Tuple(bb, 10d),
        new Tuple(ba, 2d)
    ));
    assertThat(brange2.get(), contains(
        new Tuple(bb, 10d),
        new Tuple(ba, 2d),
        new Tuple(bc, 0.1d)
    ));
  }

  @Test
  public void zcard() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 10d, "b");
    pipe.zadd("foo", 0.1d, "c");
    pipe.zadd("foo", 2d, "a");

    Response<Long> result = pipe.zcard("foo");

    pipe.sync();

    assertThat(result.get(), equalTo(3L));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bfoo, 0.1d, bc);
    pipe.zadd(bfoo, 2d, ba);

    Response<Long> bresult = pipe.zcard(bfoo);

    pipe.sync();

    assertThat(bresult.get(), equalTo(3L));
  }

  @Test
  public void zscore() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 10d, "b");
    pipe.zadd("foo", 0.1d, "c");
    pipe.zadd("foo", 2d, "a");

    Response<Double> result1 = pipe.zscore("foo", "b");
    Response<Double> result2 = pipe.zscore("foo", "c");
    Response<Double> result3 = pipe.zscore("foo", "s");

    pipe.sync();

    assertThat(result1.get(), closeTo(10d, 0.001));
    assertThat(result2.get(), closeTo(0.1d, 0.001));
    assertThat(result3.get(), nullValue());

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bfoo, 0.1d, bc);
    pipe.zadd(bfoo, 2d, ba);

    Response<Double> bresult1 = pipe.zscore(bfoo, bb);
    Response<Double> bresult2 = pipe.zscore(bfoo, bc);
    Response<Double> bresult3 = pipe.zscore(bfoo, SafeEncoder.encode("s"));

    pipe.sync();

    assertThat(bresult1.get(), closeTo(10d, 0.001));
    assertThat(bresult2.get(), closeTo(0.1d, 0.001));
    assertThat(bresult3.get(), nullValue());
  }

  @Test
  public void zmscore() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 10d, "b");
    pipe.zadd("foo", 0.1d, "c");
    pipe.zadd("foo", 2d, "a");

    Response<List<Double>> score = pipe.zmscore("foo", "b", "c", "s");

    pipe.sync();

    assertThat(score.get(), contains(10d, 0.1d, null));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bfoo, 0.1d, bc);
    pipe.zadd(bfoo, 2d, ba);

    Response<List<Double>> bscore = pipe.zmscore(bfoo, bb, bc, SafeEncoder.encode("s"));

    pipe.sync();

    assertThat(bscore.get(), contains(10d, 0.1d, null));
  }

  @Test
  public void zpopmax() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 10d, "b");
    pipe.zadd("foo", 0.1d, "c");
    pipe.zadd("foo", 2d, "d");

    pipe.sync();

    pipe.zpopmax("foo");
    pipe.zpopmax("foo");
    pipe.zpopmax("foo");
    pipe.zpopmax("foo");
    pipe.zpopmax("foo");

    assertThat(pipe.syncAndReturnAll(), contains(
        equalTo(new Tuple("b", 10d)),
        equalTo(new Tuple("d", 2d)),
        equalTo(new Tuple("a", 1d)),
        equalTo(new Tuple("c", 0.1d)),
        nullValue()
    ));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bfoo, 0.1d, bc);
    pipe.zadd(bfoo, 2d, ba);

    pipe.sync();

    pipe.zpopmax(bfoo);
    pipe.zpopmax(bfoo);
    pipe.zpopmax(bfoo);
    pipe.zpopmax(bfoo);

    assertThat(pipe.syncAndReturnAll(), contains(
        equalTo(new Tuple(bb, 10d)),
        equalTo(new Tuple(ba, 2d)),
        equalTo(new Tuple(bc, 0.1d)),
        nullValue()
    ));
  }

  @Test
  public void zpopmaxWithCount() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 10d, "b");
    pipe.zadd("foo", 0.1d, "c");
    pipe.zadd("foo", 2d, "d");
    pipe.zadd("foo", 0.03, "e");

    Response<List<Tuple>> actual1 = pipe.zpopmax("foo", 2);
    Response<List<Tuple>> actual2 = pipe.zpopmax("foo", 3);
    Response<List<Tuple>> actual3 = pipe.zpopmax("foo", 1);

    pipe.sync();

    assertThat(actual1.get(), contains(
        new Tuple("b", 10d),
        new Tuple("d", 2d)
    ));

    assertThat(actual2.get(), contains(
        new Tuple("a", 1d),
        new Tuple("c", 0.1d),
        new Tuple("e", 0.03d)
    ));

    assertThat(actual3.get(), empty());

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bfoo, 0.1d, bc);
    pipe.zadd(bfoo, 2d, ba);

    Response<List<Tuple>> bactual1 = pipe.zpopmax(bfoo, 1);
    Response<List<Tuple>> bactual2 = pipe.zpopmax(bfoo, 1);
    Response<List<Tuple>> bactual3 = pipe.zpopmax(bfoo, 1);
    Response<List<Tuple>> bactual4 = pipe.zpopmax(bfoo, 1);

    pipe.sync();

    assertThat(bactual1.get(), contains(
        new Tuple(bb, 10d)
    ));

    assertThat(bactual2.get(), contains(
        new Tuple(ba, 2d)
    ));

    assertThat(bactual3.get(), contains(
        new Tuple(bc, 0.1d)
    ));

    assertThat(bactual4.get(), empty());
  }

  @Test
  public void zpopmin() {
    pipe.zadd("foo", 1d, "a", ZAddParams.zAddParams().nx());
    pipe.zadd("foo", 10d, "b", ZAddParams.zAddParams().nx());
    pipe.zadd("foo", 0.1d, "c", ZAddParams.zAddParams().nx());
    pipe.zadd("foo", 2d, "a", ZAddParams.zAddParams().nx());

    Response<List<Tuple>> range = pipe.zpopmin("foo", 2);
    Response<Tuple> item = pipe.zpopmin("foo");

    pipe.sync();

    assertThat(range.get(), contains(
        new Tuple("c", 0.1d),
        new Tuple("a", 1d)
    ));

    assertThat(item.get(), equalTo(new Tuple("b", 10d)));

    // Binary

    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bfoo, 0.1d, bc);
    pipe.zadd(bfoo, 2d, ba);

    Response<List<Tuple>> brange = pipe.zpopmin(bfoo, 2);
    Response<Tuple> bitem = pipe.zpopmin(bfoo);

    pipe.sync();

    assertThat(brange.get(), contains(
        new Tuple(bc, 0.1d),
        new Tuple(ba, 2d)
    ));

    assertThat(bitem.get(), equalTo(new Tuple(bb, 10d)));
  }

  @Test
  public void zcount() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 10d, "b");
    pipe.zadd("foo", 0.1d, "c");
    pipe.zadd("foo", 2d, "a");

    pipe.sync();

    pipe.zcount("foo", 0.01d, 2.1d);
    pipe.zcount("foo", "(0.01", "+inf");

    assertThat(pipe.syncAndReturnAll(), contains(
        2L,
        3L
    ));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bfoo, 0.1d, bc);
    pipe.zadd(bfoo, 2d, ba);

    pipe.sync();

    pipe.zcount(bfoo, 0.01d, 2.1d);
    pipe.zcount(bfoo, SafeEncoder.encode("(0.01"), SafeEncoder.encode("+inf"));

    assertThat(pipe.syncAndReturnAll(), contains(
        2L,
        3L
    ));
  }

  @Test
  public void zlexcount() {
    pipe.zadd("foo", 1, "a");
    pipe.zadd("foo", 1, "b");
    pipe.zadd("foo", 1, "c");
    pipe.zadd("foo", 1, "aa");

    pipe.sync();

    pipe.zlexcount("foo", "[aa", "(c");
    pipe.zlexcount("foo", "-", "+");
    pipe.zlexcount("foo", "-", "(c");
    pipe.zlexcount("foo", "[aa", "+");

    assertThat(pipe.syncAndReturnAll(), contains(
        2L,
        4L,
        3L,
        3L
    ));
  }

  @Test
  public void zlexcountBinary() {
    // Binary
    pipe.zadd(bfoo, 1, ba);
    pipe.zadd(bfoo, 1, bc);
    pipe.zadd(bfoo, 1, bb);

    pipe.sync();

    pipe.zlexcount(bfoo, bInclusiveB, bExclusiveC);
    pipe.zlexcount(bfoo, bLexMinusInf, bLexPlusInf);

    assertThat(pipe.syncAndReturnAll(), contains(
        1L,
        3L
    ));
  }

  @Test
  public void zrangebyscore() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 10d, "b");
    pipe.zadd("foo", 0.1d, "c");
    pipe.zadd("foo", 2d, "a");

    Response<List<String>> range1 = pipe.zrangeByScore("foo", 0d, 2d);
    Response<List<String>> range2 = pipe.zrangeByScore("foo", 0d, 2d, 0, 1);
    Response<List<String>> range3 = pipe.zrangeByScore("foo", 0d, 2d, 1, 1);
    Response<List<String>> range4 = pipe.zrangeByScore("foo", "-inf", "(2");

    pipe.sync();

    assertThat(range1.get(), contains("c", "a"));
    assertThat(range2.get(), contains("c"));
    assertThat(range3.get(), contains("a"));
    assertThat(range4.get(), contains("c"));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bfoo, 0.1d, bc);
    pipe.zadd(bfoo, 2d, ba);

    Response<List<byte[]>> brange1 = pipe.zrangeByScore(bfoo, 0d, 2d);
    Response<List<byte[]>> brange2 = pipe.zrangeByScore(bfoo, 0d, 2d, 0, 1);
    Response<List<byte[]>> brange3 = pipe.zrangeByScore(bfoo, 0d, 2d, 1, 1);
    Response<List<byte[]>> brange4 = pipe.zrangeByScore(bfoo, SafeEncoder.encode("-inf"), SafeEncoder.encode("(2"));

    pipe.sync();

    assertThat(brange1.get(), contains(bc, ba));
    assertThat(brange2.get(), contains(bc));
    assertThat(brange3.get(), contains(ba));
    assertThat(brange4.get(), contains(bc));
  }

  @Test
  public void zrevrangebyscore() {
    pipe.zadd("foo", 1.0d, "a");
    pipe.zadd("foo", 2.0d, "b");
    pipe.zadd("foo", 3.0d, "c");
    pipe.zadd("foo", 4.0d, "d");
    pipe.zadd("foo", 5.0d, "e");

    Response<List<String>> range1 = pipe.zrevrangeByScore("foo", 3d, Double.NEGATIVE_INFINITY, 0, 1);
    Response<List<String>> range2 = pipe.zrevrangeByScore("foo", 3.5d, Double.NEGATIVE_INFINITY, 0, 2);
    Response<List<String>> range3 = pipe.zrevrangeByScore("foo", 3.5d, Double.NEGATIVE_INFINITY, 1, 1);
    Response<List<String>> range4 = pipe.zrevrangeByScore("foo", 4d, 2d);
    Response<List<String>> range5 = pipe.zrevrangeByScore("foo", "4", "2", 0, 2);
    Response<List<String>> range6 = pipe.zrevrangeByScore("foo", "+inf", "(4");

    pipe.sync();

    assertThat(range1.get(), contains("c"));
    assertThat(range2.get(), contains("c", "b"));
    assertThat(range3.get(), contains("b"));
    assertThat(range4.get(), contains("d", "c", "b"));
    assertThat(range5.get(), contains("d", "c"));
    assertThat(range6.get(), contains("e"));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bfoo, 0.1d, bc);
    pipe.zadd(bfoo, 2d, ba);

    Response<List<byte[]>> brange1 = pipe.zrevrangeByScore(bfoo, 2d, 0d);
    Response<List<byte[]>> brange2 = pipe.zrevrangeByScore(bfoo, 2d, 0d, 0, 1);
    Response<List<byte[]>> brange3 = pipe.zrevrangeByScore(bfoo, SafeEncoder.encode("+inf"), SafeEncoder.encode("(2"));
    Response<List<byte[]>> brange4 = pipe.zrevrangeByScore(bfoo, 2d, 0d, 1, 1);

    pipe.sync();

    assertThat(brange1.get(), contains(ba, bc));
    assertThat(brange2.get(), contains(ba));
    assertThat(brange3.get(), contains(bb));
    assertThat(brange4.get(), contains(bc));
  }

  @Test
  public void zrangebyscoreWithScores() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 10d, "b");
    pipe.zadd("foo", 0.1d, "c");
    pipe.zadd("foo", 2d, "a");

    Response<List<Tuple>> range1 = pipe.zrangeByScoreWithScores("foo", 0d, 2d);
    Response<List<Tuple>> range2 = pipe.zrangeByScoreWithScores("foo", 0d, 2d, 0, 1);
    Response<List<Tuple>> range3 = pipe.zrangeByScoreWithScores("foo", 0d, 2d, 1, 1);

    pipe.sync();

    assertThat(range1.get(), contains(
        new Tuple("c", 0.1d),
        new Tuple("a", 2d)
    ));

    assertThat(range2.get(), contains(
        new Tuple("c", 0.1d)
    ));

    assertThat(range3.get(), contains(
        new Tuple("a", 2d)
    ));

    // Binary

    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bfoo, 0.1d, bc);
    pipe.zadd(bfoo, 2d, ba);

    Response<List<Tuple>> brange1 = pipe.zrangeByScoreWithScores(bfoo, 0d, 2d);
    Response<List<Tuple>> brange2 = pipe.zrangeByScoreWithScores(bfoo, 0d, 2d, 0, 1);
    Response<List<Tuple>> brange3 = pipe.zrangeByScoreWithScores(bfoo, 0d, 2d, 1, 1);

    pipe.sync();

    assertThat(brange1.get(), contains(
        new Tuple(bc, 0.1d),
        new Tuple(ba, 2d)
    ));

    assertThat(brange2.get(), contains(
        new Tuple(bc, 0.1d)
    ));

    assertThat(brange3.get(), contains(
        new Tuple(ba, 2d)
    ));
  }

  @Test
  public void zrevrangebyscoreWithScores() {
    pipe.zadd("foo", 1.0d, "a");
    pipe.zadd("foo", 2.0d, "b");
    pipe.zadd("foo", 3.0d, "c");
    pipe.zadd("foo", 4.0d, "d");
    pipe.zadd("foo", 5.0d, "e");

    Response<List<Tuple>> range1 = pipe.zrevrangeByScoreWithScores("foo", 3d, Double.NEGATIVE_INFINITY, 0, 1);
    Response<List<Tuple>> range2 = pipe.zrevrangeByScoreWithScores("foo", 3.5d, Double.NEGATIVE_INFINITY, 0, 2);
    Response<List<Tuple>> range3 = pipe.zrevrangeByScoreWithScores("foo", 3.5d, Double.NEGATIVE_INFINITY, 1, 1);
    Response<List<Tuple>> range4 = pipe.zrevrangeByScoreWithScores("foo", 4d, 2d);

    pipe.sync();

    assertThat(range1.get(), contains(
        new Tuple("c", 3.0d)
    ));

    assertThat(range2.get(), contains(
        new Tuple("c", 3.0d),
        new Tuple("b", 2.0d)
    ));

    assertThat(range3.get(), contains(
        new Tuple("b", 2.0d)
    ));

    assertThat(range4.get(), contains(
        new Tuple("d", 4.0d),
        new Tuple("c", 3.0d),
        new Tuple("b", 2.0d)
    ));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bfoo, 0.1d, bc);
    pipe.zadd(bfoo, 2d, ba);

    Response<List<Tuple>> brange1 = pipe.zrevrangeByScoreWithScores(bfoo, 2d, 0d);
    Response<List<Tuple>> brange2 = pipe.zrevrangeByScoreWithScores(bfoo, 2d, 0d, 0, 1);
    Response<List<Tuple>> brange3 = pipe.zrevrangeByScoreWithScores(bfoo, 2d, 0d, 1, 1);

    pipe.sync();

    assertThat(brange1.get(), contains(
        new Tuple(ba, 2d),
        new Tuple(bc, 0.1d)
    ));

    assertThat(brange2.get(), contains(
        new Tuple(ba, 2d)
    ));

    assertThat(brange3.get(), contains(
        new Tuple(bc, 0.1d)
    ));
  }

  @Test
  public void zremrangeByRank() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 10d, "b");
    pipe.zadd("foo", 0.1d, "c");
    pipe.zadd("foo", 2d, "a");

    Response<Long> result = pipe.zremrangeByRank("foo", 0, 0);
    Response<List<String>> items = pipe.zrange("foo", 0, 100);

    pipe.sync();

    assertThat(result.get(), equalTo(1L));
    assertThat(items.get(), contains("a", "b"));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bfoo, 0.1d, bc);
    pipe.zadd(bfoo, 2d, ba);

    Response<Long> bresult = pipe.zremrangeByRank(bfoo, 0, 0);
    Response<List<byte[]>> bitems = pipe.zrange(bfoo, 0, 100);

    pipe.sync();

    assertThat(bresult.get(), equalTo(1L));
    assertThat(bitems.get(), contains(ba, bb));
  }

  @Test
  public void zremrangeByScore() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 10d, "b");
    pipe.zadd("foo", 0.1d, "c");
    pipe.zadd("foo", 2d, "a");

    Response<Long> result = pipe.zremrangeByScore("foo", 0, 2);
    Response<List<String>> items = pipe.zrange("foo", 0, 100);

    pipe.sync();

    assertThat(result.get(), equalTo(2L));
    assertThat(items.get(), contains("b"));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bfoo, 0.1d, bc);
    pipe.zadd(bfoo, 2d, ba);

    Response<Long> bresult = pipe.zremrangeByScore(bfoo, 0, 2);
    Response<List<byte[]>> bitems = pipe.zrange(bfoo, 0, 100);

    pipe.sync();

    assertThat(bresult.get(), equalTo(2L));
    assertThat(bitems.get(), contains(bb));
  }

  @Test
  public void zremrangeByScoreExclusive() {
    pipe.zadd("foo", 1d, "a");
    pipe.zadd("foo", 0d, "c");
    pipe.zadd("foo", 2d, "b");

    Response<Long> result = pipe.zremrangeByScore("foo", "(0", "(2");

    pipe.sync();

    assertThat(result.get(), equalTo(1L));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 0d, bc);
    pipe.zadd(bfoo, 2d, bb);

    Response<Long> bresult = pipe.zremrangeByScore(bfoo, "(0".getBytes(), "(2".getBytes());

    pipe.sync();

    assertThat(bresult.get(), equalTo(1L));
  }

  @Test
  public void zremrangeByLex() {
    pipe.zadd("foo", 1, "a");
    pipe.zadd("foo", 1, "b");
    pipe.zadd("foo", 1, "c");
    pipe.zadd("foo", 1, "aa");

    Response<Long> result = pipe.zremrangeByLex("foo", "[aa", "(c");
    Response<List<String>> items = pipe.zrangeByLex("foo", "-", "+");

    pipe.sync();

    assertThat(result.get(), equalTo(2L));
    assertThat(items.get(), contains("a", "c"));
  }

  @Test
  public void zremrangeByLexBinary() {
    pipe.zadd(bfoo, 1, ba);
    pipe.zadd(bfoo, 1, bc);
    pipe.zadd(bfoo, 1, bb);

    Response<Long> bresult = pipe.zremrangeByLex(bfoo, bInclusiveB, bExclusiveC);
    Response<List<byte[]>> bitems = pipe.zrangeByLex(bfoo, bLexMinusInf, bLexPlusInf);

    pipe.sync();

    assertThat(bresult.get(), equalTo(1L));
    assertThat(bitems.get(), contains(ba, bc));
  }

  @Test
  public void zunion() {
    pipe.zadd("foo", 1, "a");
    pipe.zadd("foo", 2, "b");
    pipe.zadd("bar", 2, "a");
    pipe.zadd("bar", 2, "b");

    ZParams params = new ZParams();
    params.weights(2, 2.5);
    params.aggregate(ZParams.Aggregate.SUM);

    Response<List<String>> union1 = pipe.zunion(params, "foo", "bar");
    Response<List<Tuple>> union2 = pipe.zunionWithScores(params, "foo", "bar");

    pipe.sync();

    assertThat(union1.get(), contains("a", "b"));
    assertThat(union2.get(), contains(
        new Tuple("a", new Double(7)),
        new Tuple("b", new Double(9))
    ));

    // Binary
    pipe.zadd(bfoo, 1, ba);
    pipe.zadd(bfoo, 2, bb);
    pipe.zadd(bbar, 2, ba);
    pipe.zadd(bbar, 2, bb);

    Response<List<byte[]>> bunion1 = pipe.zunion(params, bfoo, bbar);
    Response<List<Tuple>> bunion2 = pipe.zunionWithScores(params, bfoo, bbar);

    pipe.sync();

    assertThat(bunion1.get(), contains(ba, bb));
    assertThat(bunion2.get(), contains(
        new Tuple(ba, new Double(7)),
        new Tuple(bb, new Double(9))
    ));
  }

  @Test
  public void zunionstore() {
    pipe.zadd("foo", 1, "a");
    pipe.zadd("foo", 2, "b");
    pipe.zadd("bar", 2, "a");
    pipe.zadd("bar", 2, "b");

    Response<Long> result = pipe.zunionstore("dst", "foo", "bar");
    Response<List<Tuple>> items = pipe.zrangeWithScores("dst", 0, 100);

    pipe.sync();

    assertThat(result.get(), equalTo(2L));
    assertThat(items.get(), contains(
        new Tuple("a", new Double(3)),
        new Tuple("b", new Double(4))
    ));

    // Binary
    pipe.zadd(bfoo, 1, ba);
    pipe.zadd(bfoo, 2, bb);
    pipe.zadd(bbar, 2, ba);
    pipe.zadd(bbar, 2, bb);

    Response<Long> bresult = pipe.zunionstore(SafeEncoder.encode("dst"), bfoo, bbar);
    Response<List<Tuple>> bitems = pipe.zrangeWithScores(SafeEncoder.encode("dst"), 0, 100);

    pipe.sync();

    assertThat(bresult.get(), equalTo(2L));
    assertThat(bitems.get(), contains(
        new Tuple(ba, new Double(3)),
        new Tuple(bb, new Double(4))
    ));
  }

  @Test
  public void zunionstoreParams() {
    pipe.zadd("foo", 1, "a");
    pipe.zadd("foo", 2, "b");
    pipe.zadd("bar", 2, "a");
    pipe.zadd("bar", 2, "b");

    ZParams params = new ZParams();
    params.weights(2, 2.5);
    params.aggregate(ZParams.Aggregate.SUM);

    Response<Long> result = pipe.zunionstore("dst", params, "foo", "bar");
    Response<List<Tuple>> items = pipe.zrangeWithScores("dst", 0, 100);

    pipe.sync();

    assertThat(result.get(), equalTo(2L));
    assertThat(items.get(), contains(
        new Tuple("a", new Double(7)),
        new Tuple("b", new Double(9))
    ));

    // Binary
    pipe.zadd(bfoo, 1, ba);
    pipe.zadd(bfoo, 2, bb);
    pipe.zadd(bbar, 2, ba);
    pipe.zadd(bbar, 2, bb);

    ZParams bparams = new ZParams();
    bparams.weights(2, 2.5);
    bparams.aggregate(ZParams.Aggregate.SUM);

    Response<Long> bresult = pipe.zunionstore(SafeEncoder.encode("dst"), bparams, bfoo, bbar);
    Response<List<Tuple>> bitems = pipe.zrangeWithScores(SafeEncoder.encode("dst"), 0, 100);

    pipe.sync();

    assertThat(bresult.get(), equalTo(2L));
    assertThat(bitems.get(), contains(
        new Tuple(ba, new Double(7)),
        new Tuple(bb, new Double(9))
    ));
  }

  @Test
  public void zinter() {
    pipe.zadd("foo", 1, "a");
    pipe.zadd("foo", 2, "b");
    pipe.zadd("bar", 2, "a");

    ZParams params = new ZParams();
    params.weights(2, 2.5);
    params.aggregate(ZParams.Aggregate.SUM);
    Response<List<String>> inter1 = pipe.zinter(params, "foo", "bar");

    Response<List<Tuple>> inter2 = pipe.zinterWithScores(params, "foo", "bar");

    pipe.sync();

    assertThat(inter1.get(), contains("a"));
    assertThat(inter2.get(), contains(new Tuple("a", new Double(7))));

    // Binary
    pipe.zadd(bfoo, 1, ba);
    pipe.zadd(bfoo, 2, bb);
    pipe.zadd(bbar, 2, ba);

    ZParams bparams = new ZParams();
    bparams.weights(2, 2.5);
    bparams.aggregate(ZParams.Aggregate.SUM);
    Response<List<byte[]>> binter1 = pipe.zinter(params, bfoo, bbar);

    Response<List<Tuple>> binter2 = pipe.zinterWithScores(bparams, bfoo, bbar);

    pipe.sync();

    assertThat(binter1.get(), contains(ba));
    assertThat(binter2.get(), contains(new Tuple(ba, new Double(7))));
  }

  @Test
  public void zinterstore() {
    pipe.zadd("foo", 1, "a");
    pipe.zadd("foo", 2, "b");
    pipe.zadd("bar", 2, "a");

    Response<Long> result1 = pipe.zinterstore("dst", "foo", "bar");
    Response<List<Tuple>> items1 = pipe.zrangeWithScores("dst", 0, 100);

    pipe.sync();

    assertThat(result1.get(), equalTo(1L));
    assertThat(items1.get(), contains(new Tuple("a", new Double(3))));

    // Binary
    pipe.zadd(bfoo, 1, ba);
    pipe.zadd(bfoo, 2, bb);
    pipe.zadd(bbar, 2, ba);

    Response<Long> bresult1 = pipe.zinterstore(SafeEncoder.encode("dst"), bfoo, bbar);
    Response<List<Tuple>> bitems1 = pipe.zrangeWithScores(SafeEncoder.encode("dst"), 0, 100);

    pipe.sync();

    assertThat(bresult1.get(), equalTo(1L));
    assertThat(bitems1.get(), contains(new Tuple(ba, new Double(3))));
  }

  @Test
  public void zintertoreParams() {
    pipe.zadd("foo", 1, "a");
    pipe.zadd("foo", 2, "b");
    pipe.zadd("bar", 2, "a");

    ZParams params = new ZParams();
    params.weights(2, 2.5);
    params.aggregate(ZParams.Aggregate.SUM);
    Response<Long> result1 = pipe.zinterstore("dst", params, "foo", "bar");

    Response<List<Tuple>> items1 = pipe.zrangeWithScores("dst", 0, 100);

    pipe.sync();

    assertThat(result1.get(), equalTo(1L));
    assertThat(items1.get(), contains(new Tuple("a", new Double(7))));

    // Binary
    pipe.zadd(bfoo, 1, ba);
    pipe.zadd(bfoo, 2, bb);
    pipe.zadd(bbar, 2, ba);

    ZParams bparams = new ZParams();
    bparams.weights(2, 2.5);
    bparams.aggregate(ZParams.Aggregate.SUM);
    Response<Long> bresult1 = pipe.zinterstore(SafeEncoder.encode("dst"), bparams, bfoo, bbar);

    Response<List<Tuple>> bitems1 = pipe.zrangeWithScores(SafeEncoder.encode("dst"), 0, 100);

    pipe.sync();

    assertThat(bresult1.get(), equalTo(1L));
    assertThat(bitems1.get(), contains(new Tuple(ba, new Double(7))));
  }

  @Test
  @SinceRedisVersion(value="7.0.0")
  public void zintercard() {
    pipe.zadd("foo", 1, "a");
    pipe.zadd("foo", 2, "b");
    pipe.zadd("bar", 2, "a");
    pipe.zadd("bar", 1, "b");

    Response<Long> result1 = pipe.zintercard("foo", "bar");
    Response<Long> result2 = pipe.zintercard(1, "foo", "bar");

    pipe.sync();

    assertThat(result1.get(), equalTo(2L));
    assertThat(result2.get(), equalTo(1L));

    // Binary
    pipe.zadd(bfoo, 1, ba);
    pipe.zadd(bfoo, 2, bb);
    pipe.zadd(bbar, 2, ba);
    pipe.zadd(bbar, 2, bb);

    Response<Long> bresult1 = pipe.zintercard(bfoo, bbar);
    Response<Long> bresult2 = pipe.zintercard(1, bfoo, bbar);

    pipe.sync();

    assertThat(bresult1.get(), equalTo(2L));
    assertThat(bresult2.get(), equalTo(1L));
  }

  @Test
  public void zscan() {
    pipe.zadd("foo", 1, "a");
    pipe.zadd("foo", 2, "b");

    Response<ScanResult<Tuple>> result = pipe.zscan("foo", SCAN_POINTER_START);

    pipe.sync();

    assertThat(result.get().getCursor(), equalTo(SCAN_POINTER_START));
    assertThat(result.get().getResult().stream().map(Tuple::getElement).collect(Collectors.toList()),
        containsInAnyOrder("a", "b"));

    // binary
    pipe.zadd(bfoo, 1, ba);
    pipe.zadd(bfoo, 1, bb);

    Response<ScanResult<Tuple>> bResult = pipe.zscan(bfoo, SCAN_POINTER_START_BINARY);

    pipe.sync();

    assertThat(bResult.get().getCursor(), equalTo(SCAN_POINTER_START));
    assertThat(bResult.get().getResult().stream().map(Tuple::getBinaryElement).collect(Collectors.toList()),
        containsInAnyOrder(ba, bb));
  }

  @Test
  public void zscanMatch() {
    ScanParams params = new ScanParams();
    params.match("a*");

    pipe.zadd("foo", 2, "b");
    pipe.zadd("foo", 1, "a");
    pipe.zadd("foo", 11, "aa");
    Response<ScanResult<Tuple>> result = pipe.zscan("foo", SCAN_POINTER_START, params);

    pipe.sync();

    assertThat(result.get().getCursor(), equalTo(SCAN_POINTER_START));
    assertThat(result.get().getResult().stream().map(Tuple::getElement).collect(Collectors.toList()),
        containsInAnyOrder("a", "aa"));

    // binary
    params = new ScanParams();
    params.match(bbarstar);

    pipe.zadd(bfoo, 2, bbar1);
    pipe.zadd(bfoo, 1, bbar2);
    pipe.zadd(bfoo, 11, bbar3);
    Response<ScanResult<Tuple>> bResult = pipe.zscan(bfoo, SCAN_POINTER_START_BINARY, params);

    pipe.sync();

    assertThat(bResult.get().getCursor(), equalTo(SCAN_POINTER_START));
    assertThat(bResult.get().getResult().stream().map(Tuple::getBinaryElement).collect(Collectors.toList()),
        containsInAnyOrder(bbar1, bbar2, bbar3));
  }

  @Test
  public void zscanCount() {
    ScanParams params = new ScanParams();
    params.count(2);

    pipe.zadd("foo", 1, "a1");
    pipe.zadd("foo", 2, "a2");
    pipe.zadd("foo", 3, "a3");
    pipe.zadd("foo", 4, "a4");
    pipe.zadd("foo", 5, "a5");

    Response<ScanResult<Tuple>> result = pipe.zscan("foo", SCAN_POINTER_START, params);

    pipe.sync();

    assertThat(result.get().getResult(), not(empty()));

    // binary
    params = new ScanParams();
    params.count(2);

    pipe.zadd(bfoo, 2, bbar1);
    pipe.zadd(bfoo, 1, bbar2);
    pipe.zadd(bfoo, 11, bbar3);

    Response<ScanResult<Tuple>> bResult = pipe.zscan(bfoo, SCAN_POINTER_START_BINARY, params);

    pipe.sync();

    assertThat(bResult.get().getResult(), not(empty()));
  }

  @Test
  public void infinity() {
    pipe.zadd("key", Double.POSITIVE_INFINITY, "pos");

    Response<Double> score1 = pipe.zscore("key", "pos");

    pipe.zadd("key", Double.NEGATIVE_INFINITY, "neg");

    Response<Double> score2 = pipe.zscore("key", "neg");

    pipe.zadd("key", 0d, "zero");

    Response<List<Tuple>> set = pipe.zrangeWithScores("key", 0, -1);

    pipe.sync();

    assertThat(score1.get(), equalTo(Double.POSITIVE_INFINITY));
    assertThat(score2.get(), equalTo(Double.NEGATIVE_INFINITY));
    assertThat(set.get().stream().map(Tuple::getScore).collect(Collectors.toList()),
        contains(Double.NEGATIVE_INFINITY, 0d, Double.POSITIVE_INFINITY));
  }

  @Test
  public void bzpopmax() {
    pipe.zadd("foo", 1d, "a", ZAddParams.zAddParams().nx());
    pipe.zadd("foo", 10d, "b", ZAddParams.zAddParams().nx());
    pipe.zadd("bar", 0.1d, "c", ZAddParams.zAddParams().nx());

    Response<KeyValue<String, Tuple>> item1 = pipe.bzpopmax(0, "foo", "bar");

    pipe.sync();

    assertThat(item1.get().getKey(), equalTo("foo"));
    assertThat(item1.get().getValue(), equalTo(new Tuple("b", 10d)));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bbar, 0.1d, bc);

    Response<KeyValue<byte[], Tuple>> bitem1 = pipe.bzpopmax(0, bfoo, bbar);

    pipe.sync();

    assertThat(bitem1.get().getKey(), equalTo(bfoo));
    assertThat(bitem1.get().getValue(), equalTo(new Tuple(bb, 10d)));
  }

  @Test
  public void bzpopmin() {
    pipe.zadd("foo", 1d, "a", ZAddParams.zAddParams().nx());
    pipe.zadd("foo", 10d, "b", ZAddParams.zAddParams().nx());
    pipe.zadd("bar", 0.1d, "c", ZAddParams.zAddParams().nx());

    Response<KeyValue<String, Tuple>> item1 = pipe.bzpopmin(0, "bar", "foo");

    pipe.sync();

    assertThat(item1.get(), equalTo(new KeyValue<>("bar", new Tuple("c", 0.1))));

    // Binary
    pipe.zadd(bfoo, 1d, ba);
    pipe.zadd(bfoo, 10d, bb);
    pipe.zadd(bbar, 0.1d, bc);

    Response<KeyValue<byte[], Tuple>> bitem1 = pipe.bzpopmin(0, bbar, bfoo);

    pipe.sync();

    assertThat(bitem1.get().getKey(), equalTo(bbar));
    assertThat(bitem1.get().getValue(), equalTo(new Tuple(bc, 0.1)));
  }

  @Test
  public void zdiff() {
    pipe.zadd("foo", 1.0, "a");
    pipe.zadd("foo", 2.0, "b");
    pipe.zadd("bar", 1.0, "a");

    Response<List<String>> diff1 = pipe.zdiff("bar1", "bar2");
    Response<List<String>> diff2 = pipe.zdiff("foo", "bar");
    Response<List<Tuple>> diff3 = pipe.zdiffWithScores("foo", "bar");

    pipe.sync();

    assertThat(diff1.get(), empty());
    assertThat(diff2.get(), contains("b"));
    assertThat(diff3.get(), contains(new Tuple("b", 2.0d)));

    // binary
    pipe.zadd(bfoo, 1.0, ba);
    pipe.zadd(bfoo, 2.0, bb);
    pipe.zadd(bbar, 1.0, ba);

    Response<List<byte[]>> bdiff1 = pipe.zdiff(bbar1, bbar2);
    Response<List<byte[]>> bdiff2 = pipe.zdiff(bfoo, bbar);
    Response<List<Tuple>> bdiff3 = pipe.zdiffWithScores(bfoo, bbar);

    pipe.sync();

    assertThat(bdiff1.get(), empty());
    assertThat(bdiff2.get(), contains(bb));
    assertThat(bdiff3.get(), contains(new Tuple(bb, 2.0d)));
  }

  @Test
  public void zdiffstore() {
    pipe.zadd("foo", 1.0, "a");
    pipe.zadd("foo", 2.0, "b");
    pipe.zadd("bar", 1.0, "a");

    Response<Long> result1 = pipe.zdiffstore("bar3", "bar1", "bar2");
    Response<Long> result2 = pipe.zdiffstore("bar3", "foo", "bar");
    Response<List<String>> items = pipe.zrange("bar3", 0, -1);

    pipe.sync();

    assertThat(result1.get(), equalTo(0L));
    assertThat(result2.get(), equalTo(1L));
    assertThat(items.get(), contains("b"));

    // binary
    pipe.zadd(bfoo, 1.0, ba);
    pipe.zadd(bfoo, 2.0, bb);
    pipe.zadd(bbar, 1.0, ba);

    Response<Long> bresult1 = pipe.zdiffstore(bbar3, bbar1, bbar2);
    Response<Long> bresult2 = pipe.zdiffstore(bbar3, bfoo, bbar);
    Response<List<byte[]>> bitems = pipe.zrange(bbar3, 0, -1);

    pipe.sync();

    assertThat(bresult1.get(), equalTo(0L));
    assertThat(bresult2.get(), equalTo(1L));
    assertThat(bitems.get(), contains(bb));
  }

  @Test
  public void zrandmember() {
    Response<String> item1 = pipe.zrandmember("foo");
    Response<List<String>> items1 = pipe.zrandmember("foo", 1);
    Response<List<Tuple>> items2 = pipe.zrandmemberWithScores("foo", 1);

    pipe.sync();

    assertThat(item1.get(), nullValue());
    assertThat(items1.get(), empty());
    assertThat(items2.get(), empty());

    Map<String, Double> hash = new HashMap<>();
    hash.put("bar1", 1d);
    hash.put("bar2", 10d);
    hash.put("bar3", 0.1d);
    pipe.zadd("foo", hash);

    Response<String> item2 = pipe.zrandmember("foo");
    Response<List<String>> items3 = pipe.zrandmember("foo", 2);
    Response<List<Tuple>> items4 = pipe.zrandmemberWithScores("foo", 2);

    pipe.sync();

    assertThat(item2.get(), in(hash.keySet()));
    assertThat(items3.get(), hasSize(2));
    assertThat(items4.get(), hasSize(2));
    items4.get().forEach(t -> assertEquals(hash.get(t.getElement()), t.getScore(), 0d));

    // Binary
    Response<byte[]> bitem1 = pipe.zrandmember(bfoo);
    Response<List<byte[]>> bitems1 = pipe.zrandmember(bfoo, 1);
    Response<List<Tuple>> bitems2 = pipe.zrandmemberWithScores(bfoo, 1);

    pipe.sync();

    assertThat(bitem1.get(), nullValue());
    assertThat(bitems1.get(), empty());
    assertThat(bitems2.get(), empty());

    Map<byte[], Double> bhash = new HashMap<>();
    bhash.put(bbar1, 1d);
    bhash.put(bbar2, 10d);
    bhash.put(bbar3, 0.1d);
    pipe.zadd(bfoo, bhash);

    Response<byte[]> bitem2 = pipe.zrandmember(bfoo);
    Response<List<byte[]>> bitems3 = pipe.zrandmember(bfoo, 2);
    Response<List<Tuple>> bitems4 = pipe.zrandmemberWithScores(bfoo, 2);

    pipe.sync();

    AssertUtil.assertByteArrayCollectionContains(bhash.keySet(), bitem2.get());
    assertThat(bitems3.get(), hasSize(2));
    assertThat(bitems4.get(), hasSize(2));
    bitems4.get().forEach(t -> assertEquals(getScoreFromByteMap(bhash, t.getBinaryElement()), t.getScore(), 0d));
  }

  private Double getScoreFromByteMap(Map<byte[], Double> bhash, byte[] key) {
    for (Map.Entry<byte[], Double> en : bhash.entrySet()) {
      if (Arrays.equals(en.getKey(), key)) {
        return en.getValue();
      }
    }
    return null;
  }

  @Test
  @SinceRedisVersion(value="7.0.0")
  public void zmpop() {
    pipe.zadd("foo", 1d, "a", ZAddParams.zAddParams().nx());
    pipe.zadd("foo", 10d, "b", ZAddParams.zAddParams().nx());
    pipe.zadd("foo", 0.1d, "c", ZAddParams.zAddParams().nx());
    pipe.zadd("foo", 2d, "a", ZAddParams.zAddParams().nx());

    Response<KeyValue<String, List<Tuple>>> single = pipe.zmpop(SortedSetOption.MAX, "foo");
    Response<KeyValue<String, List<Tuple>>> range = pipe.zmpop(SortedSetOption.MIN, 2, "foo");
    Response<KeyValue<String, List<Tuple>>> nullRange = pipe.zmpop(SortedSetOption.MAX, "foo");

    pipe.sync();

    assertThat(single.get().getValue(), contains(new Tuple("b", 10d)));

    assertThat(range.get().getValue(), contains(
        new Tuple("c", 0.1d),
        new Tuple("a", 1d)
    ));

    assertThat(nullRange.get(), nullValue());
  }

  @Test
  @SinceRedisVersion(value="7.0.0")
  public void bzmpopSimple() {
    pipe.zadd("foo", 1d, "a", ZAddParams.zAddParams().nx());
    pipe.zadd("foo", 10d, "b", ZAddParams.zAddParams().nx());
    pipe.zadd("foo", 0.1d, "c", ZAddParams.zAddParams().nx());
    pipe.zadd("foo", 2d, "a", ZAddParams.zAddParams().nx());

    Response<KeyValue<String, List<Tuple>>> single = pipe.bzmpop(1L, SortedSetOption.MAX, "foo");
    Response<KeyValue<String, List<Tuple>>> range = pipe.bzmpop(1L, SortedSetOption.MIN, 2, "foo");
    Response<KeyValue<String, List<Tuple>>> nullRange = pipe.bzmpop(1L, SortedSetOption.MAX, "foo");

    pipe.sync();

    assertThat(single.get().getValue(), contains(new Tuple("b", 10d)));

    assertThat(range.get().getValue(), contains(
        new Tuple("c", 0.1d),
        new Tuple("a", 1d)
    ));

    assertThat(nullRange.get(), nullValue());
  }
}
