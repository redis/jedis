package redis.clients.jedis.mcf;

import java.io.Closeable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import redis.clients.jedis.*;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.util.KeyValue;

/**
 * This is high memory dependent solution as all the appending commands will be hold in memory until
 * {@link MultiDbPipeline#sync() SYNC} (or {@link MultiDbPipeline#close() CLOSE}) gets called.
 */
@Experimental
public class MultiDbPipeline extends AbstractPipeline implements Closeable {

  private final MultiDbConnectionSupplier failoverProvider;
  private final Queue<KeyValue<CommandArguments, Response<?>>> commands = new LinkedList<>();

  public MultiDbPipeline(MultiDbConnectionProvider pooledProvider, CommandObjects commandObjects) {
    super(commandObjects);
    this.failoverProvider = new MultiDbConnectionSupplier(pooledProvider);
  }

  @Override
  protected final <T> Response<T> appendCommand(CommandObject<T> commandObject) {
    CommandArguments args = commandObject.getArguments();
    Response<T> response = new Response<>(commandObject.getBuilder());
    commands.add(KeyValue.of(args, response));
    return response;
  }

  @Override
  public void close() {
    sync();
    // connection prepared and closed (in try-with-resources) in sync()
  }

  /**
   * Sends the buffered commands to the active database over one connection and reads all their
   * replies. A reply is decoded when its {@link Response#get()} is called, so keep the
   * {@link Response} returned by each command. Server errors are not thrown here: the corresponding
   * {@code get()} throws them.
   */
  @Override
  public void sync() {
    if (commands.isEmpty()) return;

    try (Connection connection = failoverProvider.getConnection()) {

      commands.forEach((command) -> connection.sendCommand(command.getKey()));
      // following connection.getMany(int) flushes anyway, so no flush here.

      List<Object> unformatted = connection.getMany(commands.size());
      unformatted.forEach((rawReply) -> commands.poll().getValue().set(rawReply));
    }
  }

  public Response<Long> waitReplicas(int replicas, long timeout) {
    return appendCommand(commandObjects.waitReplicas(replicas, timeout));
  }

  public Response<KeyValue<Long, Long>> waitAOF(long numLocal, long numReplicas, long timeout) {
    return appendCommand(commandObjects.waitAOF(numLocal, numReplicas, timeout));
  }
}
