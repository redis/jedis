package redis.clients.jedis.commands;

import java.util.List;
import redis.clients.jedis.Response;

public interface ScriptingKeyPipelineCommands {

  Response<Object> eval(String script);

  Response<Object> eval(String script, int keyCount, String... params);

  Response<Object> eval(String script, List<String> keys, List<String> args);

  Response<Object> evalReadonly(String script, List<String> keys, List<String> args);

  Response<Object> evalsha(String sha1);

  Response<Object> evalsha(String sha1, int keyCount, String... params);

  Response<Object> evalsha(String sha1, List<String> keys, List<String> args);

  Response<Object> evalshaReadonly(String sha1, List<String> keys, List<String> args);
}
