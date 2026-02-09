package redis.clients.jedis.params;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.args.HotkeysMetric;

import static org.junit.jupiter.api.Assertions.*;

public class HotkeysParamsTest {

    private HotkeysParams getDefaultValue() {
        return new HotkeysParams();
    }

    @Nested
    class EqualityAndHashCodeTests {

        @Test
        public void equalsWithIdenticalParams() {
            HotkeysParams firstParam = getDefaultValue();
            HotkeysParams secondParam = getDefaultValue();
            assertEquals(firstParam, secondParam);
        }

        @Test
        public void hashCodeWithIdenticalParams() {
            HotkeysParams firstParam = getDefaultValue();
            HotkeysParams secondParam = getDefaultValue();
            assertEquals(firstParam.hashCode(), secondParam.hashCode());
        }

        @Test
        public void equalsWithDifferentParams() {
            HotkeysParams firstParam = getDefaultValue();
            firstParam.metrics(HotkeysMetric.CPU).count(10);
            HotkeysParams secondParam = getDefaultValue();
            secondParam.metrics(HotkeysMetric.NET).count(20);
            assertNotEquals(firstParam, secondParam);
        }

        @Test
        public void hashCodeWithDifferentParams() {
            HotkeysParams firstParam = getDefaultValue();
            firstParam.metrics(HotkeysMetric.CPU).count(10);
            HotkeysParams secondParam = getDefaultValue();
            secondParam.metrics(HotkeysMetric.NET).count(20);
            assertNotEquals(firstParam.hashCode(), secondParam.hashCode());
        }

        @Test
        public void equalsWithNull() {
            HotkeysParams firstParam = getDefaultValue();
            HotkeysParams secondParam = null;
            assertFalse(firstParam.equals(secondParam));
        }

        @Test
        public void equalsWithSameInstance() {
            HotkeysParams param = getDefaultValue();
            assertTrue(param.equals(param));
        }

        @Test
        public void equalsWithDifferentMetrics() {
            HotkeysParams firstParam = getDefaultValue();
            firstParam.metrics(HotkeysMetric.CPU);
            HotkeysParams secondParam = getDefaultValue();
            secondParam.metrics(HotkeysMetric.NET);
            assertNotEquals(firstParam, secondParam);
        }

        @Test
        public void equalsWithDifferentSlots() {
            HotkeysParams firstParam = getDefaultValue();
            firstParam.metrics(HotkeysMetric.CPU).slots(1, 2, 3);
            HotkeysParams secondParam = getDefaultValue();
            secondParam.metrics(HotkeysMetric.CPU).slots(4, 5, 6);
            assertNotEquals(firstParam, secondParam);
        }
    }

