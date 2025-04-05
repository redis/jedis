package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class SortingParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        SortingParams firstParam = getDefaultValue();
        SortingParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        SortingParams firstParam = getDefaultValue();
        SortingParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        SortingParams firstParam = getDefaultValue();
        firstParam.limit(15, 20);
        SortingParams secondParam = getDefaultValue();
        secondParam.limit(10, 15);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        SortingParams firstParam = getDefaultValue();
        firstParam.limit(15, 20);
        SortingParams secondParam = getDefaultValue();
        secondParam.limit(10, 15);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        SortingParams firstParam = getDefaultValue();
        SortingParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private SortingParams getDefaultValue() {
        return new SortingParams();
    }
}
