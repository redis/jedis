package redis.clients.jedis.params;

import org.junit.Test;
import redis.clients.jedis.args.ClientType;

import static org.junit.Assert.*;

public class ClientKillParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        ClientKillParams firstParam = getDefaultValue();
        ClientKillParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        ClientKillParams firstParam = getDefaultValue();
        ClientKillParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }
    
    @Test
    public void checkEqualsVariousParams() {
        ClientKillParams firstParam = getDefaultValue();
        firstParam.type(ClientType.NORMAL);
        ClientKillParams secondParam = getDefaultValue();
        secondParam.skipMe(ClientKillParams.SkipMe.NO);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        ClientKillParams firstParam = getDefaultValue();
        firstParam.type(ClientType.NORMAL);
        ClientKillParams secondParam = getDefaultValue();
        secondParam.skipMe(ClientKillParams.SkipMe.NO);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        ClientKillParams firstParam = getDefaultValue();
        ClientKillParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    private ClientKillParams getDefaultValue() {
        return new ClientKillParams();
    }
}
