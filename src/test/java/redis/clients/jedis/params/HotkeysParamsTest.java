package redis.clients.jedis.params;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.args.HotkeysMetric;

import static org.junit.jupiter.api.Assertions.*;

public class HotkeysParamsTest {

    // ========== Category 1: Standard Jedis Pattern Tests (equals/hashCode) ==========

    @Test
    public void checkEqualsIdenticalParams() {
        HotkeysParams firstParam = getDefaultValue();
        HotkeysParams secondParam = getDefaultValue();
        assertTrue(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeIdenticalParams() {
        HotkeysParams firstParam = getDefaultValue();
        HotkeysParams secondParam = getDefaultValue();
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsVariousParams() {
        HotkeysParams firstParam = getDefaultValue();
        firstParam.metrics(HotkeysMetric.CPU).count(10);
        HotkeysParams secondParam = getDefaultValue();
        secondParam.metrics(HotkeysMetric.NET).count(20);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkHashCodeVariousParams() {
        HotkeysParams firstParam = getDefaultValue();
        firstParam.metrics(HotkeysMetric.CPU).count(10);
        HotkeysParams secondParam = getDefaultValue();
        secondParam.metrics(HotkeysMetric.NET).count(20);
        assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    @Test
    public void checkEqualsWithNull() {
        HotkeysParams firstParam = getDefaultValue();
        HotkeysParams secondParam = null;
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkEqualsSameInstance() {
        HotkeysParams param = getDefaultValue();
        assertTrue(param.equals(param));
    }

    @Test
    public void checkEqualsWithDifferentMetrics() {
        HotkeysParams firstParam = getDefaultValue();
        firstParam.metrics(HotkeysMetric.CPU);
        HotkeysParams secondParam = getDefaultValue();
        secondParam.metrics(HotkeysMetric.NET);
        assertFalse(firstParam.equals(secondParam));
    }

    @Test
    public void checkEqualsWithDifferentSlots() {
        HotkeysParams firstParam = getDefaultValue();
        firstParam.metrics(HotkeysMetric.CPU).slots(1, 2, 3);
        HotkeysParams secondParam = getDefaultValue();
        secondParam.metrics(HotkeysMetric.CPU).slots(4, 5, 6);
        assertFalse(firstParam.equals(secondParam));
    }

    // ========== Category 2: Validation Tests (IllegalArgumentException) ==========

    @Test
    public void metricsNullThrowsException() {
        HotkeysParams params = getDefaultValue();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            params.metrics(null);
        });
        assertEquals("metrics must not be null", exception.getMessage());
    }

    @Test
    public void metricsEmptyArrayThrowsException() {
        HotkeysParams params = getDefaultValue();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            params.metrics(new HotkeysMetric[0]);
        });
        assertEquals("at least one metric is required", exception.getMessage());
    }

    @Test
    public void countTooLowThrowsException() {
        HotkeysParams params = getDefaultValue();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            params.count(9);
        });
        assertEquals("count must be between 1 and 64", exception.getMessage());
    }

