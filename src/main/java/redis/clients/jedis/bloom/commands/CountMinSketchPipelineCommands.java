package redis.clients.jedis.bloom.commands;

import java.util.List;
import java.util.Map;
import redis.clients.jedis.Response;
import redis.clients.jedis.bloom.CmsCellSize;

public interface CountMinSketchPipelineCommands {

  Response<String> cmsInitByDim(String key, long width, long depth);

  /**
   * Pipeline variant of {@link CountMinSketchCommands#cmsInitByDim(String, long, long, CmsCellSize)}.
   * @since 8.1
   */
  Response<String> cmsInitByDim(String key, long width, long depth, CmsCellSize cellSize);

  Response<String> cmsInitByProb(String key, double error, double probability);

  /**
   * Pipeline variant of {@link CountMinSketchCommands#cmsInitByProb(String, double, double, CmsCellSize)}.
   * @since 8.1
   */
  Response<String> cmsInitByProb(String key, double error, double probability,
      CmsCellSize cellSize);

  Response<List<Long>> cmsIncrBy(String key, Map<String, Long> itemIncrements);

  Response<List<Long>> cmsQuery(String key, String... items);

  Response<String> cmsMerge(String destKey, String... keys);

  Response<String> cmsMerge(String destKey, Map<String, Long> keysAndWeights);

  Response<Map<String, Object>> cmsInfo(String key);
}
