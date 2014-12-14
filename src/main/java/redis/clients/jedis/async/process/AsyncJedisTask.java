package redis.clients.jedis.async.process;

import redis.clients.jedis.Builder;
import redis.clients.jedis.async.callback.AsyncResponseCallback;
import redis.clients.jedis.async.response.BasicResponseBuilder;
import redis.clients.jedis.exceptions.JedisException;

import java.nio.ByteBuffer;

public class AsyncJedisTask {
  private final byte[] request;

  private int writtenIndex = 0;

  private final AsyncResponseCallback callback;
  private BasicResponseBuilder responseBuilder;
  private Builder responseTypeConverter;

  public AsyncJedisTask(byte[] request, AsyncResponseCallback callback) {
    this.request = request;
    this.callback = callback;
  }

  public AsyncJedisTask(byte[] request, Builder responseTypeConverter,
      AsyncResponseCallback callback) {
    this.request = request;
    this.responseTypeConverter = responseTypeConverter;
    this.callback = callback;
  }

  public byte[] getRequest() {
    return request;
  }

  public Builder getResponseTypeConverter() {
    return responseTypeConverter;
  }

  public AsyncResponseCallback getCallback() {
    return callback;
  }

  public int getWrittenIndex() {
    return writtenIndex;
  }

  public void setWrittenIndex(int writtenIndex) {
    this.writtenIndex = writtenIndex;
  }

  public <T> void initializeResponseBuilder() {
    responseBuilder = new BasicResponseBuilder<T>();
  }

  public void appendPartialResponse(final ByteBuffer buffer) {
    getResponseBuilder().appendPartialResponse(buffer);
  }

  public boolean isReadComplete() {
    return getResponseBuilder().isComplete();
  }

  public void callback() {
    BasicResponseBuilder responseBuilder = getResponseBuilder();
    Builder responseTypeConverter = getResponseTypeConverter();
    Object response = responseBuilder.getResponse();
    JedisException exception = responseBuilder.getException();

    try {
      if (responseTypeConverter != null && exception == null) {
        response = responseTypeConverter.build(response);
      }

      getCallback().execute(response, exception);
    } catch (Exception e) {
      getCallback().execute(null, new JedisException(e));
    }

  }

  public BasicResponseBuilder getResponseBuilder() {
    if (responseBuilder == null) {
      initializeResponseBuilder();
    }

    return responseBuilder;
  }
}
