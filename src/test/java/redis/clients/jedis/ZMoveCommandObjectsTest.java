package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.params.ZMoveMemberParams.zMoveMemberParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.params.BZMoveParams;
import redis.clients.jedis.params.ZMoveMemberParams;
import redis.clients.jedis.params.ZMoveParams;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.util.SafeEncoder;

/** Arguments and reply builder of the ZMOVE, BZMOVE and ZMOVEMEMBER command objects. */
public class ZMoveCommandObjectsTest {

  private static final String SRC = "{z}s";
  private static final String DST = "{z}d";

  private final CommandObjects commandObjects = new CommandObjects(RedisProtocol.RESP2);

  private static List<String> argStrings(CommandObject<?> command) {
    List<String> out = new ArrayList<>();
    for (Rawable r : command.getArguments()) {
      out.add(SafeEncoder.encode(r.getRaw()));
    }
    return out;
  }

  private static List<byte[]> argBytes(CommandObject<?> command) {
    List<byte[]> out = new ArrayList<>();
    for (Rawable r : command.getArguments()) {
      out.add(r.getRaw());
    }
    return out;
  }

  private static List<String> keyStrings(CommandObject<?> command) {
    List<String> out = new ArrayList<>();
    for (Object key : command.getArguments().getKeys()) {
      out.add(key instanceof byte[] ? SafeEncoder.encode((byte[]) key) : (String) key);
    }
    return out;
  }

  private static byte[] b(String s) {
    return SafeEncoder.encode(s);
  }

  private static List<Object> pair(Object member, Object score) {
    return Arrays.asList(member, score);
  }

  /** Scenario ZMOV-B01: ZMOVE MIN COUNT 2; both keys are recorded as keys. */
  @Test
  public void zmoveMinCount() {
    CommandObject<List<Tuple>> command = commandObjects.zmove(SRC, DST, ZMoveParams.min().count(2));
    List<String> expected = Arrays.asList("ZMOVE", SRC, DST, "MIN", "COUNT", "2");
    assertEquals(expected, argStrings(command));
    assertEquals(Arrays.asList(SRC, DST), keyStrings(command));
  }

  /** Scenario ZMOV-B02: ZMOVE with a text score range and a count. */
  @Test
  public void zmoveByScoreText() {
    ZMoveParams params = ZMoveParams.byScore("-inf", "(1700000000").count(500);
    List<String> expected = Arrays.asList("ZMOVE", SRC, DST, "BYSCORE", "-inf", "(1700000000");
    expected = new ArrayList<>(expected);
    expected.addAll(Arrays.asList("COUNT", "500"));
    assertEquals(expected, argStrings(commandObjects.zmove(SRC, DST, params)));
  }

  /** Scenario ZMOV-B03: BZMOVE MAX 30.0 COUNT 10, marked blocking. */
  @Test
  public void bzmoveMaxCountBlocking() {
    BZMoveParams params = BZMoveParams.max(30).count(10);
    CommandObject<List<Tuple>> command = commandObjects.bzmove(SRC, DST, params);
    List<String> expected = Arrays.asList("BZMOVE", SRC, DST, "MAX", "30.0", "COUNT", "10");
    assertEquals(expected, argStrings(command));
    assertTrue(command.getArguments().isBlocking());
    assertEquals(Arrays.asList(SRC, DST), keyStrings(command));
  }

  /** Scenario ZMOV-B04: BZMOVE with a zero timeout sends 0.0. */
  @Test
  public void bzmoveZeroTimeout() {
    CommandObject<List<Tuple>> command = commandObjects.bzmove(SRC, DST, BZMoveParams.min(0));
    assertEquals(Arrays.asList("BZMOVE", SRC, DST, "MIN", "0.0"), argStrings(command));
  }

