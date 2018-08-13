package redis.clients.jedis.tests.benchmark;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Calendar;

import redis.clients.jedis.util.Hashing;

public class ShardedBenchmark {
  private static final int TOTAL_OPERATIONS = 10000000;

  public static void main(String[] args) throws UnknownHostException, IOException {

    long begin = Calendar.getInstance().getTimeInMillis();

    for (int n = 0; n <= TOTAL_OPERATIONS; n++) {
      String key = "foo" + n;
      Hashing.MD5.hash(key);
    }

    long elapsed = Calendar.getInstance().getTimeInMillis() - begin;

    System.out.println(((1000 * TOTAL_OPERATIONS) / elapsed) + " MD5 ops");

    begin = Calendar.getInstance().getTimeInMillis();

    for (int n = 0; n <= TOTAL_OPERATIONS; n++) {
      String key = "foo" + n;
      Hashing.MURMUR_HASH.hash(key);
    }

    elapsed = Calendar.getInstance().getTimeInMillis() - begin;

    System.out.println(((1000 * TOTAL_OPERATIONS) / elapsed) + " Murmur ops");

  }
}