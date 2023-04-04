package redis.clients.jedis.doc;
import org.junit.Test;


/**
 * Run documentation example tests
 */
public class SimpleCommandsTestSuite {

    @Test
    public void runDocTests() {
        new SetGetExample().run();
    }
}
