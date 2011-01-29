package com.googlecode.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;

public class JedisTest extends JedisTestBase {

    @Test
    public void checkBinaryData() {
	final byte[] bigdata = new byte[1777];
	for (int b = 0; b < bigdata.length; b++) {
	    bigdata[b] = (byte) ((byte) b % 255);
	}
	final Map<String, String> hash = ImmutableMap.of("data", new String(
		bigdata, Charsets.UTF_8));

	assertThat(jedis.hmset("foo", hash), is(true));
	assertThat(jedis.hgetAll("foo"), is(hash));
    }

    @Test(expectedExceptions = JedisException.class)
    public void timeoutConnection() throws Exception {
	jedis.auth("foobared");
	jedis.configSet("timeout", "1");
	// we need to sleep a long time since redis check for idle connections
	// every 10 seconds or so
	Thread.sleep(20000);
	jedis.get("foo");
    }

    @Test
    public void useWithoutConnecting() {
	final Jedis jedis = JedisFactory.newJedisInstance();
	jedis.auth("foobared");
	jedis.dbSize();
    }
}