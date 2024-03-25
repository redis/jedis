package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

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

    private ZRangeParams getDefaultValue() {
        return new ZRangeParams(0, 0);
    }
}
