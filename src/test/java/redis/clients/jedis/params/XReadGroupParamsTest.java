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

public class XReadGroupParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        XReadGroupParams firstParam = getDefaultValue();
        XReadGroupParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        XReadGroupParams firstParam = getDefaultValue();
        XReadGroupParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        XReadGroupParams firstParam = getDefaultValue();
        firstParam.block(14);
        XReadGroupParams secondParam = getDefaultValue();
        secondParam.block(15);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        XReadGroupParams firstParam = getDefaultValue();
        firstParam.block(14);
        XReadGroupParams secondParam = getDefaultValue();
        secondParam.block(15);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousMaxCount() {
        XReadGroupParams firstParam = getDefaultValue();
        firstParam.maxCount(80);
        XReadGroupParams secondParam = getDefaultValue();
        secondParam.maxCount(81);
        assertFalse(firstParam.equals(secondParam));
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousMaxSize() {
        XReadGroupParams firstParam = getDefaultValue();
        firstParam.maxSize(65536);
        XReadGroupParams secondParam = getDefaultValue();
        secondParam.maxSize(65537);
        assertFalse(firstParam.equals(secondParam));
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        XReadGroupParams firstParam = getDefaultValue();
        XReadGroupParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private XReadGroupParams getDefaultValue() {
        return new XReadGroupParams();
    }

    @Nested
    class AddParamsTests {

        @Test
        public void allOptionsWithNewTokensAfterCount() {
            CommandArguments args = new CommandArguments(Protocol.Command.XREADGROUP);
            XReadGroupParams.xReadGroupParams().count(50).maxCount(80).maxSize(65536).block(5000)
                    .noAck().claim(10000).addParams(args);
            assertThat(args, hasArguments(Protocol.Command.XREADGROUP, //
                Keyword.COUNT, RawableFactory.from(50), //
                Keyword.MAXCOUNT, RawableFactory.from(80), //
                Keyword.MAXSIZE, RawableFactory.from(65536L), //
                Keyword.BLOCK, RawableFactory.from(5000), //
                Keyword.NOACK, //
                Keyword.CLAIM, RawableFactory.from(10000L)));
        }

        @Test
        public void unsetOptionsAreOmitted() {
            CommandArguments args = new CommandArguments(Protocol.Command.XREADGROUP);
            XReadGroupParams.xReadGroupParams().addParams(args);
            assertThat(args, hasArgumentCount(1)); // just the command
        }

        @Test
        public void maxCountAndMaxSizeOnly() {
            CommandArguments args = new CommandArguments(Protocol.Command.XREADGROUP);
            XReadGroupParams.xReadGroupParams().maxCount(3).maxSize(1024).addParams(args);
            assertThat(args, hasArguments(Protocol.Command.XREADGROUP, //
                Keyword.MAXCOUNT, RawableFactory.from(3), //
                Keyword.MAXSIZE, RawableFactory.from(1024L)));
        }
    }
}
