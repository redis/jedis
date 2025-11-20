// EXAMPLE: set_and_get
// HIDE_START
package io.redis.examples;

import redis.clients.jedis.RedisClient;

// REMOVE_START
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
// REMOVE_END

public class SetGetExample {

  @Test
  public void run() {

    RedisClient jedis = new RedisClient("redis://localhost:6379");
    // HIDE_END

    String status = jedis.set("bike:1", "Process 134");

    if ("OK".equals(status)) System.out.println("Successfully added a bike.");

    String value = jedis.get("bike:1");

    if (value != null) System.out.println("The name of the bike is: " + value + ".");

    // REMOVE_START
    assertEquals("OK", status);
    assertEquals("Process 134", value);
    // REMOVE_END

// HIDE_START
    jedis.close();
  }
}
// HIDE_END
