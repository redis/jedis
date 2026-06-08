package redis.clients.jedis;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.util.SafeEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static redis.clients.jedis.Protocol.ResponseKeyword.MESSAGE;
import static redis.clients.jedis.Protocol.ResponseKeyword.SUBSCRIBE;

public class JedisPubSubBaseTest  {
    @Test
    public void testProceed_givenThreadInterrupt_exitLoop() throws InterruptedException {
        // setup
        final JedisPubSubBase<String> pubSub = new JedisPubSubBase<String>() {

            @Override
            public void onMessage(String channel, String message) {
                fail("this should not happen when thread is interrupted");
            }

            @Override
            protected String encode(byte[] raw) {
                return SafeEncoder.encode(raw);
            }
        };

        final Connection mockConnection = mock(Connection.class);
        final List<Object> mockSubscribe = Arrays.asList(
                SUBSCRIBE.getRaw(), "channel".getBytes(), 1L
        );
        final List<Object> mockResponse = Arrays.asList(
                MESSAGE.getRaw(), "channel".getBytes(), "message".getBytes()
        );

        when(mockConnection.getUnflushedObject()).
                thenReturn(mockSubscribe, mockResponse);


        // action
        final AtomicReference<Throwable> workerError = new AtomicReference<>();
        final Thread thread = new Thread(() -> {
            Thread.currentThread().interrupt();
            try {
                pubSub.proceed(mockConnection, "channel");
            } catch (Throwable t) {
                workerError.set(t);
            }
        }, "pubsub-interrupt-test");
        thread.start();

        thread.join(TimeUnit.SECONDS.toMillis(1));
        assertFalse(thread.isAlive(),
            "proceed() should return promptly when the calling thread is interrupted");
        if (workerError.get() != null) {
            throw new AssertionError("proceed() threw unexpectedly", workerError.get());
        }
    }
}
