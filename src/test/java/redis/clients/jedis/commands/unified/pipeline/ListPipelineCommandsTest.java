package redis.clients.jedis.commands.unified.pipeline;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;

import io.redis.test.annotations.SinceRedisVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.Response;
import redis.clients.jedis.args.ListDirection;
import redis.clients.jedis.args.ListPosition;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.LPosParams;
import redis.clients.jedis.util.KeyValue;

@RunWith(Parameterized.class)
public class ListPipelineCommandsTest extends PipelineCommandsTestBase {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  protected final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  protected final byte[] bfoo1 = { 0x01, 0x02, 0x03, 0x04, 0x05 };
  protected final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
  protected final byte[] bcar = { 0x09, 0x0A, 0x0B, 0x0C };
  protected final byte[] bA = { 0x0A };
  protected final byte[] bB = { 0x0B };
  protected final byte[] bC = { 0x0C };
  protected final byte[] b1 = { 0x01 };
  protected final byte[] b2 = { 0x02 };
  protected final byte[] b3 = { 0x03 };
  protected final byte[] bhello = { 0x04, 0x02 };
  protected final byte[] bx = { 0x02, 0x04 };
  protected final byte[] bdst = { 0x11, 0x12, 0x13, 0x14 };

  public ListPipelineCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void rpush() {
    pipe.rpush("foo", "bar");
    pipe.rpush("foo", "foo");
    pipe.rpush("foo", "bar", "foo");

    assertThat(pipe.syncAndReturnAll(), contains(
        1L,
        2L,
        4L
    ));

    // Binary
    pipe.rpush(bfoo, bbar);
    pipe.rpush(bfoo, bfoo);
    pipe.rpush(bfoo, bbar, bfoo);

    assertThat(pipe.syncAndReturnAll(), contains(
        1L,
        2L,
        4L
    ));
  }

  @Test
  public void lpush() {
    pipe.lpush("foo", "bar");
    pipe.lpush("foo", "foo");
    pipe.lpush("foo", "bar", "foo");

    assertThat(pipe.syncAndReturnAll(), contains(
        1L,
        2L,
        4L
    ));

    // Binary
    pipe.lpush(bfoo, bbar);
    pipe.lpush(bfoo, bfoo);
    pipe.lpush(bfoo, bbar, bfoo);

    assertThat(pipe.syncAndReturnAll(), contains(
        1L,
        2L,
        4L
    ));
  }

  @Test
  public void llen() {
    pipe.llen("foo");
    pipe.lpush("foo", "bar");
    pipe.lpush("foo", "car");
    pipe.llen("foo");

    assertThat(pipe.syncAndReturnAll(), contains(
        0L,
        1L,
        2L,
        2L
    ));

    // Binary
    pipe.llen(bfoo);
    pipe.lpush(bfoo, bbar);
    pipe.lpush(bfoo, bcar);
    pipe.llen(bfoo);

    assertThat(pipe.syncAndReturnAll(), contains(
        0L,
        1L,
        2L,
        2L
    ));
  }

  @Test
  public void llenNotOnList() {
    pipe.set("foo", "bar");
    pipe.llen("foo");

    assertThat(pipe.syncAndReturnAll(), contains(
        equalTo("OK"),
        instanceOf(JedisDataException.class)
    ));

    // Binary
    pipe.set(bfoo, bbar);
    pipe.llen(bfoo);

    assertThat(pipe.syncAndReturnAll(), contains(
        equalTo("OK"),
        instanceOf(JedisDataException.class)
    ));
  }

  @Test
  public void lrange() {
    pipe.rpush("foo", "a");
    pipe.rpush("foo", "b");
    pipe.rpush("foo", "c");

    Response<List<String>> range1 = pipe.lrange("foo", 0, 2);
    Response<List<String>> range2 = pipe.lrange("foo", 0, 20);
    Response<List<String>> range3 = pipe.lrange("foo", 1, 2);
    Response<List<String>> range4 = pipe.lrange("foo", 2, 1);

    pipe.sync();

    assertThat(range1.get(), contains("a", "b", "c"));
    assertThat(range2.get(), contains("a", "b", "c"));
    assertThat(range3.get(), contains("b", "c"));
    assertThat(range4.get(), empty());

    // Binary
    pipe.rpush(bfoo, bA);
    pipe.rpush(bfoo, bB);
    pipe.rpush(bfoo, bC);

    Response<List<byte[]>> brange1 = pipe.lrange(bfoo, 0, 2);
    Response<List<byte[]>> brange2 = pipe.lrange(bfoo, 0, 20);
    Response<List<byte[]>> brange3 = pipe.lrange(bfoo, 1, 2);
    Response<List<byte[]>> brange4 = pipe.lrange(bfoo, 2, 1);

    pipe.sync();

    assertThat(brange1.get(), contains(bA, bB, bC));
    assertThat(brange2.get(), contains(bA, bB, bC));
    assertThat(brange3.get(), contains(bB, bC));
    assertThat(brange4.get(), empty());
  }

