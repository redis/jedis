package redis.clients.jedis.tests.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * Created by alompo on 27.11.17.
 */
public class AwaitilityUtils {
    private AwaitilityUtils() {
    }

    /**
     * We force await to wait at least the desired amount of time
     * Using await alone with the until clause will timeout by default after 10 seconds
     * which means that if the amount of time we need to wait is greater it will timeout too early
     * But with the atLeast clause it will work fine.
     * @param durationInMilliseconds
     */
    public static void waitFor(long durationInMilliseconds) {
        final long now = System.currentTimeMillis();
        await().atLeast(durationInMilliseconds, TimeUnit.MILLISECONDS).until(timeIsElapsed(now, durationInMilliseconds));
    }

    private static Callable<Boolean> timeIsElapsed(final long now, final long duration) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return System.currentTimeMillis() - now >= duration;
            }
        };
    }
}
