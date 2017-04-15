package redis.clients.jedis;

public interface JedisFacade extends BasicCommands, BinaryJedisCommands,
		MultiKeyBinaryCommands, AdvancedBinaryJedisCommands,
		BinaryScriptingCommands, JedisCommands, MultiKeyCommands,
		AdvancedJedisCommands, ScriptingCommands {
}
