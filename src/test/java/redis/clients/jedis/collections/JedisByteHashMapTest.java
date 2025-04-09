package redis.clients.jedis.collections;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.util.JedisByteHashMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JedisByteHashMapTest {
  private static JedisByteHashMap map = new JedisByteHashMap();

  private byte[][] keys = { { 'k', 'e', 'y', '1' }, { 'k', 'e', 'y', '2' }, { 'k', 'e', 'y', '3' } };
  private byte[][] vals = { { 'v', 'a', 'l', '1' }, { 'v', 'a', 'l', '2' }, { 'v', 'a', 'l', '3' } };

  @BeforeEach
  public void before() throws Exception {
    map.clear();
  }

  private boolean arrayContainsKey(byte[][] arr, byte[] key) {
    for (byte[] anArr : arr) {
      if (Arrays.equals(anArr, key)) {
        return true;
      }
    }
    return false;
  }

  private boolean entryContainsKV(Set<Map.Entry<byte[], byte[]>> s, byte[] key, byte[] value) {
    for (Map.Entry<byte[], byte[]> en : s) {
      if (Arrays.equals(en.getKey(), key) && Arrays.equals(en.getValue(), value)) {
        return true;
      }
    }
    return false;
  }

  private boolean entrySetSame(Set<Map.Entry<byte[], byte[]>> s1, Set<Map.Entry<byte[], byte[]>> s2) {
    for (Map.Entry<byte[], byte[]> en1 : s1) {
      if (!entryContainsKV(s2, en1.getKey(), en1.getValue())) {
        return false;
      }
    }
    for (Map.Entry<byte[], byte[]> en2 : s2) {
      if (!entryContainsKV(s1, en2.getKey(), en2.getValue())) {
        return false;
      }
    }

    return true;
  }

  @Test
  public void mapOperations() {
    // put
    map.put(keys[0], vals[0]);
    assertEquals(1, map.size());

    // putAll
    Map<byte[], byte[]> kvMap = new HashMap<>();
    kvMap.put(keys[1], vals[1]);
    kvMap.put(keys[2], vals[2]);
    map.putAll(kvMap);
    assertEquals(3, map.size());

    // containsKey
    assertTrue(map.containsKey(keys[0]));

    // containsValue
    assertTrue(map.containsValue(vals[0]));

    // entrySet
    Set<Entry<byte[], byte[]>> entries = map.entrySet();
    assertEquals(3, entries.size());
    for (Entry<byte[], byte[]> entry : entries) {
      assertTrue(arrayContainsKey(keys, entry.getKey()));
      assertTrue(arrayContainsKey(vals, entry.getValue()));
    }

    // get
    assertArrayEquals(vals[0], map.get(keys[0]));

    // isEmpty
    assertFalse(map.isEmpty());

    // keySet
    for (byte[] key : map.keySet()) {
      assertTrue(arrayContainsKey(keys, key));
    }

    // values
    for (byte[] value : map.values()) {
      assertTrue(arrayContainsKey(vals, value));
    }

    // remove
    map.remove(keys[0]);
    assertEquals(2, map.size());

    // clear
    map.clear();
    assertEquals(0, map.size());
  }

  @Test
  public void serialize() throws Exception {
    for (int i = 0; i < keys.length; i++) {
      map.put(keys[i], vals[i]);
    }

    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    ObjectOutputStream objOut = new ObjectOutputStream(byteOut);
    objOut.writeObject(map);

    ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
    ObjectInputStream objIn = new ObjectInputStream(byteIn);
    JedisByteHashMap mapRead = (JedisByteHashMap) objIn.readObject();

    assertTrue(entrySetSame(map.entrySet(), mapRead.entrySet()));
  }
}
