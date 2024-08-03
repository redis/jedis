package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class SetParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        SetParams firstParam = getDefaultValue();
        SetParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        SetParams firstParam = getDefaultValue();
        SetParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        SetParams firstParam = getDefaultValue();
        firstParam.nx();
        SetParams secondParam = getDefaultValue();
        secondParam.xx();
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        SetParams firstParam = getDefaultValue();
        firstParam.nx();
        SetParams secondParam = getDefaultValue();
        secondParam.xx();
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        SetParams firstParam = getDefaultValue();
        SetParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private SetParams getDefaultValue() {
        return new SetParams();
    }
}
