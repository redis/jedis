package redis.clients.jedis.modules.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import redis.clients.jedis.json.Path;

public class PathTest {

  @Test
  public void testRootPathConstant() {
    assertEquals(".", Path.ROOT_PATH.toString());
  }

  @Test
  public void testStaticFactoryMethod() {
    assertEquals(".a.b", Path.of(".a.b").toString());
  }

  @Test
  public void testPathEquals() {
    assertEquals(Path.of(".a.b.c"), Path.of(".a.b.c"));
    assertNotEquals(Path.of(".a.b.c"), Path.of(".b.c"));
    assertNotEquals(Path.of(".a.b.c"), null);
    assertNotEquals(Path.of(".a.b.c"), ".a.b.c");
    Path aPath = Path.of(".a");
    assertEquals(aPath, aPath);
  }

  @Test
  public void testPathHashCode() {
    assertEquals(Path.of(".a.b.c").hashCode(), Path.of(".a.b.c").hashCode());
    assertNotEquals(Path.of(".a.b.c").hashCode(), Path.of(".b.c").hashCode());
  }
  
  @Test
  public void testJsonPointer() {
    assertEquals(Path.ofJsonPointer(""), Path.ROOT_PATH);
    assertEquals(Path.ofJsonPointer("/"), Path.of(".[\"\"]"));
    assertEquals(Path.ofJsonPointer("//0"), Path.of(".[\"\"].[0]"));
    assertEquals(Path.ofJsonPointer("//"), Path.of(".[\"\"].[\"\"]"));
    assertEquals(Path.ofJsonPointer("// "), Path.of(".[\"\"].[\" \"]"));
    assertEquals(Path.ofJsonPointer("/a/b/c"), Path.of(".[\"a\"].[\"b\"].[\"c\"]"));
    assertEquals(Path.ofJsonPointer("/a/0/c"), Path.of(".[\"a\"].[0].[\"c\"]"));
    assertEquals(Path.ofJsonPointer("/a/0b/c"), Path.of(".[\"a\"].[\"0b\"].[\"c\"]"));
    assertEquals(Path.ofJsonPointer("/ab/cd/1010"), Path.of(".[\"ab\"].[\"cd\"].[1010]"));
    assertEquals(Path.ofJsonPointer("/a/b/c").hashCode(), Path.of(".[\"a\"].[\"b\"].[\"c\"]").hashCode());
  
    // escape test
    assertEquals(Path.ofJsonPointer("/a/~0"), Path.of(".[\"a\"].[\"~\"]"));
    assertEquals(Path.ofJsonPointer("/a/~1"), Path.of(".[\"a\"].[\"/\"]"));
    assertEquals(Path.ofJsonPointer("/a/~0/c"), Path.of(".[\"a\"].[\"~\"].[\"c\"]"));
    assertEquals(Path.ofJsonPointer("/a/~1/c"), Path.of(".[\"a\"].[\"/\"].[\"c\"]"));
    assertEquals(Path.ofJsonPointer("/a/~~/c"), Path.of(".[\"a\"].[\"~~\"].[\"c\"]"));
    assertEquals(Path.ofJsonPointer("/~/~~~/~"), Path.of(".[\"~\"].[\"~~~\"].[\"~\"]"));
    assertEquals(Path.ofJsonPointer("/~/~~~/~~"), Path.of(".[\"~\"].[\"~~~\"].[\"~~\"]"));
    assertEquals(Path.ofJsonPointer("/~/~~~0/~~"), Path.of(".[\"~\"].[\"~~~\"].[\"~~\"]"));
    assertEquals(Path.ofJsonPointer("/~/'.'/~~"), Path.of(".[\"~\"].[\"'.'\"].[\"~~\"]"));
  
    // json path escape test
    assertEquals(Path.ofJsonPointer("/\t"), Path.of(".[\"\t\"]"));
    assertEquals(Path.ofJsonPointer("/\u0074"), Path.of(".[\"\u0074\"]"));
    assertEquals(Path.ofJsonPointer("/'"), Path.of(".[\"'\"]"));
    assertEquals(Path.ofJsonPointer("/\'"), Path.of(".[\"\'\"]"));
    assertEquals(Path.ofJsonPointer("/\""), Path.of(".[\"\"\"]"));
    assertEquals(Path.ofJsonPointer("/\n"), Path.of(".[\"\n\"]"));
    assertEquals(Path.ofJsonPointer("/\\"), Path.of(".[\"\\\"]"));
  }
}
