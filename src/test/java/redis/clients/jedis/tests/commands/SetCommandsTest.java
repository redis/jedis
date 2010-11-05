package redis.clients.jedis.tests.commands;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

import redis.clients.jedis.tests.JedisTest;

public class SetCommandsTest extends JedisCommandTestBase {
	final byte[] bfoo  = {0x01, 0x02, 0x03, 0x04};
	final byte[] bbar  = {0x05, 0x06, 0x07, 0x08};
	final byte[] bcar  = {0x09, 0x0A, 0x0B, 0x0C};
	final byte[] ba  = {0x0A};
	final byte[] bb  = {0x0B};
	final byte[] bc  = {0x0C};
	final byte[] bd  = {0x0D};
	final byte[] bx  = {0x42};
	

    @Test
    public void sadd() {
	int status = jedis.sadd("foo", "a");
	assertEquals(1, status);

	status = jedis.sadd("foo", "a");
	assertEquals(0, status);
	
	int bstatus = jedis.sadd(bfoo, ba);
	assertEquals(1, bstatus);

	bstatus = jedis.sadd(bfoo, ba);
	assertEquals(0, bstatus);

    }

    @Test
    public void smembers() {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	Set<String> expected = new LinkedHashSet<String>();
	expected.add("a");
	expected.add("b");

	Set<String> members = jedis.smembers("foo");

	assertEquals(expected, members);
	
	//Binary
	jedis.sadd(bfoo, ba);
	jedis.sadd(bfoo, bb);

	Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
	bexpected.add(ba);
	bexpected.add(bb);

	Set<byte[]> bmembers = jedis.smembers(bfoo);

	assertTrue(JedisTest.isListAreEquals(bexpected, bmembers));
    }
    

    @Test
    public void srem() {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	int status = jedis.srem("foo", "a");

	Set<String> expected = new LinkedHashSet<String>();
	expected.add("b");

	assertEquals(1, status);
	assertEquals(expected, jedis.smembers("foo"));

	status = jedis.srem("foo", "bar");

	assertEquals(0, status);
	
	//Binary
	
	jedis.sadd(bfoo, ba);
	jedis.sadd(bfoo, bb);

	int bstatus = jedis.srem(bfoo, ba);

	Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
	bexpected.add(bb);

	assertEquals(1, bstatus);
	assertTrue(JedisTest.isListAreEquals(bexpected, jedis.smembers(bfoo)));

	bstatus = jedis.srem(bfoo, bbar);

	assertEquals(0, bstatus);

    }

    @Test
    public void spop() {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	String member = jedis.spop("foo");

	assertTrue("a".equals(member) || "b".equals(member));
	assertEquals(1, jedis.smembers("foo").size());

	member = jedis.spop("bar");
	assertNull(member);
	
	//Binary
	jedis.sadd(bfoo, ba);
	jedis.sadd(bfoo, bb);

	byte[] bmember = jedis.spop(bfoo);

	assertTrue(Arrays.equals(ba, bmember) || Arrays.equals(bb,bmember));
	assertEquals(1, jedis.smembers(bfoo).size());

	bmember = jedis.spop(bbar);
	assertNull(bmember);

    }

    @Test
    public void smove() {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	jedis.sadd("bar", "c");

	int status = jedis.smove("foo", "bar", "a");

	Set<String> expectedSrc = new LinkedHashSet<String>();
	expectedSrc.add("b");

	Set<String> expectedDst = new LinkedHashSet<String>();
	expectedDst.add("c");
	expectedDst.add("a");

	assertEquals(status, 1);
	assertEquals(expectedSrc, jedis.smembers("foo"));
	assertEquals(expectedDst, jedis.smembers("bar"));

	status = jedis.smove("foo", "bar", "a");

	assertEquals(status, 0);
	
	//Binary
	jedis.sadd(bfoo, ba);
	jedis.sadd(bfoo, bb);

	jedis.sadd(bbar, bc);

	int bstatus = jedis.smove(bfoo, bbar, ba);

	Set<byte[]> bexpectedSrc = new LinkedHashSet<byte[]>();
	bexpectedSrc.add(bb);

	Set<byte[]> bexpectedDst = new LinkedHashSet<byte[]>();
	bexpectedDst.add(bc);
	bexpectedDst.add(ba);

	assertEquals(bstatus, 1);
	assertTrue(JedisTest.isListAreEquals(bexpectedSrc, jedis.smembers(bfoo)));
	assertTrue(JedisTest.isListAreEquals(bexpectedDst, jedis.smembers(bbar)));

	bstatus = jedis.smove(bfoo, bbar, ba);
	assertEquals(bstatus, 0);

    }

