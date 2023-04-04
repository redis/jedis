// HIDE_START
package redis.clients.jedis.doc;
import redis.clients.jedis.Jedis;

public class SetGetExample {

    public void run() {

        Jedis jedis = new Jedis("localhost", 6379);

        // HIDE_END
        String status = jedis.set("bike:1", "Process 134");

        if ("OK".equals(status))
            System.out.println("Successfully added a bike.");

        String value = jedis.get("bike:1");

        if ( value != null)
            System.out.println("The name of the bike is: " + value  + ".");

        // HIDE_START
        // Output:
        // Successfully added a bike.
        // The name of the bike is: Process 134.
        // HIDE_END
    }
}
