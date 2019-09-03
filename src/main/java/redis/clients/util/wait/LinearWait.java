package redis.clients.util.wait;


/**
 * A linear wait is an exponential with with a retry factor of 1
 *
 *  @author nosqlgeek (david.maier@redislabs.com)
 */
public class LinearWait extends ExponentialWait {


    //Default
    //Wait for max. 4mins: 2000 * 120 = 240000ms
    public static final long WAIT_TIME = 2000;
    public static final int MAX_RETRIES = 120;


    /**
     * Default Ctor
     *
     * @param eval
     */
    public LinearWait(IEvaluator eval) {
        super(WAIT_TIME, 1, MAX_RETRIES, eval);
    }

    /**
     * Full Ctor
     *
     * @param waitTime
     * @param maxRetries
     * @param eval
     */
    public LinearWait(long waitTime, int maxRetries, IEvaluator eval) {

        super(waitTime, 1, maxRetries, eval);
    }

}
