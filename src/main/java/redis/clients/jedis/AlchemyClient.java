package redis.clients.jedis;

import java.util.ArrayList;
import java.util.List;
import redis.clients.jedis.BinaryClient;
import redis.clients.jedis.Protocol.IRedisCommand;
import redis.clients.jedis.Protocol.IRedisKeyword;
import redis.clients.util.SafeEncoder;


public class AlchemyClient extends BinaryClient {

	
	   public static enum Command implements IRedisCommand {
	        PING, SET, GET, QUIT, EXISTS, DEL, TYPE, FLUSHDB, KEYS, RANDOMKEY, RENAME, RENAMENX, RENAMEX, DBSIZE, EXPIRE, EXPIREAT, TTL, SELECT, MOVE, FLUSHALL, GETSET, MGET, SETNX, SETEX, MSET, MSETNX, DECRBY, DECR, INCRBY, INCR, APPEND, SUBSTR, HSET, HGET, HSETNX, HMSET, HMGET, HINCRBY, HEXISTS, HDEL, HLEN, HKEYS, HVALS, HGETALL, RPUSH, LPUSH, LLEN, LRANGE, LTRIM, LINDEX, LSET, LREM, LPOP, RPOP, RPOPLPUSH, SADD, SMEMBERS, SREM, SPOP, SMOVE, SCARD, SISMEMBER, SINTER, SINTERSTORE, SUNION, SUNIONSTORE, SDIFF, SDIFFSTORE, SRANDMEMBER, ZADD, ZRANGE, ZREM, ZINCRBY, ZRANK, ZREVRANK, ZREVRANGE, ZCARD, ZSCORE, MULTI, DISCARD, EXEC, WATCH, UNWATCH, SORT, BLPOP, BRPOP, AUTH, SUBSCRIBE, PUBLISH, UNSUBSCRIBE, PSUBSCRIBE, PUNSUBSCRIBE, ZCOUNT, ZRANGEBYSCORE, ZREMRANGEBYRANK, ZREMRANGEBYSCORE, ZUNIONSTORE, ZINTERSTORE, SAVE, BGSAVE, BGREWRITEAOF, LASTSAVE, SHUTDOWN, INFO, MONITOR, SLAVEOF, CONFIG, STRLEN, SYNC, LPUSHX, PERSIST, RPUSHX, ECHO, LINSERT, DEBUG, BRPOPLPUSH, SETBIT, GETBIT,
	      /* ALCHEMY DATABASE */
	      CREATE, DROP, DESC, DUMP, INSERT, SCANSELECT, UPDATE, DELETE, LUA, RETURN, SIZE;

	        public final byte[] _raw;

	        Command() {
	            _raw = SafeEncoder.encode(this.name());
	        }

			public byte[] raw() {
				// TODO Auto-generated method stub
				return _raw;
			}
	    }

	    public static enum Keyword implements IRedisKeyword{
	        AGGREGATE, ALPHA, ASC, BY, DESC, GET, LIMIT, MESSAGE, NO, NOSORT, PMESSAGE, PSUBSCRIBE, PUNSUBSCRIBE, OK, ONE, QUEUED, SET, STORE, SUBSCRIBE, UNSUBSCRIBE, WEIGHTS, WITHSCORES, RESETSTAT,
	        /* ALCHEMY DATABASE */
	        TABLE, INDEX, ON, TO, MYSQL, FILE, INTO, VALUES, FROM, WHERE;

	        public final byte[] _raw;

	        Keyword() {
	            _raw = SafeEncoder.encode(this.name().toLowerCase());
	        }

			public byte[] raw() {
				// TODO Auto-generated method stub
				return _raw;
			}

	    }
	
	
	public AlchemyClient(String host) {
		super(host);
		// TODO Auto-generated constructor stub
	}

    /* ALCHEMY DATABASE START */
    public void createTable(final byte[] tablename,
                            final byte[] column_definitions) {
        final List<byte[]> args = new ArrayList<byte[]>();
        args.add(Keyword.TABLE.raw());
        args.add(tablename);
        String s_cdefs   = new String(column_definitions);
        String p_s_cdefs = "(" + s_cdefs + ")";
        byte[] bArray    = p_s_cdefs.getBytes();
        args.add(bArray);
        sendCommand(Command.CREATE, args.toArray(new byte[args.size()][]));
    }
    public void dropTable(final byte[] tablename) {
        final List<byte[]> args = new ArrayList<byte[]>();
        args.add(Keyword.TABLE.raw());
        args.add(tablename);
        sendCommand(Command.DROP, args.toArray(new byte[args.size()][]));
    }

