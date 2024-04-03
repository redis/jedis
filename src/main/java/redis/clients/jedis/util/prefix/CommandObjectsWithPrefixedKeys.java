package redis.clients.jedis.util.prefix;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObjects;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

public class CommandObjectsWithPrefixedKeys extends CommandObjects {
    private final String prefixString;
    private final byte[] prefixBytes;

    public CommandObjectsWithPrefixedKeys(String prefixString) {
        this.prefixString = prefixString;
        prefixBytes = SafeEncoder.encode(prefixString);
    }

    @Override
    protected CommandArguments commandArguments(ProtocolCommand command) {
        return new CommandArgumentsWithPrefixedKeys(command, prefixString, prefixBytes);
    }
}
