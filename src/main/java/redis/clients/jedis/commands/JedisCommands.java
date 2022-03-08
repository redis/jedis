package redis.clients.jedis.commands;

public interface JedisCommands extends KeyCommands, StringCommands, ListCommands, HashCommands,
    SetCommands, SortedSetCommands, GeoCommands, HyperLogLogCommands, StreamCommands,
    ScriptingKeyCommands, FunctionCommands {
}
