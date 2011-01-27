package com.googlecode.jedis;

import static com.googlecode.jedis.JedisConfig.newJedisConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.testng.annotations.Test;

public class JedisFactoryTest {

    @Test
    public void newJedisInstance() {
	Jedis jedis = JedisFactory.newJedisInstance();
	assertThat(jedis.getJedisConfig().getHost(), is("localhost"));
	assertThat(jedis.isConnected(), is(true));
    }

    @Test
    public void newJedisInstanceWithParams() {
	Jedis jedis = JedisFactory.newJedisInstance(newJedisConfig().password(
		"foobared"));
	assertThat(jedis.isConnected(), is(true));
    }

}
