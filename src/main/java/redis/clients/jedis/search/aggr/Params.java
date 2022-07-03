package redis.clients.jedis.search.aggr;

public class Params {

    private final String name;
    private final Object value;

    public Params(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public final String getName() {
        return name;
    }

    public final Object getValue() {
        return value;
    }
}
