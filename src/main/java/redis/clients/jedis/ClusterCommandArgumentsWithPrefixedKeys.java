package redis.clients.jedis;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

public class ClusterCommandArgumentsWithPrefixedKeys extends ClusterCommandArguments {
    private final byte[] prefix;
    private final String prefixString;

    public ClusterCommandArgumentsWithPrefixedKeys(ProtocolCommand command, String prefixString) {
        super(command);
        this.prefixString = prefixString;
        prefix = SafeEncoder.encode(prefixString);
    }

    public CommandArguments key(Object key) {
        return super.key(namespacedKey(key));
    }

    private Object namespacedKey(Object key) {
        if (key instanceof Rawable) {
            byte[] raw = ((Rawable) key).getRaw();
            return RawableFactory.from(namespacedKeyBytes(raw));
        }

        if (key instanceof byte[]) {
            return namespacedKeyBytes((byte[]) key);
        }

        if (key instanceof String) {
            String raw = (String) key;
            return prefixString + raw;
        }

        throw new IllegalArgumentException("\"" + key.toString() + "\" is not a valid argument.");
    }

    private byte[] namespacedKeyBytes(byte[] key) {
        byte[] namespaced = new byte[prefix.length + key.length];
        System.arraycopy(prefix, 0, namespaced, 0, prefix.length);
        System.arraycopy(key, 0, namespaced, prefix.length, key.length);
        return namespaced;
    }
}
