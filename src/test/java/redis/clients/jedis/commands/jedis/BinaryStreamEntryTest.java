package redis.clients.jedis.commands.jedis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.params.XAddParams;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.resps.StreamEntry;
import redis.clients.jedis.resps.StreamEntryBinary;
import redis.clients.jedis.util.ByteArrayComparator;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.*;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class BinaryStreamEntryTest extends JedisCommandsTestBase {

  public BinaryStreamEntryTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testBinaryStreamEntry() {
    final byte[] stream = "illegal_utf-8".getBytes();
    final byte[] group = "illegal_g1".getBytes();
    final byte[] consumer = "illegal_c1".getBytes();
    final byte[] value = new byte[] { (byte) 0xc3, (byte) 0x28 };

    // create group on empty stream
    jedis.xgroupCreate("illegal_utf-8", "illegal_g1", new StreamEntryID(), true);

    byte[] fieldKey1 = "fv".getBytes(StandardCharsets.UTF_8);
    Map<byte[], byte[]> map1 = Collections.singletonMap(fieldKey1, value);
    byte[] id1 = jedis.xadd(stream, XAddParams.xAddParams(), map1);

    Map.Entry<byte[], StreamEntryID> query = new AbstractMap.SimpleImmutableEntry<>(stream, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    List<Map.Entry<byte[], List<StreamEntryBinary>>> res =
            jedis.xreadGroupBinary(
                    group, consumer,
                    XReadGroupParams.xReadGroupParams().count(1).block(1000),
                    query
            );

    assertEquals(1, res.size());
    List<StreamEntryBinary> entries = res.get(0).getValue();
    byte[] returned = entries.get(0).getFields().get(fieldKey1);

    assertArrayEquals(value, returned);

    byte[] fieldKey2 = "fv2".getBytes(StandardCharsets.UTF_8);
    Map<byte[], byte[]> map2 = Collections.singletonMap(fieldKey2, value);
    byte[] id2 = jedis.xadd(stream, XAddParams.xAddParams(), map2);

    Map<String, StreamEntryID> query2 = new HashMap<>();
    query2.put("illegal_utf-8", StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    List<Map.Entry<String, List<StreamEntry>>> res2 =
            jedis.xreadGroup(
                    "illegal_g1", "illegal_c1",
                    XReadGroupParams.xReadGroupParams().count(1).block(1000),
                    query2
            );

    assertEquals(1, res2.size());
    List<StreamEntry> entries2 = res2.get(0).getValue();
    String returned2 = entries2.get(0).getFields().get(new String(fieldKey2));
    assertFalse(Arrays.equals(value, returned2.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  public void xreadBinary() {
    final byte[] stream1 = "xb-stream1".getBytes();
    final byte[] stream2 = "xb-stream2".getBytes();
    final byte[] field = "xb-f1".getBytes();
    final byte[] value = "xb-v1".getBytes();

    // before any entry
    Map.Entry<byte[], StreamEntryID> query1 = new AbstractMap.SimpleImmutableEntry<>(stream1, new StreamEntryID());
    Map.Entry<byte[], StreamEntryID> query2 = new AbstractMap.SimpleImmutableEntry<>(stream2, new StreamEntryID());
    assertNull(jedis.xreadBinary(XReadParams.xReadParams().block(1), query1));
    assertNull(jedis.xreadBinary(XReadParams.xReadParams(), query1));

    // add entries
    Map<byte[], byte[]> map = new HashMap<>();
    map.put(field, value);
    byte[] id1 = jedis.xadd(stream1, XAddParams.xAddParams(), map);
    byte[] id2 = jedis.xadd(stream2, XAddParams.xAddParams(), map);

    // read single stream
    List<Map.Entry<byte[], List<StreamEntryBinary>>> res1 = jedis
        .xreadBinary(XReadParams.xReadParams().count(1).block(1), query1);
    assertEquals(1, res1.size());
    assertArrayEquals(stream1, res1.get(0).getKey());
    assertEquals(1, res1.get(0).getValue().size());
    StreamEntryBinary e1 = res1.get(0).getValue().get(0);
    assertArrayEquals(id1, e1.getID().toString().getBytes());
    assertArrayEquals(value, e1.getFields().get(field));

    // read both streams
    List<Map.Entry<byte[], List<StreamEntryBinary>>> res2 = jedis
        .xreadBinary(XReadParams.xReadParams().count(2).block(1), query1, query2);
    assertEquals(2, res2.size());
    assertArrayEquals(stream1, res2.get(0).getKey());
    assertArrayEquals(stream2, res2.get(1).getKey());
    assertEquals(1, res2.get(0).getValue().size());
    assertEquals(1, res2.get(1).getValue().size());
    StreamEntryBinary e2 = res2.get(0).getValue().get(0);
    StreamEntryBinary e3 = res2.get(1).getValue().get(0);
    assertArrayEquals(id1, e2.getID().toString().getBytes());
    assertArrayEquals(id2, e3.getID().toString().getBytes());
    assertArrayEquals(value, e2.getFields().get(field));
  }

  @Test
  public void xreadBinaryAsMap() {
    final byte[] stream1 = "xbaM-stream1".getBytes();
    final byte[] stream2 = "xbaM-stream2".getBytes();
    final byte[] field = "xbaM-f1".getBytes();
    final byte[] value = "xbaM-v1".getBytes();

    // before any entry
    Map.Entry<byte[], StreamEntryID> q1 = new AbstractMap.SimpleImmutableEntry<>(stream1, new StreamEntryID());
    assertNull(jedis.xreadBinaryAsMap(XReadParams.xReadParams().block(1), q1));
    assertNull(jedis.xreadBinaryAsMap(XReadParams.xReadParams(), q1));

    // add entries
    Map<byte[], byte[]> map = new HashMap<>();
    map.put(field, value);
    byte[] id1 = jedis.xadd(stream1, XAddParams.xAddParams(), map);
    byte[] id2 = jedis.xadd(stream2, XAddParams.xAddParams(), map);
    byte[] id3 = jedis.xadd(stream1, XAddParams.xAddParams(), map);

    // read single stream as map
    Map<byte[], List<StreamEntryBinary>> m1 = jedis.xreadBinaryAsMap(
      XReadParams.xReadParams().count(2), new AbstractMap.SimpleImmutableEntry<>(stream1, new StreamEntryID()));

    byte[] key = m1.keySet().iterator().next();
    assertArrayEquals(stream1, key);

    List<StreamEntryBinary> list1 = m1.get(key);
    assertEquals(2, list1.size());
    assertArrayEquals(id1, list1.get(0).getID().toString().getBytes());
    assertArrayEquals(id3, list1.get(1).getID().toString().getBytes());

    // read both streams as map
    Map<byte[], List<StreamEntryBinary>> m2 = jedis.xreadBinaryAsMap(
      XReadParams.xReadParams().count(1), new AbstractMap.SimpleImmutableEntry<>(stream1, new StreamEntryID()),
        new AbstractMap.SimpleImmutableEntry<>(stream2, new StreamEntryID()));

    byte[] key1 = new byte[0];
    byte[] key2 = new byte[0];

    for (Map.Entry<byte[], List<StreamEntryBinary>> entry : m2.entrySet()) {
        if (ByteArrayComparator.compare(entry.getKey(), stream1) == 0) {
            key1 = entry.getKey();
        } else if (ByteArrayComparator.compare(entry.getKey(), stream2) == 0) {
            key2 = entry.getKey();
        }
    }

    List<StreamEntryBinary> list2 = m2.get(key1);
    List<StreamEntryBinary> list3 = m2.get(key2);
    assertEquals(2, m2.size());
    assertArrayEquals(id1, list2.get(0).getID().toString().getBytes());
    assertArrayEquals(id2, list3.get(0).getID().toString().getBytes());

    list2 = m2.entrySet().stream()
        .filter(e -> Arrays.equals(e.getKey(), stream1))
        .map(Map.Entry::getValue).findFirst().orElse(new ArrayList<>());
    list3 = m2.entrySet().stream()
        .filter(e -> Arrays.equals(e.getKey(), stream2))
        .map(Map.Entry::getValue).findFirst().orElse(new ArrayList<>());
    assertEquals(2, m2.size());
    assertArrayEquals(id1, list2.get(0).getID().toString().getBytes());
    assertArrayEquals(id2, list3.get(0).getID().toString().getBytes());
  }

  @Test
  public void xreadGroupBinary() {
    final byte[] stream = "xg-stream".getBytes();
    final byte[] group = "xg-g1".getBytes();
    final byte[] consumer = "xg-c1".getBytes();
    final byte[] field = "xg-f1".getBytes();
    final byte[] value = "xg-v1".getBytes();

    // create group on empty stream
    jedis.xgroupCreate("xg-stream", "xg-g1", new StreamEntryID(), true);

    Map.Entry<byte[], StreamEntryID> q1 = new AbstractMap.SimpleImmutableEntry<>(stream, new StreamEntryID());

    // before any entry
    List<Map.Entry<byte[], List<StreamEntryBinary>>> before = jedis.xreadGroupBinary(group,
      consumer, XReadGroupParams.xReadGroupParams().block(1), q1);
    assertNotNull(before);
    assertEquals(1, before.size());
    assertArrayEquals(stream, before.get(0).getKey());
    assertTrue(before.get(0).getValue().isEmpty());

    List<Map.Entry<byte[], List<StreamEntryBinary>>> beforeNoBlock = jedis.xreadGroupBinary(group,
      consumer, XReadGroupParams.xReadGroupParams(), q1);
    assertNotNull(beforeNoBlock);
    assertEquals(1, beforeNoBlock.size());
    assertArrayEquals(stream, beforeNoBlock.get(0).getKey());
    assertTrue(beforeNoBlock.get(0).getValue().isEmpty());

    // add entries
    Map<byte[], byte[]> map = singletonMap(field, value);
    byte[] id1 = jedis.xadd(stream, XAddParams.xAddParams().maxLen(3), map);

    // read with group
    Map.Entry<byte[], StreamEntryID> readNew = new AbstractMap.SimpleImmutableEntry<>(stream, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    List<Map.Entry<byte[], List<StreamEntryBinary>>> res2 = jedis.xreadGroupBinary(group, consumer,
      XReadGroupParams.xReadGroupParams().count(1).block(1), readNew);
    assertEquals(1, res2.size());
    assertArrayEquals(stream, res2.get(0).getKey());
    assertArrayEquals(id1, res2.get(0).getValue().get(0).getID().toString().getBytes());
  }

  @Test
  public void xreadGroupBinaryAsMap() {
    final byte[] stream = "xgaM-stream".getBytes();
    final byte[] group = "xgaM-g1".getBytes();
    final byte[] consumer = "xgaM-c1".getBytes();
    final byte[] field = "xgaM-f1".getBytes();
    final byte[] value = "xgaM-v1".getBytes();

    jedis.xgroupCreate("xgaM-stream", "xgaM-g1", new StreamEntryID(), true);

    // before any entry
    Map.Entry<byte[], StreamEntryID> q1 = new AbstractMap.SimpleImmutableEntry<>(stream, new StreamEntryID());
    Map<byte[], List<StreamEntryBinary>> before = jedis.xreadGroupBinaryAsMap(group, consumer, XReadGroupParams.xReadGroupParams().block(1), q1);

    byte[] key = before.keySet().iterator().next();
    assertArrayEquals(stream, key);

    List<StreamEntryBinary> list = before.get(key);
    assertNotNull(list);
    assertEquals(0, list.size());


    list = before.entrySet().stream()
        .filter(e -> Arrays.equals(e.getKey(), stream)).map(Map.Entry::getValue).findFirst()
        .orElse(null);
    assertNotNull(before);
    assertEquals(1, before.size());
    assertNotNull(list);
    assertEquals(0, list.size());

    // add entries
    Map<byte[], byte[]> map = singletonMap(field, value);
    byte[] id1 = jedis.xadd(stream, XAddParams.xAddParams(), map);

    // read as map
    Map.Entry<byte[], StreamEntryID> readNew = new AbstractMap.SimpleImmutableEntry<>(stream, StreamEntryID.XREADGROUP_UNDELIVERED_ENTRY);
    Map<byte[], List<StreamEntryBinary>> m = jedis.xreadGroupBinaryAsMap(group, consumer,
      XReadGroupParams.xReadGroupParams().count(1).block(1), readNew);

    byte[] key1 = m.keySet().iterator().next();
    assertArrayEquals(stream, key1);

    List<StreamEntryBinary> list1 = m.get(key1);
    assertNotNull(list1);
    assertArrayEquals(id1, list1.get(0).getID().toString().getBytes());


    list1 = m.entrySet().stream()
        .filter(e -> Arrays.equals(e.getKey(), stream)).map(Map.Entry::getValue).findFirst()
        .orElse(new ArrayList<>());
    assertEquals(1, m.size());
    assertArrayEquals(id1, list1.get(0).getID().toString().getBytes());
  }
}
