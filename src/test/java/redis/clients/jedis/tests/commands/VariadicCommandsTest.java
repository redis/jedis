package redis.clients.jedis.tests.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class VariadicCommandsTest extends JedisCommandTestBase {
	final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
    final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
    final byte[] bcar = { 0x09, 0x0A, 0x0B, 0x0C };
    final byte[] bcar2 = { 0x09, 0x0A, 0x0B, 0x0D };
    final byte[] bcar3 = { 0x10, 0x0B, 0x0C, 0x0E };
    final byte[] bfoo1 = { 0x01, 0x02, 0x03, 0x04, 0x0A };
    final byte[] bfoo2 = { 0x01, 0x02, 0x03, 0x04, 0x0B };
    final byte[] bsbar = { 0x15, 0x16, 0x17, 0x18 };
    final byte[] bscar = { 0x19, 0x1A, 0x1B, 0x1C };
    final byte[] bsfoo = { 0x11, 0x12, 0x13, 0x14 };
    final byte[] blfoo = { 0x12, 0x13, 0x14, 0x15 };
    final byte[] blbar = { 0x15, 0x16, 0x17, 0x18 };
    final byte[] blbar2 = { 0x16, 0x17, 0x18, 0x19 };
    final byte[] blbar3 = { 0x17, 0x18, 0x19, 0x10 };
	
	@Test
    public void hdel() {
	    //String
        Map<String, String> hash = new HashMap<String, String>();
        hash.put("bar", "car");
        hash.put("car", "bar");
        hash.put("foo2", "bar");
        jedis.hmset("foo", hash);

        assertEquals(0, jedis.hdel("bar", "foo", "foo1").intValue());
        assertEquals(0, jedis.hdel("foo", "foo", "foo1").intValue());
        assertEquals(2, jedis.hdel("foo", "bar", "foo2").intValue());
        assertEquals(null, jedis.hget("foo", "bar"));

        // Binary
        Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
        bhash.put(bbar, bcar);
        bhash.put(bcar, bbar);
        bhash.put(bfoo2, bbar);
        jedis.hmset(bfoo, bhash);

        assertEquals(0, jedis.hdel(bbar, bfoo, bfoo1).intValue());
        assertEquals(0, jedis.hdel(bfoo, bfoo, bfoo1).intValue());
        assertEquals(2, jedis.hdel(bfoo, bbar, bfoo2).intValue());
        assertEquals(null, jedis.hget(bfoo, bbar));
        
        // String Set
        Map<String, String> sshash = new HashMap<String, String>();
        sshash.put("bar", "car");
        sshash.put("car", "bar");
        sshash.put("car2", "bar2");
        jedis.hmset("ssfoo", sshash);

        Set<String> sstorem = new HashSet<String>();
        sstorem.add("bar");
        sstorem.add("car2");
        assertEquals(0, jedis.hdel("ssbar", "foo").intValue());
        assertEquals(0, jedis.hdel("ssfoo", "foo").intValue());
        assertEquals(2, jedis.hdel("ssfoo", sstorem).intValue());
        assertEquals(null, jedis.hget("ssfoo", "bar"));

        // Binary Set
        Map<byte[], byte[]> bshash = new HashMap<byte[], byte[]>();
        bshash.put(bbar, bcar);
        bshash.put(bcar, bbar);
        bshash.put(bcar2, bbar);
        jedis.hmset(bsfoo, bshash);

        Set<byte[]> bstorem = new HashSet<byte[]>();
        bstorem.add(bbar);
        bstorem.add(bcar2);
        assertEquals(0, jedis.hdel(bsbar, bfoo).intValue());
        assertEquals(0, jedis.hdel(bsfoo, bfoo).intValue());
        assertEquals(2, jedis.hdel(bsfoo, bstorem).intValue());
        assertEquals(null, jedis.hget(bsfoo, bbar));

    }
	
	@Test
    public void rpush() {
		long size = jedis.rpush("foo", "bar", "foo");
        assertEquals(2, size);
        
        List<String> expected = new ArrayList<String>();
        expected.add("bar");
        expected.add("foo");
        
        List<String> values = jedis.lrange("foo",0,-1);
        assertEquals(expected, values);
        
        // Binary
        size = jedis.rpush(bfoo, bbar, bfoo);
        assertEquals(2, size);
        
        List<byte[]> bexpected = new ArrayList<byte[]>();
        bexpected.add(bbar);
        bexpected.add(bfoo);

        List<byte[]> bvalues = jedis.lrange(bfoo, 0, -1);
        assertEquals(bexpected, bvalues);
        
        // String List
        List<String> slexpected = new ArrayList<String>();
        slexpected.add("bar");
        slexpected.add("bar2");
        slexpected.add("bar3");
        long slsize = jedis.rpush("slfoo", slexpected);
        assertEquals(3, slsize);
        slsize = jedis.rpush("slfoo", "foo");
        assertEquals(4, slsize);
        
        // Binary List
        List<byte[]> blexpected = new ArrayList<byte[]>();
        blexpected.add(blbar);
        blexpected.add(blbar2);
        blexpected.add(blbar3);
        long blsize = jedis.rpush(blfoo, blexpected);
        assertEquals(3, blsize);
        blsize = jedis.rpush(blfoo, bfoo);
        assertEquals(4, blsize);

    }
	
	@Test
    public void lpush() {
		long size = jedis.lpush("foo", "bar", "foo");
        assertEquals(2, size);
        
        List<String> expected = new ArrayList<String>();
        expected.add("foo");
        expected.add("bar");
        
        List<String> values = jedis.lrange("foo",0,-1);
        assertEquals(expected, values);
        
        // Binary
        size = jedis.lpush(bfoo, bbar, bfoo);
        assertEquals(2, size);
        
        List<byte[]> bexpected = new ArrayList<byte[]>();
        bexpected.add(bfoo);
        bexpected.add(bbar);

        List<byte[]> bvalues = jedis.lrange(bfoo, 0, -1);
        assertEquals(bexpected, bvalues);
        
        // String List
        List<String> slexpected = new ArrayList<String>();
        slexpected.add("bar");
        slexpected.add("bar2");
        slexpected.add("bar3");
        long slsize = jedis.lpush("slfoo", slexpected);
        assertEquals(3, slsize);
        slsize = jedis.lpush("slfoo", "foo");
        assertEquals(4, slsize);
        
        // Binary List
        List<byte[]> blexpected = new ArrayList<byte[]>();
        blexpected.add(blbar);
        blexpected.add(blbar2);
        blexpected.add(blbar3);
        long blsize = jedis.lpush(blfoo, blexpected);
        assertEquals(3, blsize);
        blsize = jedis.lpush(blfoo, bfoo);
        assertEquals(4, blsize);
        
    }
	
	@Test
    public void sadd() {
        long status = jedis.sadd("foo", "bar", "foo1");
        assertEquals(2, status);

        status = jedis.sadd("foo", "bar", "car");
        assertEquals(1, status);

        status = jedis.sadd("foo", "bar", "foo1");
        assertEquals(0, status);

        status = jedis.sadd(bfoo, bbar, bfoo1);
        assertEquals(2, status);

        status = jedis.sadd(bfoo, bbar, bcar);
        assertEquals(1, status);

        status = jedis.sadd(bfoo, bbar, bfoo1);
        assertEquals(0, status);
        
        //String Set
        Set<String> ssexpected = new HashSet<String>();
        ssexpected.add("bar");
        ssexpected.add("bar2");
        long ssstatus = jedis.sadd("ssfoo", ssexpected);
        assertEquals(2, ssstatus);
        
        ssstatus = jedis.sadd("ssfoo", ssexpected);
        assertEquals(0, ssstatus);
        ssexpected.remove("bar2");
        ssstatus = jedis.sadd("ssfoo", ssexpected);
        assertEquals(0, ssstatus);

        // Binary Set
        Set<byte[]> bsexpected = new HashSet<byte[]>();
        bsexpected.add(bbar);
        bsexpected.add(bcar);
        long bsstatus = jedis.sadd(bsfoo, bsexpected);
        assertEquals(2, bsstatus);

        bsstatus = jedis.sadd(bsfoo, bsexpected);
        assertEquals(0, bsstatus);
        bsexpected.remove(bcar);
        bsstatus = jedis.sadd(bsfoo, bsexpected);
        assertEquals(0, bsstatus);
    }
	
    @Test
    public void srem() {
        //String
        jedis.sadd("foo", "a");
        jedis.sadd("foo", "b");
        jedis.sadd("foo", "c");

        long mstatus = jedis.srem("foo", "a", "b");

        Set<String> mexpected = new HashSet<String>();
        mexpected.add("c");

        assertEquals(2, mstatus);
        assertEquals(mexpected, jedis.smembers("foo"));

        mstatus = jedis.srem("foo", "bar");

        assertEquals(0, mstatus);

        //Binary
        jedis.sadd(bfoo, bbar);
        jedis.sadd(bfoo, bcar);
        jedis.sadd(bfoo, bcar2);

        long bstatus = jedis.srem(bfoo, bbar, bcar);

        Set<byte[]> bexpected = new HashSet<byte[]>();
        bexpected.add(bcar2);
        assertEquals(2, bstatus);
        assertEquals(bexpected, jedis.smembers(bfoo));

        bstatus = jedis.srem(bfoo, bcar3);
        assertEquals(0, bstatus);
        
        
        //String Set
        jedis.sadd("ssfoo", "a");
        jedis.sadd("ssfoo", "b");
        jedis.sadd("ssfoo", "c");

        Set<String> storem = new HashSet<String>();
        storem.add("a");
        storem.add("b");
        long sstatus = jedis.srem("ssfoo", storem);

        Set<String> sexpected = new HashSet<String>();
        sexpected.add("c");
        assertEquals(2, sstatus);
        assertEquals(sexpected, jedis.smembers("ssfoo"));

        sstatus = jedis.srem("ssfoo", "bar");
        assertEquals(0, sstatus);

        //Binary Set
        jedis.sadd(bsfoo, bbar);
        jedis.sadd(bsfoo, bcar);
        jedis.sadd(bsfoo, bcar2);

        Set<byte[]> btorem = new HashSet<byte[]>();
        btorem.add(bbar);
        btorem.add(bcar);
        long bsstatus = jedis.srem(bsfoo, btorem);

        Set<byte[]> bsexpected = new HashSet<byte[]>();
        bsexpected.add(bcar2);
        assertEquals(2, bsstatus);
        assertEquals(bsexpected, jedis.smembers(bsfoo));

        bsstatus = jedis.srem(bsfoo, bbar);
        assertEquals(0, bsstatus);

    }
	
	@Test
    public void zadd() {
	 	Map<String, Double> scoreMembers = new HashMap<String, Double>();
	 	scoreMembers.put("bar", 1d);
	 	scoreMembers.put("foo", 10d);
	 	
        long status = jedis.zadd("foo", scoreMembers);
        assertEquals(2, status);

        scoreMembers.clear();
	 	scoreMembers.put("car", 0.1d);
	 	scoreMembers.put("bar", 2d);
	 		        
        status = jedis.zadd("foo", scoreMembers);
        assertEquals(1, status);

        //Binary
        Map<byte[], Double> bscoreMembers = new HashMap<byte[], Double>();
	 	bscoreMembers.put(bbar, 1d);
	 	bscoreMembers.put(bfoo, 10d);
	 	
        status = jedis.zadd(bfoo, bscoreMembers);
        assertEquals(2, status);

        bscoreMembers.clear();
	 	bscoreMembers.put(bcar, 0.1d);
	 	bscoreMembers.put(bbar, 2d);
	 		        
        status = jedis.zadd(bfoo, bscoreMembers);
        assertEquals(1, status);
        
        //String Set
        jedis.zadd("ssfoo", 1d, "a");
        jedis.zadd("ssfoo", 2d, "b");
        jedis.zadd("ssfoo", 2d, "d");

        Set<String> sstorem = new HashSet<String>();
        sstorem.add("a");
        sstorem.add("d");
        long ssstatus = jedis.zrem("ssfoo", sstorem);

        Set<String> ssexpected = new LinkedHashSet<String>();
        ssexpected.add("b");
        assertEquals(2, ssstatus);
        assertEquals(ssexpected, jedis.zrange("ssfoo", 0, 100));

        ssstatus = jedis.zrem("ssfoo", "bar");
        assertEquals(0, ssstatus);

        // Binary Set
        jedis.zadd(bsfoo, 1d, bbar);
        jedis.zadd(bsfoo, 2d, bcar);
        jedis.zadd(bsfoo, 2d, bcar2);

        Set<byte[]> bstorem = new HashSet<byte[]>();
        bstorem.add(bbar);
        bstorem.add(bcar2);
        long bsstatus = jedis.zrem(bsfoo, bstorem);

        Set<byte[]> bsexpected = new LinkedHashSet<byte[]>();
        bsexpected.add(bcar);
        assertEquals(2, bsstatus);
        assertEquals(bsexpected, jedis.zrange(bsfoo, 0, 100));

        bsstatus = jedis.zrem(bsfoo, bcar3);
        assertEquals(0, bsstatus);
    }

    @Test
    public void zrem() {
        jedis.zadd("foo", 1d, "bar");
        jedis.zadd("foo", 2d, "car");
        jedis.zadd("foo", 3d, "foo1");

        long status = jedis.zrem("foo", "bar", "car");

        Set<String> expected = new LinkedHashSet<String>();
        expected.add("foo1");

        assertEquals(2, status);
        assertEquals(expected, jedis.zrange("foo", 0, 100));

        status = jedis.zrem("foo", "bar", "car");
        assertEquals(0, status);
        
        status = jedis.zrem("foo", "bar", "foo1");
        assertEquals(1, status);

        //Binary
        jedis.zadd(bfoo, 1d, bbar);
        jedis.zadd(bfoo, 2d, bcar);
        jedis.zadd(bfoo, 3d, bfoo1);

        status = jedis.zrem(bfoo, bbar, bcar);

        Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
        bexpected.add(bfoo);

        assertEquals(2, status);
        assertEquals(bexpected, jedis.zrange(bfoo, 0, 100));

        status = jedis.zrem(bfoo, bbar, bcar);
        assertEquals(0, status);
        
        status = jedis.zrem(bfoo, bbar, bfoo1);
        assertEquals(1, status);
        
        //String Set
        jedis.zadd("ssfoo", 1d, "a");
        jedis.zadd("ssfoo", 2d, "b");
        jedis.zadd("ssfoo", 2d, "d");

        Set<String> sstorem = new HashSet<String>();
        sstorem.add("a");
        sstorem.add("d");
        long ssstatus = jedis.zrem("ssfoo", sstorem);

        Set<String> ssexpected = new LinkedHashSet<String>();
        ssexpected.add("b");
        assertEquals(2, ssstatus);
        assertEquals(ssexpected, jedis.zrange("ssfoo", 0, 100));

        ssstatus = jedis.zrem("ssfoo", "bar");
        assertEquals(0, ssstatus);

        //Binary Set
        jedis.zadd(bsfoo, 1d, bbar);
        jedis.zadd(bsfoo, 2d, bcar);
        jedis.zadd(bsfoo, 2d, bcar2);

        Set<byte[]> bstorem = new HashSet<byte[]>();
        bstorem.add(bbar);
        bstorem.add(bcar2);
        long bsstatus = jedis.zrem(bsfoo, bstorem);

        Set<byte[]> bsexpected = new LinkedHashSet<byte[]>();
        bsexpected.add(bcar);
        assertEquals(2, bsstatus);
        assertEquals(bsexpected, jedis.zrange(bsfoo, 0, 100));

        bsstatus = jedis.zrem(bsfoo, bbar);
        assertEquals(0, bsstatus);

    }   
}