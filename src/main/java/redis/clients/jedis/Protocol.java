package redis.clients.jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.*;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.RedisOutputStream;
import redis.clients.jedis.util.SafeEncoder;

public final class Protocol {

  private static final String ASK_PREFIX = "ASK ";
  private static final String MOVED_PREFIX = "MOVED ";
  private static final String CLUSTERDOWN_PREFIX = "CLUSTERDOWN ";
  private static final String BUSY_PREFIX = "BUSY ";
  private static final String NOSCRIPT_PREFIX = "NOSCRIPT ";
  private static final String WRONGPASS_PREFIX = "WRONGPASS";
  private static final String NOPERM_PREFIX = "NOPERM";
  private static final String EXECABORT_PREFIX = "EXECABORT ";

  public static final String DEFAULT_HOST = "localhost";
  public static final int DEFAULT_PORT = 6379;
  public static final int DEFAULT_SENTINEL_PORT = 26379;
  public static final int DEFAULT_TIMEOUT = 2000;
  public static final int DEFAULT_DATABASE = 0;

  /**
   * Type of CHARSET will be changed to {@link java.nio.charset.Charset} in next major release.
   */
  public static final String CHARSET = "UTF-8";

  public static final byte DOLLAR_BYTE = '$';
  public static final byte ASTERISK_BYTE = '*';
  public static final byte PLUS_BYTE = '+';
  public static final byte MINUS_BYTE = '-';
  public static final byte COLON_BYTE = ':';

  @Deprecated public static final String SENTINEL_MASTERS = "masters";
  @Deprecated public static final String SENTINEL_GET_MASTER_ADDR_BY_NAME = "get-master-addr-by-name";
  @Deprecated public static final String SENTINEL_RESET = "reset";
  @Deprecated public static final String SENTINEL_SLAVES = "slaves";
  @Deprecated public static final String SENTINEL_FAILOVER = "failover";
  @Deprecated public static final String SENTINEL_MONITOR = "monitor";
  @Deprecated public static final String SENTINEL_REMOVE = "remove";
  @Deprecated public static final String SENTINEL_SET = "set";

  @Deprecated public static final String CLUSTER_NODES = "nodes";
  @Deprecated public static final String CLUSTER_MEET = "meet";
  @Deprecated public static final String CLUSTER_RESET = "reset";
  @Deprecated public static final String CLUSTER_ADDSLOTS = "addslots";
  @Deprecated public static final String CLUSTER_DELSLOTS = "delslots";
  @Deprecated public static final String CLUSTER_INFO = "info";
  @Deprecated public static final String CLUSTER_GETKEYSINSLOT = "getkeysinslot";
  @Deprecated public static final String CLUSTER_SETSLOT = "setslot";
  @Deprecated public static final String CLUSTER_SETSLOT_NODE = "node";
  @Deprecated public static final String CLUSTER_SETSLOT_MIGRATING = "migrating";
  @Deprecated public static final String CLUSTER_SETSLOT_IMPORTING = "importing";
  @Deprecated public static final String CLUSTER_SETSLOT_STABLE = "stable";
  @Deprecated public static final String CLUSTER_FORGET = "forget";
  @Deprecated public static final String CLUSTER_FLUSHSLOT = "flushslots";
  @Deprecated public static final String CLUSTER_KEYSLOT = "keyslot";
  @Deprecated public static final String CLUSTER_COUNTKEYINSLOT = "countkeysinslot";
  @Deprecated public static final String CLUSTER_SAVECONFIG = "saveconfig";
  @Deprecated public static final String CLUSTER_REPLICATE = "replicate";
  @Deprecated public static final String CLUSTER_SLAVES = "slaves";
  @Deprecated public static final String CLUSTER_FAILOVER = "failover";
  @Deprecated public static final String CLUSTER_SLOTS = "slots";

  @Deprecated public static final String PUBSUB_CHANNELS = "channels";
  @Deprecated public static final String PUBSUB_NUMSUB = "numsub";
  @Deprecated public static final String PUBSUB_NUM_PAT = "numpat";

