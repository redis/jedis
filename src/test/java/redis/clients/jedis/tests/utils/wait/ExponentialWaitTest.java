package redis.clients.jedis.tests.utils.wait;

import static org.junit.Assert.*;
import org.junit.*;
import redis.clients.util.wait.ExponentialWait;
import redis.clients.util.wait.IWaitStrategy;
import redis.clients.util.wait.TimerEvaluator;


/**
 * Test the exponential wait strategy
 *
 * @author nosqlgeek (david.maier@redislabs.com)
 */
public class ExponentialWaitTest  {

    //To wait max. this amount of time. Huge value which is passed over to wait forever
    public static final long MAX_TIME = 300000;

    /**
     * Test the exponential wait in general
     *
     * @throws InterruptedException
     */
    @Test
    public void testExponentialWait() throws InterruptedException {

        /*
         * We expect to wait 50 + 250 + 1250 = 1550ms
         * But we would anyway stop waiting after MAX_TIME
         */
        IWaitStrategy w = new ExponentialWait(50, 5, 3, new TimerEvaluator(MAX_TIME));

        long expected = 1550;
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
     * Test the exponential wait with the default settings
     *
     * @throws InterruptedException
     */
    @Test
    public void testExponentialWaitDefault() throws InterruptedException {

        IWaitStrategy w = new ExponentialWait(new TimerEvaluator(MAX_TIME));

        //4 mins
        long expected = 255000;
        long tolerance = (expected / 10)*2;

        long start = System.currentTimeMillis();
        w.waitFor();
        long end = System.currentTimeMillis();

        System.out.println("Waited for: " + (end - start) + " ms");

        assertTrue((end - start) <= (expected + tolerance));
    }

    /**
     * Test if waiting completes as expected
     *
     * @throws InterruptedException
     */
    @Test
    public void testExponentialWaitCompleted() throws InterruptedException {

        //100 + 200 + 400 + 800 = 1500ms
        IWaitStrategy w = new ExponentialWait(100, 2, 4, new TimerEvaluator(3000));

        long start = System.currentTimeMillis();
        w.waitFor();
        long end = System.currentTimeMillis();

        System.out.println("Waited for: " + (end - start) + " ms");

        //Waiting ends after the exponential wait time is over (approx. 1500ms)
        assertTrue(end - start <= 2000);
    }


    /**
     * Test if the evaluator is interupting the wait as expected
     *
     * @throws InterruptedException
     */
    @Test
    public void testExponentialWaitEvalSuccess() throws InterruptedException {

        //100 + 200 + 400 + 800 = 1500ms
        IWaitStrategy w = new ExponentialWait(100, 2, 4, new TimerEvaluator(500));

        long start = System.currentTimeMillis();
        w.waitFor();
        long end = System.currentTimeMillis();

        System.out.println("Waited for: " + (end - start) + " ms");

        //Waiting ends before the exponential wait time is over (approx. 1500ms)
        assertTrue(end - start <= 1200);
    }

}
