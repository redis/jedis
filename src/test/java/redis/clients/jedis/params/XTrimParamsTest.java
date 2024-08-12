package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class XTrimParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        XTrimParams firstParam = getDefaultValue();
        XTrimParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        XTrimParams firstParam = getDefaultValue();
        XTrimParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        XTrimParams firstParam = getDefaultValue();
        firstParam.maxLen(15);
        XTrimParams secondParam = getDefaultValue();
        secondParam.maxLen(16);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        XTrimParams firstParam = getDefaultValue();
        firstParam.maxLen(15);
        XTrimParams secondParam = getDefaultValue();
        secondParam.maxLen(16);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        XTrimParams firstParam = getDefaultValue();
        XTrimParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private XTrimParams getDefaultValue() {
        return new XTrimParams();
    }
}
