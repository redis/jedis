package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class ZParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        ZParams firstParam = getDefaultValue();
        ZParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        ZParams firstParam = getDefaultValue();
        ZParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        ZParams firstParam = getDefaultValue();
        firstParam.aggregate(ZParams.Aggregate.MIN);
        ZParams secondParam = getDefaultValue();
        secondParam.aggregate(ZParams.Aggregate.MAX);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        ZParams firstParam = getDefaultValue();
        firstParam.aggregate(ZParams.Aggregate.MIN);
        ZParams secondParam = getDefaultValue();
        secondParam.aggregate(ZParams.Aggregate.MAX);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        ZParams firstParam = getDefaultValue();
        ZParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private ZParams getDefaultValue() {
        return new ZParams();
    }
}
