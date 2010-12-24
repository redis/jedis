package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static redis.clients.jedis.JedisConfig.newJedisConfig;

import org.testng.annotations.Test;

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
