package redis.clients.jedis;

import redis.clients.jedis.annots.Internal;

@Internal
public class PushConsumerContext {
  private final PushMessage message;
  private boolean processed = false;

  public PushConsumerContext(PushMessage message) {
    this.message = message;
  }

  public PushMessage getMessage() {
    return message;
  }

  public boolean isProcessed() {
    return processed;
  }

  public void setProcessed(boolean processed) {
    this.processed = processed;
  }

}
