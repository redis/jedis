package redis.clients.jedis.modules.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import redis.clients.jedis.json.Path2;

public class Path2Test {

  @Test(expected = NullPointerException.class)
  public void _null() {
    Path2.of(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void empty() {
    Path2.of("");
  }

  @Test
  public void root() {
    assertEquals("$", Path2.ROOT_PATH.toString());
    assertEquals(Path2.ROOT_PATH, new Path2("$"));
    assertEquals(Path2.ROOT_PATH, Path2.of("$"));
  }

  @Test
  public void test() {
    assertEquals("$.a.b", Path2.of("$.a.b").toString());
    assertEquals("$.a.b", new Path2("$.a.b").toString());
    assertEquals("$.a.b", Path2.of(".a.b").toString());
    assertEquals("$.a.b", new Path2(".a.b").toString());
    assertEquals("$.a.b", Path2.of("a.b").toString());
    assertEquals("$.a.b", new Path2("a.b").toString());
  }

  @Test
  public void equals() {
    assertTrue(new Path2("a.b").equals(Path2.of(".a.b")));
    assertTrue(Path2.of("a.b").equals(new Path2(".a.b")));
  }
  
  @Test
  public void testJsonPointer() {
    assertEquals(Path2.ofJsonPointer(""), Path2.ROOT_PATH);
    assertEquals(Path2.ofJsonPointer("/"), Path2.of("$.[\"\"]"));
    assertEquals(Path2.ofJsonPointer("//0"), Path2.of("$.[\"\"].[0]"));
    assertEquals(Path2.ofJsonPointer("//"), Path2.of("$.[\"\"].[\"\"]"));
    assertEquals(Path2.ofJsonPointer("// "), Path2.of("$.[\"\"].[\" \"]"));
    assertEquals(Path2.ofJsonPointer("/a/b/c"), Path2.of("$.[\"a\"].[\"b\"].[\"c\"]"));
    assertEquals(Path2.ofJsonPointer("/a/0/c"), Path2.of("$.[\"a\"].[0].[\"c\"]"));
    assertEquals(Path2.ofJsonPointer("/a/0b/c"), Path2.of("$.[\"a\"].[\"0b\"].[\"c\"]"));
    assertEquals(Path2.ofJsonPointer("/ab/cd/1010"), Path2.of("$.[\"ab\"].[\"cd\"].[1010]"));
    assertEquals(Path2.ofJsonPointer("/a/b/c").hashCode(), Path2.of("$.[\"a\"].[\"b\"].[\"c\"]").hashCode());
  
    // escape test
    assertEquals(Path2.ofJsonPointer("/a/~0"), Path2.of("$.[\"a\"].[\"~\"]"));
    assertEquals(Path2.ofJsonPointer("/a/~1"), Path2.of("$.[\"a\"].[\"/\"]"));
    assertEquals(Path2.ofJsonPointer("/a/~0/c"), Path2.of("$.[\"a\"].[\"~\"].[\"c\"]"));
    assertEquals(Path2.ofJsonPointer("/a/~1/c"), Path2.of("$.[\"a\"].[\"/\"].[\"c\"]"));
    assertEquals(Path2.ofJsonPointer("/a/~~/c"), Path2.of("$.[\"a\"].[\"~~\"].[\"c\"]"));
    assertEquals(Path2.ofJsonPointer("/~/~~~/~"), Path2.of("$.[\"~\"].[\"~~~\"].[\"~\"]"));
    assertEquals(Path2.ofJsonPointer("/~/~~~/~~"), Path2.of("$.[\"~\"].[\"~~~\"].[\"~~\"]"));
    assertEquals(Path2.ofJsonPointer("/~/~~~0/~~"), Path2.of("$.[\"~\"].[\"~~~\"].[\"~~\"]"));
    assertEquals(Path2.ofJsonPointer("/~/'.'/~~"), Path2.of("$.[\"~\"].[\"'.'\"].[\"~~\"]"));
  
    // json path escape test
    assertEquals(Path2.ofJsonPointer("/\t"), Path2.of("$.[\"\t\"]"));
    assertEquals(Path2.ofJsonPointer("/\u0074"), Path2.of("$.[\"\u0074\"]"));
    assertEquals(Path2.ofJsonPointer("/'"), Path2.of("$.[\"'\"]"));
    assertEquals(Path2.ofJsonPointer("/\'"), Path2.of("$.[\"\'\"]"));
    assertEquals(Path2.ofJsonPointer("/\""), Path2.of("$.[\"\"\"]"));
    assertEquals(Path2.ofJsonPointer("/\n"), Path2.of("$.[\"\n\"]"));
    assertEquals(Path2.ofJsonPointer("/\\"), Path2.of("$.[\"\\\"]"));
  }
}
