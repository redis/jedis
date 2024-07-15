// EXAMPLE: json_tutorial
// REMOVE_START
package io.redis.examples;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

// REMOVE_END
// HIDE_START
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.json.Path2;

import org.json.JSONArray;
import org.json.JSONObject;
import redis.clients.jedis.params.SetParams;
// HIDE_END

// HIDE_START
public class Json_tutorial {
    @Test
    public void run() {
        try (UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379")) {

        //REMOVE_START
        // Clear any keys here before using them in tests.
        jedis.del("bike", "crashes", "newbike", "riders");
        //REMOVE_END
// HIDE_END


        // STEP_START set_get
        String res1 = jedis.jsonSet("bike", new Path2("$"), "\"Hyperion\"");
        System.out.println(res1);   // >>> OK

        Object res2 = jedis.jsonGet("bike", new Path2("$"));
        System.out.println(res2);   // >>> ["Hyperion"]

        String res3 = jedis.jsonType("bike", new Path2("$")).toString();
        System.out.println(res3);   // >>> [class java.lang.String]
        // STEP_END

        // Tests for 'set_get' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START str
        List<Long> res4 = jedis.jsonStrLen("bike", new Path2("$"));
        System.out.println(res4);   // >>> [8]

        List<Long> res5 = jedis.jsonStrAppend("bike", new Path2("$"), " (Enduro bikes)");
        System.out.println(res5);   // >>> [25]

        Object res6 = jedis.jsonGet("bike", new Path2("$"));
        System.out.println(res6);   // >>> ["Hyperion (Enduro bikes)"]
        // STEP_END

        // Tests for 'str' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START num
        String res7 = jedis.jsonSet("crashes", new Path2("$"), 0);
        System.out.println(res7);   // >>> OK

        Object res8 = jedis.jsonNumIncrBy("crashes", new Path2("$"), 1);
        System.out.println(res8);   // >>> [1]

        Object res9 = jedis.jsonNumIncrBy("crashes", new Path2("$"), 1.5);
        System.out.println(res9);   // >>> [2.5]

        Object res10 = jedis.jsonNumIncrBy("crashes", new Path2("$"), -0.75);
        System.out.println(res10);   // >>> [1.75]
        // STEP_END

        // Tests for 'num' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START arr
        
        // STEP_END

        // Tests for 'arr' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START arr2

        // STEP_END

        // Tests for 'arr2' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START obj

        // STEP_END

        // Tests for 'obj' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START set_bikes

        // STEP_END

        // Tests for 'set_bikes' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START get_bikes

        // STEP_END

        // Tests for 'get_bikes' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START get_mtnbikes

        // STEP_END

        // Tests for 'get_mtnbikes' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START get_models

        // STEP_END

        // Tests for 'get_models' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START get2mtnbikes

        // STEP_END

        // Tests for 'get2mtnbikes' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START filter1

        // STEP_END

        // Tests for 'filter1' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START filter2

        // STEP_END

        // Tests for 'filter2' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START filter3

        // STEP_END

        // Tests for 'filter3' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START filter4

        // STEP_END

        // Tests for 'filter4' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START update_bikes

        // STEP_END

        // Tests for 'update_bikes' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START update_filters1

        // STEP_END

        // Tests for 'update_filters1' step.
        // REMOVE_START

        // REMOVE_END


        // STEP_START update_filters2

        // STEP_END

        // Tests for 'update_filters2' step.
        // REMOVE_START

        // REMOVE_END


// HIDE_START
        }
    }
}
// HIDE_END

