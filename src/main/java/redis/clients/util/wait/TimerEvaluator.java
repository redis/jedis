package redis.clients.util.wait;


/**
 * This evaluator checks if a specific duration is over. We will use this evaluator for testing purposes.
 * It allows to validate if one of the Wait strategies works as expected from a timing point of view.
 *
 * @author nosqlgeek (david.maier@redislabs.com)
 */
public class TimerEvaluator implements IEvaluator {


    //Attributes
    private long startTime;
    private long endTime;
    private long duration;


    /**
     * Default Ctor
     */
    public TimerEvaluator(long duration) {

        startTime = -1;
        endTime = -1;
        this.duration = duration;
    }


    /**
     * Check if the duration was reached since you checked the last time
     *
     * @return
     */
    @Override
    public boolean check() {

        if (startTime == -1) {

            startTime = System.currentTimeMillis();

        } else {

            endTime = System.currentTimeMillis();

            if (endTime - startTime >= duration) return true;
        }

        //Return false by default
        return false;
    }
}