    @Nested
    class ValidationTests {

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
            assertDoesNotThrow(() -> params.slots((int[]) null));
        }
    }

    @Nested
    class BuilderTests {

        @Test
        public void staticFactoryMethodReturnsInstance() {
            HotkeysParams params = HotkeysParams.hotkeysParams();
            assertNotNull(params);
            assertInstanceOf(HotkeysParams.class, params);
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
    }

    @Nested
    class EdgeCaseBehaviorTests {

        @Test
        public void slotsEmptyArrayNotAdded() {
            HotkeysParams params = getDefaultValue();
            assertDoesNotThrow(() -> params.metrics(HotkeysMetric.CPU).slots(new int[0]));
        }

        @Test
        public void lastBuilderCallWins() {
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

            assertNotEquals(firstParam, secondParam);
        }
    }

    @Nested
    class AddParamsTests {

        @Test
        public void addParamsWithMinimalConfiguration() {
            HotkeysParams params = getDefaultValue();
            params.metrics(HotkeysMetric.CPU);

            CommandArguments args = new CommandArguments(Protocol.Command.HOTKEYS);
            params.addParams(args);

            // Expected: HOTKEYS METRICS 1 CPU
            assertEquals(4, args.size());
            assertEquals(Protocol.Keyword.METRICS, args.get(1));
            assertEquals(HotkeysMetric.CPU, args.get(3));
        }

        @Test
        public void addParamsWithAllOptions() {
            HotkeysParams params = getDefaultValue();
            params.metrics(HotkeysMetric.CPU, HotkeysMetric.NET)
                .count(20)
                .duration(60)
                .sample(5)
                .slots(100, 200, 300);

            CommandArguments args = new CommandArguments(Protocol.Command.HOTKEYS);
            params.addParams(args);

            // Expected: HOTKEYS METRICS 2 CPU NET COUNT 20 DURATION 60 SAMPLE 5 SLOTS 3 100 200 300
            assertEquals(16, args.size());
            assertEquals(Protocol.Keyword.METRICS, args.get(1));
            assertEquals(HotkeysMetric.CPU, args.get(3));
            assertEquals(HotkeysMetric.NET, args.get(4));
            assertEquals(Protocol.Keyword.COUNT, args.get(5));
            assertEquals(Protocol.Keyword.DURATION, args.get(7));
            assertEquals(Protocol.Keyword.SAMPLE, args.get(9));
            assertEquals(Protocol.Keyword.SLOTS, args.get(11));
        }

        @Test
        public void addParamsWithEmptySlotsNotAdded() {
            HotkeysParams params = getDefaultValue();
            params.metrics(HotkeysMetric.CPU).slots(new int[0]);

            CommandArguments args = new CommandArguments(Protocol.Command.HOTKEYS);
            params.addParams(args);

            // Expected: HOTKEYS METRICS 1 CPU (no SLOTS)
            assertEquals(4, args.size());
            assertEquals(Protocol.Keyword.METRICS, args.get(1));
            assertEquals(HotkeysMetric.CPU, args.get(3));
        }

        @Test
        public void addParamsWithMultipleMetrics() {
            HotkeysParams params = getDefaultValue();
            params.metrics(HotkeysMetric.CPU, HotkeysMetric.NET);

            CommandArguments args = new CommandArguments(Protocol.Command.HOTKEYS);
            params.addParams(args);

            // Expected: HOTKEYS METRICS 2 CPU NET
            assertEquals(5, args.size());
            assertEquals(Protocol.Keyword.METRICS, args.get(1));
            assertEquals(HotkeysMetric.CPU, args.get(3));
            assertEquals(HotkeysMetric.NET, args.get(4));
        }

        @Test
        public void addParamsWithOnlyCount() {
            HotkeysParams params = getDefaultValue();
            params.metrics(HotkeysMetric.CPU).count(30);

            CommandArguments args = new CommandArguments(Protocol.Command.HOTKEYS);
            params.addParams(args);

            // Expected: HOTKEYS METRICS 1 CPU COUNT 30
            assertEquals(6, args.size());
            assertEquals(Protocol.Keyword.METRICS, args.get(1));
            assertEquals(HotkeysMetric.CPU, args.get(3));
            assertEquals(Protocol.Keyword.COUNT, args.get(4));
        }

        @Test
        public void addParamsWithOnlyDuration() {
            HotkeysParams params = getDefaultValue();
            params.metrics(HotkeysMetric.NET).duration(120);

            CommandArguments args = new CommandArguments(Protocol.Command.HOTKEYS);
            params.addParams(args);

            // Expected: HOTKEYS METRICS 1 NET DURATION 120
            assertEquals(6, args.size());
            assertEquals(Protocol.Keyword.METRICS, args.get(1));
            assertEquals(HotkeysMetric.NET, args.get(3));
            assertEquals(Protocol.Keyword.DURATION, args.get(4));
        }

        @Test
        public void addParamsWithOnlySample() {
            HotkeysParams params = getDefaultValue();
            params.metrics(HotkeysMetric.CPU).sample(10);

            CommandArguments args = new CommandArguments(Protocol.Command.HOTKEYS);
            params.addParams(args);

            // Expected: HOTKEYS METRICS 1 CPU SAMPLE 10
            assertEquals(6, args.size());
            assertEquals(Protocol.Keyword.METRICS, args.get(1));
            assertEquals(HotkeysMetric.CPU, args.get(3));
            assertEquals(Protocol.Keyword.SAMPLE, args.get(4));
        }

        @Test
        public void addParamsWithoutMetricsThrowsException() {
            HotkeysParams params = getDefaultValue();
            // Don't set metrics

            CommandArguments args = new CommandArguments(Protocol.Command.HOTKEYS);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> params.addParams(args));
            assertEquals("metrics must not be null", exception.getMessage());
        }
    }
}

