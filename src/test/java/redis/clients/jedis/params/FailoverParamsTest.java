package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class FailoverParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        FailoverParams firstParam = getDefaultValue();
        FailoverParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        FailoverParams firstParam = getDefaultValue();
        FailoverParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        FailoverParams firstParam = getDefaultValue();
        firstParam.timeout(15);
        FailoverParams secondParam = getDefaultValue();
        secondParam.timeout(20);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        FailoverParams firstParam = getDefaultValue();
        firstParam.timeout(15);
        FailoverParams secondParam = getDefaultValue();
        secondParam.timeout(20);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        FailoverParams firstParam = getDefaultValue();
        FailoverParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private FailoverParams getDefaultValue() {
        return new FailoverParams();
    }
}
