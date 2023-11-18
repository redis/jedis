package redis.clients.jedis;

public class DefaultDataBuilder implements DataBuilder {

    @Override
    public Object build(Object data) {
        // For all other types of data, simply return the data as is.
        return data;
    }
}
