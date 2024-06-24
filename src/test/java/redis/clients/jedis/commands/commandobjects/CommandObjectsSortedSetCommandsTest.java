package redis.clients.jedis.commands.commandobjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.SortedSetOption;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;
import redis.clients.jedis.params.ZParams;
import redis.clients.jedis.params.ZRangeParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.util.KeyValue;

/**
 * Tests related to <a href="https://redis.io/commands/?group=sorted-set">Sorted set</a> commands.
 */
public class CommandObjectsSortedSetCommandsTest extends CommandObjectsStandaloneTestBase {

  public CommandObjectsSortedSetCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testZaddAndZcard() {
    String key = "zset";
    String member = "member1";
    double score = 1.0;

    Map<String, Double> scoreMembers = new HashMap<>();
    scoreMembers.put("member2", 2.0);
    scoreMembers.put("member3", 3.0);

    ZAddParams params = ZAddParams.zAddParams().nx();

    Long zadd = exec(commandObjects.zadd(key, score, member));
    assertThat(zadd, equalTo(1L));

    Long zaddParams = exec(commandObjects.zadd(key, score, member, params));
    assertThat(zaddParams, equalTo(0L));

    Long zaddMultiple = exec(commandObjects.zadd(key, scoreMembers));
    assertThat(zaddMultiple, equalTo(2L));

    Long zaddMultipleParams = exec(commandObjects.zadd(key, scoreMembers, params));
    assertThat(zaddMultipleParams, equalTo(0L));

    Long zcard = exec(commandObjects.zcard(key));
    assertThat(zcard, equalTo(3L));
  }

  @Test
  public void testZaddAndZcardBinary() {
    byte[] key = "zset".getBytes();
    byte[] member = "member1".getBytes();
    double score = 1.0;

    Map<byte[], Double> binaryScoreMembers = new HashMap<>();
    binaryScoreMembers.put("member2".getBytes(), 2.0);
    binaryScoreMembers.put("member3".getBytes(), 3.0);

    ZAddParams params = ZAddParams.zAddParams().nx();

    Long zadd = exec(commandObjects.zadd(key, score, member));
    assertThat(zadd, equalTo(1L));

    Long zaddParams = exec(commandObjects.zadd(key, score, member, params));
    assertThat(zaddParams, equalTo(0L));

    Long zaddMultiple = exec(commandObjects.zadd(key, binaryScoreMembers));
    assertThat(zaddMultiple, equalTo(2L));

    Long zaddMultipleParams = exec(commandObjects.zadd(key, binaryScoreMembers, params));
    assertThat(zaddMultipleParams, equalTo(0L));

    Long zcard = exec(commandObjects.zcard(key));
    assertThat(zcard, equalTo(3L));
  }

  @Test
  public void testZIncrAndZincrBy() {
    String key = "zset";
    String member = "member";
    double initialScore = 1.0;
    double increment = 2.0;

    ZAddParams zAddParams = ZAddParams.zAddParams().xx();

    ZIncrByParams zIncrByParams = ZIncrByParams.zIncrByParams().xx();

    Long zadd = exec(commandObjects.zadd(key, initialScore, member));
    assertThat(zadd, equalTo(1L));

    Double zaddIncr = exec(commandObjects.zaddIncr(key, increment, member, zAddParams));
    assertThat(zaddIncr, closeTo(initialScore + increment, 0.001));

    Double zscoreAfterZaddincr = exec(commandObjects.zscore(key, member));
    assertThat(zscoreAfterZaddincr, closeTo(initialScore + increment, 0.001));

    Double zincrBy = exec(commandObjects.zincrby(key, increment, member));
    assertThat(zincrBy, closeTo(initialScore + increment * 2, 0.001));

    Double zscoreAfterZincrBy = exec(commandObjects.zscore(key, member));
    assertThat(zscoreAfterZincrBy, closeTo(initialScore + increment * 2, 0.001));

    Double zincrByParams = exec(commandObjects.zincrby(key, increment, member, zIncrByParams));
    assertThat(zincrByParams, closeTo(initialScore + increment * 3, 0.001));

    Double zscoreAfterZincrByParams = exec(commandObjects.zscore(key, member));
    assertThat(zscoreAfterZincrByParams, closeTo(initialScore + increment * 3, 0.001));
  }

  @Test
  public void testZIncrAndZincrByBinary() {
    byte[] key = "zset".getBytes();
    byte[] member = "member".getBytes();
    double initialScore = 1.0;
    double increment = 2.0;

    ZAddParams zAddParams = ZAddParams.zAddParams().xx();

    ZIncrByParams zIncrByParams = ZIncrByParams.zIncrByParams().xx();

    Long zadd = exec(commandObjects.zadd(key, initialScore, member));
    assertThat(zadd, equalTo(1L));

    Double zaddIncr = exec(commandObjects.zaddIncr(key, increment, member, zAddParams));
    assertThat(zaddIncr, closeTo(initialScore + increment, 0.001));

    Double zscoreAfterZaddIncr = exec(commandObjects.zscore(key, member));
    assertThat(zscoreAfterZaddIncr, closeTo(initialScore + increment, 0.001));

    Double zincrBy = exec(commandObjects.zincrby(key, increment, member));
    assertThat(zincrBy, closeTo(initialScore + increment * 2, 0.001));

    Double zscoreAfterZincrBy = exec(commandObjects.zscore(key, member));
    assertThat(zscoreAfterZincrBy, closeTo(initialScore + increment * 2, 0.001));

    Double zincrByParams = exec(commandObjects.zincrby(key, increment, member, zIncrByParams));
    assertThat(zincrByParams, closeTo(initialScore + increment * 3, 0.001));

    Double zscoreAfterZincrByParams = exec(commandObjects.zscore(key, member));
    assertThat(zscoreAfterZincrByParams, closeTo(initialScore + increment * 3, 0.001));
  }

  @Test
  public void testZrem() {
    String key = "zset";
    String member1 = "one";
    String member2 = "two";
    double score1 = 1.0;
    double score2 = 2.0;

    exec(commandObjects.zadd(key, score1, member1));
    exec(commandObjects.zadd(key, score2, member2));

    List<String> zrangeBefore = exec(commandObjects.zrange(key, 0, -1));
    assertThat(zrangeBefore, containsInAnyOrder(member1, member2));

    Long removedCount = exec(commandObjects.zrem(key, member1));
    assertThat(removedCount, equalTo(1L));

    List<String> zrangeAfter = exec(commandObjects.zrange(key, 0, -1));
    assertThat(zrangeAfter, containsInAnyOrder(member2));
  }

  @Test
  public void testZremBinary() {
    byte[] key = "zset".getBytes();
    byte[] member1 = "one".getBytes();
    byte[] member2 = "two".getBytes();
    double score1 = 1.0;
    double score2 = 2.0;

    exec(commandObjects.zadd(key, score1, member1));
    exec(commandObjects.zadd(key, score2, member2));

    List<byte[]> zrangeBefore = exec(commandObjects.zrange(key, 0, -1));
    assertThat(zrangeBefore, containsInAnyOrder(member1, member2));

    Long removedCount = exec(commandObjects.zrem(key, member1));
    assertThat(removedCount, equalTo(1L));

    List<byte[]> zrangeAfter = exec(commandObjects.zrange(key, 0, -1));
    assertThat(zrangeAfter, containsInAnyOrder(member2));
  }

  @Test
  public void testZrandmember() {
    String key = "zset";
    String member1 = "one";
    String member2 = "two";
    double score1 = 1.0;
    double score2 = 2.0;

    exec(commandObjects.zadd(key, score1, member1));
    exec(commandObjects.zadd(key, score2, member2));

    String randomMember = exec(commandObjects.zrandmember(key));

    assertThat(randomMember, anyOf(equalTo(member1), equalTo(member2)));

    byte[] randomMemberBinary = exec(commandObjects.zrandmember(key.getBytes()));

    assertThat(randomMemberBinary, anyOf(equalTo(member1.getBytes()), equalTo(member2.getBytes())));

    List<String> randomMembers = exec(commandObjects.zrandmember(key, 2));

    assertThat(randomMembers, containsInAnyOrder(member1, member2));

    List<byte[]> randomMembersBinary = exec(commandObjects.zrandmember(key.getBytes(), 2));

    assertThat(randomMembersBinary.get(0), anyOf(equalTo(member1.getBytes()), equalTo(member2.getBytes())));
    assertThat(randomMembersBinary.get(1), anyOf(equalTo(member1.getBytes()), equalTo(member2.getBytes())));
  }

