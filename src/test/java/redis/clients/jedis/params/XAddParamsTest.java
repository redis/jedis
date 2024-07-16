package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class XAddParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        XAddParams firstParam = getDefaultValue();
        XAddParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        XAddParams firstParam = getDefaultValue();
        XAddParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        XAddParams firstParam = getDefaultValue();
        firstParam.id(15);
        XAddParams secondParam = getDefaultValue();
        secondParam.id(20);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        XAddParams firstParam = getDefaultValue();
        firstParam.id(15);
        XAddParams secondParam = getDefaultValue();
        secondParam.id(20);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        XAddParams firstParam = getDefaultValue();
        XAddParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private XAddParams getDefaultValue() {
        return new XAddParams();
    }
}