  public static final byte[] BYTES_TRUE = toByteArray(1);
  public static final byte[] BYTES_FALSE = toByteArray(0);
  public static final byte[] BYTES_TILDE = SafeEncoder.encode("~");
  public static final byte[] BYTES_EQUAL = SafeEncoder.encode("=");
  public static final byte[] BYTES_ASTERISK = SafeEncoder.encode("*");

  public static final byte[] POSITIVE_INFINITY_BYTES = "+inf".getBytes();
  public static final byte[] NEGATIVE_INFINITY_BYTES = "-inf".getBytes();

  private Protocol() {
    // this prevent the class from instantiation
  }

  public static void sendCommand(final RedisOutputStream os, final ProtocolCommand command,
      final byte[]... args) {
    sendCommand(os, command.getRaw(), args);
  }

  private static void sendCommand(final RedisOutputStream os, final byte[] command,
      final byte[]... args) {
    try {
      os.write(ASTERISK_BYTE);
      os.writeIntCrLf(args.length + 1);
      os.write(DOLLAR_BYTE);
      os.writeIntCrLf(command.length);
      os.write(command);
      os.writeCrLf();

      for (final byte[] arg : args) {
        os.write(DOLLAR_BYTE);
        os.writeIntCrLf(arg.length);
        os.write(arg);
        os.writeCrLf();
      }
    } catch (IOException e) {
      throw new JedisConnectionException(e);
    }
  }

  private static void processError(final RedisInputStream is) {
    String message = is.readLine();
    // TODO: I'm not sure if this is the best way to do this.
    // Maybe Read only first 5 bytes instead?
    if (message.startsWith(MOVED_PREFIX)) {
      String[] movedInfo = parseTargetHostAndSlot(message);
      throw new JedisMovedDataException(message, new HostAndPort(movedInfo[1],
          Integer.parseInt(movedInfo[2])), Integer.parseInt(movedInfo[0]));
    } else if (message.startsWith(ASK_PREFIX)) {
      String[] askInfo = parseTargetHostAndSlot(message);
      throw new JedisAskDataException(message, new HostAndPort(askInfo[1],
          Integer.parseInt(askInfo[2])), Integer.parseInt(askInfo[0]));
    } else if (message.startsWith(CLUSTERDOWN_PREFIX)) {
      throw new JedisClusterException(message);
    } else if (message.startsWith(BUSY_PREFIX)) {
      throw new JedisBusyException(message);
    } else if (message.startsWith(NOSCRIPT_PREFIX)) {
      throw new JedisNoScriptException(message);
    } else if (message.startsWith(WRONGPASS_PREFIX)) {
      throw new JedisAccessControlException(message);
    } else if (message.startsWith(NOPERM_PREFIX)) {
      throw new JedisAccessControlException(message);
    } else if (message.startsWith(EXECABORT_PREFIX)) {
      throw new AbortedTransactionException(message);
    }
    throw new JedisDataException(message);
  }

  public static String readErrorLineIfPossible(RedisInputStream is) {
    final byte b = is.readByte();
    // if buffer contains other type of response, just ignore.
    if (b != MINUS_BYTE) {
      return null;
    }
    return is.readLine();
  }

  private static String[] parseTargetHostAndSlot(String clusterRedirectResponse) {
    String[] response = new String[3];
    String[] messageInfo = clusterRedirectResponse.split(" ");
    String[] targetHostAndPort = HostAndPort.extractParts(messageInfo[2]);
    response[0] = messageInfo[1];
    response[1] = targetHostAndPort[0];
    response[2] = targetHostAndPort[1];
    return response;
  }

  private static Object process(final RedisInputStream is) {
    final byte b = is.readByte();
    switch (b) {
    case PLUS_BYTE:
      return processStatusCodeReply(is);
    case DOLLAR_BYTE:
      return processBulkReply(is);
    case ASTERISK_BYTE:
      return processMultiBulkReply(is);
    case COLON_BYTE:
      return processInteger(is);
    case MINUS_BYTE:
      processError(is);
      return null;
    default:
      throw new JedisConnectionException("Unknown reply: " + (char) b);
    }
  }

