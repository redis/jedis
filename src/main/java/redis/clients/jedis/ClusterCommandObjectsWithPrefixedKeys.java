package redis.clients.jedis;

import redis.clients.jedis.commands.ProtocolCommand;

public class ClusterCommandObjectsWithPrefixedKeys extends ClusterCommandObjects {
    private final String prefixString;

    public ClusterCommandObjectsWithPrefixedKeys(String prefixString) {
        this.prefixString = prefixString;
    }

    @Override
    protected ClusterCommandArguments commandArguments(ProtocolCommand command) {
        return new ClusterCommandArgumentsWithPrefixedKeys(command, prefixString);
    }
}
