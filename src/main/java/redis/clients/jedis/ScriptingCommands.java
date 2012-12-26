package redis.clients.jedis;

import java.util.List;

public interface ScriptingCommands {
    Object eval(String script, int keyCount, String... params);

    Object eval(String script, List<String> keys, List<String> args);

    Object eval(String script);

    Object evalsha(String script);

    Object evalsha(String sha1, List<String> keys, List<String> args);

    Object evalsha(String sha1, int keyCount, String... params);

    Boolean scriptExists(String sha1);

    List<Boolean> scriptExists(String... sha1);

    String scriptLoad(String script);
}
