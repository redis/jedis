package redis.clients.jedis.misc;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.ClientSetInfoConfig;
import redis.clients.jedis.exceptions.JedisValidationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ClientSetInfoConfigTest {

  @Test
  public void replaceSpacesWithHyphens() {
    assertEquals("Redis-Java-client",
        ClientSetInfoConfig.withLibNameSuffix("Redis Java client").getLibNameSuffix());
  }

  @Test
  public void errorForBraces() {
    Arrays.asList('(', ')', '[', ']', '{', '}')
        .forEach(brace -> assertThrows(JedisValidationException.class,
            () -> ClientSetInfoConfig.withLibNameSuffix("" + brace)));
  }
}
