package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.util.SafeEncoder;

public class HashImportTest {

  @Test
  public void ofStringFields() {
    HashImport fs = HashImport.of("name", "email", "age");
    assertEquals(3, fs.size());
    assertFalse(fs.isDiscarded());
    assertTrue(fs.name().startsWith("j:"));
    assertArrayEquals(SafeEncoder.encode("name"), fs.fields().get(0));
    assertArrayEquals(SafeEncoder.encode("age"), fs.fields().get(2));
  }

  @Test
  public void namesAreUniquePerInstance() {
    assertNotEquals(HashImport.of("a").name(), HashImport.of("a").name());
  }

  @Test
  public void rejectsEmpty() {
    assertThrows(IllegalArgumentException.class, () -> HashImport.of(new String[0]));
    assertThrows(IllegalArgumentException.class, () -> HashImport.of(new byte[0][]));
  }

  @Test
  public void rejectsNullField() {
    assertThrows(IllegalArgumentException.class, () -> HashImport.of((String) null));
    assertThrows(IllegalArgumentException.class, () -> HashImport.of("ok", null));
    assertThrows(IllegalArgumentException.class, () -> HashImport.of("a".getBytes(), null));
  }

  @Test
  public void rejectsEmptyFieldName() {
    assertThrows(IllegalArgumentException.class, () -> HashImport.of(""));
    assertThrows(IllegalArgumentException.class, () -> HashImport.of("ok", ""));
    assertThrows(IllegalArgumentException.class, () -> HashImport.of(new byte[0]));
    assertThrows(IllegalArgumentException.class, () -> HashImport.of("a".getBytes(), new byte[0]));
  }

  @Test
  public void rejectsDuplicateStringFields() {
    assertThrows(IllegalArgumentException.class, () -> HashImport.of("dup", "dup"));
  }

  @Test
  public void rejectsDuplicateBinaryFieldsByContent() {
    assertThrows(IllegalArgumentException.class,
      () -> HashImport.of(new byte[] { 1, 2 }, new byte[] { 1, 2 }));
  }

  @Test
  public void binaryFieldsAreDefensivelyCopied() {
    byte[] field = { 1, 2, 3 };
    HashImport fs = HashImport.of(field);
    field[0] = 9; // mutate caller's array after construction
    assertArrayEquals(new byte[] { 1, 2, 3 }, fs.fields().get(0));
  }

  @Test
  public void closeMarksDiscarded() {
    HashImport fs = HashImport.of("a");
    assertFalse(fs.isDiscarded());
    fs.close();
    assertTrue(fs.isDiscarded());
    fs.close(); // idempotent
    assertTrue(fs.isDiscarded());
  }
}
