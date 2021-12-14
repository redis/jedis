package redis.clients.jedis;

import java.util.LinkedList;
import java.util.Queue;

public class Queable {
  private Queue<Response<?>> pipelinedResponses = new LinkedList<>();

  /**
   * WARNING: This method will be {@code final} in next major release.
   */
  protected void clean() {
    pipelinedResponses.clear();
  }

  /**
   * WARNING: This method will be {@code final} in next major release.
   */
  protected Response<?> generateResponse(Object data) {
    Response<?> response = pipelinedResponses.poll();
    if (response != null) {
      response.set(data);
    }
    return response;
  }

  /**
   * @deprecated Use {@link Queable#enqueResponse(redis.clients.jedis.Builder)}.
   */
  @Deprecated
  protected <T> Response<T> getResponse(Builder<T> builder) {
    return enqueResponse(builder);
  }

  protected final <T> Response<T> enqueResponse(Builder<T> builder) {
    Response<T> lr = new Response<>(builder);
    pipelinedResponses.add(lr);
    return lr;
  }

  /**
   * @deprecated This method will be removed in next major release.
   */
  @Deprecated
  protected boolean hasPipelinedResponse() {
    return !pipelinedResponses.isEmpty();
  }

  /**
   * WARNING: This method will be {@code final} in next major release.
   */
  protected int getPipelinedResponseLength() {
    return pipelinedResponses.size();
  }
}
