package redis.clients.jedis.commands;

public interface JedisXBinaryCommands extends KeyBinaryCommands, StringBinaryCommands,
    ListBinaryCommands, HashBinaryCommands, SetBinaryCommands, SortedSetBinaryCommands,
    GeoBinaryCommands, HyperLogLogBinaryCommands, StreamBinaryCommands, ScriptingKeyBinaryCommands,
    MiscellaneousBinaryCommands {
}
