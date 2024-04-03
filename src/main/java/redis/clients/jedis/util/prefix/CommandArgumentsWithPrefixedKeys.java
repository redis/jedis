package redis.clients.jedis.util.prefix;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.commands.ProtocolCommand;

public class CommandArgumentsWithPrefixedKeys extends CommandArguments {
    private final byte[] prefixBytes;
    private final String prefixString;

    public CommandArgumentsWithPrefixedKeys(ProtocolCommand command, String prefixString, byte[] prefixBytes) {
        super(command);
        this.prefixString = prefixString;
        this.prefixBytes = prefixBytes;
    }

    public CommandArguments key(Object key) {
        return super.key(Prefixer.prefixKey(key, prefixString, prefixBytes));
    }
}