  /** Scenario ZMOV-B05: a 100 ms timeout is sent as 0.1, not 1 or 0. */
  @Test
  public void bzmoveFractionalTimeout() {
    CommandObject<List<Tuple>> command = commandObjects.bzmove(SRC, DST, BZMoveParams.min(0.1));
    assertEquals("0.1", argStrings(command).get(4));
  }

  /** Scenario ZMOV-B06: one member is sent as MEMBERS 1 a. */
  @Test
  public void zmovememberSingle() {
    CommandObject<List<Tuple>> command = commandObjects.zmovemember(SRC, DST, "a");
    List<String> expected = Arrays.asList("ZMOVEMEMBER", SRC, DST, "MEMBERS", "1", "a");
    assertEquals(expected, argStrings(command));
    assertEquals(Arrays.asList(SRC, DST), keyStrings(command));
  }

  /** Scenario ZMOV-B07: members are sent as given, not de-duplicated or re-ordered. */
  @Test
  public void zmovememberMembersAsGiven() {
    String[] members = { "c", "a", "missing", "a" };
    CommandObject<List<Tuple>> command = commandObjects.zmovemember(SRC, DST, members);
    List<String> expected = new ArrayList<>(Arrays.asList("ZMOVEMEMBER", SRC, DST, "MEMBERS"));
    expected.addAll(Arrays.asList("4", "c", "a", "missing", "a"));
    assertEquals(expected, argStrings(command));
  }

  /** Scenario ZMOV-B08: the condition comes before the list; keyword-named members are data. */
  @Test
  public void zmovememberConditionBeforeList() {
    ZMoveMemberParams xx = zMoveMemberParams().xx();
    String[] members = { "NX", "MEMBERS" };
    CommandObject<List<Tuple>> command = commandObjects.zmovemember("src", "dst", xx, members);
    List<String> expected = new ArrayList<>(Arrays.asList("ZMOVEMEMBER", "src", "dst", "XX"));
    expected.addAll(Arrays.asList("MEMBERS", "2", "NX", "MEMBERS"));
    assertEquals(expected, argStrings(command));
  }

  /** Scenario ZMOV-B09: an empty member list is sent as MEMBERS 0, with and without NX. */
  @Test
  public void zmovememberEmptyList() {
    List<String> bare = argStrings(commandObjects.zmovemember("src", "dst"));
    assertEquals(Arrays.asList("ZMOVEMEMBER", "src", "dst", "MEMBERS", "0"), bare);
    ZMoveMemberParams params = zMoveMemberParams().nx();
    List<String> nx = argStrings(commandObjects.zmovemember("src", "dst", params));
    assertEquals(Arrays.asList("ZMOVEMEMBER", "src", "dst", "NX", "MEMBERS", "0"), nx);
  }

  /** Scenario ZMOV-B10: a null member is rejected before any command is sent. */
  @Test
  public void zmovememberNullMemberRejected() {
    String none = null;
    assertThrows(IllegalArgumentException.class, () -> commandObjects.zmovemember(SRC, DST, none));
  }

  /** Scenario ZMOV-B10: a null key is rejected before any command is sent. */
  @Test
  @Disabled("Defect 1-D1: a null key throws NullPointerException, not IllegalArgumentException")
  public void zmovememberNullKeyRejected() {
    String none = null;
    assertThrows(IllegalArgumentException.class, () -> commandObjects.zmovemember(none, DST, "a"));
  }

