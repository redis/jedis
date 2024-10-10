package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class ModuleLoadExParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        ModuleLoadExParams firstParam = getDefaultValue();
        ModuleLoadExParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        ModuleLoadExParams firstParam = getDefaultValue();
        ModuleLoadExParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        ModuleLoadExParams firstParam = getDefaultValue();
        firstParam.arg("123");
        ModuleLoadExParams secondParam = getDefaultValue();
        secondParam.arg("234");
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        ModuleLoadExParams firstParam = getDefaultValue();
        firstParam.arg("123");
        ModuleLoadExParams secondParam = getDefaultValue();
        secondParam.arg("234");
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        ModuleLoadExParams firstParam = getDefaultValue();
        ModuleLoadExParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private ModuleLoadExParams getDefaultValue() {
        return new ModuleLoadExParams();
    }
}
