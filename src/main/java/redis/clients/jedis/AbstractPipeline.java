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

  /**
   * Completes {@code response} with the failure that stopped its reply from being read, so that
   * {@link Response#get()} rethrows it instead of reporting the response as not synced yet. Has no
   * effect on a response already completed.
   * @param response the buffered response whose reply was lost
   * @param failure the failure to report from {@link Response#get()}
   * @since 8.1
   */
  protected static void completeExceptionally(Response<?> response, Throwable failure) {
    response.completeExceptionally(failure);
  }

  public Response<Long> publish(String channel, String message) {
    return appendCommand(commandObjects.publish(channel, message));
  }

  public Response<Long> publish(byte[] channel, byte[] message) {
    return appendCommand(commandObjects.publish(channel, message));
  }
}