  @Test
  public void testZrandmemberWithScores() {
    String key = "zset";
    String member1 = "one";
    String member2 = "two";
    double score1 = 1.0;
    double score2 = 2.0;

    exec(commandObjects.zadd(key, score1, member1));
    exec(commandObjects.zadd(key, score2, member2));

    List<Tuple> randomMembersWithScores = exec(commandObjects.zrandmemberWithScores(key, 2));

    assertThat(randomMembersWithScores, hasSize(2));
    assertThat(randomMembersWithScores, containsInAnyOrder(new Tuple(member1, score1), new Tuple(member2, score2)));

    List<Tuple> randomMembersWithScoresBinary = exec(commandObjects.zrandmemberWithScores(key.getBytes(), 2));

    assertThat(randomMembersWithScoresBinary, hasSize(2));

    assertThat(randomMembersWithScoresBinary.get(0).getBinaryElement(), anyOf(equalTo(member1.getBytes()), equalTo(member2.getBytes())));
    assertThat(randomMembersWithScoresBinary.get(0).getScore(), anyOf(equalTo(score1), equalTo(score2)));

    assertThat(randomMembersWithScoresBinary.get(1).getBinaryElement(), anyOf(equalTo(member1.getBytes()), equalTo(member2.getBytes())));
    assertThat(randomMembersWithScoresBinary.get(1).getScore(), anyOf(equalTo(score1), equalTo(score2)));
  }

  @Test
  public void testZscore() {
    String key = "zset";
    String member1 = "one";
    double score1 = 1.0;

    exec(commandObjects.zadd(key, score1, member1));

    Double score = exec(commandObjects.zscore(key, member1));
    assertThat(score, equalTo(score1));

    Double scoreBinary = exec(commandObjects.zscore(key.getBytes(), member1.getBytes()));
    assertThat(scoreBinary, equalTo(score1));
  }

  @Test
  public void testZmscore() {
    String key = "zset";
    String member1 = "one";
    String member2 = "two";
    double score1 = 1.0;
    double score2 = 2.0;

    exec(commandObjects.zadd(key, score1, member1));
    exec(commandObjects.zadd(key, score2, member2));

    List<Double> scores = exec(commandObjects.zmscore(key, member1, member2));
    assertThat(scores, contains(score1, score2));

    List<Double> scoresBinary = exec(commandObjects.zmscore(key.getBytes(), member1.getBytes(), member2.getBytes()));
    assertThat(scoresBinary, contains(score1, score2));
  }

  @Test
  public void testZrankAndZrevrank() {
    String key = "zset";
    String member1 = "one";
    String member2 = "two";
    double score1 = 1.0;
    double score2 = 2.0;

    exec(commandObjects.zadd(key, score1, member1));
    exec(commandObjects.zadd(key, score2, member2));

    Long rankMember1 = exec(commandObjects.zrank(key, member1));
    assertThat(rankMember1, equalTo(0L));

    Long rankMember2 = exec(commandObjects.zrank(key, member2));
    assertThat(rankMember2, equalTo(1L));

    Long rankMember1Binary = exec(commandObjects.zrank(key.getBytes(), member1.getBytes()));
    assertThat(rankMember1Binary, equalTo(0L));

    Long rankMember2Binary = exec(commandObjects.zrank(key.getBytes(), member2.getBytes()));
    assertThat(rankMember2Binary, equalTo(1L));

    Long revRankMember1 = exec(commandObjects.zrevrank(key, member1));
    assertThat(revRankMember1, equalTo(1L));

    Long revRankMember2 = exec(commandObjects.zrevrank(key, member2));
    assertThat(revRankMember2, equalTo(0L));

    Long revRankMember1Binary = exec(commandObjects.zrevrank(key.getBytes(), member1.getBytes()));
    assertThat(revRankMember1Binary, equalTo(1L));

    Long revRankMember2Binary = exec(commandObjects.zrevrank(key.getBytes(), member2.getBytes()));
    assertThat(revRankMember2Binary, equalTo(0L));
  }

  @Test
  public void testZrankWithScoreAndZrevrankWithScore() {
    String key = "zset";
    String member1 = "one";
    String member2 = "two";
    double score1 = 1.0;
    double score2 = 2.0;

    exec(commandObjects.zadd(key, score1, member1));
    exec(commandObjects.zadd(key, score2, member2));

    KeyValue<Long, Double> rankWithScoreMember1 = exec(commandObjects.zrankWithScore(key, member1));
    assertThat(rankWithScoreMember1.getKey(), equalTo(0L));
    assertThat(rankWithScoreMember1.getValue(), equalTo(score1));

    KeyValue<Long, Double> rankWithScoreMember2 = exec(commandObjects.zrankWithScore(key, member2));
    assertThat(rankWithScoreMember2.getKey(), equalTo(1L));
    assertThat(rankWithScoreMember2.getValue(), equalTo(score2));

    KeyValue<Long, Double> rankWithScoreMember1Binary = exec(commandObjects.zrankWithScore(key.getBytes(), member1.getBytes()));
    assertThat(rankWithScoreMember1Binary.getKey(), equalTo(0L));
    assertThat(rankWithScoreMember1Binary.getValue(), equalTo(score1));

    KeyValue<Long, Double> rankWithScoreMember2Binary = exec(commandObjects.zrankWithScore(key.getBytes(), member2.getBytes()));
    assertThat(rankWithScoreMember2Binary.getKey(), equalTo(1L));
    assertThat(rankWithScoreMember2Binary.getValue(), equalTo(score2));

    KeyValue<Long, Double> revRankWithScoreMember1 = exec(commandObjects.zrevrankWithScore(key, member1));
    assertThat(revRankWithScoreMember1.getKey(), equalTo(1L));
    assertThat(revRankWithScoreMember1.getValue(), equalTo(score1));

    KeyValue<Long, Double> revRankWithScoreMember2 = exec(commandObjects.zrevrankWithScore(key, member2));
    assertThat(revRankWithScoreMember2.getKey(), equalTo(0L));
    assertThat(revRankWithScoreMember2.getValue(), equalTo(score2));

    KeyValue<Long, Double> revRankWithScoreMember1Binary = exec(commandObjects.zrevrankWithScore(key.getBytes(), member1.getBytes()));
    assertThat(revRankWithScoreMember1Binary.getKey(), equalTo(1L));
    assertThat(revRankWithScoreMember1Binary.getValue(), equalTo(score1));

    KeyValue<Long, Double> revRankWithScoreMember2Binary = exec(commandObjects.zrevrankWithScore(key.getBytes(), member2.getBytes()));
    assertThat(revRankWithScoreMember2Binary.getKey(), equalTo(0L));
    assertThat(revRankWithScoreMember2Binary.getValue(), equalTo(score2));
  }

  @Test
  public void testZpopmax() {
    String key = "zset";
    String member1 = "one";
    String member2 = "two";
    double score1 = 1.0;
    double score2 = 2.0;

    exec(commandObjects.zadd(key, score1, member1));
    exec(commandObjects.zadd(key, score2, member2));

    Tuple poppedMax = exec(commandObjects.zpopmax(key));
    assertThat(poppedMax.getElement(), equalTo(member2));
    assertThat(poppedMax.getScore(), equalTo(score2));

    List<Tuple> poppedMaxMultiple = exec(commandObjects.zpopmax(key, 2));
    assertThat(poppedMaxMultiple, hasSize(1)); // Since we already popped the max, only one remains
    assertThat(poppedMaxMultiple.get(0).getElement(), equalTo(member1));
    assertThat(poppedMaxMultiple.get(0).getScore(), equalTo(score1));
  }

  @Test
  public void testZpopmaxBinary() {
    byte[] key = "zset".getBytes();
    String member1 = "one";
    String member2 = "two";
    double score1 = 1.0;
    double score2 = 2.0;

    exec(commandObjects.zadd(key, score1, member1.getBytes()));
    exec(commandObjects.zadd(key, score2, member2.getBytes()));

    Tuple poppedMaxBinary = exec(commandObjects.zpopmax(key));
    assertThat(poppedMaxBinary.getBinaryElement(), equalTo(member2.getBytes()));
    assertThat(poppedMaxBinary.getScore(), equalTo(score2));

    List<Tuple> poppedMaxMultipleBinary = exec(commandObjects.zpopmax(key, 2));
    assertThat(poppedMaxMultipleBinary, hasSize(1)); // Since we already popped the max, only one remains
    assertThat(poppedMaxMultipleBinary.get(0).getBinaryElement(), equalTo(member1.getBytes()));
    assertThat(poppedMaxMultipleBinary.get(0).getScore(), equalTo(score1));
  }

