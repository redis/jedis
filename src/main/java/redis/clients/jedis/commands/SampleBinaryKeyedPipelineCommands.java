package redis.clients.jedis.commands;

import java.util.List;
import redis.clients.jedis.Response;
import redis.clients.jedis.args.FlushMode;

public interface SampleBinaryKeyedPipelineCommands {

  Response<Long> waitReplicas(byte[] sampleKey, int replicas, long timeout);

  Response<Object> eval(byte[] script, byte[] sampleKey);

  Response<Object> evalsha(byte[] sha1, byte[] sampleKey);
//
//  Response<Boolean> scriptExists(byte[] sha1, byte[] sampleKey);

  Response<List<Boolean>> scriptExists(byte[] sampleKey, byte[]... sha1s);

  Response<byte[]> scriptLoad(byte[] script, byte[] sampleKey);

  Response<String> scriptFlush(byte[] sampleKey);

  Response<String> scriptFlush(byte[] sampleKey, FlushMode flushMode);

  Response<String> scriptKill(byte[] sampleKey);
}
