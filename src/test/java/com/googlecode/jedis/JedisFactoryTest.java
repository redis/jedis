package com.googlecode.jedis;

import static com.googlecode.jedis.JedisConfig.newJedisConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.testng.annotations.Test;

import com.googlecode.jedis.Jedis;
import com.googlecode.jedis.JedisFactory;

public class JedisFactoryTest {

    @Test
    public void newJedisInstance() {
	Jedis jedis = JedisFactory.newJedisInstance();
	// TODO: do something meaningful
    }

    @Test
    public void newJedisInstanceWithParams() {
	Jedis jedis = JedisFactory.newJedisInstance(newJedisConfig()
		.password("foobared"));
	jedis.flushAll();
	jedis.set("foo", "bar");
	assertThat(jedis.get("foo"), is("bar"));
    }

}
