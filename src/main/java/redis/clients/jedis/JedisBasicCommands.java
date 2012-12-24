package redis.clients.jedis;

import redis.clients.util.Slowlog;

import java.util.List;


public interface JedisBasicCommands {
    List<String> configGet(String pattern);

    String configSet(String parameter, String value);

    Object eval(String script, int keyCount, String... params);

    Object eval(String script, List<String> keys, List<String> args);

    Object eval(String script);

    Object evalsha(String script);

    Object evalsha(String sha1, List<String> keys, List<String> args);

    Object evalsha(String sha1, int keyCount, String... params);

    Boolean scriptExists(String sha1);

    List<Boolean> scriptExists(String... sha1);

    String scriptLoad(String script);

    List<Slowlog> slowlogGet();

    List<Slowlog> slowlogGet(long entries);

    Long objectRefcount(String string);

    String objectEncoding(String string);

    Long objectIdletime(String string);
}