  @Test
  public void ltrim() {
    pipe.lpush("foo", "1");
    pipe.lpush("foo", "2");
    pipe.lpush("foo", "3");

    Response<String> status = pipe.ltrim("foo", 0, 1);
    Response<Long> len = pipe.llen("foo");
    Response<List<String>> range = pipe.lrange("foo", 0, 100);

    pipe.sync();

    assertThat(status.get(), equalTo("OK"));
    assertThat(len.get(), equalTo(2L));
    assertThat(range.get(), contains("3", "2"));

    // Binary
    pipe.lpush(bfoo, b1);
    pipe.lpush(bfoo, b2);
    pipe.lpush(bfoo, b3);

    Response<String> bstatus = pipe.ltrim(bfoo, 0, 1);
    Response<Long> blen = pipe.llen(bfoo);
    Response<List<byte[]>> brange = pipe.lrange(bfoo, 0, 100);

    pipe.sync();

    assertThat(bstatus.get(), equalTo("OK"));
    assertThat(blen.get(), equalTo(2L));
    assertThat(brange.get(), contains(b3, b2));
  }

  @Test
  public void lset() {
    pipe.lpush("foo", "1");
    pipe.lpush("foo", "2");
    pipe.lpush("foo", "3");

    Response<String> status = pipe.lset("foo", 1, "bar");
    Response<List<String>> range = pipe.lrange("foo", 0, 100);

    pipe.sync();

    assertThat(status.get(), equalTo("OK"));
    assertThat(range.get(), contains("3", "bar", "1"));

    // Binary
    pipe.lpush(bfoo, b1);
    pipe.lpush(bfoo, b2);
    pipe.lpush(bfoo, b3);

    Response<String> bstatus = pipe.lset(bfoo, 1, bbar);
    Response<List<byte[]>> brange = pipe.lrange(bfoo, 0, 100);

    pipe.sync();

    assertThat(bstatus.get(), equalTo("OK"));
    assertThat(brange.get(), contains(b3, bbar, b1));
  }

  @Test
  public void lindex() {
    pipe.lpush("foo", "1");
    pipe.lpush("foo", "2");
    pipe.lpush("foo", "3");

    Response<String> index1 = pipe.lindex("foo", 0);
    Response<String> index2 = pipe.lindex("foo", 100);

    pipe.sync();

    assertThat(index1.get(), equalTo("3"));
    assertThat(index2.get(), nullValue());

    // Binary
    pipe.lpush(bfoo, b1);
    pipe.lpush(bfoo, b2);
    pipe.lpush(bfoo, b3);

    Response<byte[]> bindex1 = pipe.lindex(bfoo, 0);
    Response<byte[]> bindex2 = pipe.lindex(bfoo, 100);

    pipe.sync();

    assertThat(bindex1.get(), equalTo(b3));
    assertThat(bindex2.get(), nullValue());
  }

  @Test
  public void lrem() {
    pipe.lpush("foo", "hello");
    pipe.lpush("foo", "hello");
    pipe.lpush("foo", "x");
    pipe.lpush("foo", "hello");
    pipe.lpush("foo", "c");
    pipe.lpush("foo", "b");
    pipe.lpush("foo", "a");

    Response<Long> result1 = pipe.lrem("foo", -2, "hello");
    Response<List<String>> range = pipe.lrange("foo", 0, 1000);
    Response<Long> result2 = pipe.lrem("bar", 100, "foo");

    pipe.sync();

    assertThat(result1.get(), equalTo(2L));
    assertThat(range.get(), contains("a", "b", "c", "hello", "x"));
    assertThat(result2.get(), equalTo(0L));

    // Binary
    pipe.lpush(bfoo, bhello);
    pipe.lpush(bfoo, bhello);
    pipe.lpush(bfoo, bx);
    pipe.lpush(bfoo, bhello);
    pipe.lpush(bfoo, bC);
    pipe.lpush(bfoo, bB);
    pipe.lpush(bfoo, bA);

    Response<Long> bresult1 = pipe.lrem(bfoo, -2, bhello);
    Response<List<byte[]>> brange = pipe.lrange(bfoo, 0, 1000);
    Response<Long> bresult2 = pipe.lrem(bbar, 100, bfoo);

    pipe.sync();

    assertThat(bresult1.get(), equalTo(2L));
    assertThat(brange.get(), contains(bA, bB, bC, bhello, bx));
    assertThat(bresult2.get(), equalTo(0L));
  }

