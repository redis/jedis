package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit coverage for {@link HashSlotRanges}: the slots-or-ranges wire format
 * ({@code 123,456,789-1000}) of cluster maintenance pushes — parsing, membership, counting,
 * iteration order and rejection of malformed input.
 */
@Tag("unit")
public class HashSlotRangesTest {

  @Test
  public void parseSingleSlot() {
    HashSlotRanges r = HashSlotRanges.parse("42");
    assertTrue(r.contains(42));
    assertFalse(r.contains(41));
    assertFalse(r.contains(43));
    assertEquals(1, r.slotCount());
    assertEquals(Arrays.asList(42), slots(r));
  }

  @Test
  public void parseInclusiveRange() {
    HashSlotRanges r = HashSlotRanges.parse("10-13");
    assertFalse(r.contains(9));
    assertTrue(r.contains(10));
    assertTrue(r.contains(13));
    assertFalse(r.contains(14));
    assertEquals(4, r.slotCount());
    assertEquals(Arrays.asList(10, 11, 12, 13), slots(r));
  }

  @Test
  public void parseDegenerateRange() { // from == to is a one-slot range
    HashSlotRanges r = HashSlotRanges.parse("7-7");
    assertTrue(r.contains(7));
    assertEquals(1, r.slotCount());
    assertEquals(Arrays.asList(7), slots(r));
  }

  @Test
  public void parseMixedSlotsAndRanges() {
    HashSlotRanges r = HashSlotRanges.parse("123,456,789-1000");
    assertTrue(r.contains(123));
    assertTrue(r.contains(456));
    assertTrue(r.contains(789));
    assertTrue(r.contains(900));
    assertTrue(r.contains(1000));
    assertFalse(r.contains(0));
    assertFalse(r.contains(124));
    assertFalse(r.contains(788));
    assertFalse(r.contains(1001));
    assertEquals(2 + (1000 - 789 + 1), r.slotCount());
  }

  @Test
  public void forEachSlotVisitsInWireOrder() { // not sorted: wire order is preserved
    HashSlotRanges r = HashSlotRanges.parse("5,1-3,9");
    assertEquals(Arrays.asList(5, 1, 2, 3, 9), slots(r));
  }

  @Test
  public void overlappingRangesCountSlotsPerRange() { // no dedup: count is the sum of the ranges
    HashSlotRanges r = HashSlotRanges.parse("1-3,2-4");
    assertEquals(6, r.slotCount());
    assertEquals(Arrays.asList(1, 2, 3, 2, 3, 4), slots(r));
    assertTrue(r.contains(2));
  }

  @Test
  public void parseAcceptsFullSlotSpace() {
    HashSlotRanges r = HashSlotRanges.parse("0-16383");
    assertTrue(r.contains(0));
    assertTrue(r.contains(16383));
    assertFalse(r.contains(-1));
    assertFalse(r.contains(16384));
    assertEquals(Protocol.CLUSTER_HASHSLOTS, r.slotCount());
  }

  @Test
  public void parseAcceptsBoundarySlots() {
    assertTrue(HashSlotRanges.parse("0").contains(0));
    assertTrue(HashSlotRanges.parse("16383").contains(16383));
  }

  @Test
  public void toStringIsTheWireSource() {
    assertEquals("123,456,789-1000", HashSlotRanges.parse("123,456,789-1000").toString());
    assertEquals("7", HashSlotRanges.parse("7").toString());
  }

  @Test
  public void parseRejectsNullOrEmpty() {
    assertThrows(IllegalArgumentException.class, () -> HashSlotRanges.parse(null));
    assertThrows(IllegalArgumentException.class, () -> HashSlotRanges.parse(""));
  }

  @Test
  public void parseRejectsNonNumericTokens() {
    assertThrows(IllegalArgumentException.class, () -> HashSlotRanges.parse("abc"));
    assertThrows(IllegalArgumentException.class, () -> HashSlotRanges.parse("1,x"));
    assertThrows(IllegalArgumentException.class, () -> HashSlotRanges.parse("1-x"));
    assertThrows(IllegalArgumentException.class, () -> HashSlotRanges.parse("x-5"));
    assertThrows(IllegalArgumentException.class, () -> HashSlotRanges.parse("1.5"));
    assertThrows(IllegalArgumentException.class, () -> HashSlotRanges.parse(" 1"));
  }

  @Test
  public void parseRejectsEmptyTokens() {
    assertThrows(IllegalArgumentException.class, () -> HashSlotRanges.parse("1,,2"));
    assertThrows(IllegalArgumentException.class, () -> HashSlotRanges.parse(",1"));
    assertThrows(IllegalArgumentException.class, () -> HashSlotRanges.parse("1-"));
    assertThrows(IllegalArgumentException.class, () -> HashSlotRanges.parse("-5"));
    assertThrows(IllegalArgumentException.class, () -> HashSlotRanges.parse("-"));
  }

  @Test
  public void parseRejectsInvertedRange() {
    assertThrows(IllegalArgumentException.class, () -> HashSlotRanges.parse("10-9"));
    assertThrows(IllegalArgumentException.class, () -> HashSlotRanges.parse("1,10-9"));
  }

  @Test
  public void parseRejectsSlotsOutsideHashSlotSpace() {
    assertThrows(IllegalArgumentException.class, () -> HashSlotRanges.parse("16384"));
    assertThrows(IllegalArgumentException.class, () -> HashSlotRanges.parse("0-16384"));
    assertThrows(IllegalArgumentException.class, () -> HashSlotRanges.parse("16383-99999"));
    assertThrows(IllegalArgumentException.class, () -> HashSlotRanges.parse("1,2,3,16384"));
  }

  @Test
  public void parseRejectsAnyBadTokenEvenAfterValidOnes() { // whole string is rejected atomically
    assertThrows(IllegalArgumentException.class, () -> HashSlotRanges.parse("1-3,5,bad"));
  }

  private static List<Integer> slots(HashSlotRanges r) {
    List<Integer> out = new ArrayList<>();
    r.forEachSlot(out::add);
    return out;
  }
}
