package redis.clients.jedis.params;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GeoAddParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        GeoAddParams firstParam = getDefaultValue();
        GeoAddParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        GeoAddParams firstParam = getDefaultValue();
        GeoAddParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        GeoAddParams firstParam = getDefaultValue();
        firstParam.nx();
        GeoAddParams secondParam = getDefaultValue();
        secondParam.xx();
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        GeoAddParams firstParam = getDefaultValue();
        firstParam.nx();
        GeoAddParams secondParam = getDefaultValue();
        secondParam.xx();
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        GeoAddParams firstParam = getDefaultValue();
        GeoAddParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private GeoAddParams getDefaultValue() {
        return new GeoAddParams();
    }
}
