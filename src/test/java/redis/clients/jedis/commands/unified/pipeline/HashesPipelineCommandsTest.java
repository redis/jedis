package redis.clients.jedis.commands.unified.pipeline;

import static redis.clients.jedis.util.AssertUtil.assertPipelineSyncAll;

import java.util.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.RedisProtocol;

@RunWith(Parameterized.class)
public class HashesPipelineCommandsTest extends PipelineCommandsTestBase {

  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
  final byte[] bcar = { 0x09, 0x0A, 0x0B, 0x0C };

  final byte[] bbar1 = { 0x05, 0x06, 0x07, 0x08, 0x0A };
  final byte[] bbar2 = { 0x05, 0x06, 0x07, 0x08, 0x0B };
  final byte[] bbar3 = { 0x05, 0x06, 0x07, 0x08, 0x0C };
  final byte[] bbarstar = { 0x05, 0x06, 0x07, 0x08, '*' };

  public HashesPipelineCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void hset() {
    pipe.hset("foo", "bar", "car");
    pipe.hset("foo", "bar", "foo");

    // Binary
    pipe.hset(bfoo, bbar, bcar);
    pipe.hset(bfoo, bbar, bfoo);

    assertPipelineSyncAll(
        Arrays.<Object>asList(1L, 0L, 1L, 0L),
        pipe.syncAndReturnAll());
  }

  @Test
  public void hget() {
    pipe.hset("foo", "bar", "car");
    pipe.hget("bar", "foo");
    pipe.hget("foo", "car");
    pipe.hget("foo", "bar");

    // Binary
    pipe.hset(bfoo, bbar, bcar);
    pipe.hget(bbar, bfoo);
    pipe.hget(bfoo, bcar);
    pipe.hget(bfoo, bbar);

    assertPipelineSyncAll(
        Arrays.<Object>asList(
            1L, null, null, "car",
            1L, null, null, bcar),
        pipe.syncAndReturnAll());
  }

  @Test
  public void hsetnx() {
    pipe.hsetnx("foo", "bar", "car");
    pipe.hget("foo", "bar");

    pipe.hsetnx("foo", "bar", "foo");
    pipe.hget("foo", "bar");

    pipe.hsetnx("foo", "car", "bar");
    pipe.hget("foo", "car");

    // Binary
    pipe.hsetnx(bfoo, bbar, bcar);
    pipe.hget(bfoo, bbar);

    pipe.hsetnx(bfoo, bbar, bfoo);
    pipe.hget(bfoo, bbar);

    pipe.hsetnx(bfoo, bcar, bbar);
    pipe.hget(bfoo, bcar);

    assertPipelineSyncAll(
        Arrays.<Object>asList(
            1L, "car", 0L, "car", 1L, "bar",
            1L, bcar, 0L, bcar, 1L, bbar),
        pipe.syncAndReturnAll());
  }

  @Test
  public void hmset() {
    Map<String, String> hash = new HashMap<>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    pipe.hmset("foo", hash);
    pipe.hget("foo", "bar");
    pipe.hget("foo", "car");

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    pipe.hmset(bfoo, bhash);
    pipe.hget(bfoo, bbar);
    pipe.hget(bfoo, bcar);

    assertPipelineSyncAll(
        Arrays.<Object>asList("OK", "car", "bar", "OK", bcar, bbar),
        pipe.syncAndReturnAll());
  }

  @Test
  public void hsetVariadic() {
    Map<String, String> hash = new HashMap<>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    pipe.hset("foo", hash);
    pipe.hget("foo", "bar");
    pipe.hget("foo", "car");

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    pipe.hset(bfoo, bhash);
    pipe.hget(bfoo, bbar);
    pipe.hget(bfoo, bcar);

    assertPipelineSyncAll(
        Arrays.<Object>asList(2L, "car", "bar", 2L, bcar, bbar),
        pipe.syncAndReturnAll());
  }

  @Test
  public void hmget() {
    Map<String, String> hash = new HashMap<>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    pipe.hmset("foo", hash);

    pipe.hmget("foo", "bar", "car", "foo");
    List<String> expected = new ArrayList<>();
    expected.add("car");
    expected.add("bar");
    expected.add(null);

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    pipe.hmset(bfoo, bhash);

    pipe.hmget(bfoo, bbar, bcar, bfoo);
    List<byte[]> bexpected = new ArrayList<>();
    bexpected.add(bcar);
    bexpected.add(bbar);
    bexpected.add(null);

    assertPipelineSyncAll(
        Arrays.<Object>asList(
            "OK", Arrays.asList("car", "bar", null),
            "OK", Arrays.asList(bcar, bbar, null)),
        pipe.syncAndReturnAll());
  }

  @Test
  public void hincrBy() {
    pipe.hincrBy("foo", "bar", 1);
    pipe.hincrBy("foo", "bar", -1);
    pipe.hincrBy("foo", "bar", -10);

    // Binary
    pipe.hincrBy(bfoo, bbar, 1);
    pipe.hincrBy(bfoo, bbar, -1);
    pipe.hincrBy(bfoo, bbar, -10);

    assertPipelineSyncAll(
        Arrays.<Object>asList(1L, 0L, -10L, 1L, 0L, -10L),
        pipe.syncAndReturnAll());
  }

