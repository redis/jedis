/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package redis.clients.jedis.tests;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import redis.clients.jedis.ClusterInvocationHandler;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.JedisPool;
import redis.clients.util.SafeEncoder;

/**
 *
 * @author causton
 */
public class ClusterProxyTest extends JedisClusterTest {

    @Override
    JedisCommands getStringInterface(Set<HostAndPort> nodes) {
        return ClusterInvocationHandler.getProxy(nodes);
    }

    @Override
    void shutdown(JedisCommands jc) {
        ClusterInvocationHandler.shutdown((ClusterInvocationHandler.BinaryAndStringCommands) jc);
    }

    @Override
    Map<String, JedisPool> getClusterNodes(JedisCommands jc) {
        return ClusterInvocationHandler.getClusterNodes((ClusterInvocationHandler.BinaryAndStringCommands) jc);
    }

    @Test
    public void testABinaryCommand() {
        Set<HostAndPort> nodes = new HashSet<HostAndPort>();
        nodes.add(new HostAndPort("127.0.0.1", 7379));
        ClusterInvocationHandler.BinaryAndStringCommands cluster = ClusterInvocationHandler.getProxy(nodes);

        byte[] keyBytes = SafeEncoder.encode("key-for-binary-test");
        byte[] valBytes = SafeEncoder.encode("val-for-binary-test");
        String response = cluster.set(keyBytes, valBytes);
        assertEquals("OK", response);

        byte[] newBytes = cluster.get(keyBytes);

        //Check that array references are different
        assertFalse(newBytes == valBytes);

        assertTrue("Check returned value bytes are same as set bytes", Arrays.equals(valBytes, newBytes));
    }
}
