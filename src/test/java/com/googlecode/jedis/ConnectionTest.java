package com.googlecode.jedis;

import static com.googlecode.jedis.JedisConfig.newJedisConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.net.UnknownHostException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ConnectionTest {
    private Connection client;

    @Test(expectedExceptions = UnknownHostException.class)
    public void checkUnkownHost() throws UnknownHostException, IOException {
	client.setConfig(newJedisConfig().host("fooobarrrr"));
	client.connect();
    }

    @Test(expectedExceptions = IOException.class)
    public void checkWrongPort() throws UnknownHostException, IOException {
	client.setConfig(newJedisConfig().port(666));
	client.connect();
    }

    @Test
    public void noArgConstructor() throws UnknownHostException, IOException {
	client.connect();
	assertThat(client.isConnected(), is(true));
    }

    @BeforeMethod
    public void setUp() throws Exception {
	client = new Connection();
    }

    @AfterMethod
    public void tearDown() throws Exception {
	client.disconnect();
    }
}