  @Test
  public void lpop() {
    Response<String> response1 = pipe.lpop("foo");
    Response<List<String>> response2 = pipe.lpop("foo", 0);

    pipe.rpush("foo", "a");
    pipe.rpush("foo", "b");
    pipe.rpush("foo", "c");

    Response<String> response3 = pipe.lpop("foo");
    Response<List<String>> response4 = pipe.lpop("foo", 10);
    Response<String> response5 = pipe.lpop("foo");
    Response<List<String>> response6 = pipe.lpop("foo", 1);

    pipe.sync();

    assertThat(response1.get(), nullValue());
    assertThat(response2.get(), nullValue());
    assertThat(response3.get(), equalTo("a"));
    assertThat(response4.get(), contains("b", "c"));
    assertThat(response5.get(), nullValue());
    assertThat(response6.get(), nullValue());

    // Binary
    Response<byte[]> bresponse1 = pipe.lpop(bfoo);
    Response<List<byte[]>> bresponse2 = pipe.lpop(bfoo, 0);

    pipe.rpush(bfoo, bA);
    pipe.rpush(bfoo, bB);
    pipe.rpush(bfoo, bC);

    Response<byte[]> bresponse3 = pipe.lpop(bfoo);
    Response<List<byte[]>> bresponse4 = pipe.lpop(bfoo, 10);
    Response<byte[]> bresponse5 = pipe.lpop(bfoo);
    Response<List<byte[]>> bresponse6 = pipe.lpop(bfoo, 1);

    pipe.sync();

    assertThat(bresponse1.get(), nullValue());
    assertThat(bresponse2.get(), nullValue());
    assertThat(bresponse3.get(), equalTo(bA));
    assertThat(bresponse4.get(), contains(bB, bC));
    assertThat(bresponse5.get(), nullValue());
    assertThat(bresponse6.get(), nullValue());
  }

  @Test
  public void rpop() {
    Response<String> response1 = pipe.rpop("foo");
    Response<List<String>> response2 = pipe.rpop("foo", 0);

    pipe.rpush("foo", "a");
    pipe.rpush("foo", "b");
    pipe.rpush("foo", "c");

    Response<String> response3 = pipe.rpop("foo");
    Response<List<String>> response4 = pipe.rpop("foo", 10);
    Response<String> response5 = pipe.rpop("foo");
    Response<List<String>> response6 = pipe.rpop("foo", 1);

    pipe.sync();

    assertThat(response1.get(), nullValue());
    assertThat(response2.get(), nullValue());
    assertThat(response3.get(), equalTo("c"));
    assertThat(response4.get(), contains("b", "a"));
    assertThat(response5.get(), nullValue());
    assertThat(response6.get(), nullValue());

    // Binary
    Response<byte[]> bresponse1 = pipe.rpop(bfoo);
    Response<List<byte[]>> bresponse2 = pipe.rpop(bfoo, 0);

    pipe.rpush(bfoo, bA);
    pipe.rpush(bfoo, bB);
    pipe.rpush(bfoo, bC);

    Response<byte[]> bresponse3 = pipe.rpop(bfoo);
    Response<List<byte[]>> bresponse4 = pipe.rpop(bfoo, 10);
    Response<byte[]> bresponse5 = pipe.rpop(bfoo);
    Response<List<byte[]>> bresponse6 = pipe.rpop(bfoo, 1);

    pipe.sync();

    assertThat(bresponse1.get(), nullValue());
    assertThat(bresponse2.get(), nullValue());
    assertThat(bresponse3.get(), equalTo(bC));
    assertThat(bresponse4.get(), contains(bB, bA));
    assertThat(bresponse5.get(), nullValue());
    assertThat(bresponse6.get(), nullValue());
  }

