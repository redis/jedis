package redis.clients.jedis.commands.jedis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.params.XAddParams;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.resps.StreamEntryBinary;

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
  public void xreadBinary() {
    byte[] stream1 = "xb-stream1".getBytes();
    byte[] stream2 = "xb-stream2".getBytes();
    byte[] field = "f1".getBytes();
    byte[] value = "0-0".getBytes();

    // before any entry
    Map.Entry<byte[], byte[]> query1 = new AbstractMap.SimpleImmutableEntry<>(stream1, value);
    Map.Entry<byte[], byte[]> query2 = new AbstractMap.SimpleImmutableEntry<>(stream2, value);
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
    byte[] stream1 = "xb-stream1".getBytes();
    byte[] stream2 = "xb-stream2".getBytes();
    byte[] field = "f1".getBytes();
    byte[] value = "0-0".getBytes();

    // before any entry
    Map.Entry<byte[], byte[]> q1 = new AbstractMap.SimpleImmutableEntry<>(stream1, value);
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
      XReadParams.xReadParams().count(2), new AbstractMap.SimpleImmutableEntry<>(stream1, value));

    assertArrayEquals(new byte[][] { stream1 }, m1.keySet().toArray(new byte[0][]));

    List<StreamEntryBinary> list1 = m1.entrySet().stream()
        .filter(e -> Arrays.equals(e.getKey(), stream1)).map(Map.Entry::getValue).findFirst()
        .orElse(new ArrayList<>());
    assertEquals(2, list1.size());
    assertArrayEquals(id1, list1.get(0).getID().toString().getBytes());
    assertArrayEquals(id3, list1.get(1).getID().toString().getBytes());

    // read both streams as map
    Map<byte[], List<StreamEntryBinary>> m2 = jedis.xreadBinaryAsMap(
      XReadParams.xReadParams().count(1), new AbstractMap.SimpleImmutableEntry<>(stream1, value),
      new AbstractMap.SimpleImmutableEntry<>(stream2, value));

    List<StreamEntryBinary> list2 = m2.entrySet().stream()
        .filter(e -> Arrays.equals(e.getKey(), stream1)).map(Map.Entry::getValue).findFirst()
        .orElse(new ArrayList<>());
    List<StreamEntryBinary> list3 = m2.entrySet().stream()
        .filter(e -> Arrays.equals(e.getKey(), stream2)).map(Map.Entry::getValue).findFirst()
        .orElse(new ArrayList<>());
    assertEquals(2, m2.size());
    assertArrayEquals(id1, list2.get(0).getID().toString().getBytes());
    assertArrayEquals(id2, list3.get(0).getID().toString().getBytes());
  }

  @Test
  public void xreadGroupBinary() {
    byte[] stream = "xg-stream".getBytes();
    byte[] group = "g1".getBytes();
    byte[] consumer = "c1".getBytes();
    byte[] field = "f1".getBytes();
    byte[] value = "0-0".getBytes();

    // create group on empty stream
    jedis.xgroupCreate(stream, group, value, true);

    Map.Entry<byte[], byte[]> q1 = new AbstractMap.SimpleImmutableEntry<>(stream, value);

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
    Map.Entry<byte[], byte[]> readNew = new AbstractMap.SimpleImmutableEntry<>(stream,
        ">".getBytes());
    List<Map.Entry<byte[], List<StreamEntryBinary>>> res2 = jedis.xreadGroupBinary(group, consumer,
      XReadGroupParams.xReadGroupParams().count(1).block(1), readNew);
    assertEquals(1, res2.size());
    assertArrayEquals(stream, res2.get(0).getKey());
    assertArrayEquals(id1, res2.get(0).getValue().get(0).getID().toString().getBytes());
  }

  @Test
  public void xreadGroupBinaryAsMap() {
    byte[] stream = "xg-stream".getBytes();
    byte[] group = "g1".getBytes();
    byte[] consumer = "c1".getBytes();
    byte[] field = "f1".getBytes();
    byte[] value = "0-0".getBytes();

    jedis.xgroupCreate(stream, group, value, true);

    // before any entry
    Map.Entry<byte[], byte[]> q1 = new AbstractMap.SimpleImmutableEntry<>(stream, value);
    Map<byte[], List<StreamEntryBinary>> before = jedis.xreadGroupBinaryAsMap(group, consumer,
      XReadGroupParams.xReadGroupParams().block(1), q1);

    List<StreamEntryBinary> list = before.entrySet().stream()
        .filter(e -> Arrays.equals(e.getKey(), stream)).map(Map.Entry::getValue).findFirst()
        .orElse(null);
    assertNotNull(before);
    assertEquals(1, before.size());
    assertEquals(0, list.size());

    // add entries
    Map<byte[], byte[]> map = singletonMap(field, value);
    byte[] id1 = jedis.xadd(stream, XAddParams.xAddParams(), map);

    // read as map
    Map.Entry<byte[], byte[]> readNew = new AbstractMap.SimpleImmutableEntry<>(stream,
        ">".getBytes());
    Map<byte[], List<StreamEntryBinary>> m = jedis.xreadGroupBinaryAsMap(group, consumer,
      XReadGroupParams.xReadGroupParams().count(1).block(1), readNew);
    List<StreamEntryBinary> list1 = m.entrySet().stream()
        .filter(e -> Arrays.equals(e.getKey(), stream)).map(Map.Entry::getValue).findFirst()
        .orElse(new ArrayList<>());
    assertEquals(1, m.size());
    assertArrayEquals(id1, list1.get(0).getID().toString().getBytes());
  }
}
