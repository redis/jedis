package redis.clients.jedis.params;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.args.Rawable;
import redis.clients.jedis.args.RawableFactory;
import redis.clients.jedis.util.CommandArgumentsMatchers;
import redis.clients.jedis.util.ProtocolTestUtil;

import java.io.IOException;
import java.util.Iterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static redis.clients.jedis.util.CommandArgumentsMatchers.*;

public class ZRangeParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        ZRangeParams firstParam = getDefaultValue();
        ZRangeParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        ZRangeParams firstParam = getDefaultValue();
        ZRangeParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        ZRangeParams firstParam = getDefaultValue();
        firstParam.limit(15, 20);
        ZRangeParams secondParam = getDefaultValue();
        secondParam.limit(16, 21);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        ZRangeParams firstParam = getDefaultValue();
        firstParam.limit(15, 20);
        ZRangeParams secondParam = getDefaultValue();
        secondParam.limit(16, 21);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        ZRangeParams firstParam = getDefaultValue();
        ZRangeParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    @Nested
    class BuilderTests {

        @Test
        public void testZrangeParamsIntMinMax() {
            int min = 0;
            int max = 1;
            ZRangeParams params = ZRangeParams.zrangeParams(min, max);
            CommandArguments args = new CommandArguments(Protocol.Command.ZRANGE);
            params.addParams(args);

            assertThat(args, hasArgumentCount(3));

            assertThat(args, hasArguments(
                Protocol.Command.ZRANGE,
                RawableFactory.from(min),
                RawableFactory.from(max)
            ));
        }

        //Test that long values are serialized as long (not double) to avoid precision loss
        @Test
        public void testZrangeParamsLongMinMax() {
            long min = Integer.MAX_VALUE + 1L;
            long max = Integer.MAX_VALUE + 2L;

            ZRangeParams params = ZRangeParams.zrangeParams(min, max);
            CommandArguments args = new CommandArguments(Protocol.Command.ZRANGE);
            params.addParams(args);

            assertThat(args, hasArgumentCount(3));

            assertThat(args, hasArguments(
                Protocol.Command.ZRANGE,
                RawableFactory.from(min),
                RawableFactory.from(max)
            ));
        }

        //Test that int factory method delegates to long constructor and produces the same Rawable encoding
        @Test
        public void testZrangeParamsIntMinMaxDelegatesToLong() {
            int min = 100;
            int max = 200;
            ZRangeParams intParams = ZRangeParams.zrangeParams(min, max);
            ZRangeParams longParams = ZRangeParams.zrangeParams((long) min, (long) max);
            CommandArguments intArgs = new CommandArguments(Protocol.Command.ZRANGE);
            intParams.addParams(intArgs);
            CommandArguments longArgs = new CommandArguments(Protocol.Command.ZRANGE);
            longParams.addParams(longArgs);

            // Both should produce identical arguments
            assertEquals(intArgs.size(), longArgs.size());
            Iterator<Rawable> intIter = intArgs.iterator();
            Iterator<Rawable> longIter = longArgs.iterator();
            while (intIter.hasNext() && longIter.hasNext()) {
                assertEquals(intIter.next(), longIter.next());
            }
        }

        // Test that zrangeParams(double, double) produces BYSCORE args
        @Test
        public void testZrangeParamsByScore() {
            double min = 0.1;
            double max = 1.1;
            ZRangeParams params = ZRangeParams.zrangeByScoreParams(min, max);
            CommandArguments args = new CommandArguments(Protocol.Command.ZRANGE);
            params.addParams(args);

            assertThat(args, hasArgumentCount(4));
            assertThat(args, hasArguments(
                Protocol.Command.ZRANGE,
                RawableFactory.from(min),
                RawableFactory.from(max),
                Protocol.Keyword.BYSCORE
            ));
        }

        // Test the actual RESP protocol output
        @Test
        public void testProtocolOutputWithLongValues() throws IOException {
            long largeStart = 3_000_000_000L;
            long largeEnd = 3_000_000_099L;

            ZRangeParams params = ZRangeParams.zrangeParams(largeStart, largeEnd);
            CommandArguments args = new CommandArguments(Protocol.Command.ZRANGE);
            params.addParams(args);

            // Capture the RESP protocol output
            String respOutput = ProtocolTestUtil.captureCommandOutput(args);

            // RESP protocol uses CRLF (\r\n) line endings
            String expected = "*3\r\n" + "$6\r\n" + "ZRANGE\r\n" + "$10\r\n" + "3000000000\r\n" + "$10\r\n" + "3000000099\r\n";
            assertEquals(expected, respOutput);
        }

    }

    private ZRangeParams getDefaultValue() {
        return new ZRangeParams(0, 0);
    }
}