  @Test
  public void testZpopmin() {
    String key = "zset";
    String member1 = "one";
    String member2 = "two";
    double score1 = 1.0;
    double score2 = 2.0;

    exec(commandObjects.zadd(key, score1, member1));
    exec(commandObjects.zadd(key, score2, member2));

    Tuple poppedMin = exec(commandObjects.zpopmin(key));
    assertThat(poppedMin.getElement(), equalTo(member1));
    assertThat(poppedMin.getScore(), equalTo(score1));

    List<Tuple> poppedMinMultiple = exec(commandObjects.zpopmin(key, 2));
    assertThat(poppedMinMultiple, hasSize(1)); // Since we already popped the min, only one remains
    assertThat(poppedMinMultiple.get(0).getElement(), equalTo(member2));
    assertThat(poppedMinMultiple.get(0).getScore(), equalTo(score2));
  }

  @Test
  public void testZpopminBinary() {
    byte[] key = "zset".getBytes();
    String member1 = "one";
    String member2 = "two";
    double score1 = 1.0;
    double score2 = 2.0;

    exec(commandObjects.zadd(key, score1, member1.getBytes()));
    exec(commandObjects.zadd(key, score2, member2.getBytes()));

    Tuple poppedMinBinary = exec(commandObjects.zpopmin(key));
    assertThat(poppedMinBinary.getBinaryElement(), equalTo(member1.getBytes()));
    assertThat(poppedMinBinary.getScore(), equalTo(score1));

    List<Tuple> poppedMinMultipleBinary = exec(commandObjects.zpopmin(key, 2));
    assertThat(poppedMinMultipleBinary, hasSize(1)); // Since we already popped the min, only one remains
    assertThat(poppedMinMultipleBinary.get(0).getBinaryElement(), equalTo(member2.getBytes()));
    assertThat(poppedMinMultipleBinary.get(0).getScore(), equalTo(score2));
  }

  @Test
  public void testBzpopmaxAndBzpopmin() {
    String key1 = "zset1";
    String key2 = "zset2";
    String member1 = "one";
    String member2 = "two";
    double score1 = 1.0;
    double score2 = 2.0;
    double timeout = 2.0; // 2 seconds timeout for blocking operations

    exec(commandObjects.zadd(key1, score1, member1));
    exec(commandObjects.zadd(key2, score2, member2));

    KeyValue<String, Tuple> poppedMax = exec(commandObjects.bzpopmax(timeout, key1, key2));
    assertThat(poppedMax.getKey(), anyOf(equalTo(key1), equalTo(key2)));
    assertThat(poppedMax.getValue().getScore(), anyOf(equalTo(score1), equalTo(score2)));

    KeyValue<String, Tuple> poppedMin = exec(commandObjects.bzpopmin(timeout, key1, key2));
    assertThat(poppedMin.getKey(), anyOf(equalTo(key1), equalTo(key2)));
    assertThat(poppedMin.getValue().getScore(), anyOf(equalTo(score1), equalTo(score2)));
  }

  @Test
  public void testBzpopmaxAndBzpopminBinary() {
    byte[] key1 = "zset1".getBytes();
    byte[] key2 = "zset2".getBytes();
    String member1 = "one";
    String member2 = "two";
    double score1 = 1.0;
    double score2 = 2.0;
    double timeout = 2.0; // 2 seconds timeout for blocking operations

    exec(commandObjects.zadd(key1, score1, member1.getBytes()));
    exec(commandObjects.zadd(key2, score2, member2.getBytes()));

    KeyValue<byte[], Tuple> poppedMaxBinary = exec(commandObjects.bzpopmax(timeout, key1, key2));
    assertThat(poppedMaxBinary.getKey(), anyOf(equalTo(key1), equalTo(key2)));
    assertThat(poppedMaxBinary.getValue().getScore(), anyOf(equalTo(score1), equalTo(score2)));

    KeyValue<byte[], Tuple> poppedMinBinary = exec(commandObjects.bzpopmin(timeout, key1, key2));
    assertThat(poppedMinBinary.getKey(), anyOf(equalTo(key1), equalTo(key2)));
    assertThat(poppedMinBinary.getValue().getScore(), anyOf(equalTo(score1), equalTo(score2)));
  }

  @Test
  public void testZcount() {
    String key = "zset";
    String member1 = "one";
    String member2 = "two";
    String member3 = "three";
    double score1 = 1.0;
    double score2 = 2.0;
    double score3 = 3.0;

    exec(commandObjects.zadd(key, score1, member1));
    exec(commandObjects.zadd(key, score2, member2));
    exec(commandObjects.zadd(key, score3, member3));

    Long countInNumericRange = exec(commandObjects.zcount(key, 1.5, 2.5));
    assertThat(countInNumericRange, equalTo(1L));

    Long countInStringRange = exec(commandObjects.zcount(key, "(1", "3"));
    assertThat(countInStringRange, equalTo(2L));

    Long countInNumericRangeBinary = exec(commandObjects.zcount(key.getBytes(), 1.5, 2.5));
    assertThat(countInNumericRangeBinary, equalTo(1L));

    Long countInBinaryRange = exec(commandObjects.zcount(key.getBytes(), "(1".getBytes(), "3".getBytes()));
    assertThat(countInBinaryRange, equalTo(2L));
  }

  @Test
  public void testZrange() {
    String key = "zset";
    String member1 = "one";
    String member2 = "two";
    double score1 = 1.0;
    double score2 = 2.0;

    exec(commandObjects.zadd(key, score1, member1));
    exec(commandObjects.zadd(key, score2, member2));

    List<String> range = exec(commandObjects.zrange(key, 0, -1));
    assertThat(range, contains(member1, member2));

    List<byte[]> rangeBinary = exec(commandObjects.zrange(key.getBytes(), 0, -1));
    assertThat(rangeBinary, contains(member1.getBytes(), member2.getBytes()));

    ZRangeParams zRangeParams = ZRangeParams.zrangeParams(0, -1);

    List<String> rangeWithParams = exec(commandObjects.zrange(key, zRangeParams));
    assertThat(rangeWithParams, hasItems(member1, member2));

    List<byte[]> rangeWithParamsBinary = exec(commandObjects.zrange(key.getBytes(), zRangeParams));
    assertThat(rangeWithParamsBinary.get(0), equalTo(member1.getBytes()));
    assertThat(rangeWithParamsBinary.get(1), equalTo(member2.getBytes()));
  }

  @Test
  public void testZrangeWithScores() {
    String key = "zset";
    String member1 = "one";
    String member2 = "two";
    double score1 = 1.0;
    double score2 = 2.0;

    exec(commandObjects.zadd(key, score1, member1));
    exec(commandObjects.zadd(key, score2, member2));

    List<Tuple> rangeWithScores = exec(commandObjects.zrangeWithScores(key, 0, -1));

    assertThat(rangeWithScores, hasSize(2));
    assertThat(rangeWithScores.get(0).getElement(), equalTo(member1));
    assertThat(rangeWithScores.get(0).getScore(), equalTo(score1));
    assertThat(rangeWithScores.get(1).getElement(), equalTo(member2));
    assertThat(rangeWithScores.get(1).getScore(), equalTo(score2));

    List<Tuple> rangeWithScoresBinary = exec(commandObjects.zrangeWithScores(key.getBytes(), 0, -1));

    assertThat(rangeWithScoresBinary, hasSize(2));
    assertThat(rangeWithScoresBinary.get(0).getBinaryElement(), equalTo(member1.getBytes()));
    assertThat(rangeWithScoresBinary.get(0).getScore(), equalTo(score1));
    assertThat(rangeWithScoresBinary.get(1).getBinaryElement(), equalTo(member2.getBytes()));
    assertThat(rangeWithScoresBinary.get(1).getScore(), equalTo(score2));

    ZRangeParams zRangeParams = ZRangeParams.zrangeParams(0, -1);

    List<Tuple> rangeWithScoresParams = exec(commandObjects.zrangeWithScores(key, zRangeParams));

    assertThat(rangeWithScoresParams, hasSize(2));
    assertThat(rangeWithScoresParams.get(0).getElement(), equalTo(member1));
    assertThat(rangeWithScoresParams.get(0).getScore(), equalTo(score1));
    assertThat(rangeWithScoresParams.get(1).getElement(), equalTo(member2));
    assertThat(rangeWithScoresParams.get(1).getScore(), equalTo(score2));

    List<Tuple> rangeWithScoresParamsBinary = exec(commandObjects.zrangeWithScores(key.getBytes(), zRangeParams));

    assertThat(rangeWithScoresParamsBinary, hasSize(2));
    assertThat(rangeWithScoresParamsBinary.get(0).getBinaryElement(), equalTo(member1.getBytes()));
    assertThat(rangeWithScoresParamsBinary.get(0).getScore(), equalTo(score1));
    assertThat(rangeWithScoresParamsBinary.get(1).getBinaryElement(), equalTo(member2.getBytes()));
    assertThat(rangeWithScoresParamsBinary.get(1).getScore(), equalTo(score2));
  }