  @Test
  public void rpoplpush() {
    pipe.rpush("foo", "a");
    pipe.rpush("foo", "b");
    pipe.rpush("foo", "c");

    pipe.rpush("dst", "foo");
    pipe.rpush("dst", "bar");

    Response<String> element = pipe.rpoplpush("foo", "dst");
    Response<List<String>> srcRange = pipe.lrange("foo", 0, 1000);
    Response<List<String>> dstRange = pipe.lrange("dst", 0, 1000);

    pipe.sync();

    assertThat(element.get(), equalTo("c"));
    assertThat(srcRange.get(), contains("a", "b"));
    assertThat(dstRange.get(), contains("c", "foo", "bar"));

    // Binary
    pipe.rpush(bfoo, bA);
    pipe.rpush(bfoo, bB);
    pipe.rpush(bfoo, bC);

    pipe.rpush(bdst, bfoo);
    pipe.rpush(bdst, bbar);

    Response<byte[]> belement = pipe.rpoplpush(bfoo, bdst);
    Response<List<byte[]>> bsrcRange = pipe.lrange(bfoo, 0, 1000);
    Response<List<byte[]>> bdstRange = pipe.lrange(bdst, 0, 1000);

    pipe.sync();

    assertThat(belement.get(), equalTo(bC));
    assertThat(bsrcRange.get(), contains(bA, bB));
    assertThat(bdstRange.get(), contains(bC, bfoo, bbar));
  }

  @Test
  public void blpop() throws InterruptedException {
    Response<List<String>> result1 = pipe.blpop(1, "foo");

    pipe.lpush("foo", "bar");

    Response<List<String>> result2 = pipe.blpop(1, "foo");

    // Multi keys
    Response<List<String>> result3 = pipe.blpop(1, "foo", "foo1");

    pipe.lpush("foo", "bar");
    pipe.lpush("foo1", "bar1");

    Response<List<String>> result4 = pipe.blpop(1, "foo1", "foo");

    pipe.sync();

    assertThat(result1.get(), nullValue());
    assertThat(result2.get(), contains("foo", "bar"));
    assertThat(result3.get(), nullValue());
    assertThat(result4.get(), contains("foo1", "bar1"));

    // Binary
    pipe.lpush(bfoo, bbar);

    Response<List<byte[]>> bresult1 = pipe.blpop(1, bfoo);

    // Binary Multi keys
    Response<List<byte[]>> bresult2 = pipe.blpop(1, bfoo, bfoo1);

    pipe.lpush(bfoo, bbar);
    pipe.lpush(bfoo1, bcar);

    Response<List<byte[]>> bresult3 = pipe.blpop(1, bfoo1, bfoo);

    pipe.sync();

    assertThat(bresult1.get(), contains(bfoo, bbar));
    assertThat(bresult2.get(), nullValue());
    assertThat(bresult3.get(), contains(bfoo1, bcar));
  }

  @Test
  public void blpopDouble() {
    Response<KeyValue<String, String>> result1 = pipe.blpop(0.1, "foo");

    pipe.lpush("foo", "bar");

    Response<KeyValue<String, String>> result2 = pipe.blpop(3.2, "foo");

    // Multi keys
    Response<KeyValue<String, String>> result3 = pipe.blpop(0.18, "foo", "foo1");

    pipe.lpush("foo", "bar");
    pipe.lpush("foo1", "bar1");

    Response<KeyValue<String, String>> result4 = pipe.blpop(1d, "foo1", "foo");

    pipe.sync();

    assertThat(result1.get(), nullValue());
    assertThat(result2.get(), equalTo(new KeyValue<>("foo", "bar")));
    assertThat(result3.get(), nullValue());
    assertThat(result4.get(), equalTo(new KeyValue<>("foo1", "bar1")));

    // Binary
    pipe.lpush(bfoo, bbar);

    Response<KeyValue<byte[], byte[]>> bresult1 = pipe.blpop(3.12, bfoo);

    // Binary Multi keys
    Response<KeyValue<byte[], byte[]>> bresult2 = pipe.blpop(0.11, bfoo, bfoo1);

    pipe.lpush(bfoo, bbar);
    pipe.lpush(bfoo1, bcar);

    Response<KeyValue<byte[], byte[]>> bresult3 = pipe.blpop(1d, bfoo1, bfoo);

    pipe.sync();

    assertThat(bresult1.get().getKey(), equalTo(bfoo));
    assertThat(bresult1.get().getValue(), equalTo(bbar));
    assertThat(bresult2.get(), nullValue());
    assertThat(bresult3.get().getKey(), equalTo(bfoo1));
    assertThat(bresult3.get().getValue(), equalTo(bcar));
  }

