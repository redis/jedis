package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class ScanParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        ScanParams firstParam = getDefaultValue();
        ScanParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        ScanParams firstParam = getDefaultValue();
        ScanParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        ScanParams firstParam = getDefaultValue();
        firstParam.count(15);
        ScanParams secondParam = getDefaultValue();
        secondParam.count(16);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        ScanParams firstParam = getDefaultValue();
        firstParam.count(15);
        ScanParams secondParam = getDefaultValue();
        secondParam.count(16);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        ScanParams firstParam = getDefaultValue();
        ScanParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private ScanParams getDefaultValue() {
        return new ScanParams();
    }
}
