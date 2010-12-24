package redis.clients.jedis;

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
	assertThat(jedis.bgrewriteaof(),
		is("Background append only file rewriting started"));
    }

    @Test
    public void bgsave() {
	try {
	    assertThat(jedis.bgsave(), is("Background saving started"));
	} catch (JedisException e) {
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
	List<String> info = jedis.configGet("maxmemory");
	String memory = info.get(1);
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
	long before = jedis.lastsave();
	String st = "";
	while (!st.equals("OK")) {
	    try {
		Thread.sleep(1000);
		st = jedis.save();
	    } catch (JedisException e) {

	    }
	}
	long after = jedis.lastsave();
	assertThat((after - before), greaterThan(0L));
    }

    @Test
    public void monitor() {
	jedis.monitor(new JedisMonitor() {
	    @Override
	    public void onCommand(String command) {
		assertThat(command, containsString("OK"));
		client.disconnect();
	    }
	});
    }

    @Test
    public void save() {
	assertThat(jedis.save(), is("OK"));
    }

    @Test
    public void sync() {
	jedis.sync();
    }
}