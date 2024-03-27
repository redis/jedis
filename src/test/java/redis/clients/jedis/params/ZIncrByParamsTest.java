package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class ZIncrByParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        ZIncrByParams firstParam = getDefaultValue();
        ZIncrByParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        ZIncrByParams firstParam = getDefaultValue();
        ZIncrByParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        ZIncrByParams firstParam = getDefaultValue();
        firstParam.nx();
        ZIncrByParams secondParam = getDefaultValue();
        secondParam.xx();
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        ZIncrByParams firstParam = getDefaultValue();
        firstParam.nx();
        ZIncrByParams secondParam = getDefaultValue();
        secondParam.xx();
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        ZIncrByParams firstParam = getDefaultValue();
        ZIncrByParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private ZIncrByParams getDefaultValue() {
        return new ZIncrByParams();
    }
}
