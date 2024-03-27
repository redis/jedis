package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class ZAddParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        ZAddParams firstParam = getDefaultValue();
        ZAddParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        ZAddParams firstParam = getDefaultValue();
        ZAddParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        ZAddParams firstParam = getDefaultValue();
        firstParam.nx();
        ZAddParams secondParam = getDefaultValue();
        secondParam.xx();
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        ZAddParams firstParam = getDefaultValue();
        firstParam.nx();
        ZAddParams secondParam = getDefaultValue();
        secondParam.xx();
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        ZAddParams firstParam = getDefaultValue();
        ZAddParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private ZAddParams getDefaultValue() {
        return new ZAddParams();
    }
}
