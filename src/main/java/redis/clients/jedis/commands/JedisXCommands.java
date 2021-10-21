package redis.clients.jedis.commands;

public interface JedisXCommands extends KeyCommands, StringCommands, ListCommands, HashCommands,
    SetCommands, SortedSetCommands, GeoCommands, HyperLogLogCommands, StreamCommands,
    ScriptingKeyCommands, MiscellaneousCommands {
}
