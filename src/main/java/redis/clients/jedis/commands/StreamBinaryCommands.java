package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;

import redis.clients.jedis.params.XAddParams;
import redis.clients.jedis.params.XAutoClaimParams;
import redis.clients.jedis.params.XClaimParams;
import redis.clients.jedis.params.XPendingParams;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.params.XTrimParams;

public interface StreamBinaryCommands {
//
//  byte[] xadd(byte[] key, byte[] id, Map<byte[], byte[]> hash, long maxLen, boolean approximateLength);

  default byte[] xadd(byte[] key, Map<byte[], byte[]> hash, XAddParams params) {
    return xadd(key, params, hash);
  }

  byte[] xadd(byte[] key, XAddParams params, Map<byte[], byte[]> hash);

  long xlen(byte[] key);

  List<byte[]> xrange(byte[] key, byte[] start, byte[] end);

  List<byte[]> xrange(byte[] key, byte[] start, byte[] end, int count);

  List<byte[]> xrevrange(byte[] key, byte[] end, byte[] start);

  List<byte[]> xrevrange(byte[] key, byte[] end, byte[] start, int count);

  long xack(byte[] key, byte[] group, byte[]... ids);

  String xgroupCreate(byte[] key, byte[] groupname, byte[] id, boolean makeStream);

  String xgroupSetID(byte[] key, byte[] groupname, byte[] id);

  long xgroupDestroy(byte[] key, byte[] groupname);

  long xgroupDelConsumer(byte[] key, byte[] groupname, byte[] consumerName);

  long xdel(byte[] key, byte[]... ids);

  long xtrim(byte[] key, long maxLen, boolean approximateLength);

  long xtrim(byte[] key, XTrimParams params);

  Object xpending(byte[] key, byte[] groupname);

  List<Object> xpending(byte[] key, byte[] groupname, byte[] start, byte[] end, int count, byte[] consumername);

  List<Object> xpending(byte[] key, byte[] groupname, XPendingParams params);

  List<byte[]> xclaim(byte[] key, byte[] group, byte[] consumername, long minIdleTime, XClaimParams params, byte[]... ids);

  List<byte[]> xclaimJustId(byte[] key, byte[] group, byte[] consumername, long minIdleTime, XClaimParams params, byte[]... ids);

  List<Object> xautoclaim(byte[] key, byte[] groupName, byte[] consumerName,
      long minIdleTime, byte[] start, XAutoClaimParams params);

  List<Object> xautoclaimJustId(byte[] key, byte[] groupName, byte[] consumerName,
      long minIdleTime, byte[] start, XAutoClaimParams params);

  Object xinfoStream(byte[] key);

  List<Object> xinfoGroup(byte[] key);

  List<Object> xinfoConsumers(byte[] key, byte[] group);

  List<byte[]> xread(XReadParams xReadParams, Map.Entry<byte[], byte[]>... streams);

  List<byte[]> xreadGroup(byte[] groupname, byte[] consumer, XReadGroupParams xReadGroupParams,
      Map.Entry<byte[], byte[]>... streams);

}
