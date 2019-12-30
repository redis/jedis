package redis.clients.jedis;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ShardedJedisPipeline extends PipelineBase {
  private BinaryShardedJedis jedis;
  private List<FutureResult> results = new ArrayList<>();
  private Queue<Client> clients = new LinkedList<>();

  private static class FutureResult {
    private Client client;

    public FutureResult(Client client) {
      this.client = client;
    }

    public Object get() {
      return client.getOne();
    }
  }

  public void setShardedJedis(BinaryShardedJedis jedis) {
    this.jedis = jedis;
  }

  public List<Object> getResults() {
    List<Object> r = new ArrayList<Object>();
    for (FutureResult fr : results) {
      r.add(fr.get());
    }
    return r;
  }

  /**
   * Synchronize pipeline by reading all responses. This operation closes the pipeline. In order to
   * get return values from pipelined commands, capture the different Response&lt;?&gt; of the
   * commands you execute.
   */
  public void sync() {
    for (Client client : clients) {
      generateResponse(client.getOne());
    }
  }

  /**
   * Synchronize pipeline by reading all responses. This operation closes the pipeline. Whenever
   * possible try to avoid using this version and use ShardedJedisPipeline.sync() as it won't go
   * through all the responses and generate the right response type (usually it is a waste of time).
   * @return A list of all the responses in the order you executed them.
   */
  public List<Object> syncAndReturnAll() {
    List<Object> formatted = new ArrayList<Object>();
    for (Client client : clients) {
      formatted.add(generateResponse(client.getOne()).get());
    }
    return formatted;
  }

  @Override
  protected Client getClient(String key) {
    Client client = jedis.getShard(key).getClient();
    clients.add(client);
    results.add(new FutureResult(client));
    return client;
  }

  @Override
  protected Client getClient(byte[] key) {
    Client client = jedis.getShard(key).getClient();
    clients.add(client);
    results.add(new FutureResult(client));
    return client;
  }
}