    public void createIndex(final byte[] indexname,
                            final byte[] tablename,
                            final byte[] column) {
        final List<byte[]> args = new ArrayList<byte[]>();
        args.add(Keyword.INDEX.raw());
        args.add(indexname);
        args.add(Keyword.ON.raw());
        args.add(tablename);
        args.add(column);
        sendCommand(Command.CREATE, args.toArray(new byte[args.size()][]));
    }
    public void dropIndex(final byte[] indexname) {
        final List<byte[]> args = new ArrayList<byte[]>();
        args.add(Keyword.INDEX.raw());
        args.add(indexname);
        sendCommand(Command.DROP, args.toArray(new byte[args.size()][]));
    }

    public void desc(final byte[] tablename) {
        sendCommand(Command.DESC, tablename);
    }
    public void dump(final byte[] tablename) {
        sendCommand(Command.DUMP, tablename);
    }
    public void dumpToMysql(final byte[] tablename,
                              final byte[] mysql_tablename) {
        final List<byte[]> args = new ArrayList<byte[]>();
        args.add(tablename);
        args.add(Keyword.TO.raw());
        args.add(Keyword.MYSQL.raw());
        if (mysql_tablename.length != 0) {
            args.add(mysql_tablename);
        }
        sendCommand(Command.DUMP, args.toArray(new byte[args.size()][]));
    }
    public void dumpToFile(final byte[] tablename, final byte[] filename) {
        final List<byte[]> args = new ArrayList<byte[]>();
        args.add(tablename);
        args.add(Keyword.TO.raw());
        args.add(Keyword.FILE.raw());
        args.add(filename);
        sendCommand(Command.DUMP, args.toArray(new byte[args.size()][]));
    }

    public void insert(final byte[] tablename, final byte[] values_list) {
        final List<byte[]> args = new ArrayList<byte[]>();
        args.add(Keyword.INTO.raw());
        args.add(tablename);
        args.add(Keyword.VALUES.raw());
        String s_vlist   = new String(values_list);
        String p_s_vilst = "(" + s_vlist + ")";
        byte[] bArray    = p_s_vilst.getBytes();
        args.add(bArray);
        sendCommand(Command.INSERT, args.toArray(new byte[args.size()][]));
    }
    public void insert_ret_size(final byte[] tablename,
                                final byte[] values_list) {
        final List<byte[]> args = new ArrayList<byte[]>();
        args.add(Keyword.INTO.raw());
        args.add(tablename);
        args.add(Keyword.VALUES.raw());
        String s_vlist   = new String(values_list);
        String p_s_vilst = "(" + s_vlist + ")";
        byte[] bArray    = p_s_vilst.getBytes();
        args.add(bArray);
        args.add(Command.RETURN.raw());
        args.add(Command.SIZE.raw());
        sendCommand(Command.INSERT, args.toArray(new byte[args.size()][]));
    }
    public void select(final byte[] column_list,
                       final byte[] tablename,
                       final byte[] where_clause) {
        final List<byte[]> args = new ArrayList<byte[]>();
        args.add(column_list);
        args.add(Keyword.FROM.raw());
        args.add(tablename);
        args.add(Keyword.WHERE.raw());
        args.add(where_clause);
        sendCommand(Command.SELECT, args.toArray(new byte[args.size()][]));
    }
    public void scanSelect(final byte[] column_list,
                           final byte[] tablename,
                           final byte[] where_clause) {
        final List<byte[]> args = new ArrayList<byte[]>();
        args.add(column_list);
        args.add(Keyword.FROM.raw());
        args.add(tablename);
        if (where_clause.length != 0) {
            args.add(Keyword.WHERE.raw());
            args.add(where_clause);
        }
        sendCommand(Command.SCANSELECT, args.toArray(new byte[args.size()][]));
    }
    public void update(final byte[] tablename,
                       final byte[] update_list,
                       final byte[] where_clause) {
        final List<byte[]> args = new ArrayList<byte[]>();
        args.add(tablename);
        args.add(Keyword.SET.raw());
        args.add(update_list);
        args.add(Keyword.WHERE.raw());
        args.add(where_clause);
        sendCommand(Command.UPDATE, args.toArray(new byte[args.size()][]));
    }
    public void sqlDelete(final byte[] tablename,
                          final byte[] where_clause) {
        final List<byte[]> args = new ArrayList<byte[]>();
        args.add(Keyword.FROM.raw());
        args.add(tablename);
        args.add(Keyword.WHERE.raw());
        args.add(where_clause);
        sendCommand(Command.DELETE, args.toArray(new byte[args.size()][]));
    }

    public void lua(final byte[] command) {
        sendCommand(Command.LUA, command);
    }
    /* ALCHEMY DATABASE END */

    
}
