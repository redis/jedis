package redis.clients.jedis.keys;

public class TypeSafeKey {

    private final String key;

    public String getKey() {
        return key;
    }

    public TypeSafeKey(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return key;
    }
}