  @Test
  public void hincrByFloat() {
    pipe.hincrByFloat("foo", "bar", 1.5d);
    pipe.hincrByFloat("foo", "bar", -1.5d);
    pipe.hincrByFloat("foo", "bar", -10.7d);

    // Binary
    pipe.hincrByFloat(bfoo, bbar, 1.5d);
    pipe.hincrByFloat(bfoo, bbar, -1.5d);
    pipe.hincrByFloat(bfoo, bbar, -10.7d);

    assertPipelineSyncAll(
        Arrays.<Object>asList(1.5, 0d, -10.7, 1.5, 0d, -10.7),
        pipe.syncAndReturnAll());
  }

  @Test
  public void hexists() {
    Map<String, String> hash = new HashMap<>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    pipe.hset("foo", hash);

    pipe.hexists("bar", "foo");
    pipe.hexists("foo", "foo");
    pipe.hexists("foo", "bar");

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    pipe.hset(bfoo, bhash);

    pipe.hexists(bbar, bfoo);
    pipe.hexists(bfoo, bfoo);
    pipe.hexists(bfoo, bbar);

    assertPipelineSyncAll(
        Arrays.<Object>asList(
            2L, false, false, true,
            2L, false, false, true),
        pipe.syncAndReturnAll());
  }

  @Test
  public void hdel() {
    Map<String, String> hash = new HashMap<>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    pipe.hset("foo", hash);

    pipe.hdel("bar", "foo");
    pipe.hdel("foo", "foo");
    pipe.hdel("foo", "bar");
    pipe.hget("foo", "bar");

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    pipe.hset(bfoo, bhash);

    pipe.hdel(bbar, bfoo);
    pipe.hdel(bfoo, bfoo);
    pipe.hdel(bfoo, bbar);
    pipe.hget(bfoo, bbar);

    assertPipelineSyncAll(
        Arrays.<Object>asList(
            2L, 0L, 0L, 1L, null,
            2L, 0L, 0L, 1L, null),
        pipe.syncAndReturnAll());
  }

  @Test
  public void hlen() {
    Map<String, String> hash = new HashMap<>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    pipe.hset("foo", hash);

    pipe.hlen("bar");
    pipe.hlen("foo");

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    pipe.hset(bfoo, bhash);

    pipe.hlen(bbar);
    pipe.hlen(bfoo);

    assertPipelineSyncAll(
        Arrays.<Object>asList(2L, 0L, 2L, 2L, 0L, 2L),
        pipe.syncAndReturnAll());
  }

  @Test
  public void hkeys() {
    Map<String, String> hash = new LinkedHashMap<>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    pipe.hset("foo", hash);

    pipe.hkeys("foo");
    Set<String> expected = new LinkedHashSet<>();
    expected.add("bar");
    expected.add("car");

    // Binary
    Map<byte[], byte[]> bhash = new LinkedHashMap<>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    pipe.hset(bfoo, bhash);

    pipe.hkeys(bfoo);
    Set<byte[]> bexpected = new LinkedHashSet<>();
    bexpected.add(bbar);
    bexpected.add(bcar);

    assertPipelineSyncAll(
        Arrays.<Object>asList(
            2L, new HashSet<>(Arrays.asList("bar", "car")),
            2L, new HashSet<>(Arrays.asList(bbar, bcar))),
        pipe.syncAndReturnAll());
  }

  @Test
  public void hvals() {
    Map<String, String> hash = new LinkedHashMap<>();
    hash.put("bar", "car");
    //hash.put("car", "bar");
    pipe.hset("foo", hash);

    pipe.hvals("foo");

    // Binary
    Map<byte[], byte[]> bhash = new LinkedHashMap<>();
    bhash.put(bbar, bcar);
    //bhash.put(bcar, bbar);
    pipe.hset(bfoo, bhash);

    pipe.hvals(bfoo);

    assertPipelineSyncAll(
        Arrays.<Object>asList(
            //2L, Arrays.asList("bar", "car"),
            //2L, Arrays.asList(bbar, bcar)),
            1L, Arrays.asList("car"),
            1L, Arrays.asList(bcar)),
        pipe.syncAndReturnAll());
  }

  @Test
  public void hgetAll() {
    Map<String, String> hash = new HashMap<>();
    hash.put("bar", "car");
    //hash.put("car", "bar");
    pipe.hset("foo", hash);

    pipe.hgetAll("foo");

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<>();
    bhash.put(bbar, bcar);
    //bhash.put(bcar, bbar);
    pipe.hset(bfoo, bhash);

    pipe.hgetAll(bfoo);

//    assertPipelineSyncAll(
//        Arrays.<Object>asList(
//            1L, hash,
//            1L, bhash),
//        pipe.syncAndReturnAll());
    pipe.syncAndReturnAll();
  }

  @Test
  public void hstrlen() {
    pipe.hstrlen("foo", "key");
    pipe.hset("foo", "key", "value");
    pipe.hstrlen("foo", "key");

    pipe.hstrlen(bfoo, bbar);
    pipe.hset(bfoo, bbar, bcar);
    pipe.hstrlen(bfoo, bbar);

    assertPipelineSyncAll(
        Arrays.<Object>asList(0L, 1L, 5L, 0L, 1L, 4L),
        pipe.syncAndReturnAll());
  }
}
