package redis.clients.jedis.tests.benchmark;

import redis.clients.jedis.Protocol;
import redis.clients.util.RedisInputStream;
import redis.clients.util.RedisOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Copyright (c) 2014
 */
public class ProtocolBenchmark {
  private static final int TOTAL_OPERATIONS = 500000;

  public static void main(String[] args) throws Exception, IOException {
    long total = 0;
    for (int at = 0; at != 10; ++at) {
      long elapsed = measureInputMulti();
      long ops = ((1000 * 2 * TOTAL_OPERATIONS) / TimeUnit.NANOSECONDS.toMillis(elapsed));
      if (at >= 5) {
        total += ops;
      }
    }
    System.out.println((total / 5) + " avg");

    total = 0;
    for (int at = 0; at != 10; ++at) {
      long elapsed = measureInputStatus();
      long ops = ((1000 * 2 * TOTAL_OPERATIONS) / TimeUnit.NANOSECONDS.toMillis(elapsed));
      if (at >= 5) {
        total += ops;
      }
    }

    System.out.println((total / 5) + " avg");

    total = 0;
    for (int at = 0; at != 10; ++at) {
      long elapsed = measureCommand();
      long ops = ((1000 * 2 * TOTAL_OPERATIONS) / TimeUnit.NANOSECONDS.toMillis(elapsed));
      if (at >= 5) {
        total += ops;
      }
    }

    System.out.println((total / 5) + " avg");
  }

  private static long measureInputMulti() throws Exception {
    long duration = 0;

    InputStream is = new ByteArrayInputStream(
        "*4\r\n$3\r\nfoo\r\n$13\r\nbarbarbarfooz\r\n$5\r\nHello\r\n$5\r\nWorld\r\n".getBytes());

    RedisInputStream in = new RedisInputStream(is);
    for (int n = 0; n <= TOTAL_OPERATIONS; n++) {
      long start = System.nanoTime();
      Protocol.read(in);
      duration += (System.nanoTime() - start);
      in.reset();
    }

    return duration;
  }

  private static long measureInputStatus() throws Exception {
    long duration = 0;

    InputStream is = new ByteArrayInputStream("+OK\r\n".getBytes());

    RedisInputStream in = new RedisInputStream(is);
    for (int n = 0; n <= TOTAL_OPERATIONS; n++) {
      long start = System.nanoTime();
      Protocol.read(in);
      duration += (System.nanoTime() - start);
      in.reset();
    }

    return duration;
  }

  private static long measureCommand() throws Exception {
    long duration = 0;

    byte[] KEY = "123456789".getBytes();
    byte[] VAL = "FooBar".getBytes();

    for (int n = 0; n <= TOTAL_OPERATIONS; n++) {
      RedisOutputStream out = new RedisOutputStream(new ByteArrayOutputStream(8192));
      long start = System.nanoTime();
      Protocol.sendCommand(out, Protocol.Command.SET, KEY, VAL);
      duration += (System.nanoTime() - start);
    }

    return duration;
  }
}
