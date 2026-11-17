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

import org.junit.jupiter.api.Test;

import redis.clients.jedis.args.SortedSetOption;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.params.ZMoveMemberParams;
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

  /** ZMOVE MIN COUNT 2; both keys are recorded as keys. */
  @Test
  public void zmoveMinCount() {
    CommandObject<List<Tuple>> command = commandObjects.zmove(SRC, DST, SortedSetOption.MIN, 2);
    List<String> expected = Arrays.asList("ZMOVE", SRC, DST, "MIN", "COUNT", "2");
    assertEquals(expected, argStrings(command));
    assertEquals(Arrays.asList(SRC, DST), keyStrings(command));
  }

  /** ZMOVE with a text score range and a count. */
  @Test
  public void zmoveByScoreText() {
    List<String> expected = Arrays.asList("ZMOVE", SRC, DST, "BYSCORE", "-inf", "(1700000000");
    expected = new ArrayList<>(expected);
    expected.addAll(Arrays.asList("COUNT", "500"));
    assertEquals(expected,
      argStrings(commandObjects.zmoveByScore(SRC, DST, "-inf", "(1700000000", 500)));
  }

  /** BZMOVE MAX 30.0 COUNT 10, marked blocking. */
  @Test
  public void bzmoveMaxCountBlocking() {
    CommandObject<List<Tuple>> command = commandObjects.bzmove(SRC, DST, SortedSetOption.MAX, 30,
      10);
    List<String> expected = Arrays.asList("BZMOVE", SRC, DST, "MAX", "30.0", "COUNT", "10");
    assertEquals(expected, argStrings(command));
    assertTrue(command.getArguments().isBlocking());
    assertEquals(Arrays.asList(SRC, DST), keyStrings(command));
  }

  /** BZMOVE with a zero timeout sends 0.0. */
  @Test
  public void bzmoveZeroTimeout() {
    CommandObject<List<Tuple>> command = commandObjects.bzmove(SRC, DST, SortedSetOption.MIN, 0);
    assertEquals(Arrays.asList("BZMOVE", SRC, DST, "MIN", "0.0"), argStrings(command));
  }

  /** A 100 ms timeout is sent as 0.1, not 1 or 0. */
  @Test
  public void bzmoveFractionalTimeout() {
    CommandObject<List<Tuple>> command = commandObjects.bzmove(SRC, DST, SortedSetOption.MIN, 0.1);
    assertEquals("0.1", argStrings(command).get(4));
  }

  /** One member is sent as MEMBERS 1 a. */
  @Test
  public void zmovememberSingle() {
    CommandObject<Tuple> command = commandObjects.zmovemember(SRC, DST, "a");
    List<String> expected = Arrays.asList("ZMOVEMEMBER", SRC, DST, "MEMBERS", "1", "a");
    assertEquals(expected, argStrings(command));
    assertEquals(Arrays.asList(SRC, DST), keyStrings(command));
  }

  /** Members are sent as given, not de-duplicated or re-ordered. */
  @Test
  public void zmovememberMembersAsGiven() {
    String[] members = { "c", "a", "missing", "a" };
    CommandObject<List<Tuple>> command = commandObjects.zmovemember(SRC, DST, members);
    List<String> expected = new ArrayList<>(Arrays.asList("ZMOVEMEMBER", SRC, DST, "MEMBERS"));
    expected.addAll(Arrays.asList("4", "c", "a", "missing", "a"));
    assertEquals(expected, argStrings(command));
  }

  /** The condition comes before the list; keyword-named members are data. */
  @Test
  public void zmovememberConditionBeforeList() {
    ZMoveMemberParams xx = zMoveMemberParams().xx();
    String[] members = { "NX", "MEMBERS" };
    CommandObject<List<Tuple>> command = commandObjects.zmovemember("src", "dst", xx, members);
    List<String> expected = new ArrayList<>(Arrays.asList("ZMOVEMEMBER", "src", "dst", "XX"));
    expected.addAll(Arrays.asList("MEMBERS", "2", "NX", "MEMBERS"));
    assertEquals(expected, argStrings(command));
  }

  /** An empty member list is sent as MEMBERS 0, with and without NX. */
  @Test
  public void zmovememberEmptyList() {
    List<String> bare = argStrings(commandObjects.zmovemember("src", "dst"));
    assertEquals(Arrays.asList("ZMOVEMEMBER", "src", "dst", "MEMBERS", "0"), bare);
    ZMoveMemberParams params = zMoveMemberParams().nx();
    List<String> nx = argStrings(commandObjects.zmovemember("src", "dst", params));
    assertEquals(Arrays.asList("ZMOVEMEMBER", "src", "dst", "NX", "MEMBERS", "0"), nx);
  }

  /** A null member is rejected before any command is sent. */
  @Test
  public void zmovememberNullMemberRejected() {
    String none = null;
    assertThrows(IllegalArgumentException.class, () -> commandObjects.zmovemember(SRC, DST, none));
  }

  /** A null key is rejected by the key path before any command is sent. */
  @Test
  public void zmovememberNullKeyRejected() {
    String none = null;
    assertThrows(NullPointerException.class, () -> commandObjects.zmovemember(none, DST, "a"));
  }

  /** The reply builder reads RESP2 and RESP3 replies alike, and keeps null. */
  @Test
  public void replyBuilder() {
    CommandObjects resp3 = new CommandObjects(RedisProtocol.RESP3);
    List<Tuple> expected = Arrays.asList(new Tuple("a", 1.0), new Tuple("b", 2.0));
    List<Object> resp2Reply = Arrays.asList(pair(b("a"), b("1")), pair(b("b"), b("2")));
    List<Object> resp3Reply = Arrays.asList(pair(b("a"), 1.0), pair(b("b"), 2.0));
    List<Object> onePair = Collections.singletonList(pair(b("a"), b("1")));
    List<Object> infReply = Collections.singletonList(pair(b("a"), b("inf")));

    List<CommandObject<List<Tuple>>> resp2Commands = new ArrayList<>();
    resp2Commands.add(commandObjects.zmove(SRC, DST, SortedSetOption.MIN));
    resp2Commands.add(commandObjects.bzmove(SRC, DST, SortedSetOption.MIN, 1));
    resp2Commands.add(commandObjects.zmovemember(SRC, DST, "a", "b"));
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
    resp3Commands.add(resp3.zmove(SRC, DST, SortedSetOption.MIN));
    resp3Commands.add(resp3.bzmove(SRC, DST, SortedSetOption.MIN, 1));
    resp3Commands.add(resp3.zmovemember(SRC, DST, "a", "b"));
    for (CommandObject<List<Tuple>> command : resp3Commands) {
      assertEquals(expected, command.getBuilder().build(resp3Reply));
      assertNull(command.getBuilder().build(null));
    }
  }

  /**
   * The single-member zmovemember reads a one-pair nested reply as one Tuple on RESP2 and RESP3,
   * String and byte[], with and without params, and keeps null.
   */
  @Test
  public void zmovememberSingleReplyBuilder() {
    ZMoveMemberParams nx = zMoveMemberParams().nx();
    Tuple expected = new Tuple("a", 1.0);
    List<Object> resp2Reply = Collections.singletonList(pair(b("a"), b("1")));
    List<Object> resp3Reply = Collections.singletonList(pair(b("a"), 1.0));
    for (RedisProtocol protocol : RedisProtocol.values()) {
      CommandObjects objects = new CommandObjects(protocol);
      List<Object> reply = protocol == RedisProtocol.RESP3 ? resp3Reply : resp2Reply;
      List<CommandObject<Tuple>> commands = new ArrayList<>();
      commands.add(objects.zmovemember(SRC, DST, "a"));
      commands.add(objects.zmovemember(b(SRC), b(DST), b("a")));
      commands.add(objects.zmovemember(SRC, DST, nx, "a"));
      commands.add(objects.zmovemember(b(SRC), b(DST), nx, b("a")));
      for (CommandObject<Tuple> command : commands) {
        Tuple tuple = command.getBuilder().build(reply);
        assertEquals(expected, tuple, protocol + " " + command.getBuilder());
        assertEquals(1.0, tuple.getScore(), 0d);
        assertNull(command.getBuilder().build(null));
      }
    }
  }

  /** The byte[] overloads send the given bytes as keys and members. */
  @Test
  public void binaryOverloads() {
    byte[] src = { 0x01, '{', 'z', '}', 0x02 };
    byte[] dst = { 0x03, '{', 'z', '}', 0x04 };
    byte[] ma = { 0x0A };
    byte[] mb = { 0x0B };

    CommandObject<List<Tuple>> zmove = commandObjects.zmove(src, dst, SortedSetOption.MIN, 2);
    assertBinary(zmove, "ZMOVE", src, dst, b("MIN"), b("COUNT"), b("2"));
    assertEquals(2, zmove.getArguments().getKeys().size());
    assertArrayEquals(src, (byte[]) zmove.getArguments().getKeys().get(0));
    assertArrayEquals(dst, (byte[]) zmove.getArguments().getKeys().get(1));

    CommandObject<List<Tuple>> bzmove = commandObjects.bzmove(src, dst, SortedSetOption.MAX, 30,
      10);
    assertBinary(bzmove, "BZMOVE", src, dst, b("MAX"), b("30.0"), b("COUNT"), b("10"));
    assertTrue(bzmove.getArguments().isBlocking());

    CommandObject<Tuple> single = commandObjects.zmovemember(src, dst, ma);
    assertBinary(single, "ZMOVEMEMBER", src, dst, b("MEMBERS"), b("1"), ma);

    CommandObject<List<Tuple>> many = commandObjects.zmovemember(src, dst, mb, ma, mb);
    assertBinary(many, "ZMOVEMEMBER", src, dst, b("MEMBERS"), b("3"), mb, ma, mb);
    assertFalse(many.getArguments().isBlocking());
  }

  private List<String> zmoveTokens(CommandObject<?> command) {
    List<String> all = argStrings(command);
    return all.subList(3, all.size());
  }

  /** MIN and MAX are sent without COUNT when no count is given. */
  @Test
  public void zmoveEndWithoutCount() {
    assertEquals(Collections.singletonList("MIN"),
      zmoveTokens(commandObjects.zmove(SRC, DST, SortedSetOption.MIN)));
    assertEquals(Collections.singletonList("MAX"),
      zmoveTokens(commandObjects.zmove(SRC, DST, SortedSetOption.MAX)));
  }

  /** A text score range without a count is sent as given, with no COUNT. */
  @Test
  public void zmoveByScoreTextWithoutCount() {
    assertEquals(Arrays.asList("BYSCORE", "(1", "3"),
      zmoveTokens(commandObjects.zmoveByScore(SRC, DST, "(1", "3")));
  }

  /** Infinite double bounds are sent as -inf and +inf. */
  @Test
  public void zmoveByScoreInfiniteDoubleBounds() {
    List<String> expected = Arrays.asList("BYSCORE", "-inf", "+inf", "COUNT", "2");
    assertEquals(expected, zmoveTokens(commandObjects.zmoveByScore(SRC, DST,
      Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 2)));
  }

  /** Double bounds are sent in Java's spelling. */
  @Test
  public void zmoveByScoreDoubleSpelling() {
    assertEquals(Arrays.asList("BYSCORE", "1.0", "3.0", "COUNT", "2"),
      zmoveTokens(commandObjects.zmoveByScore(SRC, DST, 1.0, 3.0, 2)));
    assertEquals(Arrays.asList("BYSCORE", "1.0E-5", "1.0E-5"),
      zmoveTokens(commandObjects.zmoveByScore(SRC, DST, 1.0E-5, 1.0E-5)));
    assertEquals(Arrays.asList("BYSCORE", "NaN", "1.0"),
      zmoveTokens(commandObjects.zmoveByScore(SRC, DST, Double.NaN, 1)));
  }

  /** Binary bounds are sent as the given bytes, with and without a count. */
  @Test
  public void zmoveByScoreBinaryBounds() {
    byte[] src = b(SRC);
    byte[] dst = b(DST);
    byte[] min = b("(1");
    byte[] max = b("3");
    assertBinary(commandObjects.zmoveByScore(src, dst, min, max), "ZMOVE", src, dst, b("BYSCORE"),
      min, max);
    assertBinary(commandObjects.zmoveByScore(src, dst, min, max, 4), "ZMOVE", src, dst,
      b("BYSCORE"), min, max, b("COUNT"), b("4"));
  }

  /** Double bounds with binary keys are sent as Protocol.toByteArray spells them, then COUNT. */
  @Test
  public void zmoveByScoreBinaryKeysDoubleBounds() {
    byte[] src = b(SRC);
    byte[] dst = b(DST);
    assertBinary(commandObjects.zmoveByScore(src, dst, 1.0, 3.0, 2), "ZMOVE", src, dst,
      b("BYSCORE"), Protocol.toByteArray(1.0), Protocol.toByteArray(3.0), b("COUNT"), b("2"));
    assertBinary(commandObjects.zmoveByScore(src, dst, 1.0E-5, 1.0E-5), "ZMOVE", src, dst,
      b("BYSCORE"), Protocol.toByteArray(1.0E-5), Protocol.toByteArray(1.0E-5));
    assertBinary(
      commandObjects.zmoveByScore(src, dst, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 2),
      "ZMOVE", src, dst, b("BYSCORE"), b("-inf"), b("+inf"), b("COUNT"), b("2"));
  }

  /** Double bounds with binary keys use the same spelling as with String keys. */
  @Test
  public void zmoveByScoreBinaryKeysDoubleBoundsSpelling() {
    assertBinary(commandObjects.zmoveByScore(b(SRC), b(DST), 1.0, 3.0), "ZMOVE", b(SRC), b(DST),
      b("BYSCORE"), b("1.0"), b("3.0"));
    assertBinary(commandObjects.zmoveByScore(b(SRC), b(DST), Double.NaN, 1, 3), "ZMOVE", b(SRC),
      b(DST), b("BYSCORE"), b("NaN"), b("1.0"), b("COUNT"), b("3"));
  }

  /** Any count is sent as given. */
  @Test
  public void zmoveAnyCountIsSent() {
    assertEquals(Arrays.asList("MIN", "COUNT", "0"),
      zmoveTokens(commandObjects.zmove(SRC, DST, SortedSetOption.MIN, 0)));
    assertEquals(Arrays.asList("MIN", "COUNT", "-1"),
      zmoveTokens(commandObjects.zmove(SRC, DST, SortedSetOption.MIN, -1)));
    assertEquals(Arrays.asList("MIN", "COUNT", "9223372036854775807"),
      zmoveTokens(commandObjects.zmove(SRC, DST, SortedSetOption.MIN, Long.MAX_VALUE)));
  }

  /** A null bound or end is rejected before anything is sent. */
  @Test
  public void zmoveNullBoundOrEndRejected() {
    byte[] one = b("1");
    assertThrows(IllegalArgumentException.class,
      () -> commandObjects.zmoveByScore(SRC, DST, null, "1"));
    assertThrows(IllegalArgumentException.class,
      () -> commandObjects.zmoveByScore(SRC, DST, "1", null));
    assertThrows(IllegalArgumentException.class,
      () -> commandObjects.zmoveByScore(b(SRC), b(DST), null, one));
    assertThrows(IllegalArgumentException.class,
      () -> commandObjects.zmoveByScore(b(SRC), b(DST), one, null));
    assertThrows(IllegalArgumentException.class, () -> commandObjects.zmove(SRC, DST, null));
    assertThrows(IllegalArgumentException.class, () -> commandObjects.bzmove(SRC, DST, null, 1));
  }

  /** BZMOVE sends the end, then the timeout, then COUNT. */
  @Test
  public void bzmoveArgumentOrder() {
    assertEquals(Arrays.asList("MIN", "0.5"),
      zmoveTokens(commandObjects.bzmove(SRC, DST, SortedSetOption.MIN, 0.5)));
    assertEquals(Arrays.asList("MAX", "0.0", "COUNT", "5"),
      zmoveTokens(commandObjects.bzmove(SRC, DST, SortedSetOption.MAX, 0, 5)));
  }

  /** The timeout keeps its fraction in Java's spelling. */
  @Test
  public void bzmoveTimeoutFractionKept() {
    assertEquals(Arrays.asList("MIN", "5.0E-4"),
      zmoveTokens(commandObjects.bzmove(SRC, DST, SortedSetOption.MIN, 0.0005)));
    assertEquals(Arrays.asList("MIN", "1.000000001"),
      zmoveTokens(commandObjects.bzmove(SRC, DST, SortedSetOption.MIN, 1.000000001)));
  }

  /** A negative, infinite or NaN timeout is sent as given. */
  @Test
  public void bzmoveOutOfRangeTimeoutsAreSent() {
    assertEquals(Arrays.asList("MIN", "-1.0"),
      zmoveTokens(commandObjects.bzmove(SRC, DST, SortedSetOption.MIN, -1)));
    assertEquals(Arrays.asList("MIN", "+inf"),
      zmoveTokens(commandObjects.bzmove(SRC, DST, SortedSetOption.MIN, Double.POSITIVE_INFINITY)));
    assertEquals(Arrays.asList("MIN", "NaN"),
      zmoveTokens(commandObjects.bzmove(SRC, DST, SortedSetOption.MIN, Double.NaN)));
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
