package redis.clients.jedis.params;

import org.junit.Test;
import redis.clients.jedis.StreamEntryID;

import static org.junit.Assert.*;

public class XPendingParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        XPendingParams firstParam = getDefaultValue();
        XPendingParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        XPendingParams firstParam = getDefaultValue();
        XPendingParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        XPendingParams firstParam = getDefaultValue();
        firstParam.start(StreamEntryID.XGROUP_LAST_ENTRY);
        XPendingParams secondParam = getDefaultValue();
        secondParam.start(StreamEntryID.NEW_ENTRY);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        XPendingParams firstParam = getDefaultValue();
        firstParam.start(StreamEntryID.XGROUP_LAST_ENTRY);
        XPendingParams secondParam = getDefaultValue();
        secondParam.start(StreamEntryID.NEW_ENTRY);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        XPendingParams firstParam = getDefaultValue();
        XPendingParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private XPendingParams getDefaultValue() {
        return new XPendingParams();
    }
}
