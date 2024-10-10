package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class RestoreParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        RestoreParams firstParam = getDefaultValue();
        RestoreParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        RestoreParams firstParam = getDefaultValue();
        RestoreParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        RestoreParams firstParam = getDefaultValue();
        firstParam.idleTime(14);
        RestoreParams secondParam = getDefaultValue();
        secondParam.idleTime(15);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        RestoreParams firstParam = getDefaultValue();
        firstParam.idleTime(14);
        RestoreParams secondParam = getDefaultValue();
        secondParam.idleTime(15);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        RestoreParams firstParam = getDefaultValue();
        RestoreParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private RestoreParams getDefaultValue() {
        return new RestoreParams();
    }
}
