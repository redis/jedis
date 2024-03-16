package redis.clients.jedis.util.prefix;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.args.RawableFactory;

final class Prefixer {
    private Prefixer() {
    }

    static Object prefixKey(Object key, String prefixString, byte[] prefixBytes) {
        if (key instanceof Rawable) {
            byte[] raw = ((Rawable) key).getRaw();
            return RawableFactory.from(prefixKeyWithBytes(raw, prefixBytes));
        }

        if (key instanceof byte[]) {
            return prefixKeyWithBytes((byte[]) key, prefixBytes);
        }

        if (key instanceof String) {
            String raw = (String) key;
            return prefixString + raw;
        }

        throw new IllegalArgumentException("\"" + key.toString() + "\" is not a valid argument.");
    }

    private static byte[] prefixKeyWithBytes(byte[] key, byte[] prefixBytes) {
        byte[] namespaced = new byte[prefixBytes.length + key.length];
        System.arraycopy(prefixBytes, 0, namespaced, 0, prefixBytes.length);
        System.arraycopy(key, 0, namespaced, prefixBytes.length, key.length);
        return namespaced;
    }
}