    @Test
    public void countTooHighThrowsException() {
        HotkeysParams params = getDefaultValue();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            params.count(65);
        });
        assertEquals("count must be between 1 and 64", exception.getMessage());
    }

    @Test
    public void countBoundaryMinValid() {
        HotkeysParams params = getDefaultValue();
        assertDoesNotThrow(() -> params.count(10));
    }

    @Test
    public void countBoundaryMaxValid() {
        HotkeysParams params = getDefaultValue();
        assertDoesNotThrow(() -> params.count(64));
    }

    @Test
    public void durationNegativeThrowsException() {
        HotkeysParams params = getDefaultValue();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            params.duration(-1);
        });
        assertEquals("duration must be >= 0", exception.getMessage());
    }

    @Test
    public void durationZeroValid() {
        HotkeysParams params = getDefaultValue();
        assertDoesNotThrow(() -> params.duration(0));
    }

    @Test
    public void sampleTooLowThrowsException() {
        HotkeysParams params = getDefaultValue();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            params.sample(0);
        });
        assertEquals("sample must be >= 1", exception.getMessage());
    }

    @Test
    public void sampleOneValid() {
        HotkeysParams params = getDefaultValue();
        assertDoesNotThrow(() -> params.sample(1));
    }

    @Test
    public void slotTooLowThrowsException() {
        HotkeysParams params = getDefaultValue();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            params.slots(-1);
        });
        assertEquals("each slot must be between 0 and 16383", exception.getMessage());
    }

    @Test
    public void slotTooHighThrowsException() {
        HotkeysParams params = getDefaultValue();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            params.slots(16384);
        });
        assertEquals("each slot must be between 0 and 16383", exception.getMessage());
    }

    @Test
    public void slotBoundaryMinValid() {
        HotkeysParams params = getDefaultValue();
        assertDoesNotThrow(() -> params.slots(0));
    }

    @Test
    public void slotBoundaryMaxValid() {
        HotkeysParams params = getDefaultValue();
        assertDoesNotThrow(() -> params.slots(16383));
    }

    @Test
    public void slotsMultipleWithInvalidThrowsException() {
        HotkeysParams params = getDefaultValue();
        assertThrows(IllegalArgumentException.class, () -> {
            params.slots(0, 100, 16384);
        });
    }

    @Test
    public void slotsNullValid() {
        HotkeysParams params = getDefaultValue();
        assertDoesNotThrow(() -> params.slots(null));
    }

    // ========== Category 3: Builder Pattern Tests ==========

    @Test
    public void staticFactoryMethodReturnsInstance() {
        HotkeysParams params = HotkeysParams.hotkeysParams();
        assertNotNull(params);
        assertTrue(params instanceof HotkeysParams);
    }

    @Test
    public void methodChainingWorks() {
        HotkeysParams params = HotkeysParams.hotkeysParams()
            .metrics(HotkeysMetric.CPU, HotkeysMetric.NET)
            .count(20)
            .duration(60)
            .sample(5)
            .slots(0, 100, 200);
        assertNotNull(params);
    }

    @Test
    public void builderPatternProducesCorrectState() {
        HotkeysParams firstParam = HotkeysParams.hotkeysParams()
            .metrics(HotkeysMetric.CPU, HotkeysMetric.NET)
            .count(20)
            .duration(60)
            .sample(5)
            .slots(0, 100);

        HotkeysParams secondParam = HotkeysParams.hotkeysParams()
            .metrics(HotkeysMetric.CPU, HotkeysMetric.NET)
            .count(20)
            .duration(60)
            .sample(5)
            .slots(0, 100);

        assertEquals(firstParam, secondParam);
        assertEquals(firstParam.hashCode(), secondParam.hashCode());
    }

    // ========== Category 5: Edge Cases and Special Scenarios ==========

    @Test
    public void slotsEmptyArrayNotAdded() {
        HotkeysParams params = getDefaultValue();
        assertDoesNotThrow(() -> params.metrics(HotkeysMetric.CPU).slots(new int[0]));
    }

    @Test
    public void multipleCallsToSameMethodOverwrites() {
        HotkeysParams firstParam = getDefaultValue();
        firstParam.metrics(HotkeysMetric.CPU).count(10).count(20);

        HotkeysParams secondParam = getDefaultValue();
        secondParam.metrics(HotkeysMetric.CPU).count(20);

        assertEquals(firstParam, secondParam);
    }

    @Test
    public void equalsWithSameMetricsDifferentOrder() {
        HotkeysParams firstParam = getDefaultValue();
        firstParam.metrics(HotkeysMetric.CPU, HotkeysMetric.NET);

        HotkeysParams secondParam = getDefaultValue();
        secondParam.metrics(HotkeysMetric.NET, HotkeysMetric.CPU);

        assertFalse(firstParam.equals(secondParam));
    }

    private HotkeysParams getDefaultValue() {
        return new HotkeysParams();
    }
}

