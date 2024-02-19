package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class BitPosParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        BitPosParams firstParam = getDefaultValue();
        BitPosParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        BitPosParams firstParam = getDefaultValue();
        BitPosParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        BitPosParams firstParam = getDefaultValue();
        BitPosParams secondParam = getDefaultValue();
        secondParam.end(15);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        BitPosParams firstParam = getDefaultValue();
        BitPosParams secondParam = getDefaultValue();
        secondParam.start(15);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        BitPosParams firstParam = getDefaultValue();
        BitPosParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private BitPosParams getDefaultValue() {
        return new BitPosParams();
    }
}
