package redis.clients.jedis.tests.commands.async;

import org.junit.Test;
import redis.clients.jedis.async.callback.AsyncResponseCallback;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.tests.commands.JedisCommandTestBase;
import redis.clients.util.SafeEncoder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;

public class AsyncObjectCommandsTest extends AsyncJedisCommandTestBase {

    private String key = "mylist";
    private byte[] binaryKey = SafeEncoder.encode(key);


    @Test
    public void objectRefcount() throws InterruptedException {
        asyncJedis.lpush(new DoNothingCallback<Long>(), key, "hello world");

        AsyncJUnitTestCallback<Long> callback = new AsyncJUnitTestCallback<Long>();
        asyncJedis.objectRefcount(callback, key);
        callback.waitForComplete(100);

        if (callback.getException() != null) {
            throw callback.getException();
        }
        assertEquals(new Long(1L), callback.getResponse());

	    // Binary
        asyncJedis.objectRefcount(callback, binaryKey);
        callback.waitForComplete(100);

        if (callback.getException() != null) {
            throw callback.getException();
        }
        assertEquals(new Long(1L), callback.getResponse());
    }

    @Test
    public void objectEncoding() {
        asyncJedis.lpush(new DoNothingCallback<Long>(), key, "hello world");

        AsyncJUnitTestCallback<String> callback = new AsyncJUnitTestCallback<String>();
        asyncJedis.objectEncoding(callback, key);
        callback.waitForComplete(100);

        if (callback.getException() != null) {
            throw callback.getException();
        }
        assertEquals("ziplist", callback.getResponse());

        // Binary
        asyncJedis.lpush(new DoNothingCallback<Long>(), binaryKey, "hello world".getBytes());

        AsyncJUnitTestCallback<byte[]> callback2 = new AsyncJUnitTestCallback<byte[]>();
        asyncJedis.objectEncoding(callback2, binaryKey);
        callback2.waitForComplete(100);

        assertArrayEquals("ziplist".getBytes(), callback2.getResponse());
    }

    @Test
    public void objectIdletime() throws InterruptedException {
        asyncJedis.lpush(new DoNothingCallback<Long>(), key, "hello world");

        AsyncJUnitTestCallback<Long> callback = new AsyncJUnitTestCallback<Long>();
        asyncJedis.objectIdletime(callback, key);
        callback.waitForComplete(100);

        assertEquals(new Long(0), callback.getResponse());

        // Binary
        asyncJedis.objectIdletime(callback, binaryKey);
        callback.waitForComplete(100);

        assertEquals(new Long(0), callback.getResponse());
    }

}