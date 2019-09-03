package redis.clients.jedis.tests.utils.wait;

import static org.junit.Assert.*;
import org.junit.*;
import redis.clients.util.wait.IWaitStrategy;
import redis.clients.util.wait.SimpleWait;

/**
 * Test the simple wait strategy
 *
 * @author nosqlgeek (david.maier@redislabs.com)
 */
public class SimpleWaitTest {

    /**
     * Test if a simple wait is waiting for the specified duration
     *
     * @throws InterruptedException
     */
    @Test
    public void testSimpleWait() throws InterruptedException {

        long start = System.currentTimeMillis();
        IWaitStrategy w = new SimpleWait(1000);
        w.waitFor();
        long end = System.currentTimeMillis();
        assertTrue(end - start >= 1000);

    }

}
