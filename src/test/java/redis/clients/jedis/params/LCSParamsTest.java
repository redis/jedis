package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class LCSParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        LCSParams firstParam = getDefaultValue();
        LCSParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        LCSParams firstParam = getDefaultValue();
        LCSParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        LCSParams firstParam = getDefaultValue();
        firstParam.idx();
        LCSParams secondParam = getDefaultValue();
        secondParam.len();
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        LCSParams firstParam = getDefaultValue();
        firstParam.idx();
        LCSParams secondParam = getDefaultValue();
        secondParam.len();
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        LCSParams firstParam = getDefaultValue();
        LCSParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private LCSParams getDefaultValue() {
        return new LCSParams();
    }
}
