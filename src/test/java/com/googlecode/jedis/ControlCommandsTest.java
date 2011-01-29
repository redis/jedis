package com.googlecode.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;

import org.testng.annotations.Test;

public class ControlCommandsTest extends JedisTestBase {
    @Test
    public void bgrewriteaof() {
	assertThat(jedis.bgrewriteaof(), is(true));
    }

    @Test
    public void bgsave() {
	try {
	    assertThat(jedis.bgsave(), is(true));
	} catch (final JedisException e) {
	    assertThat("ERR Background save already in progress",
		    is(e.getMessage()));
	}
    }

    @Test
    public void configGet() {
	assertThat(jedis.configGet("m*"), notNullValue());
    }

    @Test
    public void configSet() {
	final List<String> info = jedis.configGet("maxmemory");
	final String memory = info.get(1);
	assertThat(jedis.configSet("maxmemory", "200"), is("OK"));
	assertThat(jedis.configSet("maxmemory", memory), is("OK"));
    }

    @Test
    public void debug() {
	jedis.set("foo", "bar");
	assertThat(jedis.debug(DebugParams.OBJECT("foo")), notNullValue());
	assertThat(jedis.debug(DebugParams.SWAPIN("foo")), notNullValue());
	assertThat(jedis.debug(DebugParams.RELOAD()), notNullValue());
    }

    @Test
    public void info() {
	assertThat(jedis.info(), notNullValue());
    }

    @Test
    public void lastsave() throws InterruptedException {
	final long before = jedis.lastsave();
	Boolean saveHappend = false;
	while (!saveHappend) {
	    try {
		Thread.sleep(1000);
		saveHappend = jedis.save();
	    } catch (final JedisException e) {
	    }
	}
	final long after = jedis.lastsave();
	assertThat((after - before), greaterThan(0L));
    }

    @Test
    public void monitor() {
	jedis.monitor(new JedisMonitor() {
	    @Override
	    public void onCommand(final String command) {
		assertThat(command, containsString("OK"));
		connection.disconnect();
	    }
	});
    }

    @Test
    public void save() {
	assertThat(jedis.save(), is(true));
    }

    @Test
    public void sync() {
	jedis.sync();
    }
}