package redis.clients.util.wait;

/**
 * An Evaluator has a single method in order to check
 * if an external condition is true.
 *
 * @author nosqlgeek (david.maier@redislabs.com)
 */
public interface IEvaluator {

    /**
     * Check if the condition is true
     *
     * @return
     */
    boolean check();

}