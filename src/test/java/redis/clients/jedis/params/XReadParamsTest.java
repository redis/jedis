package redis.clients.jedis.params;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.RawableFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArgumentCount;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArguments;

public class XReadParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        XReadParams firstParam = getDefaultValue();
        XReadParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        XReadParams firstParam = getDefaultValue();
        XReadParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        XReadParams firstParam = getDefaultValue();
        firstParam.block(14);
        XReadParams secondParam = getDefaultValue();
        secondParam.block(15);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        XReadParams firstParam = getDefaultValue();
        firstParam.block(14);
        XReadParams secondParam = getDefaultValue();
        secondParam.block(15);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousMaxCount() {
        XReadParams firstParam = getDefaultValue();
        firstParam.maxCount(80);
        XReadParams secondParam = getDefaultValue();
        secondParam.maxCount(81);
        assertFalse(firstParam.equals(secondParam));
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousMaxSize() {
        XReadParams firstParam = getDefaultValue();
        firstParam.maxSize(65536);
        XReadParams secondParam = getDefaultValue();
        secondParam.maxSize(65537);
        assertFalse(firstParam.equals(secondParam));
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        XReadParams firstParam = getDefaultValue();
        XReadParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private XReadParams getDefaultValue() {
        return new XReadParams();
    }

    @Nested
    class AddParamsTests {

        @Test
        public void allOptionsInCanonicalOrder() {
            CommandArguments args = new CommandArguments(Protocol.Command.XREAD);
            XReadParams.xReadParams().count(50).maxCount(80).maxSize(65536).block(5000)
                    .addParams(args);
            assertThat(args, hasArguments(Protocol.Command.XREAD, //
                Keyword.COUNT, RawableFactory.from(50), //
                Keyword.MAXCOUNT, RawableFactory.from(80), //
                Keyword.MAXSIZE, RawableFactory.from(65536L), //
                Keyword.BLOCK, RawableFactory.from(5000)));
        }

        @Test
        public void unsetOptionsAreOmitted() {
            CommandArguments args = new CommandArguments(Protocol.Command.XREAD);
            XReadParams.xReadParams().addParams(args);
            assertThat(args, hasArgumentCount(1)); // just the command
        }

        @Test
        public void maxCountWithoutMaxSize() {
            CommandArguments args = new CommandArguments(Protocol.Command.XREAD);
            XReadParams.xReadParams().maxCount(3).addParams(args);
            assertThat(args, hasArguments(Protocol.Command.XREAD, //
                Keyword.MAXCOUNT, RawableFactory.from(3)));
        }

        @Test
        public void maxSizeWithoutMaxCount() {
            CommandArguments args = new CommandArguments(Protocol.Command.XREAD);
            XReadParams.xReadParams().count(2).maxSize(1024).addParams(args);
            assertThat(args, hasArguments(Protocol.Command.XREAD, //
                Keyword.COUNT, RawableFactory.from(2), //
                Keyword.MAXSIZE, RawableFactory.from(1024L)));
        }
    }
}
