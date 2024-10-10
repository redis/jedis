package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class XAutoClaimParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        XAutoClaimParams firstParam = getDefaultValue();
        XAutoClaimParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        XAutoClaimParams firstParam = getDefaultValue();
        XAutoClaimParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        XAutoClaimParams firstParam = getDefaultValue();
        firstParam.count(15);
        XAutoClaimParams secondParam = getDefaultValue();
        secondParam.count(20);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        XAutoClaimParams firstParam = getDefaultValue();
        firstParam.count(15);
        XAutoClaimParams secondParam = getDefaultValue();
        secondParam.count(20);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        XAutoClaimParams firstParam = getDefaultValue();
        XAutoClaimParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private XAutoClaimParams getDefaultValue() {
        return new XAutoClaimParams();
    }
}
