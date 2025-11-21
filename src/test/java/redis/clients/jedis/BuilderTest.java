package redis.clients.jedis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.resps.StreamEntry;
import redis.clients.jedis.resps.StreamEntryBinary;
import redis.clients.jedis.util.RedisInputStream;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BuilderTest {

  // Helper methods for building test data from RESP protocol strings
  private static Object parseRespResponse(String respResponse) {
    InputStream is = new ByteArrayInputStream(respResponse.getBytes());
    return Protocol.read(new RedisInputStream(is));
  }
  
  @SuppressWarnings("unchecked")
  private static ArrayList<Object> createStreamEntryData(String id, String fieldKey,
      String fieldValue, long millisElapsedFromDelivery, long deliveredCount) {
    String respResponse =
        "*4\r\n" +                                                    // Entry with 4 elements
            "$" + id.length() + "\r\n" + id + "\r\n" +                   // Entry ID
            "*2\r\n" +                                                    // 2 field-value pairs
            "$" + fieldKey.length() + "\r\n" + fieldKey + "\r\n" +       // Field key
            "$" + fieldValue.length() + "\r\n" + fieldValue + "\r\n" +   // Field value
            ":" + millisElapsedFromDelivery + "\r\n" +                    // millisElapsedFromDelivery
            ":" + deliveredCount + "\r\n";                               // deliveredCount

    return (ArrayList<Object>) parseRespResponse(respResponse);
  }
  
  @SuppressWarnings("unchecked")
  private static ArrayList<Object> createStreamEntryBinaryData(String id, String fieldKey,
      byte[] fieldValue, long millisElapsedFromDelivery, long deliveredCount) {
    // For binary data, we need to construct the RESP response with the actual byte length
    String respResponse =
        "*4\r\n" +                                                    // Entry with 4 elements
            "$" + id.length() + "\r\n" + id + "\r\n" +                   // Entry ID
            "*2\r\n" +                                                    // 2 field-value pairs
            "$" + fieldKey.length() + "\r\n" + fieldKey + "\r\n" +       // Field key
            "$" + fieldValue.length + "\r\n";                            // Field value length

    // Manually construct the byte array with binary field value
    byte[] respBytes = respResponse.getBytes();
    byte[] crLf = "\r\n".getBytes();
    byte[] metadataBytes = (":" + millisElapsedFromDelivery + "\r\n" + ":" + deliveredCount
        + "\r\n").getBytes();

    byte[] fullResponse = new byte[respBytes.length + fieldValue.length + crLf.length
        + metadataBytes.length];
    System.arraycopy(respBytes, 0, fullResponse, 0, respBytes.length);
    System.arraycopy(fieldValue, 0, fullResponse, respBytes.length, fieldValue.length);
    System.arraycopy(crLf, 0, fullResponse, respBytes.length + fieldValue.length, crLf.length);
    System.arraycopy(metadataBytes, 0, fullResponse,
        respBytes.length + fieldValue.length + crLf.length, metadataBytes.length);

    InputStream is = new ByteArrayInputStream(fullResponse);
    return (ArrayList<Object>) Protocol.read(new RedisInputStream(is));
  }

  @Test
  public void buildDouble() {
    Double build = BuilderFactory.DOUBLE.build("1.0".getBytes());
    assertEquals(Double.valueOf(1.0), build);
    build = BuilderFactory.DOUBLE.build("inf".getBytes());
    assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), build);
    build = BuilderFactory.DOUBLE.build("+inf".getBytes());
    assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), build);
    build = BuilderFactory.DOUBLE.build("-inf".getBytes());
    assertEquals(Double.valueOf(Double.NEGATIVE_INFINITY), build);

    try {
      BuilderFactory.DOUBLE.build("".getBytes());
      Assertions.fail("Empty String should throw NumberFormatException.");
    } catch (NumberFormatException expected) {
      assertEquals("empty String", expected.getMessage());
    }
  }

  @Test
  public void buildStreamEntryListWithClaimedEntryMetadata() {
    // Simulate Redis response for a single claimed entry with metadata
    // Format: [[id, [field, value], msSinceLastDelivery, redeliveryCount]]
    List<Object> data = new ArrayList<>();
    data.add(createStreamEntryData("1234-12", "key", "value", 5000L, 2L));

    List<StreamEntry> result = BuilderFactory.STREAM_ENTRY_LIST.build(data);

    assertNotNull(result);
    assertEquals(1, result.size());

    StreamEntry streamEntry = result.get(0);
    assertEquals("1234-12", streamEntry.getID().toString());
    assertEquals("value", streamEntry.getFields().get("key"));
    assertEquals(Long.valueOf(5000), streamEntry.getMillisElapsedFromDelivery());
    assertEquals(Long.valueOf(2), streamEntry.getDeliveredCount());
  }

  @Test
  public void buildStreamEntryListWithFreshEntryZeroRedeliveries() {
    // Simulate Redis response for a fresh entry (not claimed from PEL)
    // Format: [[id, [field, value], 0, 0]]
    List<Object> data = new ArrayList<>();
    data.add(createStreamEntryData("1234-12", "key", "value", 1000L, 0L));

    List<StreamEntry> result = BuilderFactory.STREAM_ENTRY_LIST.build(data);

    assertNotNull(result);
    assertEquals(1, result.size());

    StreamEntry streamEntry = result.get(0);
    assertEquals("1234-12", streamEntry.getID().toString());
    assertEquals(Long.valueOf(1000), streamEntry.getMillisElapsedFromDelivery());
    assertEquals(Long.valueOf(0), streamEntry.getDeliveredCount());
  }

  @Test
  public void buildStreamEntryListWithMixedBatchClaimedFirstThenFresh() {
    // Simulate Redis response with mixed entries: claimed entries first, then fresh entries
    List<Object> data = new ArrayList<>();

    // Entry #1 (claimed, redeliveryCount=2)
    data.add(createStreamEntryData("1-0", "f1", "v1", 1500L, 2L));

    // Entry #2 (claimed, redeliveryCount=1)
    data.add(createStreamEntryData("2-0", "f2", "v2", 1200L, 1L));

    // Entry #3 (fresh, redeliveryCount=0)
    data.add(createStreamEntryData("3-0", "f3", "v3", 10L, 0L));

    List<StreamEntry> result = BuilderFactory.STREAM_ENTRY_LIST.build(data);

    assertNotNull(result);
    assertEquals(3, result.size());

    StreamEntry m1 = result.get(0);
    StreamEntry m2 = result.get(1);
    StreamEntry m3 = result.get(2);

    // Verify claimed entries
    assertTrue(m1.getDeliveredCount() > 0);
    assertTrue(m2.getDeliveredCount() > 0);
    assertEquals(Long.valueOf(2), m1.getDeliveredCount());
    assertEquals(Long.valueOf(1), m2.getDeliveredCount());

    // Verify fresh entry
    assertEquals(Long.valueOf(0), m3.getDeliveredCount());
  }

  @Test
  public void buildStreamEntryBinaryListWithClaimedEntryMetadata() {
    // Test binary version with claimed entry metadata
    List<Object> data = new ArrayList<>();
    data.add(
        createStreamEntryBinaryData("1234-12", "key", new byte[] { 0x00, 0x01, 0x02 }, 5000L, 2L));

    List<StreamEntryBinary> result = BuilderFactory.STREAM_ENTRY_BINARY_LIST.build(data);

    assertNotNull(result);
    assertEquals(1, result.size());

    StreamEntryBinary streamEntry = result.get(0);
    assertEquals("1234-12", streamEntry.getID().toString());
    assertEquals(Long.valueOf(5000), streamEntry.getMillisElapsedFromDelivery());
    assertEquals(Long.valueOf(2), streamEntry.getDeliveredCount());
  }

}
