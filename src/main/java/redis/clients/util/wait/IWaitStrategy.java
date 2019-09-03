package redis.clients.util.wait;

/**
 * Describes a Wait strategy. A wait strategy is implemented by implementing the method 'waitFor'.
 * All relevant parameters should be passed to the constructor.
 *
 * @author nosqlgeek (david.maier@redislabs.com)
 */
public interface IWaitStrategy {

    /**
     * Wait based on the wait strategy.
     * Waiting might be interrupted for some reason.
     * In this case an InterruptedException is thrown
     *
     * @throws InterruptedException
     */
    void waitFor() throws InterruptedException;
}
