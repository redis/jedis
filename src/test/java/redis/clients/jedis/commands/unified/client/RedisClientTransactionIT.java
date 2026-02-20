package redis.clients.jedis.commands.unified.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.AbstractTransaction;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.Response;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.unified.UnifiedJedisCommandsTestBase;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class RedisClientTransactionIT extends UnifiedJedisCommandsTestBase {

  public RedisClientTransactionIT(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return RedisClientCommandsTestHelper.getClient(protocol);
  }

  @BeforeEach
  public void setUp() {
    RedisClientCommandsTestHelper.clearData();
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

    List<Object> responses;
    List<Object> expected = new ArrayList<>(totalCount);

    try (AbstractTransaction transaction = jedis.multi()) {
      for (int i = 0; i < count; i++) {
        transaction.get("foo" + i);
        expected.add("bar" + i);
      }
      for (int i = 0; i < count; i++) {
        transaction.lrange("foobar" + i, 0, -1);
        expected.add(Arrays.asList("foo" + i, "bar" + i));
      }
      responses = transaction.exec();
    }

    for (int i = 0; i < totalCount; i++) {
      assertEquals(expected.get(i), responses.get(i));
    }
  }

  @Test
  public void watch() {
    try (AbstractTransaction tx = jedis.transaction(false)) {
      assertEquals("OK", tx.watch("mykey", "somekey"));
      tx.multi();

      jedis.set("mykey", "bar");

      tx.set("mykey", "foo");
      assertNull(tx.exec());

      assertEquals("bar", jedis.get("mykey"));
    }
  }

  /**
   * Verify manual multi and commands send before sending multi does not cause out of order
   * responses
   */
  @Test
  public void transactionManualWithCommandsBeforeMulti() {

    try (AbstractTransaction tx = jedis.transaction(false)) {

      // command before multi
      Response<String> txSetBeforeMulti = tx.set("mykey", "before_multi");
      Response<String> txGetBeforeMulti = tx.get("mykey");
      assertEquals("OK", txSetBeforeMulti.get());
      assertEquals("before_multi", txGetBeforeMulti.get());

      tx.multi();
      Response<String> txSet = tx.set("mykey", "foo");
      Response<String> txGet = tx.get("mykey");
      List<Object> txResp = tx.exec();

      assertEquals("OK", txSet.get());
      assertEquals("foo", txGet.get());
      assertEquals(2, txResp.size());
      assertEquals("OK", txResp.get(0));
      assertEquals("foo", txResp.get(1));
    }
  }

  @Test
  public void transactionManualCommandsBeforeMultiReturnResponseImmediately() {

    try (AbstractTransaction tx = jedis.transaction(false)) {
      // command before multi
      Response<String> txSet = tx.set("mykey", "foo");
      Response<String> txGet = tx.get("mykey");
      assertEquals("OK", txSet.get());
      assertEquals("foo", txGet.get());
    }
  }

  @Test
  public void transactionManualCommandsBeforeMultiPropagateException() {

    try (AbstractTransaction tx = jedis.transaction(false)) {
      Response<String> txSet = tx.set("mykey", "foo");
      Response<Long> txIncr = tx.incr("mykey");
      assertEquals("OK", txSet.get());
      JedisDataException ex = assertThrows(JedisDataException.class, txIncr::get);
      assertEquals("ERR value is not an integer or out of range", ex.getMessage());
    }
  }

  @Test
  public void publishInTransaction() {
    try (AbstractTransaction tx = jedis.multi()) {
      Response<Long> p1 = tx.publish("foo", "bar");
      Response<Long> p2 = tx.publish("foo".getBytes(), "bar".getBytes());
      tx.exec();

      assertEquals(0, p1.get().longValue());
      assertEquals(0, p2.get().longValue());
    }
  }
}
