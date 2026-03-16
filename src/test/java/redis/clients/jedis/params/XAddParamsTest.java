package redis.clients.jedis.params;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class XAddParamsTest {

    @Test
    public void checkEqualsIdenticalParams() {
        XAddParams firstParam = getDefaultValue();
        XAddParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        XAddParams firstParam = getDefaultValue();
        XAddParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        XAddParams firstParam = getDefaultValue();
        firstParam.id(15);
        XAddParams secondParam = getDefaultValue();
        secondParam.id(20);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        XAddParams firstParam = getDefaultValue();
        firstParam.id(15);
        XAddParams secondParam = getDefaultValue();
        secondParam.id(20);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        XAddParams firstParam = getDefaultValue();
        XAddParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkEqualsIdmpAutoParams() {
        XAddParams firstParam = getDefaultValue();
        firstParam.idmpAuto("producer1");
        XAddParams secondParam = getDefaultValue();
        secondParam.idmpAuto("producer1");
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdmpAutoParams() {
        XAddParams firstParam = getDefaultValue();
        firstParam.idmpAuto("producer1");
        XAddParams secondParam = getDefaultValue();
        secondParam.idmpAuto("producer1");
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousIdmpAutoParams() {
        XAddParams firstParam = getDefaultValue();
        firstParam.idmpAuto("producer1");
        XAddParams secondParam = getDefaultValue();
        secondParam.idmpAuto("producer2");
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkEqualsIdmpParams() {
        XAddParams firstParam = getDefaultValue();
        firstParam.idmp("producer1", "iid1");
        XAddParams secondParam = getDefaultValue();
        secondParam.idmp("producer1", "iid1");
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdmpParams() {
        XAddParams firstParam = getDefaultValue();
        firstParam.idmp("producer1", "iid1");
        XAddParams secondParam = getDefaultValue();
        secondParam.idmp("producer1", "iid1");
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousIdmpParams() {
        XAddParams firstParam = getDefaultValue();
        firstParam.idmp("producer1", "iid1");
        XAddParams secondParam = getDefaultValue();
        secondParam.idmp("producer1", "iid2");
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkEqualsIdmpAutoVsIdmp() {
        XAddParams firstParam = getDefaultValue();
        firstParam.idmpAuto("producer1");
        XAddParams secondParam = getDefaultValue();
        secondParam.idmp("producer1", "iid1");
        assertFalse(firstParam.equals(secondParam));
    }

    private XAddParams getDefaultValue() {
        return new XAddParams();
    }
}
