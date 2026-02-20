package redis.clients.jedis.commands.unified.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  public void publishInTransaction() {
    try (AbstractTransaction tx = jedis.multi()) {
      Response<Long> p1 = tx.publish("foo", "bar");
      Response<Long> p2 = tx.publish("foo".getBytes(), "bar".getBytes());
      tx.exec();

      assertEquals(0, p1.get().longValue());
      assertEquals(0, p2.get().longValue());
    }
  }

  @Nested
  class ResponseHandlingIT {

    @BeforeEach
    public void setUp() {
      RedisClientCommandsTestHelper.clearData();
    }

    @Test
    public void notInTransactionResponseReturnsExpectedValue() {
      // Commands executed before multi() should return immediate responses
      try (AbstractTransaction tx = jedis.transaction(false)) {
        Response<String> setResponse = tx.set("key1", "value1");
        Response<String> getResponse = tx.get("key1");

        // Responses should be available immediately (not in transaction yet)
        assertEquals("OK", setResponse.get());
        assertEquals("value1", getResponse.get());
      }
    }

    @Test
    public void notInTransactionResponsePropagatesException() {
      // Commands executed before multi() that fail should propagate exceptions
      try (AbstractTransaction tx = jedis.transaction(false)) {
        Response<String> setResponse = tx.set("key1", "not_a_number");
        Response<Long> incrResponse = tx.incr("key1");

        // Set should succeed
        assertEquals("OK", setResponse.get());

        // Incr should fail and propagate exception immediately
        JedisDataException ex = assertThrows(JedisDataException.class, incrResponse::get);
        assertEquals("ERR value is not an integer or out of range", ex.getMessage());
      }
    }

    @Test
    public void inTransactionResponseThrowsBeforeExec() {
      // Calling response.get() before exec() should throw IllegalStateException
      try (AbstractTransaction tx = jedis.multi()) {
        Response<String> response = tx.set("key1", "value1");

        // Attempting to get response before exec() should throw
        IllegalStateException ex = assertThrows(IllegalStateException.class, response::get);
        assertTrue(ex.getMessage()
            .contains("Please close pipeline or multi block before calling this method"));

        // Now exec the transaction
        tx.exec();

        // After exec, response should be available
        assertEquals("OK", response.get());
      }
    }

    @Test
    public void afterExecResponseContainsActualResults() {
      // After exec(), Response objects should contain actual results from Redis
      try (AbstractTransaction tx = jedis.multi()) {
        Response<String> setResponse = tx.set("key1", "value1");
        Response<String> getResponse = tx.get("key1");

        tx.exec();

        // Verify Response objects contain correct values
        assertEquals("OK", setResponse.get());
        assertEquals("value1", getResponse.get());
      }
    }

    @Test
    public void execReturnsListWithAllResultsInOrder() {
      // exec() should return List<Object> with all command results in order
      try (AbstractTransaction tx = jedis.multi()) {
        tx.set("key1", "value1");
        tx.get("key1");
        tx.del("key1");

        List<Object> results = tx.exec();

        // Verify all results are in the correct order
        assertEquals(3, results.size());
        assertEquals("OK", results.get(0)); // set key1
        assertEquals("value1", results.get(1)); // get key1
        assertEquals(1L, results.get(2)); // del key1
      }
    }
  }
}
