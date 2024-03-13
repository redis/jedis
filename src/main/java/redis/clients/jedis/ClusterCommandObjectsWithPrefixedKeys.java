package redis.clients.jedis;

import redis.clients.jedis.commands.ProtocolCommand;

public class ClusterCommandObjectsWithPrefixedKeys extends ClusterCommandObjects {
    // For the purposes of this demonstration, the prefix is assigned statically.
    // Additional changes are required to prevent the parent class CommandObjects
    // from calling commandArguments in its constructor, which would be a prerequisite
    // to making this field into an instance field.
    public static String PREFIX_STRING;

    @Override
    protected ClusterCommandArguments commandArguments(ProtocolCommand command) {
        return new ClusterCommandArgumentsWithPrefixedKeys(command, PREFIX_STRING);
    }
}
