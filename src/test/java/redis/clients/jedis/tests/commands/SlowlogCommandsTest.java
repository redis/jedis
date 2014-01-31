package redis.clients.jedis.tests.commands;

import java.util.List;

import org.junit.Test;

import redis.clients.util.Slowlog;

public class SlowlogCommandsTest extends JedisCommandTestBase {

    @Test
    public void slowlog() {
	// do something
	jedis.configSet("slowlog-log-slower-than", "0");
	jedis.set("foo", "bar");
	jedis.set("foo2", "bar2");

	List<Slowlog> reducedLog = jedis.slowlogGet(1);
	assertEquals(1, reducedLog.size());

	Slowlog log = reducedLog.get(0);
	assertTrue(log.getId() > 0);
	assertTrue(log.getTimeStamp() > 0);
	assertTrue(log.getExecutionTime() > 0);
	assertNotNull(log.getArgs());

	List<byte[]> breducedLog = jedis.slowlogGetBinary(1);
	assertEquals(1, breducedLog.size());

	List<Slowlog> log1 = jedis.slowlogGet();
	List<byte[]> blog1 = jedis.slowlogGetBinary();

	assertNotNull(log1);
	assertNotNull(blog1);

	long len1 = jedis.slowlogLen();

	jedis.slowlogReset();

	List<Slowlog> log2 = jedis.slowlogGet();
	List<byte[]> blog2 = jedis.slowlogGetBinary();
	long len2 = jedis.slowlogLen();

	assertTrue(len1 > len2);
	assertTrue(log1.size() > log2.size());
	assertTrue(blog1.size() > blog2.size());
    }
}