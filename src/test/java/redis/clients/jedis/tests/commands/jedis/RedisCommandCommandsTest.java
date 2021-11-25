package redis.clients.jedis.tests.commands.jedis;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RedisCommandCommandsTest extends JedisCommandsTestBase {

    @Test
    public void testCommand(){
//        List<String> response = jedis.command();
//        assertTrue(response.size() > 100);
//        assertTrue(response.contains("set"));
//        assertTrue(response.contains("get"));
    }

    @Test
    public void testCommandCount(){
        long response = jedis.commandCount();
        assertTrue(response > 100);
    }

    @Test
    public void testCommandGetKeys(){
        List<String> response = jedis.commandGetKeys("SORT", "mylist", "ALPHA", "STORE", "outlist");
        List<String> exe = new ArrayList<String>();
        exe.add("mylist");
        exe.add("outlist");

        assertEquals(exe, response);
    }
}
