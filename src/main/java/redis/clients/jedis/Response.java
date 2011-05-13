package redis.clients.jedis;

import redis.clients.jedis.exceptions.JedisDataException;

public class Response<T> {
    protected T response = null;
    private boolean built = false;
    private Builder<T> builder;

    public Response(Builder<T> b) {
        this.builder = b;
    }

    public void set(Object data) {
        response = builder.build(data);
        built = true;
    }

    public T get() {
        if (!built) {
            throw new JedisDataException(
                    "Please close pipeline or multi block before calling this method.");
        }
        return response;
    }

    public String toString() {
        return "Response " + builder.toString();
    }

}
