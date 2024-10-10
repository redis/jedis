package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class MigrateParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        MigrateParams firstParam = getDefaultValue();
        MigrateParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        MigrateParams firstParam = getDefaultValue();
        MigrateParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        MigrateParams firstParam = getDefaultValue();
        firstParam.auth("123");
        MigrateParams secondParam = getDefaultValue();
        secondParam.auth("234");
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        MigrateParams firstParam = getDefaultValue();
        firstParam.auth("123");
        MigrateParams secondParam = getDefaultValue();
        secondParam.auth("234");
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        MigrateParams firstParam = getDefaultValue();
        MigrateParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private MigrateParams getDefaultValue() {
        return new MigrateParams();
    }
}
