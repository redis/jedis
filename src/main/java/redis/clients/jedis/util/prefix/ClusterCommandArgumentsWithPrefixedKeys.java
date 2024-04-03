package redis.clients.jedis.util.prefix;

import redis.clients.jedis.ClusterCommandArguments;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.commands.ProtocolCommand;

public class ClusterCommandArgumentsWithPrefixedKeys extends ClusterCommandArguments {
    private final byte[] prefixBytes;
    private final String prefixString;

    public ClusterCommandArgumentsWithPrefixedKeys(ProtocolCommand command, String prefixString, byte[] prefixBytes) {
        super(command);
        this.prefixString = prefixString;
        this.prefixBytes = prefixBytes;
    }

    public CommandArguments key(Object key) {
        return super.key(Prefixer.prefixKey(key, prefixString, prefixBytes));
    }
}