  private static byte[] processStatusCodeReply(final RedisInputStream is) {
    return is.readLineBytes();
  }

  private static byte[] processBulkReply(final RedisInputStream is) {
    final int len = is.readIntCrLf();
    if (len == -1) {
      return null;
    }

    final byte[] read = new byte[len];
    int offset = 0;
    while (offset < len) {
      final int size = is.read(read, offset, (len - offset));
      if (size == -1) throw new JedisConnectionException(
          "It seems like server has closed the connection.");
      offset += size;
    }

    // read 2 more bytes for the command delimiter
    is.readByte();
    is.readByte();

    return read;
  }

  private static Long processInteger(final RedisInputStream is) {
    return is.readLongCrLf();
  }

  private static List<Object> processMultiBulkReply(final RedisInputStream is) {
    final int num = is.readIntCrLf();
    if (num == -1) {
      return null;
    }
    final List<Object> ret = new ArrayList<>(num);
    for (int i = 0; i < num; i++) {
      try {
        ret.add(process(is));
      } catch (JedisDataException e) {
        ret.add(e);
      }
    }
    return ret;
  }

  public static Object read(final RedisInputStream is) {
    return process(is);
  }

  public static final byte[] toByteArray(final boolean value) {
    return value ? BYTES_TRUE : BYTES_FALSE;
  }

  public static final byte[] toByteArray(final int value) {
    return SafeEncoder.encode(String.valueOf(value));
  }

  public static final byte[] toByteArray(final long value) {
    return SafeEncoder.encode(String.valueOf(value));
  }

  public static final byte[] toByteArray(final double value) {
    if (value == Double.POSITIVE_INFINITY) {
      return POSITIVE_INFINITY_BYTES;
    } else if (value == Double.NEGATIVE_INFINITY) {
      return NEGATIVE_INFINITY_BYTES;
    } else {
      return SafeEncoder.encode(String.valueOf(value));
    }
  }

  public static enum Command implements ProtocolCommand {
    PING, SET, GET, GETDEL, GETEX, @Deprecated QUIT, EXISTS, DEL, UNLINK, TYPE, FLUSHDB, KEYS, RANDOMKEY, RENAME,
    RENAMENX, RENAMEX, DBSIZE, EXPIRE, EXPIREAT, TTL, SELECT, MOVE, FLUSHALL, GETSET, MGET, SETNX,
    SETEX, MSET, MSETNX, DECRBY, DECR, INCRBY, INCR, APPEND, SUBSTR, HSET, HGET, HSETNX, HMSET,
    HMGET, HINCRBY, HEXISTS, HDEL, HLEN, HKEYS, HVALS, HGETALL, HRANDFIELD, RPUSH, LPUSH, LLEN, LRANGE, LTRIM,
    LINDEX, LSET, LREM, LPOP, RPOP, RPOPLPUSH, SADD, SMEMBERS, SREM, SPOP, SMOVE, SCARD, SISMEMBER,
    SINTER, SINTERSTORE, SUNION, SUNIONSTORE, SDIFF, SDIFFSTORE, SRANDMEMBER, ZADD, ZDIFF, ZDIFFSTORE, ZRANGE, ZREM,
    ZINCRBY, ZRANK, ZREVRANK, ZREVRANGE, ZRANDMEMBER, ZCARD, ZSCORE, ZPOPMAX, ZPOPMIN, MULTI, DISCARD, EXEC,
    WATCH, UNWATCH, SORT, BLPOP, BRPOP, AUTH, SUBSCRIBE, PUBLISH, UNSUBSCRIBE, PSUBSCRIBE,
    PUNSUBSCRIBE, PUBSUB, ZCOUNT, ZRANGEBYSCORE, ZREVRANGEBYSCORE, ZREMRANGEBYRANK,
    ZREMRANGEBYSCORE, ZUNION, ZUNIONSTORE, ZINTER, ZINTERSTORE, ZLEXCOUNT, ZRANGEBYLEX, ZREVRANGEBYLEX,
    ZREMRANGEBYLEX, SAVE, BGSAVE, BGREWRITEAOF, LASTSAVE, SHUTDOWN, INFO, MONITOR, SLAVEOF, CONFIG,
    STRLEN, @Deprecated SYNC, LPUSHX, PERSIST, RPUSHX, ECHO, LINSERT, DEBUG, BRPOPLPUSH, SETBIT, GETBIT,
    BITPOS, SETRANGE, GETRANGE, EVAL, EVALSHA, SCRIPT, SLOWLOG, OBJECT, BITCOUNT, BITOP, SENTINEL,
    DUMP, RESTORE, PEXPIRE, PEXPIREAT, PTTL, INCRBYFLOAT, PSETEX, CLIENT, TIME, MIGRATE,
    HINCRBYFLOAT, SCAN, HSCAN, SSCAN, ZSCAN, WAIT, CLUSTER, ASKING, PFADD, PFCOUNT, PFMERGE,
    READONLY, READWRITE, GEOADD, GEODIST, GEOHASH, GEOPOS, GEORADIUS, GEORADIUS_RO, GEORADIUSBYMEMBER,
    GEORADIUSBYMEMBER_RO, MODULE, BITFIELD, HSTRLEN, TOUCH, SWAPDB, MEMORY, XADD, XLEN, XDEL,
    XTRIM, XRANGE, XREVRANGE, XREAD, XACK, XGROUP, XREADGROUP, XPENDING, XCLAIM, XAUTOCLAIM, ACL, XINFO,
    BITFIELD_RO, LPOS, SMISMEMBER, ZMSCORE, BZPOPMIN, BZPOPMAX, BLMOVE, LMOVE, COPY, ROLE, FAILOVER, STRALGO;

