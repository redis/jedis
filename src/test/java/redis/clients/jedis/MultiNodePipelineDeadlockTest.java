package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.time.Duration;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class MultiNodePipelineDeadlockTest {

  @Test
  public void testPipelineDoesNotDeadlock() throws Exception {
    HostAndPort shardA = new HostAndPort("shardA", 6379);
    HostAndPort shardB = new HostAndPort("shardB", 6379);

    class FakeConnection extends Connection {
      private final String mockResponse;
      private final Runnable onClose;
      
      FakeConnection(String mockResponse, Runnable onClose) {
        super();
        this.mockResponse = mockResponse;
        this.onClose = onClose;
      }

      @Override
      public void sendCommand(CommandArguments args) {
        // do nothing
      }

      @Override
      public List<Object> getMany(int count) {
        return java.util.Collections.singletonList((Object)mockResponse.getBytes());
      }

      @Override
      public void close() {
        if (onClose != null) {
          onClose.run();
        }
      }
    }

    // A fake pool that only allows 1 connection at a time. If acquire is called again without release, it blocks.
    class FakePool {
      private final java.util.concurrent.Semaphore lock = new java.util.concurrent.Semaphore(1);
      private final String response;
      
      FakePool(String response) {
        this.response = response;
      }

      Connection getResource() {
        try {
          lock.acquire();
          return new FakeConnection(response, () -> lock.release());
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }

    FakePool poolA = new FakePool("OK");
    FakePool poolB = new FakePool("OK");

    // Create our test pipeline implementation
    class TestPipeline extends MultiNodePipelineBase {
      TestPipeline() {
        super(new CommandObjects(RedisProtocol.RESP3));
      }
      
      @Override
      protected HostAndPort getNodeKey(CommandArguments args) {
        // Simple routing based on key
        for (Object arg : args) {
          if (arg instanceof byte[]) {
            String s = redis.clients.jedis.util.SafeEncoder.encode((byte[])arg);
            if (s.equals("A")) return shardA;
            if (s.equals("B")) return shardB;
          }
        }
        return shardA;
      }

      @Override
      protected Connection getConnection(HostAndPort nodeKey) {
        return nodeKey.equals(shardA) ? poolA.getResource() : poolB.getResource();
      }
    }

    CyclicBarrier barrier = new CyclicBarrier(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);

    Future<?> f1 = executor.submit(() -> {
      try (TestPipeline p1 = new TestPipeline()) {
        CommandArguments argsA = new CommandArguments(Protocol.Command.GET).key("A");
        CommandArguments argsB = new CommandArguments(Protocol.Command.GET).key("B");
        CommandObject<String> cmdA = new CommandObject<>(argsA, BuilderFactory.STRING);
        CommandObject<String> cmdB = new CommandObject<>(argsB, BuilderFactory.STRING);
        
        p1.appendCommand(cmdA);
        barrier.await();
        
        p1.appendCommand(cmdB);
        p1.sync();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    Future<?> f2 = executor.submit(() -> {
      try (TestPipeline p2 = new TestPipeline()) {
        CommandArguments argsB = new CommandArguments(Protocol.Command.GET).key("B");
        CommandArguments argsA = new CommandArguments(Protocol.Command.GET).key("A");
        CommandObject<String> cmdB = new CommandObject<>(argsB, BuilderFactory.STRING);
        CommandObject<String> cmdA = new CommandObject<>(argsA, BuilderFactory.STRING);
        
        p2.appendCommand(cmdB);
        barrier.await();
        
        p2.appendCommand(cmdA);
        p2.sync();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    // If the fix works, both threads should complete quickly. If not, they timeout.
    try {
      assertDoesNotThrow(() -> {
        f1.get(5, TimeUnit.SECONDS);
        f2.get(5, TimeUnit.SECONDS);
      }, "Pipelines deadlocked while acquiring connections!");
    } finally {
      f1.cancel(true);
      f2.cancel(true);
      executor.shutdownNow();
    }

    executor.shutdownNow();
  }
}
