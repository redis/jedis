package redis.clients.jedis.tests;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.tree.ExpandVetoException;

import org.junit.Test;
import org.junit.Assert;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.tests.commands.JedisCommandTestBase;
import redis.clients.util.RedisOutputStream;

public class JedisTest extends JedisCommandTestBase {
    @Test
    public void useWithoutConnecting() {
	Jedis jedis = new Jedis("localhost");
	jedis.auth("foobared");
	jedis.dbSize();
    }

    @Test
    public void checkBinaryData() {
	byte[] bigdata = new byte[1777];
	for (int b = 0; b < bigdata.length; b++) {
	    bigdata[b] = (byte) ((byte) b % 255);
	}
	Map<String, String> hash = new HashMap<String, String>();
	hash.put("data", new String(bigdata, Protocol.UTF8));

	String status = jedis.hmset("foo", hash);
	assertEquals("OK", status);
	assertEquals(hash, jedis.hgetAll("foo"));
    }

    @Test
    public void connectWithShardInfo() {
	JedisShardInfo shardInfo = new JedisShardInfo("localhost",
		Protocol.DEFAULT_PORT);
	shardInfo.setPassword("foobared");
	Jedis jedis = new Jedis(shardInfo);
	jedis.get("foo");
    }
    
    @SuppressWarnings("rawtypes")
	public static boolean isListAreEquals(Collection expected, Collection result) {
    	if(expected.size() != result.size()) {
    		return false;
    	}
    	
    	final Iterator expectedit = expected.iterator();
    	while(expectedit.hasNext()) {
    		final Object exp = expectedit.next();
        	final Iterator responseit = result.iterator();
        	boolean found = false;
        	while(responseit.hasNext() && !found) {
	    		final Object resp = responseit.next();
	    		if(exp instanceof byte[] && resp instanceof byte[]) {
	    			final byte[] bexp = (byte[]) exp;
	    			final byte[] bresp = (byte[]) resp;
	    			if(Arrays.equals(bexp, bresp)) {
	    				found = true;
	    			}
	    		} else if (exp instanceof Collection && resp instanceof Collection) {
	    			final Collection subexp = (Collection) exp;
	    			final Collection subresp = (Collection) resp;
	    			if(isListAreEquals(subexp, subresp)) {
	    				found = true;
	    			}
	    		} else {
	    			if (null != exp) {
		    			if (exp.equals(resp)){
		    				found = true;
		    			}
	    			} else {
	    				if(resp == null) {
	    					found = true;
	    				}
	    			}
	    		}
        	}
        	if(!found){
        		fail("Result doesn't contain " + (null != exp ? exp.toString() : null));
        	}
    	}
    	
    	return true;
    }

//    public static boolean isArraysAreEqualsPlop(final byte[] expected, final byte[] result) {
//    	if(expected.length != result.length) {
//    		return false;
//    	}
//    	
//    	for(int i=0; i < expected.length; i++) {
//    		if(expected[i] != result[i]) {
//    			return false;
//    		}
//    	}
//    	
//    	return true;
//    }
}