    @Test
    public void scard() {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	int card = jedis.scard("foo");

	assertEquals(2, card);

	card = jedis.scard("bar");
	assertEquals(0, card);
	
	//Binary
	jedis.sadd(bfoo, ba);
	jedis.sadd(bfoo, bb);

	int bcard = jedis.scard(bfoo);

	assertEquals(2, bcard);

	bcard = jedis.scard(bbar);
	assertEquals(0, bcard);

    }

    @Test
    public void sismember() {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	int status = jedis.sismember("foo", "a");
	assertEquals(1, status);

	status = jedis.sismember("foo", "c");
	assertEquals(0, status);
	
	//Binary
	jedis.sadd(bfoo, ba);
	jedis.sadd(bfoo, bb);

	int bstatus = jedis.sismember(bfoo, ba);
	assertEquals(1, bstatus);

	bstatus = jedis.sismember(bfoo, bc);
	assertEquals(0, bstatus);

    }

    @Test
    public void sinter() {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	jedis.sadd("bar", "b");
	jedis.sadd("bar", "c");

	Set<String> expected = new LinkedHashSet<String>();
	expected.add("b");

	Set<String> intersection = jedis.sinter("foo", "bar");
	assertEquals(expected, intersection);
	
	//Binary
	jedis.sadd(bfoo, ba);
	jedis.sadd(bfoo, bb);

	jedis.sadd(bbar, bb);
	jedis.sadd(bbar, bc);

	Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
	bexpected.add(bb);

	Set<byte[]> bintersection = jedis.sinter(bfoo, bbar);
	assertTrue(JedisTest.isListAreEquals(bexpected, bintersection));

    }

    @Test
    public void sinterstore() {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	jedis.sadd("bar", "b");
	jedis.sadd("bar", "c");

	Set<String> expected = new LinkedHashSet<String>();
	expected.add("b");

	int status = jedis.sinterstore("car", "foo", "bar");
	assertEquals(1, status);

	assertEquals(expected, jedis.smembers("car"));
	
	//Binary
	jedis.sadd(bfoo, ba);
	jedis.sadd(bfoo, bb);

	jedis.sadd(bbar, bb);
	jedis.sadd(bbar, bc);

	Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
	bexpected.add(bb);

	int bstatus = jedis.sinterstore(bcar, bfoo, bbar);
	assertEquals(1, bstatus);

	assertTrue(JedisTest.isListAreEquals(bexpected, jedis.smembers(bcar)));

    }

    @Test
    public void sunion() {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	jedis.sadd("bar", "b");
	jedis.sadd("bar", "c");

	Set<String> expected = new LinkedHashSet<String>();
	expected.add("a");
	expected.add("b");
	expected.add("c");

	Set<String> union = jedis.sunion("foo", "bar");
	assertEquals(expected, union);
	
	//Binary
	jedis.sadd(bfoo, ba);
	jedis.sadd(bfoo, bb);

	jedis.sadd(bbar, bb);
	jedis.sadd(bbar, bc);

	Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
	bexpected.add(ba);
	bexpected.add(bb);
	bexpected.add(bc);

	Set<byte[]> bunion = jedis.sunion(bfoo, bbar);
	assertTrue(JedisTest.isListAreEquals(bexpected, bunion));

    }

