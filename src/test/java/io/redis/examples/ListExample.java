// EXAMPLE: list_tutorial
// HIDE_START
package io.redis.examples;

import org.junit.Test;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.args.ListDirection;
import java.util.List;
import static org.junit.Assert.*;

public class ListExample {
    @Test
    public void run() {
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");

        // HIDE_END
        // REMOVE_START
        jedis.del("bikes:repairs");
        jedis.del("bikes:finished");
        // REMOVE_END

        // STEP_START queue
        long res1 = jedis.lpush("bikes:repairs", "bike:1");
        System.out.println(res1);  // >>> 1

        long res2 = jedis.lpush("bikes:repairs", "bike:2");
        System.out.println(res2);  // >>> 2

        String res3 = jedis.rpop("bikes:repairs");
        System.out.println(res3);  // >>> bike:1

        String res4 = jedis.rpop("bikes:repairs");
        System.out.println(res4); // >>> bike:2
        // STEP_END

        // REMOVE_START
        assertEquals(1, res1);
        assertEquals(2, res2);
        assertEquals("bike:1", res3);
        assertEquals("bike:2", res4);
        // REMOVE_END

        // STEP_START stack
        long res5 = jedis.lpush("bikes:repairs", "bike:1");
        System.out.println(res5);  // >>> 1

        long res6 = jedis.lpush("bikes:repairs", "bike:2");
        System.out.println(res6);  // >>> 2

        String res7 = jedis.lpop("bikes:repairs");
        System.out.println(res7);  // >>> bike:2

        String res8 = jedis.lpop("bikes:repairs");
        System.out.println(res8);  // >>> bike:1
        // STEP_END

        // REMOVE_START
        assertEquals(1, res5);
        assertEquals(2, res6);
        assertEquals("bike:2", res7);
        assertEquals("bike:1", res8);
        // REMOVE_END

        // STEP_START llen
        long res9 = jedis.llen("bikes:repairs");
        System.out.println(res9);  // >>> 0
        // STEP_END

        // REMOVE_START
        assertEquals(0, res9);
        // REMOVE_END

        // STEP_START lmove_lrange
        long res10 = jedis.lpush("bikes:repairs", "bike:1");
        System.out.println(res10);  // >>> 1

        long res11 = jedis.lpush("bikes:repairs", "bike:2");
        System.out.println(res11);  // >>> 2

        String res12 = jedis.lmove("bikes:repairs", "bikes:finished", ListDirection.LEFT, ListDirection.LEFT);
        System.out.println(res12);  // >>> bike:2

        List<String> res13 = jedis.lrange("bikes:repairs", 0, -1);
        System.out.println(res13);  // >>> [bike:1]

        List<String> res14 = jedis.lrange("bikes:finished", 0, -1);
        System.out.println(res14);  // >>> [bike:2]
        // STEP_END

        // REMOVE_START
        assertEquals(1, res10);
        assertEquals(2, res11);
        assertEquals("bike:2", res12);
        assertEquals("[bike:1]", res13.toString());
        assertEquals("[bike:2]", res14.toString());
        jedis.del("bikes:repairs");
        // REMOVE_END

        // STEP_START lpush_rpush
        long res15 = jedis.rpush("bikes:repairs", "bike:1");
        System.out.println(res15);  // >>> 1

        long res16 = jedis.rpush("bikes:repairs", "bike:2");
        System.out.println(res16);  // >>> 2

        long res17 = jedis.lpush("bikes:repairs", "bike:important_bike");
        System.out.println(res17);  // >>> 3

        List<String> res18 = jedis.lrange("bikes:repairs", 0, -1);
        System.out.println(res18);  // >>> [bike:important_bike, bike:1, bike:2]
        // STEP_END

        // REMOVE_START
        assertEquals(1, res15);
        assertEquals(2, res16);
        assertEquals(3, res17);
        assertEquals("[bike:important_bike, bike:1, bike:2]", res18.toString());
        jedis.del("bikes:repairs");
        // REMOVE_END

        // STEP_START variadic
        long res19 = jedis.rpush("bikes:repairs", "bike:1", "bike:2", "bike:3");
        System.out.println(res19);  // >>> 3

        long res20 = jedis.lpush("bikes:repairs", "bike:important_bike", "bike:very_important_bike");
        System.out.println(res20);  // >>> 5

        List<String> res21 = jedis.lrange("bikes:repairs", 0, -1);
        System.out.println(res21);  // >>> [bike:very_important_bike, bike:important_bike, bike:1, bike:2, bike:3]
        // STEP_END

        // REMOVE_START
        assertEquals(3, res19);
        assertEquals(5, res20);
        assertEquals("[bike:very_important_bike, bike:important_bike, bike:1, bike:2, bike:3]",res21.toString());
        jedis.del("bikes:repairs");
        // REMOVE_END

        // STEP_START lpop_rpop
        long res22 = jedis.rpush("bikes:repairs", "bike:1", "bike:2", "bike:3");
        System.out.println(res22);  // >>> 3

        String res23 = jedis.rpop("bikes:repairs");
        System.out.println(res23);  // >>> bike:3

        String res24 = jedis.lpop("bikes:repairs");
        System.out.println(res24);  // >>> bike:1

        String res25 = jedis.rpop("bikes:repairs");
        System.out.println(res25);  // >>> bike:2

        String res26 = jedis.rpop("bikes:repairs");
        System.out.println(res26);  // >>> null
        // STEP_END

        // REMOVE_START
        assertEquals(3, res22);
        assertEquals("bike:3", res23);
        assertEquals("bike:1", res24);
        assertEquals("bike:2", res25);
        assertNull(res26);
        // REMOVE_END

        // STEP_START ltrim
        long res27 = jedis.lpush("bikes:repairs", "bike:1", "bike:2", "bike:3", "bike:4", "bike:5");
        System.out.println(res27);  // >>> 5

        String res28 = jedis.ltrim("bikes:repairs", 0, 2);
        System.out.println(res28);  // >>> OK

        List<String> res29 = jedis.lrange("bikes:repairs", 0, -1);
        System.out.println(res29);  // >>> [bike:5, bike:4, bike:3]
        // STEP_END

        // REMOVE_START
        assertEquals(5, res27);
        assertEquals("OK", res28);
        assertEquals("[bike:5, bike:4, bike:3]", res29.toString());
        jedis.del("bikes:repairs");
        // REMOVE_END

        // STEP_START ltrim_end_of_list
        res27 = jedis.rpush("bikes:repairs", "bike:1", "bike:2", "bike:3", "bike:4", "bike:5");
        System.out.println(res27);  // >>> 5

        res28 = jedis.ltrim("bikes:repairs", -3, -1);
        System.out.println(res2);  // >>> OK

        res29 = jedis.lrange("bikes:repairs", 0, -1);
        System.out.println(res29);  // >>> [bike:3, bike:4, bike:5]
        // STEP_END

        // REMOVE_START
        assertEquals(5, res27);
        assertEquals("OK", res28);
        assertEquals("[bike:3, bike:4, bike:5]", res29.toString());
        jedis.del("bikes:repairs");
        // REMOVE_END

        // STEP_START brpop
        long res31 = jedis.rpush("bikes:repairs", "bike:1", "bike:2");
        System.out.println(res31);  // >>> 2

        List<String> res32 = jedis.brpop(1, "bikes:repairs");
        System.out.println(res32);  // >>> (bikes:repairs, bike:2)

        List<String>  res33 = jedis.brpop(1,"bikes:repairs");
        System.out.println(res33);  // >>> (bikes:repairs, bike:1)

        List<String>  res34 = jedis.brpop(1,"bikes:repairs");
        System.out.println(res34);  // >>> null
        // STEP_END

        // REMOVE_START
        assertEquals(2, res31);
        assertEquals("[bikes:repairs, bike:2]", res32.toString());
        assertEquals( "[bikes:repairs, bike:1]", res33.toString());
        assertNull(res34);
        jedis.del("bikes:repairs");
        jedis.del("new_bikes");
        // REMOVE_END

        // STEP_START rule_1
        long res35 = jedis.del("new_bikes");
        System.out.println(res35);  // >>> 0

        long res36 = jedis.lpush("new_bikes", "bike:1", "bike:2", "bike:3");
        System.out.println(res36);  // >>> 3
        // STEP_END

        // REMOVE_START
        assertEquals(0, res35);
        assertEquals(3, res36);
        jedis.del("new_bikes");
        // REMOVE_END

        // STEP_START rule_1.1
        String res37 = jedis.set("new_bikes", "bike:1");
        System.out.println(res37);  // >>> OK

        String res38 = jedis.type("new_bikes");
        System.out.println(res38);  // >>> string

        try {
            long res39  = jedis.lpush("new_bikes", "bike:2", "bike:3");
        } catch (Exception e) {
            e.printStackTrace();
            // >>> redis.clients.jedis.exceptions.JedisDataException:
            // >>> WRONGTYPE Operation against a key holding the wrong kind of value
        }
        // STEP_END

        // REMOVE_START
        assertEquals("OK",res37);
        assertEquals("string",res38);
        jedis.del("new_bikes");
        // REMOVE_END

        // STEP_START rule_2
        jedis.lpush("bikes:repairs", "bike:1", "bike:2", "bike:3");
        System.out.println(res36);  // >>> 3

        boolean res40 = jedis.exists("bikes:repairs");
        System.out.println(res40);  // >>> true

        String res41 = jedis.lpop("bikes:repairs");
        System.out.println(res41);  // >>> bike:3

        String res42 = jedis.lpop("bikes:repairs");
        System.out.println(res42);  // >>> bike:2

        String res43 = jedis.lpop("bikes:repairs");
        System.out.println(res43);  // >>> bike:1

        boolean res44 = jedis.exists("bikes:repairs");
        System.out.println(res44);  // >>> false
        // STEP_END

        // REMOVE_START
        assertTrue(res40);
        assertEquals(res41, "bike:3");
        assertEquals(res42, "bike:2");
        assertEquals(res43, "bike:1");
        assertFalse(res44);
        // REMOVE_END

        // STEP_START rule_3
        long res45 = jedis.del("bikes:repairs");
        System.out.println(res45);  // >>> 0

        long res46 = jedis.llen("bikes:repairs");
        System.out.println(res46);  // >>> 0

        String res47 = jedis.lpop("bikes:repairs");
        System.out.println(res47);  // >>> null
        // STEP_END

        // REMOVE_START
        assertEquals(0, res45);
        assertEquals(0, res46);
        assertNull(res47);
        // REMOVE_END

        // STEP_START ltrim.1
        long res48 = jedis.lpush("bikes:repairs", "bike:1", "bike:2", "bike:3", "bike:4", "bike:5");
        System.out.println(res48);  // >>> 5

        String res49 = jedis.ltrim("bikes:repairs", 0, 2);
        System.out.println(res49);  // >>> OK

        List<String> res50 = jedis.lrange("bikes:repairs", 0, -1);
        System.out.println(res50);  // >>> [bike:5, bike:4, bike:3]
        // STEP_END

        // REMOVE_START
        assertEquals(5, res48);
        assertEquals("OK", res49);
        assertEquals("[bike:5, bike:4, bike:3]", res50.toString());
        jedis.del("bikes:repairs");
        // REMOVE_END

    }
}
