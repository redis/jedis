package redis.clients.jedis;

import redis.clients.jedis.commands.ProtocolCommand;

public class CommandObjectsWithPrefixedKeys extends CommandObjects {
    private final String prefixString;

    public CommandObjectsWithPrefixedKeys(String prefixString) {
        this.prefixString = prefixString;
    }

    @Override
    protected CommandArguments commandArguments(ProtocolCommand command) {
        return new CommandArgumentsWithPrefixedKeys(command, prefixString);
    }
}
