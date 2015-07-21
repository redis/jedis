/**
 * Simple ProtocolCommand for execute function
 * @author weigao<china.violetgo@gmail.com>
 * @version 1.0.0
 */
package redis.clients.jedis;

import java.util.HashSet;
import java.util.Set;

import redis.clients.jedis.ProtocolCommand;
import redis.clients.util.SafeEncoder;

public class SimpleProtocolCommand implements ProtocolCommand {
  private static Set<String> isVaildCommand = new HashSet<String>();
  static {
    isVaildCommand.add("PING");
    isVaildCommand.add("SET");
    isVaildCommand.add("GET");
    isVaildCommand.add("QUIT");
    isVaildCommand.add("EXISTS");
    isVaildCommand.add("DEL");
    isVaildCommand.add("TYPE");
    isVaildCommand.add("FLUSHDB");
    isVaildCommand.add("KEYS");
    isVaildCommand.add("RANDOMKEY");
    isVaildCommand.add("RENAME");
    isVaildCommand.add("RENAMENX");
    isVaildCommand.add("RENAMEX");
    isVaildCommand.add("DBSIZE");
    isVaildCommand.add("EXPIRE");
    isVaildCommand.add("EXPIREAT");
    isVaildCommand.add("TTL");
    isVaildCommand.add("SELECT");
    isVaildCommand.add("MOVE");
    isVaildCommand.add("FLUSHALL");
    isVaildCommand.add("GETSET");
    isVaildCommand.add("MGET");
    isVaildCommand.add("SETNX");
    isVaildCommand.add("SETEX");
    isVaildCommand.add("MSET");
    isVaildCommand.add("MSETNX");
    isVaildCommand.add("DECRBY");
    isVaildCommand.add("DECR");
    isVaildCommand.add("INCRBY");
    isVaildCommand.add("INCR");
    isVaildCommand.add("APPEND");
    isVaildCommand.add("SUBSTR");
    isVaildCommand.add("HSET");
    isVaildCommand.add("HGET");
    isVaildCommand.add("HSETNX");
    isVaildCommand.add("HMSET");
    isVaildCommand.add("HMGET");
    isVaildCommand.add("HINCRBY");
    isVaildCommand.add("HEXISTS");
    isVaildCommand.add("HDEL");
    isVaildCommand.add("HLEN");
    isVaildCommand.add("HKEYS");
    isVaildCommand.add("HVALS");
    isVaildCommand.add("HGETALL");
    isVaildCommand.add("RPUSH");
    isVaildCommand.add("LPUSH");
    isVaildCommand.add("LLEN");
    isVaildCommand.add("LRANGE");
    isVaildCommand.add("LTRIM");
    isVaildCommand.add("LINDEX");
    isVaildCommand.add("LSET");
    isVaildCommand.add("LREM");
    isVaildCommand.add("LPOP");
    isVaildCommand.add("RPOP");
    isVaildCommand.add("RPOPLPUSH");
    isVaildCommand.add("SADD");
    isVaildCommand.add("SMEMBERS");
    isVaildCommand.add("SREM");
    isVaildCommand.add("SPOP");
    isVaildCommand.add("SMOVE");
    isVaildCommand.add("SCARD");
    isVaildCommand.add("SISMEMBER");
    isVaildCommand.add("SINTER");
    isVaildCommand.add("SINTERSTORE");
    isVaildCommand.add("SUNION");
    isVaildCommand.add("SUNIONSTORE");
    isVaildCommand.add("SDIFF");
    isVaildCommand.add("SDIFFSTORE");
    isVaildCommand.add("SRANDMEMBER");
    isVaildCommand.add("ZADD");
    isVaildCommand.add("ZRANGE");
    isVaildCommand.add("ZREM");
    isVaildCommand.add("ZINCRBY");
    isVaildCommand.add("ZRANK");
    isVaildCommand.add("ZREVRANK");
    isVaildCommand.add("ZREVRANGE");
    isVaildCommand.add("ZCARD");
    isVaildCommand.add("ZSCORE");
    isVaildCommand.add("MULTI");
    isVaildCommand.add("DISCARD");
    isVaildCommand.add("EXEC");
    isVaildCommand.add("WATCH");
    isVaildCommand.add("UNWATCH");
    isVaildCommand.add("SORT");
    isVaildCommand.add("BLPOP");
    isVaildCommand.add("BRPOP");
    isVaildCommand.add("AUTH");
    isVaildCommand.add("SUBSCRIBE");
    isVaildCommand.add("PUBLISH");
    isVaildCommand.add("UNSUBSCRIBE");
    isVaildCommand.add("PSUBSCRIBE");
    isVaildCommand.add("PUNSUBSCRIBE");
    isVaildCommand.add("PUBSUB");
    isVaildCommand.add("ZCOUNT");
    isVaildCommand.add("ZRANGEBYSCORE");
    isVaildCommand.add("ZREVRANGEBYSCORE");
    isVaildCommand.add("ZREMRANGEBYRANK");
    isVaildCommand.add("ZREMRANGEBYSCORE");
    isVaildCommand.add("ZUNIONSTORE");
    isVaildCommand.add("ZINTERSTORE");
    isVaildCommand.add("ZLEXCOUNT");
    isVaildCommand.add("ZRANGEBYLEX");
    isVaildCommand.add("ZREVRANGEBYLEX");
    isVaildCommand.add("ZREMRANGEBYLEX");
    isVaildCommand.add("SAVE");
    isVaildCommand.add("BGSAVE");
    isVaildCommand.add("BGREWRITEAOF");
    isVaildCommand.add("LASTSAVE");
    isVaildCommand.add("SHUTDOWN");
    isVaildCommand.add("INFO");
    isVaildCommand.add("MONITOR");
    isVaildCommand.add("SLAVEOF");
    isVaildCommand.add("CONFIG");
    isVaildCommand.add("STRLEN");
    isVaildCommand.add("SYNC");
    isVaildCommand.add("LPUSHX");
    isVaildCommand.add("PERSIST");
    isVaildCommand.add("RPUSHX");
    isVaildCommand.add("ECHO");
    isVaildCommand.add("LINSERT");
    isVaildCommand.add("DEBUG");
    isVaildCommand.add("BRPOPLPUSH");
    isVaildCommand.add("SETBIT");
    isVaildCommand.add("GETBIT");
    isVaildCommand.add("BITPOS");
    isVaildCommand.add("SETRANGE");
    isVaildCommand.add("GETRANGE");
    isVaildCommand.add("EVAL");
    isVaildCommand.add("EVALSHA");
    isVaildCommand.add("SCRIPT");
    isVaildCommand.add("SLOWLOG");
    isVaildCommand.add("OBJECT");
    isVaildCommand.add("BITCOUNT");
    isVaildCommand.add("BITOP");
    isVaildCommand.add("SENTINEL");
    isVaildCommand.add("DUMP");
    isVaildCommand.add("RESTORE");
    isVaildCommand.add("PEXPIRE");
    isVaildCommand.add("PEXPIREAT");
    isVaildCommand.add("PTTL");
    isVaildCommand.add("INCRBYFLOAT");
    isVaildCommand.add("PSETEX");
    isVaildCommand.add("CLIENT");
    isVaildCommand.add("TIME");
    isVaildCommand.add("MIGRATE");
    isVaildCommand.add("HINCRBYFLOAT");
    isVaildCommand.add("SCAN");
    isVaildCommand.add("HSCAN");
    isVaildCommand.add("SSCAN");
    isVaildCommand.add("ZSCAN");
    isVaildCommand.add("WAIT");
    isVaildCommand.add("CLUSTER");
    isVaildCommand.add("ASKING");
    isVaildCommand.add("PFADD");
    isVaildCommand.add("PFCOUNT");
    isVaildCommand.add("PFMERGE");
    isVaildCommand.add("READONLY");
  }

  private byte[] raw = null;

  public SimpleProtocolCommand(String key) {
    key = key.toUpperCase();
    if (isVaildCommand.contains(key)) {
      raw = SafeEncoder.encode(key);
    }
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }

}
