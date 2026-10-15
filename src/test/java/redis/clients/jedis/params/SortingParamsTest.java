package redis.clients.jedis.params;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.util.SafeEncoder;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    public void checkEqualsPatternParams() {
        SortingParams firstParam = getDefaultValue().by("weight_*").get("object_*");
        SortingParams secondParam = getDefaultValue().by("weight_*").get("object_*");
        assertEquals(firstParam, secondParam);
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsBinaryPatternParams() {
        SortingParams firstParam = getDefaultValue().by(SafeEncoder.encode("weight_*"));
        SortingParams secondParam = getDefaultValue().by(SafeEncoder.encode("weight_*"));
        assertEquals(firstParam, secondParam);
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsDifferentPatternParams() {
        SortingParams firstParam = getDefaultValue().by("weight_*");
        SortingParams secondParam = getDefaultValue().by("score_*");
        assertNotEquals(firstParam, secondParam);
    }

    private SortingParams getDefaultValue() {
        return new SortingParams();
    }
}
