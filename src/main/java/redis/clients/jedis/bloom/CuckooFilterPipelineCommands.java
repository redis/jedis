package redis.clients.jedis.bloom;

import java.util.List;
import java.util.Map;
import redis.clients.jedis.Response;

public interface CuckooFilterPipelineCommands {

  Response<String> cfReserve(String key, long capacity);

  Response<String> cfReserve(String key, long capacity, CFReserveParams reserveParams);

  Response<Boolean> cfAdd(String key, String item);

  Response<Boolean> cfAddNx(String key, String item);

  Response<List<Boolean>> cfInsert(String key, String... items);

  Response<List<Boolean>> cfInsert(String key, CFInsertParams insertParams, String... items);

  Response<List<Boolean>> cfInsertNx(String key, String... items);

  Response<List<Boolean>> cfInsertNx(String key, CFInsertParams insertParams, String... items);

  Response<Boolean> cfExists(String key, String item);

  Response<Boolean> cfDel(String key, String item);

  Response<Long> cfCount(String key, String item);

  Response<Map<String, Object>> cfInfo(String key);
}