  @Test
  public void testZrangestore() {
    String srcKey = "zsetSrc";
    String destKey = "zsetDest";
    String member1 = "one";
    String member2 = "two";
    double score1 = 1.0;
    double score2 = 2.0;

    exec(commandObjects.zadd(srcKey, score1, member1));
    exec(commandObjects.zadd(srcKey, score2, member2));

    ZRangeParams zRangeParams = ZRangeParams.zrangeByScoreParams(score1, score2);

    Long zrangeStore = exec(commandObjects.zrangestore(destKey, srcKey, zRangeParams));

    assertThat(zrangeStore, equalTo(2L));

    List<Tuple> zrangeWithScores = exec(commandObjects.zrangeWithScores(destKey, 0, -1));

    assertThat(zrangeWithScores, hasSize(2));
    assertThat(zrangeWithScores.get(0).getElement(), equalTo(member1));
    assertThat(zrangeWithScores.get(1).getElement(), equalTo(member2));
  }

  @Test
  public void testZrangestoreBinary() {
    byte[] srcKey = "zsetSrcB".getBytes();
    byte[] destKey = "zsetDestB".getBytes();
    String member1 = "one";
    String member2 = "two";
    double score1 = 1.0;
    double score2 = 2.0;

    exec(commandObjects.zadd(srcKey, score1, member1.getBytes()));
    exec(commandObjects.zadd(srcKey, score2, member2.getBytes()));

    ZRangeParams zRangeParams = ZRangeParams.zrangeByScoreParams(score1, score2);

    Long zrangeStore = exec(commandObjects.zrangestore(destKey, srcKey, zRangeParams));

    assertThat(zrangeStore, equalTo(2L));

    List<Tuple> zrangeWithScores = exec(commandObjects.zrangeWithScores(destKey, 0, -1));

    assertThat(zrangeWithScores, hasSize(2));
    assertThat(zrangeWithScores.get(0).getBinaryElement(), equalTo(member1.getBytes()));
    assertThat(zrangeWithScores.get(1).getBinaryElement(), equalTo(member2.getBytes()));
  }

  @Test
  public void testZrevrange() {
    String key = "zset";
    String member1 = "one";
    String member2 = "two";
    double score1 = 1.0;
    double score2 = 2.0;

    exec(commandObjects.zadd(key, score1, member1));
    exec(commandObjects.zadd(key, score2, member2));

    List<String> revRange = exec(commandObjects.zrevrange(key, 0, -1));
    assertThat(revRange, contains(member2, member1));

    List<byte[]> revRangeBinary = exec(commandObjects.zrevrange(key.getBytes(), 0, -1));
    assertThat(revRangeBinary, contains(member2.getBytes(), member1.getBytes()));
  }

  @Test
  public void testZrevrangeWithScores() {
    String key = "zset";
    String member1 = "one";
    String member2 = "two";
    double score1 = 1.0;
    double score2 = 2.0;

    exec(commandObjects.zadd(key, score1, member1));
    exec(commandObjects.zadd(key, score2, member2));

    List<Tuple> revRangeWithScores = exec(commandObjects.zrevrangeWithScores(key, 0, -1));

    assertThat(revRangeWithScores, hasSize(2));
    assertThat(revRangeWithScores.get(0).getElement(), equalTo(member2));
    assertThat(revRangeWithScores.get(0).getScore(), equalTo(score2));
    assertThat(revRangeWithScores.get(1).getElement(), equalTo(member1));
    assertThat(revRangeWithScores.get(1).getScore(), equalTo(score1));

    List<Tuple> revRangeWithScoresBinary = exec(commandObjects.zrevrangeWithScores(key.getBytes(), 0, -1));

    assertThat(revRangeWithScoresBinary, hasSize(2));
    assertThat(revRangeWithScoresBinary.get(0).getBinaryElement(), equalTo(member2.getBytes()));
    assertThat(revRangeWithScoresBinary.get(0).getScore(), equalTo(score2));
    assertThat(revRangeWithScoresBinary.get(1).getBinaryElement(), equalTo(member1.getBytes()));
    assertThat(revRangeWithScoresBinary.get(1).getScore(), equalTo(score1));
  }

  @Test
  public void testZrangeByScore() {
    String key = "zset";
    double min = 1.0;
    double max = 10.0;
    String smin = "1";
    String smax = "10";
    byte[] bmin = "1.0".getBytes();
    byte[] bmax = "10.0".getBytes();
    int offset = 0;
    int count = 1;

    exec(commandObjects.zadd(key, 1, "one"));
    exec(commandObjects.zadd(key, 2, "two"));
    exec(commandObjects.zadd(key, 3, "three"));
    exec(commandObjects.zadd(key, 13, "four"));

    List<String> numericRange = exec(commandObjects.zrangeByScore(key, min, max));
    assertThat(numericRange, contains("one", "two", "three"));

    List<String> stringRange = exec(commandObjects.zrangeByScore(key, smin, smax));
    assertThat(stringRange, contains("one", "two", "three"));

    List<String> numericRangeOffsetCount = exec(commandObjects.zrangeByScore(key, min, max, offset, count));
    assertThat(numericRangeOffsetCount, contains("one"));

    List<String> stringRangeOffsetCount = exec(commandObjects.zrangeByScore(key, smin, smax, offset, count));
    assertThat(stringRangeOffsetCount, contains("one"));

    List<byte[]> numericRangeBinary = exec(commandObjects.zrangeByScore(key.getBytes(), min, max));
    assertThat(numericRangeBinary.get(0), equalTo("one".getBytes()));
    assertThat(numericRangeBinary.get(1), equalTo("two".getBytes()));
    assertThat(numericRangeBinary.get(2), equalTo("three".getBytes()));

    List<byte[]> stringRangeBinary = exec(commandObjects.zrangeByScore(key.getBytes(), bmin, bmax));
    assertThat(stringRangeBinary, contains("one".getBytes(), "two".getBytes(), "three".getBytes()));

    List<byte[]> numericRangeOffsetCountBinary = exec(commandObjects.zrangeByScore(key.getBytes(), min, max, offset, count));
    assertThat(numericRangeOffsetCountBinary.get(0), equalTo("one".getBytes()));

    List<byte[]> stringRangeOffsetCountBinary = exec(commandObjects.zrangeByScore(key.getBytes(), bmin, bmax, offset, count));
    assertThat(stringRangeOffsetCountBinary, contains("one".getBytes()));
  }

  @Test
  public void testZrevrangeByScore() {
    String key = "zset";
    double max = 10.0;
    double min = 1.0;
    String smax = "10";
    String smin = "1";
    byte[] bmax = "10.0".getBytes();
    byte[] bmin = "1.0".getBytes();
    int offset = 0;
    int count = 1;

    exec(commandObjects.zadd(key, 13, "four"));
    exec(commandObjects.zadd(key, 3, "three"));
    exec(commandObjects.zadd(key, 2, "two"));
    exec(commandObjects.zadd(key, 1, "one"));

    List<String> numericRevrange = exec(commandObjects.zrevrangeByScore(key, max, min));
    assertThat(numericRevrange, contains("three", "two", "one"));

    List<String> stringRevrange = exec(commandObjects.zrevrangeByScore(key, smax, smin));
    assertThat(stringRevrange, contains("three", "two", "one"));

    List<String> numericRevrangeOffsetCount = exec(commandObjects.zrevrangeByScore(key, max, min, offset, count));
    assertThat(numericRevrangeOffsetCount, contains("three"));

    List<String> stringRevrangeOffsetCount = exec(commandObjects.zrevrangeByScore(key, smax, smin, offset, count));
    assertThat(stringRevrangeOffsetCount, contains("three"));

    List<byte[]> numericRevrangeBinary = exec(commandObjects.zrevrangeByScore(key.getBytes(), max, min));
    assertThat(numericRevrangeBinary, contains("three".getBytes(), "two".getBytes(), "one".getBytes()));

    List<byte[]> stringRevrangeBinary = exec(commandObjects.zrevrangeByScore(key.getBytes(), bmax, bmin));
    assertThat(stringRevrangeBinary, contains("three".getBytes(), "two".getBytes(), "one".getBytes()));

    List<byte[]> numericRevrangeOffsetCountBinary = exec(commandObjects.zrevrangeByScore(key.getBytes(), max, min, offset, count));
    assertThat(numericRevrangeOffsetCountBinary.get(0), equalTo("three".getBytes()));

    List<byte[]> stringRevrangeOffsetCountBinary = exec(commandObjects.zrevrangeByScore(key.getBytes(), bmax, bmin, offset, count));
    assertThat(stringRevrangeOffsetCountBinary, contains("three".getBytes()));
  }

