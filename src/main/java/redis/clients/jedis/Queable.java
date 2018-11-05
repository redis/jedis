package redis.clients.jedis;

import java.util.LinkedList;
import java.util.Queue;

public class Queable {
  final private Queue<Response<?>> pipelinedResponses = new LinkedList<>();

  protected void clean() {
    pipelinedResponses.clear();
  }

  protected Response<?> generateResponse(Object data) {
    Response<?> response = pipelinedResponses.poll();
    if (response != null) {
      response.set(data);
    }
    return response;
  }

  protected <T> Response<T> getResponse(Builder<T> builder) {
    Response<T> lr = new Response<>(builder);
    pipelinedResponses.add(lr);
    return lr;
  }

  protected boolean hasPipelinedResponse() {
    return !pipelinedResponses.isEmpty();
  }

  protected int getPipelinedResponseLength() {
    return pipelinedResponses.size();
  }
}
