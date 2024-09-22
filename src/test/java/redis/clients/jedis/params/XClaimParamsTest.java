package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class XClaimParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        XClaimParams firstParam = getDefaultValue();
        XClaimParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        XClaimParams firstParam = getDefaultValue();
        XClaimParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        XClaimParams firstParam = getDefaultValue();
        firstParam.time(20);
        XClaimParams secondParam = getDefaultValue();
        secondParam.time(21);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        XClaimParams firstParam = getDefaultValue();
        firstParam.time(20);
        XClaimParams secondParam = getDefaultValue();
        secondParam.time(21);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        XClaimParams firstParam = getDefaultValue();
        XClaimParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private XClaimParams getDefaultValue() {
        return new XClaimParams();
    }
}
