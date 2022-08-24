package redis.clients.jedis.modules.search;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.util.SafeEncoder;

public class DocumentTest {

  @Test
  public void serialize() throws IOException, ClassNotFoundException {
    String id = "9f";
    double score = 10d;
    Map<String, Object> map = new HashMap<>();
    map.put("string", "c");
    map.put("float", 12d);
    byte[] payload = "1a".getBytes();
    Document document = new Document(id, map, score, payload);

    ByteArrayOutputStream aos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(aos);
    oos.writeObject(document);
    oos.flush();
    oos.close();

    ByteArrayInputStream ais = new ByteArrayInputStream(aos.toByteArray());
    ObjectInputStream ois = new ObjectInputStream(ais);
    Document read = (Document) ois.readObject();
    ois.close();

    assertEquals(id, read.getId());
    assertEquals(score, read.getScore(), 0d);
    assertArrayEquals(payload, read.getPayload());
    String exp = String.format("id:%s, score: %.1f, payload:%s, properties:%s",
            id, score, SafeEncoder.encode(payload), "[string=c, float=12.0]") ;
    assertEquals(exp, read.toString());
    assertEquals("c", read.getString("string"));
    assertEquals(Double.valueOf(12d), read.get("float"));
  }

  @Test
  public void toStringTest() {
    String id = "9f";
    double score = 10d;
    Map<String, Object> map = new HashMap<>();
    map.put("string", "c");
    map.put("float", 12d);
    byte[] payload = "1a".getBytes();
    Document document = new Document(id, map, score, payload);

    String expected = String.format("id:%s, score: %.1f, payload:%s, properties:%s",
            id, score, SafeEncoder.encode(payload), "[string=c, float=12.0]") ;
    assertEquals(expected, document.toString());
  }

  @Test
  public void toStringWithoutPayload() {
    String id = "9f";
    double score = 10d;
    Map<String, Object> map = new HashMap<>();
    map.put("string", "c");
    map.put("float", 12d);
    Document document = new Document(id, map, score);

    String expected = String.format("id:%s, score: %.1f, payload:%s, properties:%s",
            id, score, null, "[string=c, float=12.0]") ;
    assertEquals(expected, document.toString());
  }
}