    @Test
    public void sunionstore() {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	jedis.sadd("bar", "b");
	jedis.sadd("bar", "c");

	Set<String> expected = new LinkedHashSet<String>();
	expected.add("a");
	expected.add("b");
	expected.add("c");

	int status = jedis.sunionstore("car", "foo", "bar");
	assertEquals(3, status);

	assertEquals(expected, jedis.smembers("car"));
	
	//Binary
	jedis.sadd(bfoo, ba);
	jedis.sadd(bfoo, bb);

	jedis.sadd(bbar, bb);
	jedis.sadd(bbar, bc);

	Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
	bexpected.add(ba);
	bexpected.add(bb);
	bexpected.add(bc);

	int bstatus = jedis.sunionstore(bcar, bfoo, bbar);
	assertEquals(3, bstatus);

	assertTrue(JedisTest.isListAreEquals(bexpected, jedis.smembers(bcar)));

    }

    @Test
    public void sdiff() {
	jedis.sadd("foo", "x");
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");
	jedis.sadd("foo", "c");

	jedis.sadd("bar", "c");

	jedis.sadd("car", "a");
	jedis.sadd("car", "d");

	Set<String> expected = new LinkedHashSet<String>();
	expected.add("x");
	expected.add("b");

	Set<String> diff = jedis.sdiff("foo", "bar", "car");
	assertEquals(expected, diff);
	
	//Binary
	jedis.sadd(bfoo, bx);
	jedis.sadd(bfoo, ba);
	jedis.sadd(bfoo, bb);
	jedis.sadd(bfoo, bc);

	jedis.sadd(bbar, bc);

	jedis.sadd(bcar, ba);
	jedis.sadd(bcar, bd);

	Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
	bexpected.add(bx);
	bexpected.add(bb);

	Set<byte[]> bdiff = jedis.sdiff(bfoo, bbar, bcar);
	assertTrue(JedisTest.isListAreEquals(bexpected, bdiff));

    }

    @Test
    public void sdiffstore() {
	jedis.sadd("foo", "x");
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");
	jedis.sadd("foo", "c");

	jedis.sadd("bar", "c");

	jedis.sadd("car", "a");
	jedis.sadd("car", "d");

	Set<String> expected = new LinkedHashSet<String>();
	expected.add("d");
	expected.add("a");

	int status = jedis.sdiffstore("tar", "foo", "bar", "car");
	assertEquals(2, status);
	assertEquals(expected, jedis.smembers("car"));
	
	//Binary
	jedis.sadd(bfoo, bx);
	jedis.sadd(bfoo, ba);
	jedis.sadd(bfoo, bb);
	jedis.sadd(bfoo, bc);

	jedis.sadd(bbar, bc);

	jedis.sadd(bcar, ba);
	jedis.sadd(bcar, bd);

	Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
	bexpected.add(bd);
	bexpected.add(ba);

	int bstatus = jedis.sdiffstore("tar".getBytes(), bfoo, bbar, bcar);
	assertEquals(2, bstatus);
	assertTrue(JedisTest.isListAreEquals(bexpected, jedis.smembers(bcar)));

    }

    @Test
    public void srandmember() {
	jedis.sadd("foo", "a");
	jedis.sadd("foo", "b");

	String member = jedis.srandmember("foo");

	assertTrue("a".equals(member) || "b".equals(member));
	assertEquals(2, jedis.smembers("foo").size());

	member = jedis.srandmember("bar");
	assertNull(member);
	
	//Binary
	jedis.sadd(bfoo, ba);
	jedis.sadd(bfoo, bb);

	byte[] bmember = jedis.srandmember(bfoo);

	assertTrue(Arrays.equals(ba, bmember)  || Arrays.equals(bb, bmember));
	assertEquals(2, jedis.smembers(bfoo).size());

	bmember = jedis.srandmember(bbar);
	assertNull(bmember);

    }

}