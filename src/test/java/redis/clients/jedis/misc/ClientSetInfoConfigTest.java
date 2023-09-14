package redis.clients.jedis.misc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import org.junit.Test;

import redis.clients.jedis.ClientSetInfoConfig;
import redis.clients.jedis.exceptions.JedisException;

public class ClientSetInfoConfigTest {

  @Test
  public void replaceSpacesWithHyphens() {
    assertEquals("Redis-Java-client",
        ClientSetInfoConfig.withLibNameSuffix("Redis Java client").getLibNameSuffix());
  }

  @Test
  public void errorForBraces() {
    Arrays.asList('(', ')', '[', ']', '{', '}')
        .forEach(brace -> assertThrows(JedisException.class,
            () -> ClientSetInfoConfig.withLibNameSuffix("" + brace)));
  }
}
