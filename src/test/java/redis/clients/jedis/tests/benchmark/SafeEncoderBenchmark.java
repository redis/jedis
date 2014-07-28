package redis.clients.jedis.tests.benchmark;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Calendar;

import redis.clients.util.SafeEncoder;

public class SafeEncoderBenchmark {
    private static final int TOTAL_OPERATIONS = 10000000;

    public static void main(String[] args) throws UnknownHostException,
	    IOException {
	long begin = Calendar.getInstance().getTimeInMillis();

	for (int n = 0; n <= TOTAL_OPERATIONS; n++) {
	    SafeEncoder.encode("foo bar!");
	}

	long elapsed = Calendar.getInstance().getTimeInMillis() - begin;

	System.out.println(((1000 * TOTAL_OPERATIONS) / elapsed)
		+ " ops to build byte[]");

	begin = Calendar.getInstance().getTimeInMillis();

	byte[] bytes = "foo bar!".getBytes();
	for (int n = 0; n <= TOTAL_OPERATIONS; n++) {
	    SafeEncoder.encode(bytes);
	}

	elapsed = Calendar.getInstance().getTimeInMillis() - begin;

	System.out.println(((1000 * TOTAL_OPERATIONS) / elapsed)
		+ " ops to build Strings");

    }
}