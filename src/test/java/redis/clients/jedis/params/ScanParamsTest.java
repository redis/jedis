package redis.clients.jedis.params;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.util.SafeEncoder;

import static org.junit.jupiter.api.Assertions.*;

public class ScanParamsTest {

  @Test
  public void matchPreservesNonAsciiPatternAcrossCharsets() {
    Charset original = SafeEncoder.DEFAULT_CHARSET;
    // Force encode/decode charsets to differ so a default-charset decode is observable.
    SafeEncoder.DEFAULT_CHARSET = StandardCharsets.UTF_8.equals(Charset.defaultCharset())
        ? StandardCharsets.ISO_8859_1
        : StandardCharsets.UTF_8;
    try {
      String pattern = "{café}*";
      ScanParams params = new ScanParams().match(pattern);

      assertEquals(pattern, params.match());
      assertArrayEquals(params.binaryMatch(), SafeEncoder.encode(params.match()));
    } finally {
      SafeEncoder.DEFAULT_CHARSET = original;
    }
  }

    @Test
    public void checkEqualsIdenticalParams() {
        ScanParams firstParam = getDefaultValue();
        ScanParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        ScanParams firstParam = getDefaultValue();
        ScanParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        ScanParams firstParam = getDefaultValue();
        firstParam.count(15);
        ScanParams secondParam = getDefaultValue();
        secondParam.count(16);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        ScanParams firstParam = getDefaultValue();
        firstParam.count(15);
        ScanParams secondParam = getDefaultValue();
        secondParam.count(16);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        ScanParams firstParam = getDefaultValue();
        ScanParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private ScanParams getDefaultValue() {
        return new ScanParams();
    }
}
