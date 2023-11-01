package redis.clients.jedis.bloom.commands;

import java.util.List;
import java.util.Map;
import redis.clients.jedis.Response;

public interface TopKFilterPipelineCommands {

  Response<String> topkReserve(String key, long topk);

  Response<String> topkReserve(String key, long topk, long width, long depth, double decay);

  Response<List<String>> topkAdd(String key, String... items);

  Response<List<String>> topkIncrBy(String key, Map<String, Long> itemIncrements);

  Response<List<Boolean>> topkQuery(String key, String... items);

  Response<List<String>> topkList(String key);

  Response<Map<String, Long>> topkListWithCount(String key);

  Response<Map<String, Object>> topkInfo(String key);
}
