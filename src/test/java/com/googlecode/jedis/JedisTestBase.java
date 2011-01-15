package com.googlecode.jedis;

import java.io.IOException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

public class JedisTestBase extends JedisTestConfigs {

    protected Jedis jedis;

    @BeforeMethod
    public void setup() {
	jedis = JedisFactory.newJedisInstance(c1);
	jedis.flushAll();
	jedis.select(0);
    }

    @AfterMethod
    public void shutdown() throws IOException {
	jedis.disconnect();
    }

}
