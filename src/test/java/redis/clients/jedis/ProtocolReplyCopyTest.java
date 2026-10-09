package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisRedirectionException;
import redis.clients.jedis.util.KeyValue;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.SafeEncoder;

public class ProtocolReplyCopyTest {

  @ParameterizedTest
  @ValueSource(strings = { ":42\r\n", ",-0.0\r\n", ",inf\r\n", ",nan\r\n",
      "(9223372036854775808\r\n", "#t\r\n", "#f\r\n", "_\r\n", "$-1\r\n" })
  public void preservesImmutableScalarReplies(String wire) {
    Object original = read(wire);
    Object copy = Protocol.copyReply(original);
    assertEquals(original, copy);
    if (original != null) {
      assertEquals(original.getClass(), copy.getClass());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "+text\r\n", "$4\r\ntext\r\n", "=8\r\ntxt:text\r\n" })
  public void copiesStringReplyBytes(String wire) {
    byte[] original = (byte[]) read(wire);
    byte[] copy = (byte[]) Protocol.copyReply(original);
    assertNotSame(original, copy);
    original[0] = 'X';
    assertArrayEquals(SafeEncoder.encode("text"), copy);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void copiesNestedArraysAndResp3MapEntries() {
    List<?> original = (List<?>) read("*2\r\n%1\r\n+key\r\n*2\r\n+value\r\n_\r\n~1\r\n+member\r\n");
    List<?> copy = (List<?>) Protocol.copyReply(original);
    KeyValue<byte[], List<?>> pair = (KeyValue<byte[], List<?>>) ((List<?>) original.get(0)).get(0);
    pair.getKey()[0] = 'X';
    ((byte[]) pair.getValue().get(0))[0] = 'X';
    pair.getValue().clear();
    ((List<?>) original.get(1)).clear();
    KeyValue<byte[], List<?>> copiedPair = (KeyValue<byte[], List<?>>) ((List<?>) copy.get(0))
        .get(0);
    assertArrayEquals(SafeEncoder.encode("key"), copiedPair.getKey());
    assertArrayEquals(SafeEncoder.encode("value"), (byte[]) copiedPair.getValue().get(0));
    assertNull(copiedPair.getValue().get(1));
    assertArrayEquals(SafeEncoder.encode("member"), (byte[]) ((List<?>) copy.get(1)).get(0));
  }

  @Test
  public void preservesEmptyMapMarkerWithoutConfusingItWithEmptyArray() {
    Object map = Protocol.copyReply(read("%0\r\n"));
    Object array = Protocol.copyReply(read("*0\r\n"));
    assertSame(Protocol.PROTOCOL_EMPTY_MAP, map);
    assertTrue(BuilderFactory.AGGRESSIVE_ENCODED_OBJECT.build(map) instanceof Map);
    assertTrue(BuilderFactory.AGGRESSIVE_ENCODED_OBJECT.build(array) instanceof List);
    assertNotSame(Protocol.PROTOCOL_EMPTY_MAP, array);
    assertThrows(UnsupportedOperationException.class, () -> ((List<?>) map).add(null));
    List<?> nested = (List<?>) Protocol.copyReply(read("*1\r\n%0\r\n"));
    assertSame(Protocol.PROTOCOL_EMPTY_MAP, nested.get(0));
  }

  @ParameterizedTest
  @ValueSource(strings = { "ERR failure", "MOVED 12 localhost:6379", "ASK 13 localhost:6380",
      "CLUSTERDOWN failure", "BUSY failure", "NOSCRIPT failure", "NOAUTH failure",
      "WRONGPASS failure", "NOPERM failure", "NOPROTO failure" })
  public void copiesEmbeddedErrorsWithoutSharingMutableThrowableState(String message) {
    List<?> original = (List<?>) read("*1\r\n-" + message + "\r\n");
    JedisDataException error = (JedisDataException) original.get(0);
    JedisDataException copy = (JedisDataException) ((List<?>) Protocol.copyReply(original)).get(0);
    assertNotSame(error, copy);
    assertEquals(error.getClass(), copy.getClass());
    assertEquals(error.getMessage(), copy.getMessage());
    assertArrayEquals(error.getStackTrace(), copy.getStackTrace());
    error.addSuppressed(new IllegalStateException("caller mutation"));
    error.initCause(new IllegalStateException("caller cause"));
    error.setStackTrace(new StackTraceElement[0]);
    assertEquals(0, copy.getSuppressed().length);
    assertNull(copy.getCause());
    assertTrue(copy.getStackTrace().length > 0);
    if (error instanceof JedisRedirectionException) {
      JedisRedirectionException redirect = (JedisRedirectionException) error;
      JedisRedirectionException copiedRedirect = (JedisRedirectionException) copy;
      assertEquals(redirect.getSlot(), copiedRedirect.getSlot());
      assertEquals(redirect.getTargetNode(), copiedRedirect.getTargetNode());
    }
  }

  @Test
  public void rejectsValuesOutsideTheProtocolReplyDomain() {
    assertThrows(IllegalArgumentException.class, () -> Protocol.copyReply(new Object()));
  }

  private static Object read(String wire) {
    return Protocol.read(new RedisInputStream(new ByteArrayInputStream(SafeEncoder.encode(wire))),
      null);
  }
}
