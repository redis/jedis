package redis.clients.jedis;

public abstract class JedisMonitor {

  protected Connection client;

  public void proceed(Connection client) {
    this.client = client;
    this.client.setTimeoutInfinite();
    try {
      do {
        String command = client.getBulkReply();
        onCommand(command);
      } while (client.isConnected());
    } finally {
      // The server keeps streaming unless the socket closed; no new borrower may read that stream.
      client.setBroken();
    }
  }

  public abstract void onCommand(String command);
}
