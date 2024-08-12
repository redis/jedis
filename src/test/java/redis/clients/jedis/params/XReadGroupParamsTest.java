package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

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
    public void checkEqualsWithNull() {
        XReadGroupParams firstParam = getDefaultValue();
        XReadGroupParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private XReadGroupParams getDefaultValue() {
        return new XReadGroupParams();
    }
}
