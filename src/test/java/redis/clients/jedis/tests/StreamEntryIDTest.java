package redis.clients.jedis.tests;

import org.junit.Test;
import redis.clients.jedis.StreamEntryID;

/**
 * @description: TODO
 * @author: wangmiaomiao
 * @create: 2021-11-25 16:42
 **/

public class StreamEntryIDTest {
    @Test
    public void test1(){
        String id = "";
        new StreamEntryID("");
    }

    @Test
    public void test2(){
        String id = "-";
        new StreamEntryID("");
    }
    @Test
    public void test3(){
        String id = "1604650259342-0";
        new StreamEntryID("");
    }
    @Test
    public void test4(){
        String id = "a1604650259342-0";
        new StreamEntryID("");
    }
}
