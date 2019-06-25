package redis.clients.jedis.tests.collections;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.util.JedisByteHashMap;

public class JedisByteHashMapTest {
    private static JedisByteHashMap map;

    private byte[] key = "key".getBytes();
    private byte[] value = "value".getBytes();

    @BeforeClass
    public static void beforeClass() throws Exception {
        map = new JedisByteHashMap();
    }

    @Test
    public void normalTest() {
        map.put(key, value);

        Assert.assertArrayEquals("value".getBytes(), map.get(key));
        Assert.assertTrue(map.containsKey(key));
        Assert.assertTrue(map.containsValue(value));
        Assert.assertFalse(map.isEmpty());
        Assert.assertEquals(1, map.size());

        map.remove(key);
        Assert.assertEquals(0, map.size());
    }

    @Test
    public void serialize() throws Exception {
        map.put(key, value);

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream objOut = new ObjectOutputStream(byteOut);
        objOut.writeObject(map);

        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        ObjectInputStream objIn = new ObjectInputStream(byteIn);

        JedisByteHashMap mapRead = (JedisByteHashMap)objIn.readObject();

        Assert.assertArrayEquals(map.keySet().toArray(), mapRead.keySet().toArray());
        Assert.assertArrayEquals(map.values().toArray(), mapRead.values().toArray());
    }
}
