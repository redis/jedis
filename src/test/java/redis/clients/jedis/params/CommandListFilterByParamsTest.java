package redis.clients.jedis.params;

import org.junit.Test;

import static org.junit.Assert.*;

public class CommandListFilterByParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        CommandListFilterByParams firstParam = getDefaultValue();
        CommandListFilterByParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        CommandListFilterByParams firstParam = getDefaultValue();
        CommandListFilterByParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        CommandListFilterByParams firstParam = getDefaultValue();
        firstParam.filterByAclCat("admin");
        CommandListFilterByParams secondParam = getDefaultValue();
        secondParam.filterByModule("JSON");
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        CommandListFilterByParams firstParam = getDefaultValue();
        firstParam.filterByAclCat("admin");
        CommandListFilterByParams secondParam = getDefaultValue();
        secondParam.filterByModule("JSON");
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        CommandListFilterByParams firstParam = getDefaultValue();
        CommandListFilterByParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private CommandListFilterByParams getDefaultValue() {
        return new CommandListFilterByParams();
    }
}
