package com.googlecode.jedis;

import org.testng.annotations.Test;

public class ConnectionHandlingCommandsTest extends JedisTestBase {
    @Test
    public void quit() {
	jedis.quit();
    }
}