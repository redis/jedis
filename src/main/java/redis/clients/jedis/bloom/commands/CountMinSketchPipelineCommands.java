package redis.clients.jedis.bloom.commands;

import java.util.List;
import java.util.Map;
import redis.clients.jedis.Response;

public interface CountMinSketchPipelineCommands {

  Response<String> cmsInitByDim(String key, long width, long depth);

  Response<String> cmsInitByProb(String key, double error, double probability);

  Response<List<Long>> cmsIncrBy(String key, Map<String, Long> itemIncrements);

  Response<List<Long>> cmsQuery(String key, String... items);

  Response<String> cmsMerge(String destKey, String... keys);

  Response<String> cmsMerge(String destKey, Map<String, Long> keysAndWeights);

  Response<Map<String, Object>> cmsInfo(String key);
}
