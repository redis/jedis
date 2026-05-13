package redis.clients.jedis.benchmark.pubsub;

import org.openjdk.jmh.annotations.*;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * Pub/Sub push message handling benchmark.
 *
 * <p>Measures end-to-end throughput of the pub/sub pipeline:
 * <pre>
 *   publisher.publish()
 *       → server fan-out
 *       → subscriber socket read
 *       → RESP push frame parser
 *       → JedisPubSub dispatch
 *       → onMessage callback
 * </pre>
 *
 * <p>This is an integration benchmark. For pure push-frame parser performance
 * (no I/O), see {@code ProtocolReadBenchmark.readWith*PushMessages} which
 * exercises the same code paths at nanosecond resolution against pre-built
 * RESP buffers. The two are complementary:
 * <ul>
 *   <li>Microbenchmark regresses → parser/decoder code change.</li>
 *   <li>Integration benchmark regresses (alone) → network or subscriber
 *       thread dispatch logic regressed.</li>
 *   <li>Both regress together → both paths share the affected code.</li>
 * </ul>
 *
 * <p><b>Why single-threaded.</b> Pub/sub throughput on one channel is bounded
 * by the single-threaded subscriber connection's read+dispatch loop — that
 * loop is exactly what we want to track
 *
 * <p>Throughput here measures pub/sub round-trips per second
 * (publish → receive).
 *
 * <p>WARNING: {@code @Setup} flushes the target Redis. Do not run against
 * shared Redis.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Threads(1)
public class PubSubPushBenchmark {

    private static final String CHANNEL = "bench:pubsub:push";
    private static final String MESSAGE = "m";

    private static final EndpointConfig endpoint = Endpoints.getRedisEndpoint("standalone0");

    private Jedis publisher;
    private Jedis subscriberConn;
    private JedisPubSub subscriber;
    private Thread subscriberThread;

    /**
     * Single-slot hand-off between the JedisPubSub callback (on the
     * subscriber thread) and the benchmark thread. SynchronousQueue
     * guarantees strict 1:1 message-to-take pairing: each onMessage
     * put() blocks until the benchmark calls take(), preventing the
     * subscriber from running ahead and ensuring every reported op
     * represents exactly one round-trip.
     */
    private final SynchronousQueue<String> handoff = new SynchronousQueue<>();

    @Setup(Level.Trial)
    public void setup() throws InterruptedException {
        publisher = newJedis();
        subscriberConn = newJedis();

        subscriber = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                try {
                    handoff.put(message);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };

        subscriberThread = new Thread(() -> {
            try {
                subscriberConn.subscribe(subscriber, CHANNEL);
            } catch (Exception e) {
                // subscribe() returns when unsubscribed at teardown — expected.
            }
        }, "pubsub-bench-subscriber");
        subscriberThread.setDaemon(true);
        subscriberThread.start();

        // Wait until the subscription is active before letting JMH proceed
        // to warmup. Without this, the first few warmup iterations would
        // publish to a channel with no subscriber and stall on take().
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!subscriber.isSubscribed()) {
            if (System.nanoTime() > deadlineNanos) {
                throw new IllegalStateException("Subscriber did not activate within 5s");
            }
            Thread.sleep(10);
        }
    }

    @TearDown(Level.Trial)
    public void teardown() throws InterruptedException {
        if (subscriber != null && subscriber.isSubscribed()) {
            subscriber.unsubscribe();
        }
        if (subscriberThread != null) {
            subscriberThread.join(2000);
        }
        if (publisher != null) publisher.close();
        if (subscriberConn != null) subscriberConn.close();
    }

    /**
     * Publishes one message and waits for the subscriber callback to fire.
     * Throughput score = pub/sub round-trips per second.
     */
    @Benchmark
    public String publishAndReceive() throws InterruptedException {
        publisher.publish(CHANNEL, MESSAGE);
        return handoff.take();
    }

    private static Jedis newJedis() {
        Jedis j = new Jedis(endpoint.getHostAndPort());
        String pwd = endpoint.getPassword();
        if (pwd != null && !pwd.isEmpty()) j.auth(pwd);
        return j;
    }
}
