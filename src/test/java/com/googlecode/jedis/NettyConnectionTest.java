package com.googlecode.jedis;

import static com.googlecode.jedis.JedisConfig.newJedisConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.ConnectException;
import java.nio.channels.UnresolvedAddressException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class NettyConnectionTest {
    private Connection connection;

    @Test(expectedExceptions = UnresolvedAddressException.class)
    public void checkUnkownHost() throws Throwable {
	connection.setJedisConfig(newJedisConfig().host("fooobarrrr"));
	connection.connect();
    }

    @Test(expectedExceptions = ConnectException.class)
    public void checkWrongPort() throws Throwable {
	connection.setJedisConfig(newJedisConfig().port(666));
	connection.connect();
    }

    @Test
    public void noArgConstructor() throws Throwable {
	connection.connect();
	assertThat(connection.isConnected(), is(true));
    }

    @BeforeMethod
    public void setUp() throws Exception {
	connection = new NettyConnection();
	connection.setJedisConfig(newJedisConfig());
    }

    @AfterMethod
    public void tearDown() throws Exception {
	connection.disconnect();
    }
}