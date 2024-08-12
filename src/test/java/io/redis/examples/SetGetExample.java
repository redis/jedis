// EXAMPLE: set_and_get
// HIDE_START
package io.redis.examples;

import redis.clients.jedis.UnifiedJedis;
// REMOVE_START
import org.junit.Test;
import static org.junit.Assert.assertEquals;

// REMOVE_END

public class SetGetExample {

  @Test
  public void run() {

    UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");

    // HIDE_END
    String status = jedis.set("bike:1", "Process 134");

    if ("OK".equals(status)) System.out.println("Successfully added a bike.");

    String value = jedis.get("bike:1");

    if (value != null) System.out.println("The name of the bike is: " + value + ".");
    // HIDE_START

    // REMOVE_START
    assertEquals("OK", status);
    assertEquals("Process 134", value);
    // REMOVE_END
  }
}
// HIDE_END
