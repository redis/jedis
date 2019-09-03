package redis.clients.jedis.tests.utils.wait;

import static org.junit.Assert.*;
import org.junit.*;
import redis.clients.util.wait.LinearWait;
import redis.clients.util.wait.IWaitStrategy;
import redis.clients.util.wait.TimerEvaluator;

/**
 * Test the linear wait strategy
 *
 * @author nosqlgeek (david.maier@redislabs.com)
 */
public class LinearWaitTest {

    public static final long MAX_TIME = 300000;


    /**
     * Test linear wait in general
     *
     * @throws InterruptedException
     */
    @Test
    public void testLinearWait() throws InterruptedException {

        /*
         * We expect to wait 100 * 10 = 1000ms
         * But we would anyway stop waiting after MAX_TIME
         */
        IWaitStrategy w = new LinearWait(100, 10, new TimerEvaluator(MAX_TIME));

        long expected = 1000;
        long tolerance = (expected / 10)*2;

        System.out.println("Expecting to wait for: " + expected + " ms");

        long start = System.currentTimeMillis();
        w.waitFor();
        long end = System.currentTimeMillis();

        long timeWaited = end - start;

        System.out.println("Waited for: " + timeWaited + " ms");

        assertTrue(timeWaited <= (expected + tolerance));
    }

    /**
     * Test linear wait with default settings
     *
     * @throws InterruptedException
     */
    @Test
    public void testLinearWaitDefault() throws InterruptedException {

        IWaitStrategy w = new LinearWait(new TimerEvaluator(MAX_TIME));

        //4 mins
        long expected = 240000;
        long tolerance = (expected / 10)*2;

        long start = System.currentTimeMillis();
        w.waitFor();
        long end = System.currentTimeMillis();

        long timeWaited = end - start;
        System.out.println("Waited for: " + timeWaited + " ms");

        assertTrue(timeWaited <= (expected + tolerance));
    }
}