  @Test
  public void testZrangeByScoreWithScores() {
    String key = "zset";
    double min = 1.0;
    double max = 10.0;
    String smin = "1";
    String smax = "10";
    byte[] bmin = "1.0".getBytes();
    byte[] bmax = "10.0".getBytes();
    int offset = 0;
    int count = 2;

    exec(commandObjects.zadd(key, 1, "one"));
    exec(commandObjects.zadd(key, 2, "two"));
    exec(commandObjects.zadd(key, 3, "three"));

    List<Tuple> numericRange = exec(commandObjects.zrangeByScoreWithScores(key, min, max));
    assertThat(numericRange, contains(
        new Tuple("one", 1d),
        new Tuple("two", 2d),
        new Tuple("three", 3d)));

    List<Tuple> stringRange = exec(commandObjects.zrangeByScoreWithScores(key, smin, smax));
    assertThat(stringRange, contains(
        new Tuple("one", 1d),
        new Tuple("two", 2d),
        new Tuple("three", 3d)));

    List<Tuple> numericRangeOffsetCount = exec(commandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
    assertThat(numericRangeOffsetCount, contains(
        new Tuple("one", 1d),
        new Tuple("two", 2d)));

    List<Tuple> stringRangeOffsetCount = exec(commandObjects.zrangeByScoreWithScores(key, smin, smax, offset, count));
    assertThat(stringRangeOffsetCount, contains(
        new Tuple("one", 1d),
        new Tuple("two", 2d)));

    List<Tuple> numericRangeBinary = exec(commandObjects.zrangeByScoreWithScores(key.getBytes(), min, max));
    assertThat(numericRangeBinary, contains(
        new Tuple("one".getBytes(), 1d),
        new Tuple("two".getBytes(), 2d),
        new Tuple("three".getBytes(), 3d)));

    List<Tuple> stringRangeBinary = exec(commandObjects.zrangeByScoreWithScores(key.getBytes(), bmin, bmax));
    assertThat(stringRangeBinary, contains(
        new Tuple("one".getBytes(), 1d),
        new Tuple("two".getBytes(), 2d),
        new Tuple("three".getBytes(), 3d)));

    List<Tuple> numericRangeOffsetCountBinary = exec(commandObjects.zrangeByScoreWithScores(key.getBytes(), min, max, offset, count));
    assertThat(numericRangeOffsetCountBinary, contains(
        new Tuple("one".getBytes(), 1d),
        new Tuple("two".getBytes(), 2d)));

    List<Tuple> stringRangeOffsetCountBinary = exec(commandObjects.zrangeByScoreWithScores(key.getBytes(), bmin, bmax, offset, count));
    assertThat(stringRangeOffsetCountBinary, contains(
        new Tuple("one".getBytes(), 1d),
        new Tuple("two".getBytes(), 2d)));
  }

  @Test
  public void testZrevrangeByScoreWithScores() {
    String key = "zset";
    double max = 10.0;
    double min = 1.0;
    String smax = "10";
    String smin = "1";
    byte[] bmax = "10".getBytes();
    byte[] bmin = "1".getBytes();
    int offset = 0;
    int count = 2;

    exec(commandObjects.zadd(key, 3, "three"));
    exec(commandObjects.zadd(key, 2, "two"));
    exec(commandObjects.zadd(key, 1, "one"));

    List<Tuple> numericRevrange = exec(commandObjects.zrevrangeByScoreWithScores(key, max, min));
    assertThat(numericRevrange, contains(
        new Tuple("three", 3d),
        new Tuple("two", 2d),
        new Tuple("one", 1d)));

    List<Tuple> stringRevrange = exec(commandObjects.zrevrangeByScoreWithScores(key, smax, smin));
    assertThat(stringRevrange, contains(
        new Tuple("three", 3d),
        new Tuple("two", 2d),
        new Tuple("one", 1d)));

    List<Tuple> numericRevrangeOffsetCount = exec(commandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
    assertThat(numericRevrangeOffsetCount, contains(
        new Tuple("three", 3d),
        new Tuple("two", 2d)));

    List<Tuple> stringRevrangeOffsetCount = exec(commandObjects.zrevrangeByScoreWithScores(key, smax, smin, offset, count));
    assertThat(stringRevrangeOffsetCount, contains(
        new Tuple("three", 3d),
        new Tuple("two", 2d)));

    List<Tuple> numericRevrangeBinary = exec(commandObjects.zrevrangeByScoreWithScores(key.getBytes(), max, min));
    assertThat(numericRevrangeBinary, contains(
        new Tuple("three".getBytes(), 3d),
        new Tuple("two".getBytes(), 2d),
        new Tuple("one".getBytes(), 1d)));

    List<Tuple> stringRevrangeBinary = exec(commandObjects.zrevrangeByScoreWithScores(key.getBytes(), bmax, bmin));
    assertThat(stringRevrangeBinary, contains(
        new Tuple("three".getBytes(), 3d),
        new Tuple("two".getBytes(), 2d),
        new Tuple("one".getBytes(), 1d)));

    List<Tuple> numericRevrangeOffsetCountBinary = exec(commandObjects.zrevrangeByScoreWithScores(key.getBytes(), max, min, offset, count));
    assertThat(numericRevrangeOffsetCountBinary, contains(
        new Tuple("three".getBytes(), 3d),
        new Tuple("two".getBytes(), 2d)));

    List<Tuple> stringRevrangeOffsetCountBinary = exec(commandObjects.zrevrangeByScoreWithScores(key.getBytes(), bmax, bmin, offset, count));
    assertThat(stringRevrangeOffsetCountBinary, contains(
        new Tuple("three".getBytes(), 3d),
        new Tuple("two".getBytes(), 2d)));
  }

  @Test
  public void testZremrangeByRank() {
    String key = "zset";
    long start = 0;
    long stop = 1;

    exec(commandObjects.zadd(key, 1, "one"));
    exec(commandObjects.zadd(key, 2, "two"));
    exec(commandObjects.zadd(key, 3, "three"));

    Long removedCount = exec(commandObjects.zremrangeByRank(key, start, stop));
    assertThat(removedCount, equalTo(2L));

    List<String> remainingElements = exec(commandObjects.zrange(key, 0, -1));
    assertThat(remainingElements, contains("three"));
  }

  @Test
  public void testZremrangeByRankBinary() {
    byte[] key = "zset".getBytes();
    long start = 0;
    long stop = 1;

    exec(commandObjects.zadd(key, 1, "one".getBytes()));
    exec(commandObjects.zadd(key, 2, "two".getBytes()));
    exec(commandObjects.zadd(key, 3, "three".getBytes()));

    Long removedCount = exec(commandObjects.zremrangeByRank(key, start, stop));
    assertThat(removedCount, equalTo(2L));

    List<byte[]> remainingElements = exec(commandObjects.zrange(key, 0, -1));
    assertThat(remainingElements, contains("three".getBytes()));
  }

  @Test
  public void testZremrangeByScore() {
    String key = "zset";
    double min = 1.0;
    double max = 2.0;
    String smin = "1";
    String smax = "2";

    exec(commandObjects.zadd(key, 1, "one"));
    exec(commandObjects.zadd(key, 2, "two"));
    exec(commandObjects.zadd(key, 3, "three"));

    Long removedCountNumeric = exec(commandObjects.zremrangeByScore(key, min, max));
    assertThat(removedCountNumeric, equalTo(2L));

    List<String> remainingElements = exec(commandObjects.zrange(key, 0, -1));
    assertThat(remainingElements, contains("three"));

    exec(commandObjects.zadd(key, 1, "one"));
    exec(commandObjects.zadd(key, 2, "two"));

    Long removedCountString = exec(commandObjects.zremrangeByScore(key, smin, smax));
    assertThat(removedCountString, equalTo(2L));

    remainingElements = exec(commandObjects.zrange(key, 0, -1));
    assertThat(remainingElements, contains("three"));
  }

  @Test
  public void testZremrangeByScoreBinary() {
    byte[] bkey = "zset".getBytes();
    double min = 1.0;
    double max = 2.0;
    byte[] bmin = "1".getBytes();
    byte[] bmax = "2".getBytes();

    exec(commandObjects.zadd(bkey, 1, "one".getBytes()));
    exec(commandObjects.zadd(bkey, 2, "two".getBytes()));
    exec(commandObjects.zadd(bkey, 3, "three".getBytes()));

    Long removedCountNumericBinary = exec(commandObjects.zremrangeByScore(bkey, min, max));
    assertThat(removedCountNumericBinary, equalTo(2L));

    List<byte[]> remainingElements = exec(commandObjects.zrange(bkey, 0, -1));
    assertThat(remainingElements, contains("three".getBytes()));

    exec(commandObjects.zadd(bkey, 1, "one".getBytes()));
    exec(commandObjects.zadd(bkey, 2, "two".getBytes()));

    Long removedCountStringBinary = exec(commandObjects.zremrangeByScore(bkey, bmin, bmax));
    assertThat(removedCountStringBinary, equalTo(2L));

    remainingElements = exec(commandObjects.zrange(bkey, 0, -1));
    assertThat(remainingElements, contains("three".getBytes()));
  }

  @Test
  public void testZlexcount() {
    String key = "zset";
    String min = "[a", max = "(g";

    exec(commandObjects.zadd(key, 0, "abc"));
    exec(commandObjects.zadd(key, 0, "def"));
    exec(commandObjects.zadd(key, 0, "ghi"));

    Long count = exec(commandObjects.zlexcount(key, min, max));
    assertThat(count, equalTo(2L));

    Long countBinary = exec(commandObjects.zlexcount(key.getBytes(), min.getBytes(), max.getBytes()));
    assertThat(countBinary, equalTo(2L));
  }

  @Test
  public void testZrangeByLex() {
    String key = "zset";
    String min = "[abc";
    String max = "(cde";
    int offset = 0;
    int count = 2;

    exec(commandObjects.zadd(key, 0, "aaa"));
    exec(commandObjects.zadd(key, 0, "abc"));
    exec(commandObjects.zadd(key, 0, "bcd"));
    exec(commandObjects.zadd(key, 0, "cde"));

    List<String> range = exec(commandObjects.zrangeByLex(key, min, max));
    assertThat(range, contains("abc", "bcd"));

    List<String> limitedRange = exec(commandObjects.zrangeByLex(key, min, max, offset, count));
    assertThat(limitedRange, contains("abc", "bcd"));

    List<byte[]> rangeBinary = exec(commandObjects.zrangeByLex(key.getBytes(), min.getBytes(), max.getBytes()));
    assertThat(rangeBinary, contains("abc".getBytes(), "bcd".getBytes()));

    List<byte[]> limitedRangeBinary = exec(commandObjects.zrangeByLex(key.getBytes(), min.getBytes(), max.getBytes(), offset, count));
    assertThat(limitedRangeBinary, contains("abc".getBytes(), "bcd".getBytes()));
  }

  @Test
  public void testZrevrangeByLex() {
    String key = "zset";
    String max = "[cde";
    String min = "(aaa";
    int offset = 0;
    int count = 2;

    exec(commandObjects.zadd(key, 0, "aaa"));
    exec(commandObjects.zadd(key, 0, "abc"));
    exec(commandObjects.zadd(key, 0, "bcd"));
    exec(commandObjects.zadd(key, 0, "cde"));

    List<String> revRange = exec(commandObjects.zrevrangeByLex(key, max, min));
    assertThat(revRange, contains("cde", "bcd", "abc"));

    List<String> limitedRevRange = exec(commandObjects.zrevrangeByLex(key, max, min, offset, count));
    assertThat(limitedRevRange, contains("cde", "bcd"));

    List<byte[]> revRangeBinary = exec(commandObjects.zrevrangeByLex(key.getBytes(), max.getBytes(), min.getBytes()));
    assertThat(revRangeBinary.get(0), equalTo("cde".getBytes()));
    assertThat(revRangeBinary.get(1), equalTo("bcd".getBytes()));
    assertThat(revRangeBinary.get(2), equalTo("abc".getBytes()));

    List<byte[]> limitedRevRangeBinary = exec(commandObjects.zrevrangeByLex(key.getBytes(), max.getBytes(), min.getBytes(), offset, count));
    assertThat(limitedRevRangeBinary.get(0), equalTo("cde".getBytes()));
    assertThat(limitedRevRangeBinary.get(1), equalTo("bcd".getBytes()));
  }

  @Test
  public void testZremrangeByLex() {
    String key = "zset";
    String min = "[aaa";
    String max = "(ccc";

    exec(commandObjects.zadd(key, 0, "aaa"));
    exec(commandObjects.zadd(key, 0, "bbb"));
    exec(commandObjects.zadd(key, 0, "ccc"));

    Long removedCount = exec(commandObjects.zremrangeByLex(key, min, max));
    assertThat(removedCount, equalTo(2L));

    List<String> remainingElements = exec(commandObjects.zrange(key, 0, -1));
    assertThat(remainingElements, contains("ccc"));
  }

  @Test
  public void testZremrangeByLexBinary() {
    byte[] key = "zset".getBytes();
    byte[] min = "[aaa".getBytes();
    byte[] bmax = "(ccc".getBytes();

    exec(commandObjects.zadd(key, 0, "aaa".getBytes()));
    exec(commandObjects.zadd(key, 0, "bbb".getBytes()));
    exec(commandObjects.zadd(key, 0, "ccc".getBytes()));

    Long removedCount = exec(commandObjects.zremrangeByLex(key, min, bmax));
    assertThat(removedCount, equalTo(2L));

    List<byte[]> remainingElements = exec(commandObjects.zrange(key, 0, -1));
    assertThat(remainingElements, contains("ccc".getBytes()));
  }

  @Test
  public void testZscan() {
    String key = "zset";
    String cursor = "0";
    ScanParams params = new ScanParams().count(2);
    String member1 = "one";
    double score1 = 1.0;
    String member2 = "two";
    double score2 = 2.0;

    exec(commandObjects.zadd(key, score1, member1));
    exec(commandObjects.zadd(key, score2, member2));

    ScanResult<Tuple> result = exec(commandObjects.zscan(key, cursor, params));
    assertThat(result.getResult(), containsInAnyOrder(
        new Tuple(member1, score1),
        new Tuple(member2, score2)));

    ScanResult<Tuple> resultBinar = exec(commandObjects.zscan(key.getBytes(), cursor.getBytes(), params));
    assertThat(resultBinar.getResult(), containsInAnyOrder(
        new Tuple(member1, score1),
        new Tuple(member2, score2)));
  }

  @Test
  public void testZdiffAndZdiffWithScores() {
    String key1 = "zset1";
    String key2 = "zset2";
    String member1 = "one";
    double score1 = 1.0;
    String member2 = "two";
    double score2 = 2.0;

    exec(commandObjects.zadd(key1, score1, member1));
    exec(commandObjects.zadd(key1, score2, member2));
    exec(commandObjects.zadd(key2, score1, member1));

    List<String> diff = exec(commandObjects.zdiff(key1, key2));
    assertThat(diff, containsInAnyOrder(member2));

    List<Tuple> diffWithScores = exec(commandObjects.zdiffWithScores(key1, key2));
    assertThat(diffWithScores, containsInAnyOrder(new Tuple(member2, score2)));

    List<byte[]> diffBinary = exec(commandObjects.zdiff(key1.getBytes(), key2.getBytes()));
    assertThat(diffBinary, containsInAnyOrder(member2.getBytes()));

    List<Tuple> diffWithScoresBinary = exec(commandObjects.zdiffWithScores(key1.getBytes(), key2.getBytes()));
    assertThat(diffWithScoresBinary, containsInAnyOrder(new Tuple(member2.getBytes(), score2)));
  }

  @Test
  public void testZdiffStore() {
    String dstKey = "result";

    exec(commandObjects.zadd("set1", 1, "member1"));
    exec(commandObjects.zadd("set1", 2, "member2"));
    exec(commandObjects.zadd("set2", 3, "member2"));

    Long result = exec(commandObjects.zdiffStore(dstKey, "set1", "set2"));
    assertThat(result, equalTo(1L));

    List<String> resultSet = exec(commandObjects.zrange(dstKey, 0, -1));
    assertThat(resultSet, containsInAnyOrder("member1"));

    exec(commandObjects.del(dstKey));

    result = exec(commandObjects.zdiffstore(dstKey, "set1", "set2"));
    assertThat(result, equalTo(1L));

    resultSet = exec(commandObjects.zrange(dstKey, 0, -1));
    assertThat(resultSet, hasSize(1));
    assertThat(resultSet, containsInAnyOrder("member1"));
  }

  @Test
  public void testZdiffStoreBinary() {
    byte[] dstKey = "result".getBytes();

    exec(commandObjects.zadd("set1".getBytes(), 1, "member1".getBytes()));
    exec(commandObjects.zadd("set1".getBytes(), 2, "member2".getBytes()));
    exec(commandObjects.zadd("set2".getBytes(), 3, "member2".getBytes()));

    Long result = exec(commandObjects.zdiffStore(dstKey, "set1".getBytes(), "set2".getBytes()));
    assertThat(result, equalTo(1L));

    List<byte[]> resultSet = exec(commandObjects.zrange(dstKey, 0, -1));
    assertThat(resultSet, hasSize(1));
    assertThat(resultSet, containsInAnyOrder("member1".getBytes()));

    exec(commandObjects.del(dstKey));

    result = exec(commandObjects.zdiffstore(dstKey, "set1".getBytes(), "set2".getBytes()));
    assertThat(result, equalTo(1L));

    resultSet = exec(commandObjects.zrange(dstKey, 0, -1));
    assertThat(resultSet, hasSize(1));
    assertThat(resultSet, containsInAnyOrder("member1".getBytes()));
  }

  @Test
  public void testZinterAndZintercard() {
    ZParams params = new ZParams().aggregate(ZParams.Aggregate.SUM).weights(1, 2);

    exec(commandObjects.zadd("set1", 1, "member1"));
    exec(commandObjects.zadd("set2", 2, "member1"));
    exec(commandObjects.zadd("set2", 2, "member2"));

    List<String> inter = exec(commandObjects.zinter(params, "set1", "set2"));
    assertThat(inter, containsInAnyOrder("member1"));

    List<Tuple> interWithScores = exec(commandObjects.zinterWithScores(params, "set1", "set2"));
    assertThat(interWithScores, containsInAnyOrder(
        new Tuple("member1", 5.0)));

    Long card = exec(commandObjects.zintercard("set1", "set2"));
    assertThat(card, equalTo(1L));

    Long cardLimited = exec(commandObjects.zintercard(1L, "set1", "set2"));
    assertThat(cardLimited, equalTo(1L));

    List<byte[]> interBinary = exec(commandObjects.zinter(params, "set1".getBytes(), "set2".getBytes()));
    assertThat(interBinary, containsInAnyOrder("member1".getBytes()));

    List<Tuple> interWithScoresBinary = exec(commandObjects.zinterWithScores(params, "set1".getBytes(), "set2".getBytes()));
    assertThat(interWithScoresBinary, hasItem(
        new Tuple("member1".getBytes(), 5.0)));

    Long cardBinary = exec(commandObjects.zintercard("set1".getBytes(), "set2".getBytes()));
    assertThat(cardBinary, equalTo(1L));

    Long cardLimitedBinary = exec(commandObjects.zintercard(1L, "set1".getBytes(), "set2".getBytes()));
    assertThat(cardLimitedBinary, equalTo(1L));
  }

  @Test
  public void testZinterstore() {
    String dstKey = "destinationIntersect";
    String set1 = "sortedSet1";
    String set2 = "sortedSet2";
    String set3 = "sortedSet3";
    double score1 = 1.0;
    double score2 = 2.0;
    double score3 = 3.0;
    String member1 = "member1";
    String member2 = "member2";
    String member3 = "member3";

    exec(commandObjects.zadd(set1, score1, member1));
    exec(commandObjects.zadd(set1, score2, member2));

    exec(commandObjects.zadd(set2, score2, member2));
    exec(commandObjects.zadd(set2, score3, member3));

    exec(commandObjects.zadd(set3, score1, member1));
    exec(commandObjects.zadd(set3, score3, member3));

    ZParams params = new ZParams().aggregate(ZParams.Aggregate.SUM);

    Long interStore = exec(commandObjects.zinterstore(dstKey, set1, set2, set3));
    assertThat(interStore, equalTo(0L));

    Long interStoreWithParams = exec(commandObjects.zinterstore(dstKey, params, set1, set2));
    assertThat(interStoreWithParams, equalTo(1L));

    List<Tuple> dstSetContent = exec(commandObjects.zrangeWithScores(dstKey, 0, -1));
    assertThat(dstSetContent, hasSize(1));
    assertThat(dstSetContent.get(0).getElement(), equalTo(member2));
    assertThat(dstSetContent.get(0).getScore(), equalTo(score2 * 2)); // Score aggregated as SUM
  }

  @Test
  public void testZinterstoreBinary() {
    byte[] dstKey = "destinationIntersect".getBytes();
    byte[] set1 = "sortedSet1".getBytes();
    byte[] set2 = "sortedSet2".getBytes();
    byte[] set3 = "sortedSet3".getBytes();
    double score1 = 1.0;
    double score2 = 2.0;
    double score3 = 3.0;
    byte[] member1 = "member1".getBytes();
    byte[] member2 = "member2".getBytes();
    byte[] member3 = "member3".getBytes();

    exec(commandObjects.zadd(set1, score1, member1));
    exec(commandObjects.zadd(set1, score2, member2));

    exec(commandObjects.zadd(set2, score2, member2));
    exec(commandObjects.zadd(set2, score3, member3));

    exec(commandObjects.zadd(set3, score1, member1));
    exec(commandObjects.zadd(set3, score3, member3));

    ZParams params = new ZParams().aggregate(ZParams.Aggregate.SUM);

    Long interStore = exec(commandObjects.zinterstore(dstKey, set1, set2, set3));
    assertThat(interStore, equalTo(0L));

    List<Tuple> dstSetContent = exec(commandObjects.zrangeWithScores(dstKey, 0, -1));
    assertThat(dstSetContent, empty());

    Long interStoreParams = exec(commandObjects.zinterstore(dstKey, params, set1, set2));
    assertThat(interStoreParams, equalTo(1L));

    List<Tuple> dstSetParamsContent = exec(commandObjects.zrangeWithScores(dstKey, 0, -1));
    assertThat(dstSetParamsContent, hasSize(1));
    assertThat(dstSetParamsContent.get(0).getBinaryElement(), equalTo(member2));
    assertThat(dstSetParamsContent.get(0).getScore(), equalTo(score2 * 2)); // Score aggregated as SUM
  }

  @Test
  public void testZunionAndZunionWithScores() {
    String key1 = "sortedSet1";
    String key2 = "sortedSet2";
    String member1 = "member1";
    String member2 = "member2";
    double score1 = 1.0;
    double score2 = 2.0;

    exec(commandObjects.zadd(key1, score1, member1));

    exec(commandObjects.zadd(key2, score2, member1));
    exec(commandObjects.zadd(key2, score2, member2));

    ZParams params = new ZParams().aggregate(ZParams.Aggregate.SUM);

    List<String> zunion = exec(commandObjects.zunion(params, key1, key2));
    assertThat(zunion, containsInAnyOrder(member1, member2));

    List<Tuple> zunionWithScores = exec(commandObjects.zunionWithScores(params, key1, key2));
    assertThat(zunionWithScores, containsInAnyOrder(
        new Tuple(member1, score1 + score2),
        new Tuple(member2, score2)));

    List<byte[]> zunionBinary = exec(commandObjects.zunion(params, key1.getBytes(), key2.getBytes()));
    assertThat(zunionBinary, containsInAnyOrder(member1.getBytes(), member2.getBytes()));

    List<Tuple> zunionWithScoresBinary = exec(commandObjects.zunionWithScores(params, key1.getBytes(), key2.getBytes()));
    assertThat(zunionWithScoresBinary, containsInAnyOrder(
        new Tuple(member1, score1 + score2),
        new Tuple(member2, score2)));
  }

  @Test
  public void testZunionstore() {
    String dstKey = "destinationSet";
    String set1 = "sortedSet1";
    String set2 = "sortedSet2";
    double score1 = 1.0;
    double score2 = 2.0;
    double score3 = 3.0;
    String member1 = "member1";
    String member2 = "member2";
    String member3 = "member3";

    exec(commandObjects.zadd(set1, score1, member1));
    exec(commandObjects.zadd(set1, score2, member2));
    exec(commandObjects.zadd(set1, score3, member3));

    exec(commandObjects.zadd(set2, score3, member3));

    ZParams params = new ZParams().aggregate(ZParams.Aggregate.MAX);

    Long zunionStore = exec(commandObjects.zunionstore(dstKey, set1, set2));
    assertThat(zunionStore, equalTo(3L));

    List<Tuple> dstSetContent = exec(commandObjects.zrangeWithScores(dstKey, 0, -1));
    assertThat(dstSetContent, containsInAnyOrder(
        new Tuple(member1, score1),
        new Tuple(member2, score2),
        new Tuple(member3, score3 * 2)));

    Long zunionStoreParams = exec(commandObjects.zunionstore(dstKey, params, set1, set2));
    assertThat(zunionStoreParams, equalTo(3L));

    List<Tuple> dstSetContentParams = exec(commandObjects.zrangeWithScores(dstKey, 0, -1));
    assertThat(dstSetContentParams, containsInAnyOrder(
        new Tuple(member1, score1),
        new Tuple(member2, score2),
        new Tuple(member3, score3)));
  }

  @Test
  public void testZunionstoreBinary() {
    byte[] dstKey = "destinationSet".getBytes();
    byte[] set1 = "sortedSet1".getBytes();
    byte[] set2 = "sortedSet2".getBytes();
    double score1 = 1.0;
    double score2 = 2.0;
    double score3 = 3.0;
    byte[] member1 = "member1".getBytes();
    byte[] member2 = "member2".getBytes();
    byte[] member3 = "member3".getBytes();

    exec(commandObjects.zadd(set1, score1, member1));
    exec(commandObjects.zadd(set1, score2, member2));
    exec(commandObjects.zadd(set1, score3, member3));

    exec(commandObjects.zadd(set2, score3, member3));

    ZParams params = new ZParams().aggregate(ZParams.Aggregate.MAX);

    Long zunionStore = exec(commandObjects.zunionstore(dstKey, set1, set2));
    assertThat(zunionStore, equalTo(3L));

    List<Tuple> dstSetContent = exec(commandObjects.zrangeWithScores(dstKey, 0, -1));
    assertThat(dstSetContent, containsInAnyOrder(
        new Tuple(member1, score1),
        new Tuple(member2, score2),
        new Tuple(member3, score3 * 2)));

    Long zunionStoreParams = exec(commandObjects.zunionstore(dstKey, params, set1, set2));
    assertThat(zunionStoreParams, equalTo(3L));

    List<Tuple> dstSetContentParams = exec(commandObjects.zrangeWithScores(dstKey, 0, -1));
    assertThat(dstSetContentParams, containsInAnyOrder(
        new Tuple(member1, score1),
        new Tuple(member2, score2),
        new Tuple(member3, score3)));
  }

  @Test
  public void testZmpopAndZmpopWithCount() {
    String key1 = "sortedSet1";
    String key2 = "sortedSet2";
    double score1 = 1.0;
    double score2 = 2.0;
    String member1 = "member1";
    String member2 = "member2";

    exec(commandObjects.zadd(key1, score1, member1));
    exec(commandObjects.zadd(key2, score2, member2));

    KeyValue<String, List<Tuple>> zmpop = exec(commandObjects.zmpop(SortedSetOption.MAX, key1, key2));

    assertThat(zmpop, notNullValue());
    assertThat(zmpop.getKey(), either(equalTo(key1)).or(equalTo(key2)));
    assertThat(zmpop.getValue(), hasSize(1));

    KeyValue<String, List<Tuple>> zmpopCount = exec(commandObjects.zmpop(SortedSetOption.MIN, 2, key1, key2));

    assertThat(zmpopCount, notNullValue());
    assertThat(zmpopCount.getKey(), either(equalTo(key1)).or(equalTo(key2)));
    assertThat(zmpopCount.getValue(), hasSize(1));
  }

  @Test
  public void testZmpopAndZmpopWithCountBinary() {
    byte[] key1 = "sortedSet1".getBytes();
    byte[] key2 = "sortedSet2".getBytes();
    double score1 = 1.0;
    double score2 = 2.0;
    byte[] member1 = "member1".getBytes();
    byte[] member2 = "member2".getBytes();

    exec(commandObjects.zadd(key1, score1, member1));
    exec(commandObjects.zadd(key2, score2, member2));

    KeyValue<byte[], List<Tuple>> zmpopBinary = exec(commandObjects.zmpop(SortedSetOption.MAX, key1, key2));

    assertThat(zmpopBinary, notNullValue());
    assertThat(zmpopBinary.getKey(), either(equalTo(key1)).or(equalTo(key2)));
    assertThat(zmpopBinary.getValue(), hasSize(1));

    KeyValue<byte[], List<Tuple>> zmpopCountBinary = exec(commandObjects.zmpop(SortedSetOption.MIN, 2, key1, key2));

    assertThat(zmpopCountBinary, notNullValue());
    assertThat(zmpopCountBinary.getKey(), either(equalTo(key1)).or(equalTo(key2)));
    assertThat(zmpopCountBinary.getValue(), hasSize(1));
  }

  @Test
  public void testBzmpop() {
    String key1 = "sortedSet1";
    String key2 = "sortedSet2";
    double score1 = 1.0;
    double score2 = 2.0;
    String member1 = "member1";
    String member2 = "member2";
    double timeout = 0.1;

    exec(commandObjects.zadd(key1, score1, member1));
    exec(commandObjects.zadd(key1, score2, member2));

    exec(commandObjects.zadd(key2, score1, member1));
    exec(commandObjects.zadd(key2, score2, member2));

    KeyValue<String, List<Tuple>> bzmpopMax = exec(commandObjects.bzmpop(timeout, SortedSetOption.MAX, key1, key2));
    assertThat(bzmpopMax, notNullValue());
    assertThat(bzmpopMax.getKey(), anyOf(equalTo(key1), equalTo(key2)));
    assertThat(bzmpopMax.getValue(), contains(new Tuple(member2, score2)));

    KeyValue<String, List<Tuple>> bzmpopMin = exec(commandObjects.bzmpop(timeout, SortedSetOption.MIN, key1, key2));
    assertThat(bzmpopMin, notNullValue());
    assertThat(bzmpopMin.getKey(), anyOf(equalTo(key1), equalTo(key2)));
    assertThat(bzmpopMin.getValue(), contains(new Tuple(member1, score1)));
  }

  @Test
  public void testBzmpopBinary() {
    byte[] key1 = "sortedSet1".getBytes();
    byte[] key2 = "sortedSet2".getBytes();
    double score1 = 1.0;
    double score2 = 2.0;
    byte[] member1 = "member1".getBytes();
    byte[] member2 = "member2".getBytes();
    double timeout = 0.1;

    exec(commandObjects.zadd(key1, score1, member1));
    exec(commandObjects.zadd(key1, score2, member2));

    exec(commandObjects.zadd(key2, score1, member1));
    exec(commandObjects.zadd(key2, score2, member2));

    KeyValue<byte[], List<Tuple>> bzmpopMax = exec(commandObjects.bzmpop(timeout, SortedSetOption.MAX, key1, key2));
    assertThat(bzmpopMax, notNullValue());
    assertThat(bzmpopMax.getKey(), anyOf(equalTo(key1), equalTo(key2)));
    assertThat(bzmpopMax.getValue(), contains(new Tuple(member2, score2)));

    KeyValue<byte[], List<Tuple>> bzmpopMin = exec(commandObjects.bzmpop(timeout, SortedSetOption.MIN, key1, key2));
    assertThat(bzmpopMin, notNullValue());
    assertThat(bzmpopMin.getKey(), anyOf(equalTo(key1), equalTo(key2)));
    assertThat(bzmpopMin.getValue(), contains(new Tuple(member1, score1)));
  }

  @Test
  public void testBzmpopCount() {
    String key1 = "sortedSet1";
    String key2 = "sortedSet2";
    double score1 = 1.0;
    double score2 = 2.0;
    double timeout = 0.1;
    String member1 = "member1";
    String member2 = "member2";

    exec(commandObjects.zadd(key1, score1, member1));
    exec(commandObjects.zadd(key2, score2, member2));

    KeyValue<String, List<Tuple>> bzmpop = exec(commandObjects.bzmpop(timeout, SortedSetOption.MAX, 1, key1, key2));
    assertThat(bzmpop, notNullValue());
    assertThat(bzmpop.getKey(), either(equalTo(key1)).or(equalTo(key2)));
    assertThat(bzmpop.getValue(), hasSize(1));
  }

  @Test
  public void testBzmpopCountBinary() {
    byte[] key1 = "sortedSet1".getBytes();
    byte[] key2 = "sortedSet2".getBytes();
    double score1 = 1.0;
    double score2 = 2.0;
    double timeout = 0.1;
    byte[] member1 = "member1".getBytes();
    byte[] member2 = "member2".getBytes();

    exec(commandObjects.zadd(key1, score1, member1));
    exec(commandObjects.zadd(key2, score2, member2));

    KeyValue<byte[], List<Tuple>> bzmpopBinary = exec(commandObjects.bzmpop(timeout, SortedSetOption.MAX, 1, key1, key2));
    assertThat(bzmpopBinary, notNullValue());
    assertThat(bzmpopBinary.getKey(), either(equalTo(key1)).or(equalTo(key2)));
    assertThat(bzmpopBinary.getValue(), hasSize(1));
  }
}
