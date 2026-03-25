package redis.clients.jedis.params;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;

import static org.junit.jupiter.api.Assertions.*;

public class GCRAParamsTest {

  private GCRAParams getDefaultValue() {
    return GCRAParams.gcraParams(5, 10, 60.0);
  }

  @Nested
  class EqualityAndHashCodeTests {

    @Test
    public void equalsWithIdenticalParams() {
      GCRAParams first = getDefaultValue();
      GCRAParams second = getDefaultValue();
      assertEquals(first, second);
    }

    @Test
    public void hashCodeWithIdenticalParams() {
      GCRAParams first = getDefaultValue();
      GCRAParams second = getDefaultValue();
      assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void equalsWithDifferentParams() {
      GCRAParams first = GCRAParams.gcraParams(5, 10, 60.0);
      GCRAParams second = GCRAParams.gcraParams(10, 20, 120.0);
      assertNotEquals(first, second);
    }

    @Test
    public void hashCodeWithDifferentParams() {
      GCRAParams first = GCRAParams.gcraParams(5, 10, 60.0);
      GCRAParams second = GCRAParams.gcraParams(10, 20, 120.0);
      assertNotEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void equalsWithNumRequests() {
      GCRAParams first = GCRAParams.gcraParams(5, 10, 60.0).numRequests(3);
      GCRAParams second = GCRAParams.gcraParams(5, 10, 60.0).numRequests(3);
      assertEquals(first, second);
    }

    @Test
    public void notEqualsWithDifferentNumRequests() {
      GCRAParams first = GCRAParams.gcraParams(5, 10, 60.0).numRequests(3);
      GCRAParams second = GCRAParams.gcraParams(5, 10, 60.0).numRequests(5);
      assertNotEquals(first, second);
    }

    @Test
    public void notEqualsWithNull() {
      GCRAParams params = getDefaultValue();
      assertNotEquals(params, null);
    }

    @Test
    public void notEqualsWithDifferentType() {
      GCRAParams params = getDefaultValue();
      assertNotEquals(params, "not a GCRAParams");
    }

    @Test
    public void equalsWithSelf() {
      GCRAParams params = getDefaultValue();
      assertEquals(params, params);
    }
  }

  @Nested
  class ValidationTests {

    @Test
    public void maxBurstNegativeThrowsException() {
      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> GCRAParams.gcraParams(-1, 10, 60.0));
      assertEquals("maxBurst must be >= 0", ex.getMessage());
    }

    @Test
    public void maxBurstZeroValid() {
      assertDoesNotThrow(() -> GCRAParams.gcraParams(0, 10, 60.0));
    }

    @Test
    public void requestsPerPeriodZeroThrowsException() {
      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> GCRAParams.gcraParams(5, 0, 60.0));
      assertEquals("requestsPerPeriod must be >= 1", ex.getMessage());
    }

    @Test
    public void requestsPerPeriodOneValid() {
      assertDoesNotThrow(() -> GCRAParams.gcraParams(5, 1, 60.0));
    }

    @Test
    public void periodTooSmallThrowsException() {
      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> GCRAParams.gcraParams(5, 10, 0.5));
      assertEquals("period must be >= 1.0", ex.getMessage());
    }

    @Test
    public void periodTooLargeThrowsException() {
      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> GCRAParams.gcraParams(5, 10, 1e12 + 1));
      assertEquals("period must be <= 1e12", ex.getMessage());
    }

    @Test
    public void periodAtMaxValid() {
      assertDoesNotThrow(() -> GCRAParams.gcraParams(5, 10, 1e12));
    }

    @Test
    public void numRequestsZeroThrowsException() {
      GCRAParams params = getDefaultValue();
      IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> params.numRequests(0));
      assertEquals("numRequests must be >= 1", ex.getMessage());
    }

    @Test
    public void numRequestsOneValid() {
      GCRAParams params = getDefaultValue();
      assertDoesNotThrow(() -> params.numRequests(1));
    }
  }

  @Nested
  class BuilderTests {

    @Test
    public void staticFactoryMethodReturnsInstance() {
      GCRAParams params = GCRAParams.gcraParams(5, 10, 60.0);
      assertNotNull(params);
      assertInstanceOf(GCRAParams.class, params);
    }

    @Test
    public void methodChainingWorks() {
      GCRAParams params = GCRAParams.gcraParams(5, 10, 60.0).numRequests(3);
      assertNotNull(params);
    }
  }

  @Nested
  class AddParamsTests {

    @Test
    public void addParamsWithRequiredOnly() {
      GCRAParams params = GCRAParams.gcraParams(5, 10, 60.0);
      CommandArguments args = new CommandArguments(Protocol.Command.GCRA);
      params.addParams(args);

      // GCRA + maxBurst(5) + requestsPerPeriod(10) + period(60.0) = 4 args total
      assertEquals(4, args.size());
    }

    @Test
    public void addParamsWithNumRequests() {
      GCRAParams params = GCRAParams.gcraParams(5, 10, 60.0).numRequests(3);
      CommandArguments args = new CommandArguments(Protocol.Command.GCRA);
      params.addParams(args);

      // GCRA + maxBurst(5) + requestsPerPeriod(10) + period(60.0) + NUM_REQUESTS + 3 = 6 args total
      assertEquals(6, args.size());
    }
  }
}
