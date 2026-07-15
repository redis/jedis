package redis.clients.jedis.csc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.RedisProtocol;

public class AbstractCacheConcurrencyTest {

  /**
   * A store whose read is made to block once, so a reader can be parked inside
   * {@link AbstractCache#get(CacheKey)} while another thread tries to mutate the cache.
   */
  private static class BlockingReadCache extends HashMap<CacheKey, CacheEntry> {

    private final CacheKey blockOn;
    private final CountDownLatch enteredGet = new CountDownLatch(1);
    private final CountDownLatch proceedGet = new CountDownLatch(1);

    BlockingReadCache(CacheKey blockOn) {
      this.blockOn = blockOn;
    }

    @Override
    public CacheEntry get(Object key) {
      if (blockOn.equals(key)) {
        enteredGet.countDown();
        try {
          proceedGet.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      return super.get(key);
    }
  }

  @Test
  public void getHoldsLockWhileReadingStore() throws InterruptedException {
    CacheKey readKey = new CacheKey<>(new CommandObjects(RedisProtocol.RESP3).get("read"));
    CacheKey writeKey = new CacheKey<>(new CommandObjects(RedisProtocol.RESP3).get("write"));

    BlockingReadCache store = new BlockingReadCache(readKey);
    Map<CacheKey, CacheEntry> map = store;
    TestCache cache = new TestCache(map);
    cache.set(readKey, new CacheEntry<>(readKey, "read-value", null));

    Thread reader = new Thread(() -> cache.get(readKey), "reader");
    reader.start();

    // reader is now parked inside get(), reading the store
    assertTrue(store.enteredGet.await(5, TimeUnit.SECONDS), "reader never entered get()");

    AtomicBoolean writerReturned = new AtomicBoolean(false);
    CountDownLatch writerDone = new CountDownLatch(1);
    Thread writer = new Thread(() -> {
      cache.set(writeKey, new CacheEntry<>(writeKey, "write-value", null));
      writerReturned.set(true);
      writerDone.countDown();
    }, "writer");
    writer.start();

    // a concurrent mutation must not proceed while a read is in flight
    boolean finishedEarly = writerDone.await(1, TimeUnit.SECONDS);
    boolean blocked = !finishedEarly;

    store.proceedGet.countDown();
    reader.join(5000);
    assertTrue(writerDone.await(5, TimeUnit.SECONDS), "writer never completed");
    writer.join(5000);

    assertTrue(writerReturned.get(), "writer never returned");
    assertTrue(blocked,
      "set() ran concurrently with an in-flight get(); read path is unsynchronised");
    assertEquals("read-value", cache.get(readKey).getValue());
  }
}
