package redis.clients.jedis;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.exceptions.*;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.util.KeyValue;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.RedisOutputStream;
import redis.clients.jedis.util.SafeEncoder;

public final class Protocol {

  public static final String DEFAULT_HOST = "127.0.0.1";
  public static final int DEFAULT_PORT = 6379;
  public static final int DEFAULT_SENTINEL_PORT = 26379;
  public static final int DEFAULT_TIMEOUT = 2000;
  public static final int DEFAULT_DATABASE = 0;
  public static final int CLUSTER_HASHSLOTS = 16384;

  public static final Charset CHARSET = StandardCharsets.UTF_8;

  public static final byte ASTERISK_BYTE = '*';
  public static final byte COLON_BYTE = ':';
  public static final byte COMMA_BYTE = ',';
  public static final byte DOLLAR_BYTE = '$';
  public static final byte EQUAL_BYTE = '=';
  public static final byte GREATER_THAN_BYTE = '>';
  public static final byte HASH_BYTE = '#';
  public static final byte LEFT_BRACE_BYTE = '(';
  public static final byte MINUS_BYTE = '-';
  public static final byte PERCENT_BYTE = '%';
  public static final byte PLUS_BYTE = '+';
  public static final byte TILDE_BYTE = '~';
  public static final byte UNDERSCORE_BYTE = '_';

  public static final byte[] BYTES_TRUE = toByteArray(1);
  public static final byte[] BYTES_FALSE = toByteArray(0);
  public static final byte[] BYTES_TILDE = SafeEncoder.encode("~");
  public static final byte[] BYTES_EQUAL = SafeEncoder.encode("=");
  public static final byte[] BYTES_ASTERISK = SafeEncoder.encode("*");

  public static final byte[] POSITIVE_INFINITY_BYTES = "+inf".getBytes();
  public static final byte[] NEGATIVE_INFINITY_BYTES = "-inf".getBytes();

  static final List<KeyValue> PROTOCOL_EMPTY_MAP = Collections.unmodifiableList(new ArrayList<>(0));

  private static final String ASK_PREFIX = "ASK ";
  private static final String MOVED_PREFIX = "MOVED ";
  private static final String CLUSTERDOWN_PREFIX = "CLUSTERDOWN ";
  private static final String BUSY_PREFIX = "BUSY ";
  private static final String NOSCRIPT_PREFIX = "NOSCRIPT ";
  private static final String NOAUTH_PREFIX = "NOAUTH";
  private static final String WRONGPASS_PREFIX = "WRONGPASS";
  private static final String NOPERM_PREFIX = "NOPERM";

  private static final byte[] INVALIDATE_BYTES = SafeEncoder.encode("invalidate");

  private Protocol() {
    throw new InstantiationError("Must not instantiate this class");
  }