    private final byte[] raw;

    Command() {
      raw = SafeEncoder.encode(name());
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }

  public static enum Keyword implements Rawable {
    AGGREGATE, ALPHA, ASC, BY, DESC, GET, LIMIT, MESSAGE, NO, NOSORT, PMESSAGE, PSUBSCRIBE,
    PUNSUBSCRIBE, OK, ONE, QUEUED, SET, STORE, SUBSCRIBE, UNSUBSCRIBE, WEIGHTS, WITHSCORES,
    RESETSTAT, REWRITE, RESET, FLUSH, EXISTS, LOAD, KILL, LEN, REFCOUNT, ENCODING, IDLETIME,
    GETNAME, SETNAME, LIST, MATCH, COUNT, TYPE, PING, PONG, UNLOAD, REPLACE, KEYS, PAUSE, DOCTOR,
    BLOCK, NOACK, STREAMS, KEY, CREATE, MKSTREAM, SETID, DESTROY, DELCONSUMER, MAXLEN, GROUP, ID,
    IDLE, TIME, RETRYCOUNT, FORCE, USAGE, SAMPLES, STREAM, GROUPS, CONSUMERS, HELP, FREQ, SETUSER,
    GETUSER, DELUSER, WHOAMI, CAT, GENPASS, USERS, LOG, INCR, SAVE, JUSTID, WITHVALUES, UNBLOCK,
    NOMKSTREAM, MINID, DB, ABSTTL, TO, TIMEOUT, ABORT, LCS, STRINGS, INVALIDATE;

    /**
     * @deprecated This will be private in future. Use {@link Keyword#getRaw()}.
     */
    @Deprecated
    public final byte[] raw;

    Keyword() {
      raw = SafeEncoder.encode(name().toLowerCase(Locale.ENGLISH));
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }

  public static enum SentinelKeyword implements Rawable {
    MYID, MASTERS, MASTER, SENTINELS, SLAVES, REPLICAS;

    private final byte[] raw;

    private SentinelKeyword() {
      raw = SafeEncoder.encode(name());
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }

  public static enum ClusterKeyword implements Rawable {
    MEET, RESET, INFO, FAILOVER, SLOTS, FORCE, TAKEOVER, NODES, REPLICAS, MYID;

    private final byte[] raw;

    private ClusterKeyword() {
      raw = SafeEncoder.encode(name());
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }
}
