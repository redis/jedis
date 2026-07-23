package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Unit tests for the {@link HashImport} fieldset template value object (no server).
 */
public class HashImportTest {

  @Test
  public void stringTemplate() {
    HashImport fs = HashImport.of("name", "email", "age");
    assertArrayEquals(new String[] { "name", "email", "age" }, fs.fields());
    assertNull(fs.binaryFields());
    assertEquals(3, fs.size());
    assertTrue(fs.name().startsWith("j:"));
  }

  @Test
  public void binaryTemplate() {
    byte[] f1 = SafeEncoder.encode("name");
    byte[] f2 = SafeEncoder.encode("age");
    HashImport fs = HashImport.of(f1, f2);
    assertNull(fs.fields());
    assertArrayEquals(new byte[][] { f1, f2 }, fs.binaryFields());
    assertEquals(2, fs.size());
    assertTrue(fs.name().startsWith("j:"));
  }

  @Test
  public void namesAreUniqueAndMonotonic() {
    HashImport a = HashImport.of("x");
    HashImport b = HashImport.of("x");
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
    assertThrows(IllegalArgumentException.class,
      () -> HashImport.of(SafeEncoder.encode("a"), SafeEncoder.encode("a")));
  }

  @Test
  public void constructionCopiesInput() {
    String[] fields = { "a", "b" };
    HashImport fs = HashImport.of(fields);
    fields[0] = "mutated";
    assertEquals("a", fs.fields()[0]);
  }
}
