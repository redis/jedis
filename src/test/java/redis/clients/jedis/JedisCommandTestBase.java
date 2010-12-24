package redis.clients.jedis;

import static redis.clients.jedis.JedisConfig.newJedisConfig;
import static redis.clients.jedis.JedisFactory.newJedisInstance;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public abstract class JedisCommandTestBase {

    protected Jedis jedis;

    @BeforeMethod
    // @DataProvider(name = "redis-config")
    public void jedisConnect() {
	jedis = newJedisInstance(newJedisConfig().password("foobared"));
	jedis.configSet("timeout", "300");
	jedis.flushAll();
    }

    @AfterMethod
    public void jedisShutdown() throws Exception {
	jedis.disconnect();
    }

    // TODO: remove
    @Test(dataProvider = "redis-config")
    public void parameterIntTest(JedisConfig config) {
	System.out.println("Parameterized Config is : " + config.toString());
    }

    @DataProvider(name = "redis-config")
    public Object[][] parameterIntTestProvider() {
	return new Object[][] {
		{ newJedisConfig().host("localhost").password("foobared") },
		{ newJedisConfig().host("localhost").port(6380)
			.password("foobared") } };
    }
}