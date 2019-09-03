package redis.clients.util.wait;

/**
 * A simple wait allows you to just wait for a specific amount of time.
 *
 * @author nosqlgeek (david.maier@redislabs.com)
 */
public class SimpleWait implements IWaitStrategy {


    /**
     * Time to wait for in ms
     */
    private long time;


    /**
     * Default Ctor
     *
     * @param time in ms
     */
    public SimpleWait(long time) {

        this.time = time;
    }


    @Override
    public void waitFor() throws InterruptedException {

        Thread.sleep(time);

    }
}
