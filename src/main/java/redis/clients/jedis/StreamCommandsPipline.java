package redis.clients.jedis;

import redis.clients.jedis.params.stream.*;

import java.util.List;
import java.util.Map;

public interface StreamCommandsPipline {

    Response<String> xaddDefault(String key, String... pairs);

    Response<String> xaddDefault(String key, Map<String, String> pairs);

    Response<String> xadd(String key, String entryId, String... pairs);

    Response<String> xadd(String key, String entryId, Map<String, String> pairs);

    Response<String> xaddWithMaxlen(String key, long maxLen, String entryId, String... pairs);

    Response<String> xaddWithMaxlen(String key, long maxLen, String entryId, Map<String, String> pairs);

    Response<String> xaddWithMaxlen(String key, boolean approx, long maxLen, String entryId, String... pairs);

    Response<String> xaddWithMaxlen(String key, boolean approx, long maxLen, String entryId, Map<String, String> pairs);

    Response<Long> xlen(String key);

    Response<List<StreamParams>> xrange(String key, String startEntryId, String endEntryId);

    Response<List<StreamParams>> xrange(String key, String startEntryId, String endEntryId, long count);

    Response<List<StreamParams>> xrevrange(String key, String startEntryId, String endEntryId);

    Response<List<StreamParams>> xrevrange(String key, String startEntryId, String endEntryId, long count);

    Response<Map<String, List<StreamParams>>> xread(String... params);

    Response<Map<String, List<StreamParams>>> xread(Map<String, String> pairs);

    Response<Map<String, List<StreamParams>>> xread(long count, String... params);

    Response<Map<String, List<StreamParams>>> xread(long count, Map<String, String> pairs);

    Response<Long> xdel(String key, String entryId);

    Response<Long> xtrimWithMaxlen(String key, long maxlen);

    Response<Long> xtrimWithMaxlen(String key, boolean approx, long maxlen);

    Response<String> xgroupcreate(String key, String group, String entryId);

    Response<String> xgroupcreate(String key, String group, String entryId, boolean mkstream);

    Response<String> xgroupsetid(String key, String group, String entryId);

    Response<Long> xgroupdestroy(String key, String group);

    Response<Long> xgroupdelconsumer(String key, String group, String consumer);

    Response<StreamInfo> xinfostream(String key);

    Response<List<GroupInfo>> xinfogroups(String key);

    Response<List<ConsumerInfo>> xinfoconsumers(String key, String group);

    Response<GroupPendingInfo> xpending(String key, String group);

    Response<List<PendingInfo>> xpending(String key, String group, String startEntryId, String endEntryId, long count);

    Response<List<PendingInfo>> xpending(String key, String group, String startEntryId, String endEntryId, long count, String consumer);

    Response<Map<String, List<StreamParams>>> xreadgroup(String group, String consumer, String... params);

    Response<Map<String, List<StreamParams>>> xreadgroup(String group, String consumer, Map<String, String> pairs);

    Response<Map<String, List<StreamParams>>> xreadgroup(String group, String consumer, long count, String... params);

    Response<Map<String, List<StreamParams>>> xreadgroup(String group, String consumer, long count, Map<String, String> pairs);

    Response<Long> xack(String key, String group, String... entryIds);

    Response<List<StreamParams>> xclaim(String key, String group, String consumer, long minIdleTime, String... entryIds);

    Response<List<StreamParams>> xclaim(String key, String group, String consumer, long minIdleTime, long idleTime, String... entryIds);

    Response<List<StreamParams>> xclaim(String key, String group, String consumer, long minIdleTime, long idleTime, long retryCount, String... entryIds);

    Response<List<StreamParams>> xclaimForce(String key, String group, String consumer, String... entryIds);

    Response<List<String>> xclaimJustid(String key, String group, String consumer, long minIdleTime, String... entryIds);

    Response<List<String>> xclaimJustid(String key, String group, String consumer, long minIdleTime, long idleTime, String... entryIds);

    Response<List<String>> xclaimJustid(String key, String group, String consumer, long minIdleTime, long idleTime, long retryCount, String... entryIds);

    Response<List<String>> xclaimForceAndJustid(String key, String group, String consumer, String... entryIds);
}
