package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class ShutdownParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        ShutdownParams firstParam = getDefaultValue();
        ShutdownParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        ShutdownParams firstParam = getDefaultValue();
        ShutdownParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        ShutdownParams firstParam = getDefaultValue();
        firstParam.force();
        ShutdownParams secondParam = getDefaultValue();
        secondParam.nosave();
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        ShutdownParams firstParam = getDefaultValue();
        firstParam.force();
        ShutdownParams secondParam = getDefaultValue();
        secondParam.nosave();
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        ShutdownParams firstParam = getDefaultValue();
        ShutdownParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private ShutdownParams getDefaultValue() {
        return new ShutdownParams();
    }
}
