package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class GetExParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        GetExParams firstParam = getDefaultValue();
        GetExParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        GetExParams firstParam = getDefaultValue();
        GetExParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        GetExParams firstParam = getDefaultValue();
        firstParam.ex(15);
        GetExParams secondParam = getDefaultValue();
        secondParam.px(20);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        GetExParams firstParam = getDefaultValue();
        firstParam.ex(15);
        GetExParams secondParam = getDefaultValue();
        secondParam.px(20);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        GetExParams firstParam = getDefaultValue();
        GetExParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private GetExParams getDefaultValue() {
        return new GetExParams();
    }
}
