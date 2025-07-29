package redis.clients.jedis;

import redis.clients.jedis.annots.Internal;

@Internal
public class PushConsumerContext {
  private final PushMessage message;

  private boolean forwardToClient = false;

  public PushConsumerContext(PushMessage message) {
    this.message = message;
  }

  public PushMessage getMessage() {
    return message;
  }

  public boolean isForwardToClient() {
    return forwardToClient;
  }

  public void setForwardToClient(boolean forwardToClient) {
    this.forwardToClient = forwardToClient;
  }

}
