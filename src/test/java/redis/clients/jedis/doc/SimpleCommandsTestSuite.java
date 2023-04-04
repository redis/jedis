package redis.clients.jedis.doc;
import org.junit.Test;


/**
 * Here are some basic requirements for documentation examples:
 *
 * - The code should be as clean as possible
 * - We should avoid external framework or library references as much as possible
 * - Minimal usage of assertions because the main target is to verify if the interface changed
 * - A developer should be able to execute the example (copy and paste) with the least possible effort
 *
 * This leads to the following structure of a Java example
 * 
 * - Imports refer to Jedis and Sytem packages only
 * - There is a single class that is named based on the example
 * - The class has a single 'run' method that runs the example
 * 
 * The comments '// HIDE_START' and '// HIDE_END' are used to mark code ranges that are by default hidden. 
 * The UI component will allow to unhide those lines of code.
 */
public class SimpleCommandsTestSuite {

    @Test
    public void runDocTests() {
        new SetGetExample().run();
    }
}
