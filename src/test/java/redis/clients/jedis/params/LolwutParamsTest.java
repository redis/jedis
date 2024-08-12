package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class LolwutParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        LolwutParams firstParam = getDefaultValue();
        LolwutParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        LolwutParams firstParam = getDefaultValue();
        LolwutParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        LolwutParams firstParam = getDefaultValue();
        firstParam.version(1);
        LolwutParams secondParam = getDefaultValue();
        secondParam.version(2);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        LolwutParams firstParam = getDefaultValue();
        firstParam.version(1);
        LolwutParams secondParam = getDefaultValue();
        secondParam.version(2);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        LolwutParams firstParam = getDefaultValue();
        LolwutParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private LolwutParams getDefaultValue() {
        return new LolwutParams();
    }
}
