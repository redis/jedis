package redis.clients.jedis.modules.search;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.search.Document;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DocumentTest {

  @Test
  public void serialize() throws IOException, ClassNotFoundException {
    String id = "9f";
    double score = 10d;
    Map<String, Object> map = new HashMap<>();
    map.put("string", "c");
    map.put("float", 12d);
    Document document = new Document(id, map, score);

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
    assertEquals("c", read.getString("string"));
    assertEquals(12d, read.get("float"));
  }

  @Test
  public void toStringTest() {
    String id = "9f";
    double score = 10d;
    Map<String, Object> map = new HashMap<>();
    map.put("string", "c");
    map.put("float", 12d);
    Document document = new Document(id, map, score);

    // use english language to make sure the decimal separator is the same as the toString
    String expected1 = String.format(Locale.ENGLISH, "id:%s, score: %.1f, properties:%s", id, score,
        "[string=c, float=12.0]");
    String expected2 = String.format(Locale.ENGLISH, "id:%s, score: %.1f, properties:%s", id, score,
        "[float=12.0, string=c]");

    // the order of the properties is not guaranteed, so we check both possible outcomes
    String actual = document.toString();
    assertTrue(actual.equals(expected1) || actual.equals(expected2));
  }

}
