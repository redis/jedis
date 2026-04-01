package redis.clients.jedis.json;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.util.SafeEncoder;

import static org.junit.jupiter.api.Assertions.*;

public class JsonSetParamsTest {

    @Test
    public void testNxAndXxMutuallyExclusive() {
        // NX and XX should be mutually exclusive
        JsonSetParams params1 = new JsonSetParams().nx().xx();
        CommandArguments args1 = new CommandArguments(Protocol.Command.SET);
        params1.addParams(args1);
        assertTrue(containsArgument(args1, "XX"));
        assertFalse(containsArgument(args1, "NX"));

        JsonSetParams params2 = new JsonSetParams().xx().nx();
        CommandArguments args2 = new CommandArguments(Protocol.Command.SET);
        params2.addParams(args2);
        assertTrue(containsArgument(args2, "NX"));
        assertFalse(containsArgument(args2, "XX"));
    }

    @Test
    public void testFphaTypes() {
        // Test that each fpha type is correctly added
        assertFphaType(new JsonSetParams().fp16(), "FP16");
        assertFphaType(new JsonSetParams().bf16(), "BF16");
        assertFphaType(new JsonSetParams().fp32(), "FP32");
        assertFphaType(new JsonSetParams().fp64(), "FP64");
    }

    @Test
    public void testFphaOverride() {
        // Setting another fpha type should override the previous one (last wins)
        JsonSetParams params = new JsonSetParams().fp16().fp32();
        CommandArguments args = new CommandArguments(Protocol.Command.SET);
        params.addParams(args);

        assertTrue(containsArgument(args, "FP32"));
        assertFalse(containsArgument(args, "FP16"));
    }

    @Test
    public void testCombinedParams() {
        // Test combining NX/XX with fpha types
        JsonSetParams params = new JsonSetParams().nx().fp16();
        CommandArguments args = new CommandArguments(Protocol.Command.SET);
        params.addParams(args);

        assertTrue(containsArgument(args, "NX"));
        assertTrue(containsArgument(args, "FP16"));
    }

    @Test
    public void testEmptyParams() {
        JsonSetParams params = new JsonSetParams();
        CommandArguments args = new CommandArguments(Protocol.Command.SET);
        params.addParams(args);

        // Should not contain any optional parameters
        assertFalse(containsArgument(args, "NX"));
        assertFalse(containsArgument(args, "XX"));
        assertFalse(containsArgument(args, "FP16"));
        assertFalse(containsArgument(args, "BF16"));
        assertFalse(containsArgument(args, "FP32"));
        assertFalse(containsArgument(args, "FP64"));
    }

    /**
     * Helper method to assert a specific fpha type
     */
    private void assertFphaType(JsonSetParams params, String expectedType) {
        CommandArguments args = new CommandArguments(Protocol.Command.SET);
        params.addParams(args);
        assertTrue(containsArgument(args, expectedType));
    }

    /**
     * Helper method to check if CommandArguments contains a specific string argument
     */
    private boolean containsArgument(CommandArguments args, String expected) {
        for (Rawable arg : args) {
            String argString = SafeEncoder.encode(arg.getRaw());
            if (expected.equals(argString)) {
                return true;
            }
        }
        return false;
    }
}