  @Test(timeout = 5000L)
  public void blpopDoubleWithSleep() {
    Response<KeyValue<String, String>> result = pipe.blpop(0.04, "foo");
    pipe.sync();

    assertThat(result.get(), nullValue());

    new Thread(() -> {
      try {
        Thread.sleep(30);
      } catch (InterruptedException e) {
        logger.error("", e);
      }
      jedis.lpush("foo", "bar");
    }).start();

    result = pipe.blpop(1.2, "foo");
    pipe.sync();

    assertThat(result.get().getKey(), equalTo("foo"));
    assertThat(result.get().getValue(), equalTo("bar"));
  }

  @Test
  public void brpop() {
    Response<List<String>> result1 = pipe.brpop(1, "foo");

    pipe.lpush("foo", "bar");

    Response<List<String>> result2 = pipe.brpop(1, "foo");

    // Multi keys
    Response<List<String>> result3 = pipe.brpop(1, "foo", "foo1");

    pipe.lpush("foo", "bar");
    pipe.lpush("foo1", "bar1");

    Response<List<String>> result4 = pipe.brpop(1, "foo1", "foo");

    pipe.sync();

    assertThat(result1.get(), nullValue());
    assertThat(result2.get(), contains("foo", "bar"));
    assertThat(result3.get(), nullValue());
    assertThat(result4.get(), contains("foo1", "bar1"));

    // Binary
    pipe.lpush(bfoo, bbar);

    Response<List<byte[]>> bresult1 = pipe.brpop(1, bfoo);

    // Binary Multi keys
    Response<List<byte[]>> bresult2 = pipe.brpop(1, bfoo, bfoo1);

    pipe.lpush(bfoo, bbar);
    pipe.lpush(bfoo1, bcar);

    Response<List<byte[]>> bresult3 = pipe.brpop(1, bfoo1, bfoo);

    pipe.sync();

    assertThat(bresult1.get(), contains(bfoo, bbar));
    assertThat(bresult2.get(), nullValue());
    assertThat(bresult3.get(), contains(bfoo1, bcar));
  }

  @Test
  public void brpopDouble() {
    Response<KeyValue<String, String>> result1 = pipe.brpop(0.1, "foo");

    pipe.lpush("foo", "bar");

    Response<KeyValue<String, String>> result2 = pipe.brpop(3.2, "foo");

    // Multi keys
    Response<KeyValue<String, String>> result3 = pipe.brpop(0.18, "foo", "foo1");

    pipe.lpush("foo", "bar");
    pipe.lpush("foo1", "bar1");

    Response<KeyValue<String, String>> result4 = pipe.brpop(1d, "foo1", "foo");

    pipe.sync();

    assertThat(result1.get(), nullValue());
    assertThat(result2.get(), equalTo(new KeyValue<>("foo", "bar")));
    assertThat(result3.get(), nullValue());
    assertThat(result4.get(), equalTo(new KeyValue<>("foo1", "bar1")));

    // Binary
    pipe.lpush(bfoo, bbar);

    Response<KeyValue<byte[], byte[]>> bresult1 = pipe.brpop(3.12, bfoo);

    // Binary Multi keys
    Response<KeyValue<byte[], byte[]>> bresult2 = pipe.brpop(0.11, bfoo, bfoo1);

    pipe.lpush(bfoo, bbar);
    pipe.lpush(bfoo1, bcar);

    Response<KeyValue<byte[], byte[]>> bresult3 = pipe.brpop(1d, bfoo1, bfoo);

    pipe.sync();

    assertThat(bresult1.get().getKey(), equalTo(bfoo));
    assertThat(bresult1.get().getValue(), equalTo(bbar));
    assertThat(bresult2.get(), nullValue());
    assertThat(bresult3.get().getKey(), equalTo(bfoo1));
    assertThat(bresult3.get().getValue(), equalTo(bcar));
  }

  @Test(timeout = 5000L)
  public void brpopDoubleWithSleep() {
    Response<KeyValue<String, String>> result = pipe.brpop(0.04, "foo");
    pipe.sync();

    assertThat(result.get(), nullValue());

    new Thread(() -> {
      try {
        Thread.sleep(30);
      } catch (InterruptedException e) {
        logger.error("", e);
      }
      jedis.lpush("foo", "bar");
    }).start();

    result = pipe.brpop(1.2, "foo");
    pipe.sync();

    assertThat(result.get().getKey(), equalTo("foo"));
    assertThat(result.get().getValue(), equalTo("bar"));
  }

