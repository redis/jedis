package redis.clients.jedis.util.prefix;

import redis.clients.jedis.ClusterCommandArguments;
import redis.clients.jedis.ClusterCommandObjects;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

public class ClusterCommandObjectsWithPrefixedKeys extends ClusterCommandObjects {
    private final String prefixString;
    private final byte[] prefixBytes;

    public ClusterCommandObjectsWithPrefixedKeys(String prefixString) {
        this.prefixString = prefixString;
        prefixBytes = SafeEncoder.encode(prefixString);
    }

    @Override
    protected ClusterCommandArguments commandArguments(ProtocolCommand command) {
        return new ClusterCommandArgumentsWithPrefixedKeys(command, prefixString, prefixBytes);
    }
}
