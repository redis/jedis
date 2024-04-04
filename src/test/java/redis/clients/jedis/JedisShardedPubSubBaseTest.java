package redis.clients.jedis;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static redis.clients.jedis.Protocol.ResponseKeyword.SMESSAGE;
import static redis.clients.jedis.Protocol.ResponseKeyword.SSUBSCRIBE;

public class JedisShardedPubSubBaseTest extends TestCase {

    public void testProceed_givenThreadInterrupt_exitLoop() throws InterruptedException {
        // setup
        final JedisShardedPubSubBase<String> pubSub = new JedisShardedPubSubBase<String>() {

            @Override
            public void onSMessage(String channel, String message) {
                fail("this should not happen when thread is interrupted");
            }

            @Override
            protected String encode(byte[] raw) {
                return new String(raw);
            }

        };

        final Connection mockConnection = mock(Connection.class);
        final List<Object> mockSubscribe = Arrays.asList(
                SSUBSCRIBE.getRaw(), "channel".getBytes(), 1L
        );
        final List<Object> mockResponse = Arrays.asList(
                SMESSAGE.getRaw(), "channel".getBytes(), "message".getBytes()
        );
        when(mockConnection.getUnflushedObject()).thenReturn(mockSubscribe, mockResponse);


        final CountDownLatch countDownLatch = new CountDownLatch(1);
        // action
        final Thread thread = new Thread(() -> {
            Thread.currentThread().interrupt();
            pubSub.proceed(mockConnection, "channel");

            countDownLatch.countDown();
        });
        thread.start();

        assertTrue(countDownLatch.await(20, TimeUnit.MILLISECONDS));

    }
}