package redis.clients.jedis;

import java.io.Closeable;

public abstract class AbstractPipeline extends PipeliningBase implements Closeable {

  protected AbstractPipeline(CommandObjects commandObjects) {
    super(commandObjects);
  }

  @Override
  public abstract void close();

  /**
   * Synchronize pipeline by reading all responses.
   */
  public abstract void sync();

  public Response<Long> publish(String channel, String message) {
    return appendCommand(commandObjects.publish(channel, message));
  }

  public Response<Long> publish(byte[] channel, byte[] message) {
    return appendCommand(commandObjects.publish(channel, message));
  }
}
