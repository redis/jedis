package redis.clients.jedis.timeseries;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.util.KeyValue;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Under a Turkish/Azeri default locale, {@code "first".toUpperCase()} yields the dotted
 * {@code "FİRST"}, so {@code DuplicatePolicy.valueOf(...)} throws and the parsed policy is silently
 * dropped. Parsing the TS.INFO reply must not depend on the JVM default locale.
 * <p>
 * Mutates the JVM default locale; must not run concurrently with other tests.
 */
public class TSInfoLocaleTest {

  private Locale previousLocale;

  @BeforeEach
  public void setTurkishLocale() {
    previousLocale = Locale.getDefault();
    Locale.setDefault(new Locale("tr", "TR"));
  }

  @AfterEach
  public void restoreLocale() {
    Locale.setDefault(previousLocale);
  }

  @Test
  public void duplicatePolicyParsingIsLocaleIndependent() {
    List<Object> reply = new ArrayList<>();
    reply.add(SafeEncoder.encode("duplicatePolicy"));
    reply.add(SafeEncoder.encode("first"));

    TSInfo info = TSInfo.TIMESERIES_INFO.build(reply);
    assertEquals(DuplicatePolicy.FIRST, info.getProperty("duplicatePolicy"));
  }

  @Test
  public void duplicatePolicyParsingIsLocaleIndependentResp3() {
    List<KeyValue> reply = new ArrayList<>();
    reply.add(new KeyValue(SafeEncoder.encode("duplicatePolicy"), SafeEncoder.encode("first")));

    TSInfo info = TSInfo.TIMESERIES_INFO_RESP3.build(reply);
    assertEquals(DuplicatePolicy.FIRST, info.getProperty("duplicatePolicy"));
  }
}