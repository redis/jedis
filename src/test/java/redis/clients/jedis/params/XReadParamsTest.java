package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class XReadParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        XReadParams firstParam = getDefaultValue();
        XReadParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        XReadParams firstParam = getDefaultValue();
        XReadParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        XReadParams firstParam = getDefaultValue();
        firstParam.block(14);
        XReadParams secondParam = getDefaultValue();
        secondParam.block(15);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        XReadParams firstParam = getDefaultValue();
        firstParam.block(14);
        XReadParams secondParam = getDefaultValue();
        secondParam.block(15);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        XReadParams firstParam = getDefaultValue();
        XReadParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private XReadParams getDefaultValue() {
        return new XReadParams();
    }
}