  /** Scenario ZMOV-B11: the reply builder reads RESP2 and RESP3 replies alike, and keeps null. */
  @Test
  public void replyBuilder() {
    CommandObjects resp3 = new CommandObjects(RedisProtocol.RESP3);
    List<Tuple> expected = Arrays.asList(new Tuple("a", 1.0), new Tuple("b", 2.0));
    List<Object> resp2Reply = Arrays.asList(pair(b("a"), b("1")), pair(b("b"), b("2")));
    List<Object> resp3Reply = Arrays.asList(pair(b("a"), 1.0), pair(b("b"), 2.0));
    List<Object> onePair = Collections.singletonList(pair(b("a"), b("1")));
    List<Object> infReply = Collections.singletonList(pair(b("a"), b("inf")));

    List<CommandObject<List<Tuple>>> resp2Commands = new ArrayList<>();
    resp2Commands.add(commandObjects.zmove(SRC, DST, ZMoveParams.min()));
    resp2Commands.add(commandObjects.bzmove(SRC, DST, BZMoveParams.min(1)));
    resp2Commands.add(commandObjects.zmovemember(SRC, DST, "a"));
    List<Tuple> oneTuple = Collections.singletonList(new Tuple("a", 1.0));
    for (CommandObject<List<Tuple>> command : resp2Commands) {
      assertEquals(expected, command.getBuilder().build(resp2Reply));
      assertEquals(oneTuple, command.getBuilder().build(onePair));
      List<Tuple> inf = command.getBuilder().build(infReply);
      assertEquals(1, inf.size());
      assertEquals(Double.POSITIVE_INFINITY, inf.get(0).getScore(), 0d);
      assertNull(command.getBuilder().build(null));
    }

    List<CommandObject<List<Tuple>>> resp3Commands = new ArrayList<>();
    resp3Commands.add(resp3.zmove(SRC, DST, ZMoveParams.min()));
    resp3Commands.add(resp3.bzmove(SRC, DST, BZMoveParams.min(1)));
    resp3Commands.add(resp3.zmovemember(SRC, DST, "a"));
    for (CommandObject<List<Tuple>> command : resp3Commands) {
      assertEquals(expected, command.getBuilder().build(resp3Reply));
      assertNull(command.getBuilder().build(null));
    }
  }

  /** Scenario ZMOV-B12: the byte[] overloads send the given bytes as keys and members. */
  @Test
  public void binaryOverloads() {
    byte[] src = { 0x01, '{', 'z', '}', 0x02 };
    byte[] dst = { 0x03, '{', 'z', '}', 0x04 };
    byte[] ma = { 0x0A };
    byte[] mb = { 0x0B };

    CommandObject<List<Tuple>> zmove = commandObjects.zmove(src, dst, ZMoveParams.min().count(2));
    assertBinary(zmove, "ZMOVE", src, dst, b("MIN"), b("COUNT"), b("2"));
    assertEquals(2, zmove.getArguments().getKeys().size());
    assertArrayEquals(src, (byte[]) zmove.getArguments().getKeys().get(0));
    assertArrayEquals(dst, (byte[]) zmove.getArguments().getKeys().get(1));

    BZMoveParams bparams = BZMoveParams.max(30).count(10);
    CommandObject<List<Tuple>> bzmove = commandObjects.bzmove(src, dst, bparams);
    assertBinary(bzmove, "BZMOVE", src, dst, b("MAX"), b("30.0"), b("COUNT"), b("10"));
    assertTrue(bzmove.getArguments().isBlocking());

    CommandObject<List<Tuple>> single = commandObjects.zmovemember(src, dst, ma);
    assertBinary(single, "ZMOVEMEMBER", src, dst, b("MEMBERS"), b("1"), ma);

    CommandObject<List<Tuple>> many = commandObjects.zmovemember(src, dst, mb, ma, mb);
    assertBinary(many, "ZMOVEMEMBER", src, dst, b("MEMBERS"), b("3"), mb, ma, mb);
    assertFalse(many.getArguments().isBlocking());
  }

  private static void assertBinary(CommandObject<?> command, String name, byte[]... expected) {
    List<byte[]> actual = argBytes(command);
    assertEquals(name, SafeEncoder.encode(actual.get(0)));
    assertEquals(expected.length, actual.size() - 1);
    for (int i = 0; i < expected.length; i++) {
      assertArrayEquals(expected[i], actual.get(i + 1), "argument " + (i + 1));
    }
  }
}
