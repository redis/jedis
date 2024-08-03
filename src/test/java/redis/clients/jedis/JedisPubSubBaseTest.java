package redis.clients.jedis;

import junit.framework.TestCase;
import redis.clients.jedis.util.SafeEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static redis.clients.jedis.Protocol.ResponseKeyword.MESSAGE;
import static redis.clients.jedis.Protocol.ResponseKeyword.SUBSCRIBE;

public class JedisPubSubBaseTest extends TestCase {

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


        final CountDownLatch countDownLatch = new CountDownLatch(1);
        // action
        final Thread thread = new Thread(() -> {
            Thread.currentThread().interrupt();
            pubSub.proceed(mockConnection, "channel");

            countDownLatch.countDown();
        });
        thread.start();

        assertTrue(countDownLatch.await(10, TimeUnit.MILLISECONDS));

    }
}
