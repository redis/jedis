package redis.clients.jedis.tests.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.exceptions.JedisClusterDifferentConnectionsException;
import redis.clients.jedis.tests.JedisTestBase;

public class ClusterScriptingCommandsTest extends JedisTestBase{
    private final Set<HostAndPort> jedisClusterNodes = createClusterNodesSet();
    private final JedisCluster jedisCluster = new JedisCluster(jedisClusterNodes);

    @SuppressWarnings("unchecked")
    @Test(expected = JedisClusterDifferentConnectionsException.class)
    public void testDifferentConnectionsException() {
        String script = "return {KEYS[1],KEYS[2],ARGV[1],ARGV[2],ARGV[3]}";
        List<String> keys = new ArrayList<String>();
        keys.add("key1");
        keys.add("key2");
        List<String> args = new ArrayList<String>();
        args.add("first");
        args.add("second");
        args.add("third");
        jedisCluster.eval(script, keys, args);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testEval2() {
        String script = "return redis.call('set',KEYS[1],'bar')";
        int numKeys = 1;
        String[] args = {"foo"};
        jedisCluster.eval(script, numKeys, args);
        assertEquals(jedisCluster.get("foo"), "bar");
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testScriptLoadAndScriptExists() { 
        String sha1 = jedisCluster.scriptLoad("return redis.call('get','foo')");
        assertTrue(jedisCluster.scriptExists(sha1));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testEvalsha() {
        String sha1 = jedisCluster.scriptLoad("return 10");
        Object o = jedisCluster.evalsha(sha1);
        assertEquals("10", o.toString());
    }
    
    private static Set<HostAndPort> createClusterNodesSet() {
        Set<HostAndPort> jedisClusterNodes = new HashSet<HostAndPort>();
        jedisClusterNodes.add(new HostAndPort("127.0.0.1", 7000));
        return Collections.unmodifiableSet(jedisClusterNodes);
    }
}
