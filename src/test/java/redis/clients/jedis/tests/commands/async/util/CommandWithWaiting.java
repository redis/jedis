package redis.clients.jedis.tests.commands.async.util;

import redis.clients.jedis.async.AsyncJedis;

import java.util.Map;

public class CommandWithWaiting {

  public static final int TIMEOUT_MS = 1000;

  public static void set(AsyncJedis j, byte[] key, byte[] value) {
    AsyncJUnitTestCallback<String> callback = new AsyncJUnitTestCallback<String>();
    j.set(callback, key, value);
    waitForResponse(callback);
  }

  public static void set(AsyncJedis j, String key, String value) {
    AsyncJUnitTestCallback<String> callback = new AsyncJUnitTestCallback<String>();
    j.set(callback, key, value);
    waitForResponse(callback);
  }

  public static void del(AsyncJedis j, byte[] key) {
    AsyncJUnitTestCallback<Long> callback = new AsyncJUnitTestCallback<Long>();
    j.del(callback, key);
    waitForResponse(callback);
  }

  public static void del(AsyncJedis j, byte[]... keys) {
    AsyncJUnitTestCallback<Long> callback = new AsyncJUnitTestCallback<Long>();
    j.del(callback, keys);
    waitForResponse(callback);
  }

  public static void del(AsyncJedis j, String key) {
    AsyncJUnitTestCallback<Long> callback = new AsyncJUnitTestCallback<Long>();
    j.del(callback, key);
    waitForResponse(callback);
  }

  public static void del(AsyncJedis j, String... keys) {
    AsyncJUnitTestCallback<Long> callback = new AsyncJUnitTestCallback<Long>();
    j.del(callback, keys);
    waitForResponse(callback);
  }

  public static void move(AsyncJedis j, String key, int dbNum) {
    AsyncJUnitTestCallback<Long> callback = new AsyncJUnitTestCallback<Long>();
    j.move(callback, key, dbNum);
    waitForResponse(callback);
  }

  public static void setex(AsyncJedis j, byte[] key, int seconds, byte[] value) {
    AsyncJUnitTestCallback<String> callback = new AsyncJUnitTestCallback<String>();
    j.setex(callback, key, seconds, value);
    waitForResponse(callback);
  }

  public static void setex(AsyncJedis j, String key, int seconds, String value) {
    AsyncJUnitTestCallback<String> callback = new AsyncJUnitTestCallback<String>();
    j.setex(callback, key, seconds, value);
    waitForResponse(callback);
  }

  public static void pexpire(AsyncJedis j, String key, long milliseconds) {
    AsyncJUnitTestCallback<Long> callback = new AsyncJUnitTestCallback<Long>();
    j.pexpire(callback, key, milliseconds);
    waitForResponse(callback);
  }

  public static void setbit(AsyncJedis j, String key, long offset, boolean value) {
    AsyncJUnitTestCallback<Boolean> callback = new AsyncJUnitTestCallback<Boolean>();
    j.setbit(callback, key, offset, value);
    waitForResponse(callback);
  }

  public static void hset(AsyncJedis j, byte[] key, byte[] field, byte[] value) {
    AsyncJUnitTestCallback<Long> callback = new AsyncJUnitTestCallback<Long>();
    j.hset(callback, key, field, value);
    waitForResponse(callback);
  }

  public static void hset(AsyncJedis j, String key, String field, String value) {
    AsyncJUnitTestCallback<Long> callback = new AsyncJUnitTestCallback<Long>();
    j.hset(callback, key, field, value);
    waitForResponse(callback);
  }

  public static void hmset(AsyncJedis j, byte[] key, Map<byte[], byte[]> hash) {
    AsyncJUnitTestCallback<String> callback = new AsyncJUnitTestCallback<String>();
    j.hmset(callback, key, hash);
    waitForResponse(callback);
  }

  public static void hmset(AsyncJedis j, String key, Map<String, String> hash) {
    AsyncJUnitTestCallback<String> callback = new AsyncJUnitTestCallback<String>();
    j.hmset(callback, key, hash);
    waitForResponse(callback);
  }

  public static void lpush(AsyncJedis j, byte[] key, byte[]... strings) {
    AsyncJUnitTestCallback<Long> callback = new AsyncJUnitTestCallback<Long>();
    j.lpush(callback, key, strings);
    waitForResponse(callback);
  }

  public static void lpush(AsyncJedis j, String key, String... strings) {
    AsyncJUnitTestCallback<Long> callback = new AsyncJUnitTestCallback<Long>();
    j.lpush(callback, key, strings);
    waitForResponse(callback);
  }

  public static void rpush(AsyncJedis j, byte[] key, byte[]... strings) {
    AsyncJUnitTestCallback<Long> callback = new AsyncJUnitTestCallback<Long>();
    j.rpush(callback, key, strings);
    waitForResponse(callback);
  }

  public static void rpush(AsyncJedis j, String key, String... strings) {
    AsyncJUnitTestCallback<Long> callback = new AsyncJUnitTestCallback<Long>();
    j.rpush(callback, key, strings);
    waitForResponse(callback);
  }

  public static void lpop(AsyncJedis j, byte[] key) {
    AsyncJUnitTestCallback<byte[]> callback = new AsyncJUnitTestCallback<byte[]>();
    j.lpop(callback, key);
    waitForResponse(callback);
  }

  public static void lpop(AsyncJedis j, String key) {
    AsyncJUnitTestCallback<String> callback = new AsyncJUnitTestCallback<String>();
    j.lpop(callback, key);
    waitForResponse(callback);
  }

  public static void rpop(AsyncJedis j, byte[] key) {
    AsyncJUnitTestCallback<byte[]> callback = new AsyncJUnitTestCallback<byte[]>();
    j.rpop(callback, key);
    waitForResponse(callback);
  }

  public static void rpop(AsyncJedis j, String key) {
    AsyncJUnitTestCallback<String> callback = new AsyncJUnitTestCallback<String>();
    j.rpop(callback, key);
    waitForResponse(callback);
  }

  public static void sadd(AsyncJedis j, byte[] key, byte[]... members) {
    AsyncJUnitTestCallback<Long> callback = new AsyncJUnitTestCallback<Long>();
    j.sadd(callback, key, members);
    waitForResponse(callback);
  }

  public static void sadd(AsyncJedis j, String key, String... members) {
    AsyncJUnitTestCallback<Long> callback = new AsyncJUnitTestCallback<Long>();
    j.sadd(callback, key, members);
    waitForResponse(callback);
  }

  public static void zadd(AsyncJedis j, byte[] key, double score, byte[] member) {
    AsyncJUnitTestCallback<Long> callback = new AsyncJUnitTestCallback<Long>();
    j.zadd(callback, key, score, member);
    waitForResponse(callback);
  }

  public static void zadd(AsyncJedis j, String key, double score, String member) {
    AsyncJUnitTestCallback<Long> callback = new AsyncJUnitTestCallback<Long>();
    j.zadd(callback, key, score, member);
    waitForResponse(callback);
  }

  private static <T> void waitForResponse(AsyncJUnitTestCallback<T> callback) {
    callback.getResponseWithWaiting(TIMEOUT_MS);
  }

}
