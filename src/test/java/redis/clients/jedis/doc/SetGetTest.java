// PAGES: Set, Get
// HIDE_START
package redis.clients.jedis.doc;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import redis.clients.jedis.commands.jedis.JedisCommandsTestBase;

public class SetGetTest extends JedisCommandsTestBase {

    @Test
    public void setGetTest() {

        // HIDE_END
        String status = jedis.set("bike:1", "Process 134");

        if ("OK".equals(status))
            System.out.println("Successfully set the key.");

        String value = jedis.get("bike:1");

        if ( value != null)
            System.out.println("The value is: " + value  + ".");

        // OUTPUT:
        // Successfully set the key.
        // The value is: Process 134.
        assertEquals("OK", status);
        assertEquals("Process 134", value);
    }
}
