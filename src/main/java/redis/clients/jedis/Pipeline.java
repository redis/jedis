package redis.clients.jedis;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.exceptions.JedisDataException;

public class Pipeline extends MultiKeyPipelineBase implements Closeable {

  private MultiResponseBuilder currentMulti;

  private class MultiResponseBuilder extends Builder<List<Object>> {
    private List<Response<?>> responses = new ArrayList<>();

    @Override
    public List<Object> build(Object data) {
      @SuppressWarnings("unchecked")
      List<Object> list = (List<Object>) data;
      List<Object> values = new ArrayList<>();

      if (list.size() != responses.size()) {
        throw new JedisDataException("Expected data size " + responses.size() + " but was "
            + list.size());
      }

      for (int i = 0; i < list.size(); i++) {
        Response<?> response = responses.get(i);
        response.set(list.get(i));
        Object builtResponse;
        try {
          builtResponse = response.get();
        } catch (JedisDataException e) {
          builtResponse = e;
        }
        values.add(builtResponse);
      }
      return values;
    }

    public void setResponseDependency(Response<?> dependency) {
      for (Response<?> response : responses) {
        response.setDependency(dependency);
      }
    }

    public void addResponse(Response<?> response) {
      responses.add(response);
    }
  }

  @Override
  protected <T> Response<T> getResponse(Builder<T> builder) {
    if (currentMulti != null) {
      super.getResponse(BuilderFactory.STRING); // Expected QUEUED

      Response<T> lr = new Response<>(builder);
      currentMulti.addResponse(lr);
      return lr;
    } else {
      return super.getResponse(builder);
    }
  }

  public void setClient(Client client) {
    this.client = client;
  }

  @Override
  protected Client getClient(byte[] key) {
    return client;
  }

  @Override
  protected Client getClient(String key) {
    return client;
  }

  public void clear() {
    if (isInMulti()) {
      discard();
    }

    sync();
  }

  public boolean isInMulti() {
    return currentMulti != null;
  }

  /**
   * Synchronize pipeline by reading all responses. This operation close the pipeline. In order to
   * get return values from pipelined commands, capture the different Response&lt;?&gt; of the
   * commands you execute.
   */
  public void sync() {
    if (getPipelinedResponseLength() > 0) {
      List<Object> unformatted = client.getMany(getPipelinedResponseLength());
      for (Object o : unformatted) {
        generateResponse(o);
      }
    }
  }

  /**
   * Synchronize pipeline by reading all responses. This operation close the pipeline. Whenever
   * possible try to avoid using this version and use Pipeline.sync() as it won't go through all the
   * responses and generate the right response type (usually it is a waste of time).
   * @return A list of all the responses in the order you executed them.
   */
  public List<Object> syncAndReturnAll() {
    if (getPipelinedResponseLength() > 0) {
      List<Object> unformatted = client.getMany(getPipelinedResponseLength());
      List<Object> formatted = new ArrayList<>();
      for (Object o : unformatted) {
        try {
          formatted.add(generateResponse(o).get());
        } catch (JedisDataException e) {
          formatted.add(e);
        }
      }
      return formatted;
    } else {
      return java.util.Collections.<Object> emptyList();
    }
  }

  public Response<String> discard() {
    if (currentMulti == null) throw new JedisDataException("DISCARD without MULTI");
    client.discard();
    currentMulti = null;
    return getResponse(BuilderFactory.STRING);
  }

  public Response<List<Object>> exec() {
    if (currentMulti == null) throw new JedisDataException("EXEC without MULTI");

    client.exec();
    Response<List<Object>> response = super.getResponse(currentMulti);
    currentMulti.setResponseDependency(response);
    currentMulti = null;
    return response;
  }

  public Response<String> multi() {
    if (currentMulti != null) throw new JedisDataException("MULTI calls can not be nested");

    client.multi();
    Response<String> response = getResponse(BuilderFactory.STRING); // Expecting
    // OK
    currentMulti = new MultiResponseBuilder();
    return response;
  }

  @Override
  public void close() {
    clear();
  }

}
