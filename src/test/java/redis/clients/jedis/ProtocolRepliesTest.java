package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.jedis.util.KeyValue;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Tests {@link ProtocolReplies#copy(Object)} with one group per RESP reply type. Each group covers
 * every shape the protocol reader returns for its type, read from the wire.
 */
public class ProtocolRepliesTest {

  @Nested
  @DisplayName("RESP2 simple string (+)")
  class SimpleStringReply {

    @Test
    void valueIsCopied() {
      assertCopyIsDeep(read("+OK"));
    }

    @Test
    void emptyValueIsCopied() {
      assertCopyIsDeep(read("+"));
    }
  }

  @Nested
  @DisplayName("RESP2 bulk string ($)")
  class BulkStringReply {

    @Test
    void valueIsCopied() {
      assertCopyIsDeep(read("$5", "value"));
    }

    @Test
    void emptyValueIsCopied() {
      assertCopyIsDeep(read("$0", ""));
    }

    @Test
    void binaryValueIsCopied() {
      assertCopyIsDeep(read("$4", "a\r\nb"));
    }

    @Test
    void nullIsShared() {
      assertCopyReturnsSameInstance(read("$-1"));
    }

    @Test
    void changingTheOriginalDoesNotChangeTheCopy() {
      byte[] original = (byte[]) read("$5", "value");
      byte[] copy = (byte[]) ProtocolReplies.copy(original);

      original[0] = 'X';

      assertArrayEquals(SafeEncoder.encode("value"), copy);
    }
  }

  @Nested
  @DisplayName("RESP2 integer (:)")
  class IntegerReply {

    @Test
    void positiveIsShared() {
      assertCopyReturnsSameInstance(read(":42"));
    }

    @Test
    void negativeIsShared() {
      assertCopyReturnsSameInstance(read(":-1"));
    }
  }

  @Nested
  @DisplayName("RESP2 error (-)")
  class ErrorReply {

    @Test
    void errorInArrayIsCopied() {
      assertCopyIsDeep(read("*1", "-ERR boom"));
    }

    @Test
    void movedErrorKeepsItsType() {
      Object reply = read("*1", "-MOVED 12 localhost:6379");

      assertInstanceOf(JedisMovedDataException.class, ((List<?>) reply).get(0));
      assertCopyIsDeep(reply);
    }

    @Test
    void askErrorKeepsItsType() {
      Object reply = read("*1", "-ASK 12 localhost:6379");

      assertInstanceOf(JedisAskDataException.class, ((List<?>) reply).get(0));
      assertCopyIsDeep(reply);
    }

    @Test
    void errorInNestedArrayIsCopied() {
      assertCopyIsDeep(read("*1", "*1", "-ERR boom"));
    }

    @Test
    void errorInSetIsCopied() {
      assertCopyIsDeep(read("~1", "-ERR boom"));
    }

    @Test
    void errorInPushIsCopied() {
      assertCopyIsDeep(read(">2", "$7", "message", "-ERR boom"));
    }

    @Test
    void topLevelErrorIsThrownNotReturned() {
      assertThrows(JedisDataException.class, () -> read("-ERR boom"));
    }
  }

  @Nested
  @DisplayName("RESP2 array (*)")
  class ArrayReply {

    @Test
    void flatArrayIsDeepCopied() {
      assertCopyIsDeep(read("*2", "$1", "a", "$1", "b"));
    }

    @Test
    void nestedArrayIsDeepCopied() {
      assertCopyIsDeep(read("*1", "*1", "$1", "a"));
    }

    @Test
    void arrayOfScalarsIsDeepCopied() {
      assertCopyIsDeep(read("*4", ":1", ",1.5", "#t", "_"));
    }

    @Test
    void arrayWithNullElementIsDeepCopied() {
      assertCopyIsDeep(read("*2", "$-1", "$1", "a"));
    }

    @Test
    void emptyArrayIsCopiedNotTheMapMarker() {
      Object copy = ProtocolReplies.copy(read("*0"));

      assertEquals(Collections.emptyList(), copy);
      assertNotSame(Protocol.PROTOCOL_EMPTY_MAP, copy);
    }

    @Test
    void nullIsShared() {
      assertCopyReturnsSameInstance(read("*-1"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void changingTheOriginalDoesNotChangeTheCopy() {
      List<Object> original = (List<Object>) read("*2", "$1", "a", "*1", ":1");
      List<Object> copy = (List<Object>) ProtocolReplies.copy(original);

      ((List<Object>) original.get(1)).clear();
      original.clear();

      assertEquals(2, copy.size());
      assertEquals(Collections.singletonList(1L), copy.get(1));
    }
  }

  @Nested
  @DisplayName("RESP3 null (_)")
  class NullReply {

    @Test
    void nullIsShared() {
      assertCopyReturnsSameInstance(read("_"));
    }
  }

  @Nested
  @DisplayName("RESP3 boolean (#)")
  class BooleanReply {

    @Test
    void trueIsShared() {
      assertCopyReturnsSameInstance(read("#t"));
    }

    @Test
    void falseIsShared() {
      assertCopyReturnsSameInstance(read("#f"));
    }
  }

  @Nested
  @DisplayName("RESP3 double (,)")
  class DoubleReply {

    @Test
    void valueIsShared() {
      assertCopyReturnsSameInstance(read(",1.5"));
    }

    @Test
    void infinityIsShared() {
      assertCopyReturnsSameInstance(read(",inf"));
    }

    @Test
    void negativeInfinityIsShared() {
      assertCopyReturnsSameInstance(read(",-inf"));
    }

    @Test
    void nanIsShared() {
      assertCopyReturnsSameInstance(read(",nan"));
    }
  }

  @Nested
  @DisplayName("RESP3 big number (()")
  class BigNumberReply {

    @Test
    void positiveIsShared() {
      assertCopyReturnsSameInstance(read("(12345678901234567890"));
    }

    @Test
    void negativeIsShared() {
      assertCopyReturnsSameInstance(read("(-12345678901234567890"));
    }
  }

  @Nested
  @DisplayName("RESP3 verbatim string (=)")
  class VerbatimStringReply {

    @Test
    void valueIsCopied() {
      assertCopyIsDeep(read("=9", "txt:value"));
    }

    @Test
    void emptyValueIsCopied() {
      assertCopyIsDeep(read("=4", "txt:"));
    }

    @Test
    void nullIsShared() {
      assertCopyReturnsSameInstance(read("=-1"));
    }
  }

  @Nested
  @DisplayName("RESP3 map (%)")
  class MapReply {

    @Test
    void flatMapIsDeepCopied() {
      assertCopyIsDeep(read("%1", "+key", "$5", "value"));
    }

    @Test
    void mapAsValueIsDeepCopied() {
      assertCopyIsDeep(read("%1", "+outer", "%1", "+inner", ":1"));
    }

    @Test
    void mapWithIntegerKeyIsDeepCopied() {
      assertCopyIsDeep(read("%1", ":1", "+one"));
    }

    @Test
    void mapWithArrayKeyIsDeepCopied() {
      assertCopyIsDeep(read("%1", "*1", "$1", "a", "+v"));
    }

    @Test
    void mapWithNullValueIsDeepCopied() {
      assertCopyIsDeep(read("%1", "+k", "_"));
    }

    @Test
    void emptyMapKeepsTheMarker() {
      assertCopyReturnsSameInstance(read("%0"));
    }

    @Test
    void emptyMapAsValueKeepsTheMarker() {
      List<?> copy = (List<?>) ProtocolReplies.copy(read("%1", "+k", "%0"));

      assertSame(Protocol.PROTOCOL_EMPTY_MAP, ((KeyValue<?, ?>) copy.get(0)).getValue());
    }

    @Test
    void emptyMapInArrayKeepsTheMarker() {
      List<?> copy = (List<?>) ProtocolReplies.copy(read("*1", "%0"));

      assertSame(Protocol.PROTOCOL_EMPTY_MAP, copy.get(0));
    }

    @Test
    void nullIsShared() {
      assertCopyReturnsSameInstance(read("%-1"));
    }

    @Test
    void errorValueIsThrownNotReturned() {
      assertThrows(JedisDataException.class, () -> read("%1", "+k", "-ERR boom"));
    }
  }

  @Nested
  @DisplayName("RESP3 set (~)")
  class SetReply {

    @Test
    void setIsDeepCopied() {
      assertCopyIsDeep(read("~2", "$1", "a", "$1", "b"));
    }

    @Test
    void emptySetIsCopiedNotTheMapMarker() {
      Object copy = ProtocolReplies.copy(read("~0"));

      assertEquals(Collections.emptyList(), copy);
      assertNotSame(Protocol.PROTOCOL_EMPTY_MAP, copy);
    }

    @Test
    void nullIsShared() {
      assertCopyReturnsSameInstance(read("~-1"));
    }
  }

  @Nested
  @DisplayName("RESP3 push (>)")
  class PushReply {

    @Test
    void pushIsDeepCopied() {
      assertCopyIsDeep(read(">2", "$7", "message", "$1", "a"));
    }
  }

  @Nested
  @DisplayName("Values the reader never returns")
  class Unsupported {

    @Test
    void nonProtocolValueIsRejected() {
      assertThrows(IllegalArgumentException.class, () -> ProtocolReplies.copy(new Object()));
    }

    @Test
    void nonProtocolValueInListIsRejected() {
      assertThrows(IllegalArgumentException.class,
        () -> ProtocolReplies.copy(Collections.singletonList("text")));
    }
  }

  /** Reads one reply from {@code lines}, each terminated with CRLF. */
  private static Object read(String... lines) {
    String wire = String.join("\r\n", lines) + "\r\n";
    return Protocol.read(new RedisInputStream(new ByteArrayInputStream(SafeEncoder.encode(wire))));
  }

  private static void assertCopyReturnsSameInstance(Object reply) {
    assertSame(reply, ProtocolReplies.copy(reply));
  }

  private static void assertCopyIsDeep(Object reply) {
    assertIndependent(reply, ProtocolReplies.copy(reply));
  }

  /**
   * Mutable parts must be different objects with equal content; immutable parts the same instance.
   */
  private static void assertIndependent(Object original, Object copy) {
    if (original instanceof byte[]) {
      assertNotSame(original, copy);
      assertArrayEquals((byte[]) original, (byte[]) copy);
    } else if (original == Protocol.PROTOCOL_EMPTY_MAP) {
      assertSame(original, copy);
    } else if (original instanceof List) {
      List<?> originalList = (List<?>) original;
      List<?> copyList = (List<?>) copy;
      assertNotSame(original, copy);
      assertEquals(originalList.size(), copyList.size());
      for (int i = 0; i < originalList.size(); i++) {
        assertIndependent(originalList.get(i), copyList.get(i));
      }
    } else if (original instanceof KeyValue) {
      KeyValue<?, ?> originalPair = (KeyValue<?, ?>) original;
      KeyValue<?, ?> copyPair = (KeyValue<?, ?>) copy;
      assertNotSame(original, copy);
      assertIndependent(originalPair.getKey(), copyPair.getKey());
      assertIndependent(originalPair.getValue(), copyPair.getValue());
    } else if (original instanceof JedisDataException) {
      assertNotSame(original, copy);
      assertSame(original.getClass(), copy.getClass());
      assertEquals(((JedisDataException) original).getMessage(),
        ((JedisDataException) copy).getMessage());
    } else {
      assertSame(original, copy);
    }
  }
}
