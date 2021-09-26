package redis.clients.jedis;

import java.util.LinkedList;
import java.util.Queue;

public class Queable {

  private final Queue<Response<?>> pipelinedResponses = new LinkedList<>();

  protected void clean() {
    pipelinedResponses.clear();
  }

  protected final void generateResponse(Object data) {
    pipelinedResponses.poll().set(data);
  }

  protected final <T> Response<T> enqueResponse(Builder<T> builder) {
    Response<T> lr = new Response<>(builder);
    pipelinedResponses.add(lr);
    return lr;
  }

  protected final int getPipelinedResponseLength() {
    return pipelinedResponses.size();
  }
}