  @Test
  public void lpushx() {
    pipe.lpushx("foo", "bar");
    pipe.lpush("foo", "a");
    pipe.lpushx("foo", "b");

    assertThat(pipe.syncAndReturnAll(), contains(
        0L,
        1L,
        2L
    ));

    // Binary
    pipe.lpushx(bfoo, bbar);
    pipe.lpush(bfoo, bA);
    pipe.lpushx(bfoo, bB);

    assertThat(pipe.syncAndReturnAll(), contains(
        0L,
        1L,
        2L
    ));
  }

  @Test
  public void rpushx() {
    pipe.rpushx("foo", "bar");
    pipe.lpush("foo", "a");
    pipe.rpushx("foo", "b");

    assertThat(pipe.syncAndReturnAll(), contains(
        0L,
        1L,
        2L
    ));

    // Binary
    pipe.rpushx(bfoo, bbar);
    pipe.lpush(bfoo, bA);
    pipe.rpushx(bfoo, bB);

    assertThat(pipe.syncAndReturnAll(), contains(
        0L,
        1L,
        2L
    ));
  }

  @Test
  public void linsert() {
    Response<Long> result1 = pipe.linsert("foo", ListPosition.BEFORE, "bar", "car");

    pipe.lpush("foo", "a");

    Response<Long> result2 = pipe.linsert("foo", ListPosition.AFTER, "a", "b");

    Response<List<String>> range = pipe.lrange("foo", 0, 100);

    Response<Long> result3 = pipe.linsert("foo", ListPosition.BEFORE, "bar", "car");

    pipe.sync();

    assertThat(result1.get(), equalTo(0L));
    assertThat(result2.get(), equalTo(2L));
    assertThat(range.get(), contains("a", "b"));
    assertThat(result3.get(), equalTo(-1L));

    // Binary
    Response<Long> bresult1 = pipe.linsert(bfoo, ListPosition.BEFORE, bbar, bcar);

    pipe.lpush(bfoo, bA);

    Response<Long> bresult2 = pipe.linsert(bfoo, ListPosition.AFTER, bA, bB);

    Response<List<byte[]>> brange = pipe.lrange(bfoo, 0, 100);

    Response<Long> bresult3 = pipe.linsert(bfoo, ListPosition.BEFORE, bbar, bcar);

    pipe.sync();

    assertThat(bresult1.get(), equalTo(0L));
    assertThat(bresult2.get(), equalTo(2L));
    assertThat(brange.get(), contains(bA, bB));
    assertThat(bresult3.get(), equalTo(-1L));
  }

