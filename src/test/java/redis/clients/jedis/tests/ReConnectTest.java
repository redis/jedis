import java.io.File;

import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisException;
import redis.clients.jedis.JedisPool;

import junit.framework.Assert;

public class ReConnectTest extends Assert
{
    // It except the redis-server binary file to be present at a location /opt/redis so that it can be started 
    @Test
    public void checkReconnection() throws Exception
    {
        JedisPool pool = new JedisPool("localhost", 6379);
        pool.setDefaultPoolWait(2000);
        pool.setResourcesNumber(5);
        pool.init();
        Runtime.getRuntime().exec("./redis-server", null, new File("/opt/redis/"));
        Jedis client = pool.getResource();
        String key = System.currentTimeMillis() + "";
        String value = System.currentTimeMillis() + "";
        // Do Some Operations
        assertTrue(client.exists(key).intValue() == 0);
        client.set(key, value);
        assertEquals(client.get(key), value);
        Runtime.getRuntime().exec("killall -9 redis-server");
        
        Thread.sleep(1000);
        // Wait for the exec process to finish 
        try
        {
            assertEquals(client.get(key), value);
            assertFalse(true); // Should not reach here !!
        }
        catch (JedisException e)
        {
            // This will fail since the server is down now !!
        }
        // start again
        Runtime.getRuntime().exec("./redis-server", null, new File("/opt/redis/"));
        // Wait for the exec process to finish
        Thread.sleep(1000);
        try
        {
            assertTrue(client.get(key).equals(value));
            assertFalse(true); // Should not reach here !!
        }
        catch (Exception e)
        {
            // This operation will fail !! As the first operation after
            // server-restart tries to rebuild the connection with server
        }
        key = System.currentTimeMillis() + "";
        value = System.currentTimeMillis() + "";
        client.set(key, value);
        assertTrue(client.exists(key).intValue() == 1);
        Runtime.getRuntime().exec("killall -9 redis-server");
        System.out.println("Finshed Successfully ");
        pool.returnBrokenResource(client);

    }
}
