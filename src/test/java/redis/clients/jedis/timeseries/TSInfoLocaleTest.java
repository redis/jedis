package redis.clients.jedis.timeseries;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.util.SafeEncoder;

public class TSInfoLocaleTest {

  /**
   * Under a Turkish/Azeri default locale, {@code "first".toUpperCase()} yields the dotted
   * {@code "FİRST"}, so {@code DuplicatePolicy.valueOf(...)} throws and the parsed policy is
   * silently dropped. Parsing the TS.INFO reply must not depend on the JVM default locale.
   */
  @Test
  public void duplicatePolicyParsedUnderTurkishLocale() {
    Locale previous = Locale.getDefault();
    try {
      Locale.setDefault(new Locale("tr", "TR"));

      List<Object> reply = new ArrayList<>();
      reply.add(SafeEncoder.encode("duplicatePolicy"));
      reply.add(SafeEncoder.encode("first"));

      TSInfo info = TSInfo.TIMESERIES_INFO.build(reply);
      assertEquals(DuplicatePolicy.FIRST, info.getProperty("duplicatePolicy"));
    } finally {
      Locale.setDefault(previous);
    }
  }
}
