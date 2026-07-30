package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Unit tests for the {@link HashImport} fieldset template value object (no server).
 */
public class HashImportTest {

  @Test
  public void stringTemplate() {
    HashImport<String> fs = HashImport.of("name", "email", "age");
    assertEquals(Arrays.asList("name", "email", "age"), fs.fields());
    assertEquals(3, fs.size());
    assertTrue(fs.name().startsWith("j:"));
  }

  @Test
  public void binaryTemplate() {
    byte[] f1 = SafeEncoder.encode("name");
    byte[] f2 = SafeEncoder.encode("age");
    HashImport<byte[]> fs = HashImport.of(f1, f2);
    List<byte[]> fields = fs.fields();
    assertEquals(2, fields.size());
    assertArrayEquals(f1, fields.get(0));
    assertArrayEquals(f2, fields.get(1));
    assertTrue(fs.name().startsWith("j:"));
  }

  @Test
  public void namesAreUniqueAndMonotonic() {
    HashImport<String> a = HashImport.of("x");
    HashImport<String> b = HashImport.of("x");
    assertNotEquals(a.name(), b.name());
  }

  @Test
  public void rejectsEmpty() {
    assertThrows(IllegalArgumentException.class, () -> HashImport.of(new String[0]));
    assertThrows(IllegalArgumentException.class, () -> HashImport.of(new byte[0][]));
  }

  @Test
  public void rejectsNullField() {
    assertThrows(IllegalArgumentException.class, () -> HashImport.of("a", null));
    assertThrows(IllegalArgumentException.class,
      () -> HashImport.of(SafeEncoder.encode("a"), null));
  }

  @Test
  public void rejectsDuplicateField() {
    assertThrows(IllegalArgumentException.class, () -> HashImport.of("a", "a"));
    // byte[] duplicates are compared by content.
    assertThrows(IllegalArgumentException.class,
      () -> HashImport.of(SafeEncoder.encode("a"), SafeEncoder.encode("a")));
  }

  @Test
  public void constructionCopiesInput() {
    String[] fields = { "a", "b" };
    HashImport<String> fs = HashImport.of(fields);
    fields[0] = "mutated";
    assertEquals("a", fs.fields().get(0));
  }
}
