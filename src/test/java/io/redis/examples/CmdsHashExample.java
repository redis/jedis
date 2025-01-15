// EXAMPLE: cmds_hash
// REMOVE_START
package io.redis.examples;

import org.junit.Assert;
import org.junit.Test;
// REMOVE_END

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Collections;

// HIDE_START
import redis.clients.jedis.UnifiedJedis;
// HIDE_END

import static java.util.stream.Collectors.toList;

// HIDE_START
public class CmdsHashExample {

    @Test
    public void run() {
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");

        //REMOVE_START
        // Clear any keys here before using them in tests.
        jedis.del("myhash");
        //REMOVE_END
// HIDE_END

        // STEP_START hget
        Map<String, String> hGetExampleParams = new HashMap<>();
        hGetExampleParams.put("field1", "foo");

        long hGetResult1 = jedis.hset("myhash", hGetExampleParams);
        System.out.println(hGetResult1);    // >>> 1

        String hGetResult2 = jedis.hget("myhash", "field1");
        System.out.println(hGetResult2);    // >>> foo

        String hGetResult3 = jedis.hget("myhash", "field2");
        System.out.println(hGetResult3);    // >>> null
        // STEP_END
        // REMOVE_START
        // Tests for 'hget' step.
        Assert.assertEquals(1, hGetResult1);
        Assert.assertEquals("foo", hGetResult2);
        Assert.assertNull(hGetResult3);
        jedis.del("myhash");
        // REMOVE_END

        // STEP_START hgetall
        Map<String, String> hGetAllExampleParams = new HashMap<>();
        hGetAllExampleParams.put("field1", "Hello");
        hGetAllExampleParams.put("field2", "World");

        long hGetAllResult1 = jedis.hset("myhash", hGetAllExampleParams);
        System.out.println(hGetAllResult1); // >>> 2

        Map<String, String> hGetAllResult2 = jedis.hgetAll("myhash");
        System.out.println(
            hGetAllResult2.entrySet().stream()
                    .sorted((s1, s2)-> s1.getKey().compareTo(s2.getKey()))
                    .collect(toList())
                    .toString()
        );
        // >>> [field1=Hello, field2=World]
        // STEP_END
        // REMOVE_START
        // Tests for 'hgetall' step.
        Assert.assertEquals(2, hGetAllResult1);
        Assert.assertEquals("[field1=Hello, field2=World]",
            hGetAllResult2.entrySet().stream()
                    .sorted((s1, s2)-> s1.getKey().compareTo(s2.getKey()))
                    .collect(toList())
                    .toString()
        );
        jedis.del("myhash");
        // REMOVE_END

        // STEP_START hset
        Map<String, String> hSetExampleParams = new HashMap<>();
        hSetExampleParams.put("field1", "Hello");
        long hSetResult1 = jedis.hset("myhash", hSetExampleParams);
        System.out.println(hSetResult1);    // >>> 1

        String hSetResult2 = jedis.hget("myhash", "field1");
        System.out.println(hSetResult2);    // >>> Hello

        hSetExampleParams.clear();
        hSetExampleParams.put("field2", "Hi");
        hSetExampleParams.put("field3", "World");
        long hSetResult3 = jedis.hset("myhash",hSetExampleParams);
        System.out.println(hSetResult3);    // >>> 2

        String hSetResult4 = jedis.hget("myhash", "field2");
        System.out.println(hSetResult4);    // >>> Hi

        String hSetResult5 = jedis.hget("myhash", "field3");
        System.out.println(hSetResult5);    // >>> World

        Map<String, String> hSetResult6 = jedis.hgetAll("myhash");
        
        for (String key: hSetResult6.keySet()) {
            System.out.println("Key: " + key + ", Value: " + hSetResult6.get(key));
        }
        // >>> Key: field3, Value: World
        // >>> Key: field2, Value: Hi
        // >>> Key: field1, Value: Hello
        // STEP_END
        // REMOVE_START
        // Tests for 'hset' step.
        Assert.assertEquals(1, hSetResult1);
        Assert.assertEquals("Hello", hSetResult2);
        Assert.assertEquals(2, hSetResult3);
        Assert.assertEquals("Hi", hSetResult4);
        Assert.assertEquals("World", hSetResult5);
        Assert.assertEquals(3, hSetResult6.size());
        Assert.assertEquals("Hello", hSetResult6.get("field1"));
        Assert.assertEquals("Hi", hSetResult6.get("field2"));
        Assert.assertEquals("World", hSetResult6.get("field3"));
        jedis.del("myhash");
        // REMOVE_END

        // STEP_START hvals
        Map<String, String> hValsExampleParams = new HashMap<>();
        hValsExampleParams.put("field1", "Hello");
        hValsExampleParams.put("field2", "World");

        long hValsResult1 = jedis.hset("myhash", hValsExampleParams);
        System.out.println(hValsResult1); // >>> 2

        List<String> hValsResult2 = jedis.hvals("myhash");
        Collections.sort(hValsResult2);
        System.out.println(hValsResult2);
        // >>> [Hello, World]
        // STEP_END
        // REMOVE_START       
        // Tests for 'hvals' step.
        Assert.assertEquals(2, hValsResult1);
        Assert.assertEquals("[Hello, World]", hValsResult2.toString());
        jedis.del("myhash");
        // REMOVE_END

// HIDE_START
        jedis.close();
    }
}
// HIDE_END

