package redis.clients.jedis;

import java.util.List;

public interface JedisClusterBinaryScriptingCommands extends BinaryScriptingCommands{
    List<Long> scriptExists(byte[] key, byte[][] sha1);
    
    byte[] scriptLoad(byte[] script, byte[] key);
    
    String scriptFlush(byte[] key);
    
    String scriptKill(byte[] key);
}