  @Test
  public void brpoplpush() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          logger.error("", e);
        }
        jedis.lpush("foo", "a");
      }
    }).start();

    Response<String> element = pipe.brpoplpush("foo", "bar", 0);
    Response<Long> len = pipe.llen("bar");
    Response<List<String>> range = pipe.lrange("bar", 0, -1);

    pipe.sync();

    assertThat(element.get(), equalTo("a"));
    assertThat(len.get(), equalTo(1L));
    assertThat(range.get(), contains("a"));

    // Binary

    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          logger.error("", e);
        }
        jedis.lpush(bfoo, bA);
      }
    }).start();

    Response<byte[]> belement = pipe.brpoplpush(bfoo, bbar, 0);
    Response<Long> blen = pipe.llen(bbar);
    Response<List<byte[]>> brange = pipe.lrange(bbar, 0, -1);

    pipe.sync();

    assertThat(belement.get(), equalTo(bA));
    assertThat(blen.get(), equalTo(1L));
    assertThat(brange.get(), contains(bA));
  }

  @Test
  public void lpos() {
    pipe.rpush("foo", "a");
    pipe.rpush("foo", "b");
    pipe.rpush("foo", "c");
    pipe.sync();

    pipe.lpos("foo", "b");
    pipe.lpos("foo", "d");

    assertThat(pipe.syncAndReturnAll(), contains(
        equalTo(1L),
        nullValue()
    ));

    pipe.rpush("foo", "a");
    pipe.rpush("foo", "b");
    pipe.rpush("foo", "b");
    pipe.sync();

    pipe.lpos("foo", "b", LPosParams.lPosParams());
    pipe.lpos("foo", "b", LPosParams.lPosParams().rank(3));
    pipe.lpos("foo", "b", LPosParams.lPosParams().rank(-2));
    pipe.lpos("foo", "b", LPosParams.lPosParams().rank(-5));
    pipe.lpos("foo", "b", LPosParams.lPosParams().rank(1).maxlen(2));
    pipe.lpos("foo", "b", LPosParams.lPosParams().rank(2).maxlen(2));
    pipe.lpos("foo", "b", LPosParams.lPosParams().rank(-2).maxlen(2));

    assertThat(pipe.syncAndReturnAll(), contains(
        equalTo(1L),
        equalTo(5L),
        equalTo(4L),
        nullValue(),
        equalTo(1L),
        nullValue(),
        equalTo(4L)
    ));

    Response<List<Long>> posList1 = pipe.lpos("foo", "b", LPosParams.lPosParams(), 2);
    Response<List<Long>> posList2 = pipe.lpos("foo", "b", LPosParams.lPosParams(), 0);
    Response<List<Long>> posList3 = pipe.lpos("foo", "b", LPosParams.lPosParams().rank(2), 0);
    Response<List<Long>> posList4 = pipe.lpos("foo", "b", LPosParams.lPosParams().rank(2).maxlen(5), 0);
    Response<List<Long>> posList5 = pipe.lpos("foo", "b", LPosParams.lPosParams().rank(-2), 0);
    Response<List<Long>> posList6 = pipe.lpos("foo", "b", LPosParams.lPosParams().rank(-1).maxlen(5), 2);

    pipe.sync();

    assertThat(posList1.get(), contains(1L, 4L));
    assertThat(posList2.get(), contains(1L, 4L, 5L));
    assertThat(posList3.get(), contains(4L, 5L));
    assertThat(posList4.get(), contains(4L));
    assertThat(posList5.get(), contains(4L, 1L));
    assertThat(posList6.get(), contains(5L, 4L));

    // Binary
    pipe.rpush(bfoo, bA);
    pipe.rpush(bfoo, bB);
    pipe.rpush(bfoo, bC);
    pipe.sync();

    pipe.lpos(bfoo, bB);
    pipe.lpos(bfoo, b3);

    assertThat(pipe.syncAndReturnAll(), contains(
        equalTo(1L),
        nullValue()
    ));

    pipe.rpush(bfoo, bA);
    pipe.rpush(bfoo, bB);
    pipe.rpush(bfoo, bA);
    pipe.sync();

    pipe.lpos(bfoo, bB, LPosParams.lPosParams().rank(2));
    pipe.lpos(bfoo, bB, LPosParams.lPosParams().rank(-2).maxlen(5));

    assertThat(pipe.syncAndReturnAll(), contains(
        equalTo(4L),
        equalTo(1L)
    ));

    Response<List<Long>> bposList1 = pipe.lpos(bfoo, bA, LPosParams.lPosParams().maxlen(6), 0);
    Response<List<Long>> bposList2 = pipe.lpos(bfoo, bA, LPosParams.lPosParams().maxlen(6).rank(2), 1);

    pipe.sync();

    assertThat(bposList1.get(), contains(0L, 3L, 5L));
    assertThat(bposList2.get(), contains(3L));
  }

  @Test
  public void lmove() {
    pipe.rpush("foo", "bar1", "bar2", "bar3");

    Response<String> item1 = pipe.lmove("foo", "bar", ListDirection.RIGHT, ListDirection.LEFT);
    Response<List<String>> range1 = pipe.lrange("bar", 0, -1);
    Response<List<String>> range2 = pipe.lrange("foo", 0, -1);

    pipe.sync();

    assertThat(item1.get(), equalTo("bar3"));
    assertThat(range1.get(), contains("bar3"));
    assertThat(range2.get(), contains("bar1", "bar2"));

    // Binary
    pipe.rpush(bfoo, b1, b2, b3);

    Response<byte[]> bitem1 = pipe.lmove(bfoo, bbar, ListDirection.RIGHT, ListDirection.LEFT);
    Response<List<byte[]>> brange1 = pipe.lrange(bbar, 0, -1);
    Response<List<byte[]>> brange2 = pipe.lrange(bfoo, 0, -1);

    pipe.sync();

    assertThat(bitem1.get(), equalTo(b3));
    assertThat(brange1.get(), contains(b3));
    assertThat(brange2.get(), contains(b1, b2));
  }

  @Test
  public void blmove() {
    new Thread(() -> {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        logger.error("", e);
      }
      jedis.rpush("foo", "bar1", "bar2", "bar3");
    }).start();

    Response<String> response = pipe.blmove("foo", "bar", ListDirection.RIGHT, ListDirection.LEFT, 0);
    Response<List<String>> range1 = pipe.lrange("bar", 0, -1);
    Response<List<String>> range2 = pipe.lrange("foo", 0, -1);

    pipe.sync();

    assertThat(response.get(), equalTo("bar3"));
    assertThat(range1.get(), contains("bar3"));
    assertThat(range2.get(), contains("bar1", "bar2"));

    // Binary
    new Thread(() -> {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        logger.error("", e);
      }
      jedis.rpush(bfoo, b1, b2, b3);
    }).start();

    Response<byte[]> bresponse = pipe.blmove(bfoo, bbar, ListDirection.RIGHT, ListDirection.LEFT, 0);
    Response<List<byte[]>> brange1 = pipe.lrange(bbar, 0, -1);
    Response<List<byte[]>> brange2 = pipe.lrange(bfoo, 0, -1);

    pipe.sync();

    assertThat(bresponse.get(), equalTo(b3));
    assertThat(brange1.get(), contains(b3));
    assertThat(brange2.get(), contains(b1, b2));
  }

  @Test
  @SinceRedisVersion(value="7.0.0")
  public void lmpop() {
    String mylist1 = "mylist1";
    String mylist2 = "mylist2";

    // add elements to list
    pipe.lpush(mylist1, "one1", "two1", "three1", "four1", "five1");
    pipe.lpush(mylist2, "one2", "two2", "three2", "four2", "five2");

    Response<KeyValue<String, List<String>>> elements1 = pipe.lmpop(ListDirection.LEFT, mylist1, mylist2);
    Response<KeyValue<String, List<String>>> elements2 = pipe.lmpop(ListDirection.LEFT, 5, mylist1, mylist2);
    Response<KeyValue<String, List<String>>> elements3 = pipe.lmpop(ListDirection.RIGHT, 100, mylist1, mylist2);
    Response<KeyValue<String, List<String>>> elements4 = pipe.lmpop(ListDirection.RIGHT, mylist1, mylist2);

    pipe.sync();

    assertThat(elements1.get().getKey(), equalTo(mylist1));
    assertThat(elements1.get().getValue(), contains("five1"));

    assertThat(elements2.get().getKey(), equalTo(mylist1));
    assertThat(elements2.get().getValue(), contains("four1", "three1", "two1", "one1"));

    assertThat(elements3.get().getKey(), equalTo(mylist2));
    assertThat(elements3.get().getValue(), contains("one2", "two2", "three2", "four2", "five2"));

    assertThat(elements4.get(), nullValue());
  }

  @Test
  @SinceRedisVersion(value="7.0.0")
  public void blmpopSimple() {
    String mylist1 = "mylist1";
    String mylist2 = "mylist2";

    // add elements to list
    pipe.lpush(mylist1, "one1", "two1", "three1", "four1", "five1");
    pipe.lpush(mylist2, "one2", "two2", "three2", "four2", "five2");

    Response<KeyValue<String, List<String>>> elements1 = pipe.blmpop(1L, ListDirection.LEFT, mylist1, mylist2);
    Response<KeyValue<String, List<String>>> elements2 = pipe.blmpop(1L, ListDirection.LEFT, 5, mylist1, mylist2);
    Response<KeyValue<String, List<String>>> elements3 = pipe.blmpop(1L, ListDirection.RIGHT, 100, mylist1, mylist2);
    Response<KeyValue<String, List<String>>> elements4 = pipe.blmpop(1L, ListDirection.RIGHT, mylist1, mylist2);

    pipe.sync();

    assertThat(elements1.get().getKey(), equalTo(mylist1));
    assertThat(elements1.get().getValue(), contains("five1"));

    assertThat(elements2.get().getKey(), equalTo(mylist1));
    assertThat(elements2.get().getValue(), contains("four1", "three1", "two1", "one1"));

    assertThat(elements3.get().getKey(), equalTo(mylist2));
    assertThat(elements3.get().getValue(), contains("one2", "two2", "three2", "four2", "five2"));

    assertThat(elements4.get(), nullValue());
  }
}
