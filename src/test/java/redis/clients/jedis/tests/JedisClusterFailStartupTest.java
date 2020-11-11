package redis.clients.jedis.tests;

import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.exceptions.JedisNoReachableClusterNodeException;

public class JedisClusterFailStartupTest {
    
    @Test(expected=JedisNoReachableClusterNodeException.class)
    public void test() {
        new JedisCluster(new HostAndPort("localhost", 16093));
    }

}
