package redis.clients.jedis;

import java.util.List;

public interface JedisClusterScriptingCommands extends ScriptingCommands{
    Boolean scriptExists(String sha1, String key);
    
    List<Boolean> scriptExists(String key, String... sha1);
    
    String scriptLoad(String script, String key);
}
