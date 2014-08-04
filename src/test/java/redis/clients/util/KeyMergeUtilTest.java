package redis.clients.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class KeyMergeUtilTest {

    @Test
    public void testMergeBinaryKeys() throws Exception {
	byte[] key = SafeEncoder.encode("hello");
	byte[][] keys = new byte[2][];
	keys[0] = SafeEncoder.encode("world");
	keys[1] = SafeEncoder.encode("jedis");

	byte[][] mergedKeys = KeyMergeUtil.merge(key, keys);
	assertNotNull(mergedKeys);
	assertEquals(3, mergedKeys.length);
	assertEquals(key, mergedKeys[0]);
	assertEquals(keys[0], mergedKeys[1]);
	assertEquals(keys[1], mergedKeys[2]);
    }

    @Test
    public void testMergeStringKeys() throws Exception {
	String key = "hello";
	String[] keys = new String[2];
	keys[0] = "world";
	keys[1] = "jedis";

	String[] mergedKeys = KeyMergeUtil.merge(key, keys);
	assertNotNull(mergedKeys);
	assertEquals(3, mergedKeys.length);
	assertEquals(key, mergedKeys[0]);
	assertEquals(keys[0], mergedKeys[1]);
	assertEquals(keys[1], mergedKeys[2]);
    }
}