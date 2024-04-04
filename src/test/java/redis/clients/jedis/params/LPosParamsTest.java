package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class LPosParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        LPosParams firstParam = getDefaultValue();
        LPosParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        LPosParams firstParam = getDefaultValue();
        LPosParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        LPosParams firstParam = getDefaultValue();
        firstParam.rank(1);
        LPosParams secondParam = getDefaultValue();
        secondParam.rank(2);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        LPosParams firstParam = getDefaultValue();
        firstParam.rank(1);
        LPosParams secondParam = getDefaultValue();
        secondParam.rank(2);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        LPosParams firstParam = getDefaultValue();
        LPosParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private LPosParams getDefaultValue() {
        return new LPosParams();
    }
}