  public static void sendCommand(final RedisOutputStream os, CommandArguments args) {
    try {
      os.write(ASTERISK_BYTE);
      os.writeIntCrLf(args.size());
      for (Rawable arg : args) {
        os.write(DOLLAR_BYTE);
        final byte[] bin = arg.getRaw();
        os.writeIntCrLf(bin.length);
        os.write(bin);
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
      throw new JedisMovedDataException(message, HostAndPort.from(movedInfo[1]), Integer.parseInt(movedInfo[0]));
    } else if (message.startsWith(ASK_PREFIX)) {
      String[] askInfo = parseTargetHostAndSlot(message);
      throw new JedisAskDataException(message, HostAndPort.from(askInfo[1]), Integer.parseInt(askInfo[0]));
    } else if (message.startsWith(CLUSTERDOWN_PREFIX)) {
      throw new JedisClusterException(message);
    } else if (message.startsWith(BUSY_PREFIX)) {
      throw new JedisBusyException(message);
    } else if (message.startsWith(NOSCRIPT_PREFIX)) {
      throw new JedisNoScriptException(message);
    } else if (message.startsWith(NOAUTH_PREFIX)
        || message.startsWith(WRONGPASS_PREFIX)
        || message.startsWith(NOPERM_PREFIX)) {
      throw new JedisAccessControlException(message);
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
    String[] response = new String[2];
    String[] messageInfo = clusterRedirectResponse.split(" ");
    response[0] = messageInfo[1];
    response[1] = messageInfo[2];
    return response;
  }

  private static Object process(final RedisInputStream is) {
    final byte b = is.readByte();
    // System.out.println("BYTE: " + (char) b);
    switch (b) {
      case PLUS_BYTE:
        return is.readLineBytes();
      case DOLLAR_BYTE:
      case EQUAL_BYTE:
        return processBulkReply(is);
      case ASTERISK_BYTE:
        return processMultiBulkReply(is);
      case UNDERSCORE_BYTE:
        return is.readNullCrLf();
      case HASH_BYTE:
        return is.readBooleanCrLf();
      case COLON_BYTE:
        return is.readLongCrLf();
      case COMMA_BYTE:
        return is.readDoubleCrLf();
      case LEFT_BRACE_BYTE:
        return is.readBigIntegerCrLf();
      case PERCENT_BYTE: // TODO: currently just to start working with HELLO
        return processMapKeyValueReply(is);
      case TILDE_BYTE: // TODO:
        return processMultiBulkReply(is);
      case GREATER_THAN_BYTE:
        return processMultiBulkReply(is);
      case MINUS_BYTE:
        processError(is);
        return null;
      // TODO: Blob error '!'
      default:
        throw new JedisConnectionException("Unknown reply: " + (char) b);
    }
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
      if (size == -1) {
        throw new JedisConnectionException("It seems like server has closed the connection.");
      }
      offset += size;
    }

    // read 2 more bytes for the command delimiter
    is.readByte();
    is.readByte();

    return read;
  }

  private static List<Object> processMultiBulkReply(final RedisInputStream is) {
    final int num = is.readIntCrLf();
    if (num == -1)
      return null;
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

  private static List<KeyValue> processMapKeyValueReply(final RedisInputStream is) {
    final int num = is.readIntCrLf();
    switch (num) {
      case -1:
        return null;
      case 0:
        return PROTOCOL_EMPTY_MAP;
      default:
        final List<KeyValue> ret = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
          ret.add(new KeyValue(process(is), process(is)));
        }
        return ret;
    }
  }

  public static Object read(final RedisInputStream is) {
    return process(is);
  }

  @Experimental
  public static Object read(final RedisInputStream is, final Cache cache) {
    readPushes(is, cache, false);
    return process(is);
  }

  @Experimental
  public static void readPushes(final RedisInputStream is, final Cache cache, boolean onlyPendingBuffer) {
    if (onlyPendingBuffer) {
      try {
        while (is.available() > 0 && is.peek(GREATER_THAN_BYTE)) {
          is.readByte();
          processPush(is, cache);
        }
      } catch (IOException e) {
        throw new JedisConnectionException("Failed to read pending buffer for push messages!", e);
      }
    } else {
      while (is.peek(GREATER_THAN_BYTE)) {
        is.readByte();
        processPush(is, cache);
      }
    }
  }

  private static void processPush(final RedisInputStream is, Cache cache) {
    List<Object> list = processMultiBulkReply(is);
    if (list.size() == 2 && list.get(0) instanceof byte[]
        && Arrays.equals(INVALIDATE_BYTES, (byte[]) list.get(0))) {
      cache.deleteByRedisKeys((List) list.get(1));
    }
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

    PING, AUTH, HELLO, SET, GET, GETDEL, GETEX, EXISTS, DEL, UNLINK, TYPE, FLUSHDB, FLUSHALL, MOVE,
    KEYS, RANDOMKEY, RENAME, RENAMENX, DUMP, RESTORE, DBSIZE, SELECT, SWAPDB, MIGRATE, ECHO, //
    EXPIRE, EXPIREAT, EXPIRETIME, PEXPIRE, PEXPIREAT, PEXPIRETIME, TTL, PTTL, // <-- key expiration
    MULTI, DISCARD, EXEC, WATCH, UNWATCH, SORT, SORT_RO, INFO, SHUTDOWN, MONITOR, CONFIG, LCS, //
    GETSET, MGET, SETNX, SETEX, PSETEX, MSET, MSETNX, DECR, DECRBY, INCR, INCRBY, INCRBYFLOAT,
    STRLEN, APPEND, SUBSTR, // <-- string
    SETBIT, GETBIT, BITPOS, SETRANGE, GETRANGE, BITCOUNT, BITOP, BITFIELD, BITFIELD_RO, // <-- bit (string)
    HSET, HGET, HSETNX, HMSET, HMGET, HINCRBY, HEXISTS, HDEL, HLEN, HKEYS, HVALS, HGETALL, HSTRLEN,
    HEXPIRE, HPEXPIRE, HEXPIREAT, HPEXPIREAT, HTTL, HPTTL, HEXPIRETIME, HPEXPIRETIME, HPERSIST,
    HRANDFIELD, HINCRBYFLOAT, // <-- hash
    RPUSH, LPUSH, LLEN, LRANGE, LTRIM, LINDEX, LSET, LREM, LPOP, RPOP, BLPOP, BRPOP, LINSERT, LPOS,
    RPOPLPUSH, BRPOPLPUSH, BLMOVE, LMOVE, LMPOP, BLMPOP, LPUSHX, RPUSHX, // <-- list
    SADD, SMEMBERS, SREM, SPOP, SMOVE, SCARD, SRANDMEMBER, SINTER, SINTERSTORE, SUNION, SUNIONSTORE,
    SDIFF, SDIFFSTORE, SISMEMBER, SMISMEMBER, SINTERCARD, // <-- set
    ZADD, ZDIFF, ZDIFFSTORE, ZRANGE, ZREM, ZINCRBY, ZRANK, ZREVRANK, ZREVRANGE, ZRANDMEMBER, ZCARD,
    ZSCORE, ZPOPMAX, ZPOPMIN, ZCOUNT, ZUNION, ZUNIONSTORE, ZINTER, ZINTERSTORE, ZRANGEBYSCORE,
    ZREVRANGEBYSCORE, ZREMRANGEBYRANK, ZREMRANGEBYSCORE, ZLEXCOUNT, ZRANGEBYLEX, ZREVRANGEBYLEX,
    ZREMRANGEBYLEX, ZMSCORE, ZRANGESTORE, ZINTERCARD, ZMPOP, BZMPOP, BZPOPMIN, BZPOPMAX, // <-- zset
    GEOADD, GEODIST, GEOHASH, GEOPOS, GEORADIUS, GEORADIUS_RO, GEOSEARCH, GEOSEARCHSTORE,
    GEORADIUSBYMEMBER, GEORADIUSBYMEMBER_RO, // <-- geo
    PFADD, PFCOUNT, PFMERGE, // <-- hyper log log
    XADD, XLEN, XDEL, XTRIM, XRANGE, XREVRANGE, XREAD, XACK, XGROUP, XREADGROUP, XPENDING, XCLAIM,
    XAUTOCLAIM, XINFO, // <-- stream
    EVAL, EVALSHA, SCRIPT, EVAL_RO, EVALSHA_RO, FUNCTION, FCALL, FCALL_RO, // <-- program
    SUBSCRIBE, UNSUBSCRIBE, PSUBSCRIBE, PUNSUBSCRIBE, PUBLISH, PUBSUB,
    SSUBSCRIBE, SUNSUBSCRIBE, SPUBLISH, // <-- pub sub
    SAVE, BGSAVE, BGREWRITEAOF, LASTSAVE, PERSIST, ROLE, FAILOVER, SLOWLOG, OBJECT, CLIENT, TIME,
    SCAN, HSCAN, SSCAN, ZSCAN, WAIT, CLUSTER, ASKING, READONLY, READWRITE, SLAVEOF, REPLICAOF, COPY,
    SENTINEL, MODULE, ACL, TOUCH, MEMORY, LOLWUT, COMMAND, RESET, LATENCY, WAITAOF;

    private final byte[] raw;

    private Command() {
      raw = SafeEncoder.encode(name());
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }

  public static enum Keyword implements Rawable {

    AGGREGATE, ALPHA, BY, GET, LIMIT, NO, NOSORT, ONE, SET, STORE, WEIGHTS, WITHSCORE, WITHSCORES,
    RESETSTAT, REWRITE, RESET, FLUSH, EXISTS, LOAD, LEN, HELP, SCHEDULE, MATCH, COUNT, TYPE, KEYS,
    REFCOUNT, ENCODING, IDLETIME, FREQ, REPLACE, GETNAME, SETNAME, SETINFO, LIST, ID, KILL, PERSIST,
    STREAMS, CREATE, MKSTREAM, SETID, DESTROY, DELCONSUMER, MAXLEN, GROUP, IDLE, TIME, BLOCK, NOACK,
    RETRYCOUNT, STREAM, GROUPS, CONSUMERS, JUSTID, WITHVALUES, NOMKSTREAM, MINID, CREATECONSUMER,
    SETUSER, GETUSER, DELUSER, WHOAMI, USERS, CAT, GENPASS, LOG, SAVE, DRYRUN, COPY, AUTH, AUTH2,
    NX, XX, EX, PX, EXAT, PXAT, ABSTTL, KEEPTTL, INCR, LT, GT, CH, INFO, PAUSE, UNPAUSE, UNBLOCK,
    REV, WITHCOORD, WITHDIST, WITHHASH, ANY, FROMMEMBER, FROMLONLAT, BYRADIUS, BYBOX, BYLEX, BYSCORE,
    STOREDIST, TO, FORCE, TIMEOUT, DB, UNLOAD, ABORT, IDX, MINMATCHLEN, WITHMATCHLEN, FULL,
    DELETE, LIBRARYNAME, WITHCODE, DESCRIPTION, GETKEYS, GETKEYSANDFLAGS, DOCS, FILTERBY, DUMP,
    MODULE, ACLCAT, PATTERN, DOCTOR, LATEST, HISTORY, USAGE, SAMPLES, PURGE, STATS, LOADEX, CONFIG,
    ARGS, RANK, NOW, VERSION, ADDR, SKIPME, USER, LADDR, FIELDS,
    CHANNELS, NUMPAT, NUMSUB, SHARDCHANNELS, SHARDNUMSUB, NOVALUES, MAXAGE;

    private final byte[] raw;

    private Keyword() {
      raw = SafeEncoder.encode(name());
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }

  public static enum SentinelKeyword implements Rawable {

    MYID, MASTERS, MASTER, SENTINELS, SLAVES, REPLICAS, RESET, FAILOVER, REMOVE, SET, MONITOR,
    GET_MASTER_ADDR_BY_NAME("GET-MASTER-ADDR-BY-NAME");

    private final byte[] raw;

    private SentinelKeyword() {
      raw = SafeEncoder.encode(name());
    }

    private SentinelKeyword(String str) {
      raw = SafeEncoder.encode(str);
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }

  public static enum ResponseKeyword implements Rawable {

    SUBSCRIBE, PSUBSCRIBE, UNSUBSCRIBE, PUNSUBSCRIBE, MESSAGE, PMESSAGE, PONG,
    SSUBSCRIBE, SUNSUBSCRIBE, SMESSAGE;

    private final byte[] raw;

    private ResponseKeyword() {
      raw = SafeEncoder.encode(name().toLowerCase(Locale.ENGLISH));
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }

  public static enum ClusterKeyword implements Rawable {

    MEET, RESET, INFO, FAILOVER, SLOTS, NODES, REPLICAS, SLAVES, MYID, ADDSLOTS, DELSLOTS,
    GETKEYSINSLOT, SETSLOT, NODE, MIGRATING, IMPORTING, STABLE, FORGET, FLUSHSLOTS, KEYSLOT,
    COUNTKEYSINSLOT, SAVECONFIG, REPLICATE, LINKS, ADDSLOTSRANGE, DELSLOTSRANGE, BUMPEPOCH,
    MYSHARDID, SHARDS;

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
