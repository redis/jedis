package redis.clients.jedis.commands.unified.pooled;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

import redis.clients.jedis.commands.unified.UnifiedJedisCommandsTestBase;

public class PooledPipeliningTest extends UnifiedJedisCommandsTestBase {

  protected Pipeline pipeline;
  protected Transaction transaction;

  @BeforeClass
  public static void prepare() throws InterruptedException {
    jedis = PooledCommandsTestHelper.getPooled();
  }

  @AfterClass
  public static void cleanUp() {
    jedis.close();
  }

  @Before
  public void setUp() {
    PooledCommandsTestHelper.clearData();
    pipeline = ((JedisPooled) jedis).pipelined();
    transaction = jedis.multi();
  }

  @After
  public void tearDown() {
    pipeline.close();
    transaction.close();
  }

  @Test
  public void simple() {
    final int count = 10;
    int totalCount = 0;
    for (int i = 0; i < count; i++) {
      jedis.set("foo" + i, "bar" + i);
    }
    totalCount += count;
    for (int i = 0; i < count; i++) {
      jedis.rpush("foobar" + i, "foo" + i, "bar" + i);
    }
    totalCount += count;

    List<Response<?>> responses = new ArrayList<>(totalCount);
    List<Object> expected = new ArrayList<>(totalCount);
    for (int i = 0; i < count; i++) {
      responses.add(pipeline.get("foo" + i));
      expected.add("bar" + i);
    }
    for (int i = 0; i < count; i++) {
      responses.add(pipeline.lrange("foobar" + i, 0, -1));
      expected.add(Arrays.asList("foo" + i, "bar" + i));
    }
    pipeline.sync();

    for (int i = 0; i < totalCount; i++) {
      assertEquals(expected.get(i), responses.get(i).get());
    }
  }

  @Test
  public void transaction() {
    final int count = 10;
    int totalCount = 0;
    for (int i = 0; i < count; i++) {
      jedis.set("foo" + i, "bar" + i);
    }
    totalCount += count;
    for (int i = 0; i < count; i++) {
      jedis.rpush("foobar" + i, "foo" + i, "bar" + i);
    }
    totalCount += count;

    List<Object> expected = new ArrayList<>(totalCount);
    for (int i = 0; i < count; i++) {
      transaction.get("foo" + i);
      expected.add("bar" + i);
    }
    for (int i = 0; i < count; i++) {
      transaction.lrange("foobar" + i, 0, -1);
      expected.add(Arrays.asList("foo" + i, "bar" + i));
    }

    List<Object> responses = transaction.exec();
    for (int i = 0; i < totalCount; i++) {
      assertEquals(expected.get(i), responses.get(i));
    }
  }
}
