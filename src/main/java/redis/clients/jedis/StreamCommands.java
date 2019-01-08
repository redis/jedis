package redis.clients.jedis;

import redis.clients.jedis.params.stream.*;

import java.util.List;
import java.util.Map;

public interface StreamCommands {

    String xaddDefault(String key, String... pairs);

    String xaddDefault(String key, Map<String, String> pairs);

    String xadd(String key, String entryId, String... pairs);

    String xadd(String key, String entryId, Map<String, String> pairs);

    String xaddWithMaxlen(String key, long maxLen, String entryId, String... pairs);

    String xaddWithMaxlen(String key, long maxLen, String entryId, Map<String, String> pairs);

    String xaddWithMaxlen(String key, boolean approx, long maxLen, String entryId, String... pairs);

    String xaddWithMaxlen(String key, boolean approx, long maxLen, String entryId, Map<String, String> pairs);

    long xlen(String key);

    List<StreamParams> xrange(String key, String startEntryId, String endEntryId);

    List<StreamParams> xrange(String key, String startEntryId, String endEntryId, long count);

    List<StreamParams> xrevrange(String key, String startEntryId, String endEntryId);

    List<StreamParams> xrevrange(String key, String startEntryId, String endEntryId, long count);

    Map<String, List<StreamParams>> xread(String... params);

    Map<String, List<StreamParams>> xread(Map<String, String> pairs);

    Map<String, List<StreamParams>> xread(long count, String... params);

    Map<String, List<StreamParams>> xread(long count, Map<String, String> pairs);

    NewStreamParams xreadBlock(long block, String... keys);

    NewStreamParams xreadBlock(String... keys);

    long xdel(String key, String entryId);

    long xtrimWithMaxlen(String key, long maxlen);

    long xtrimWithMaxlen(String key, boolean approx, long maxlen);

    String xgroupcreate(String key, String group, String entryId);

    String xgroupcreate(String key, String group, String entryId, boolean mkstream);

    String xgroupsetid(String key, String group, String entryId);

    long xgroupdestroy(String key, String group);

    long xgroupdelconsumer(String key, String group, String consumer);

    StreamInfo xinfostream(String key);

    List<GroupInfo> xinfogroups(String key);

    List<ConsumerInfo> xinfoconsumers(String key, String group);

    GroupPendingInfo xpending(String key, String group);

    List<PendingInfo> xpending(String key, String group, String startEntryId, String endEntryId, long count);

    List<PendingInfo> xpending(String key, String group, String startEntryId, String endEntryId, long count, String consumer);

    Map<String, List<StreamParams>> xreadgroup(String group, String consumer, String... params);

    Map<String, List<StreamParams>> xreadgroup(String group, String consumer, Map<String, String> pairs);

    Map<String, List<StreamParams>> xreadgroup(String group, String consumer, long count, String... params);

    Map<String, List<StreamParams>> xreadgroup(String group, String consumer, long count, Map<String, String> pairs);

    NewStreamParams xreadgroupBlock(String group, String consumer, long block, String... keys);

    NewStreamParams xreadgroupBlock(String group, String consumer, String... keys);

    long xack(String key, String group, String... entryIds);

    List<StreamParams> xclaim(String key, String group, String consumer, long minIdleTime, String... entryIds);

    List<StreamParams> xclaim(String key, String group, String consumer, long minIdleTime, long idleTime, String... entryIds);

    List<StreamParams> xclaim(String key, String group, String consumer, long minIdleTime, long idleTime, long retryCount, String... entryIds);

    List<StreamParams> xclaimForce(String key, String group, String consumer, String... entryIds);

    List<String> xclaimJustid(String key, String group, String consumer, long minIdleTime, String... entryIds);

    List<String> xclaimJustid(String key, String group, String consumer, long minIdleTime, long idleTime, String... entryIds);

    List<String> xclaimJustid(String key, String group, String consumer, long minIdleTime, long idleTime, long retryCount, String... entryIds);

    List<String> xclaimForceAndJustid(String key, String group, String consumer, String... entryIds);

}