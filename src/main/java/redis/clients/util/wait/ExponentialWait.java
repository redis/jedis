package redis.clients.util.wait;

/**
 * Performs an exponential wait by taking the result of an evaluator into account.
 * So it waits until a maximum number of retries is reached or until the evaluator is returning a positive result.
 *
 * @author nosqlgeek (david.maier@redislabs.com)
 */
public class ExponentialWait implements IWaitStrategy {


    //Default
    //Wait for max. 4 mins: 1000 + 2000 + 4000 + 8000 + 16000 + 32000 + 64000 + 128000 = 255000ms
    public static final long WAIT_TIME = 1000;
    public static final int RETRY_FACTOR = 2;
    public static final int MAX_RETRIES = 8;


    //Members
    private IEvaluator eval;
    private long waitTime;
    private int retryFactor;
    private int maxRetries;

    /**
     * The Ctor which uses the default values
     *
     * @param eval
     */
    public ExponentialWait(IEvaluator eval) {

        this(WAIT_TIME, RETRY_FACTOR, MAX_RETRIES, eval);
    }

    /**
     * Full Ctor
     * @param waitTime
     * @param retryFactor
     * @param maxRetries
     * @param eval
     */
    public ExponentialWait(long waitTime, int retryFactor, int maxRetries, IEvaluator eval) {

        this.waitTime = waitTime;
        this.retryFactor = retryFactor;
        this.maxRetries = maxRetries;
        this.eval = eval;
    }

    /**
     * Wait until the max. number of retries is reached or until the evaluator returns a success.
     *
     * @throws InterruptedException
     */
    @Override
    public void waitFor() throws InterruptedException {

        //Init
        int numTries = 0;
        boolean isWaiting = true;
        long currWait = waitTime;

        //While none of the conditions was met
        while (isWaiting) {

            //If the evaluator returns true then waiting is over
            if (eval.check()) {

                isWaiting = false;

            } else {

                //Continue waiting by increasing the next wait time by the given retry factor
                numTries++;
                Thread.sleep(currWait);
                currWait = currWait*retryFactor;

                //Don't wait anymore if the maximum number of retries is reached
                if (numTries == maxRetries) isWaiting = false;
            }
        }
    }
}
