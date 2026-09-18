package redis.clients.jedis.timeseries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class TSElementTest {

  @Nested
  class SingleValue {

    @Test
    public void reflexive() {
      TSElement e = new TSElement(1000L, 1.5);
      assertEquals(e, e);
    }

    @Test
    public void equalSameTimestampAndValue() {
      TSElement a = new TSElement(1000L, 1.5);
      TSElement b = new TSElement(1000L, 1.5);
      assertEquals(a, b);
      assertEquals(b, a);
      assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void notEqualDifferentTimestamp() {
      assertNotEquals(new TSElement(1000L, 1.5), new TSElement(2000L, 1.5));
    }

    @Test
    public void notEqualDifferentValue() {
      assertNotEquals(new TSElement(1000L, 1.5), new TSElement(1000L, 2.5));
    }

    @Test
    public void notEqualNull() {
      assertNotEquals(new TSElement(1000L, 1.5), null);
    }

    @Test
    public void notEqualUnrelatedType() {
      assertNotEquals(new TSElement(1000L, 1.5), "not an element");
    }

    @Test
    public void nanEqualsNan() {
      TSElement a = new TSElement(1L, Double.NaN);
      TSElement b = new TSElement(1L, Double.NaN);
      assertEquals(a, b);
      assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void positiveZeroNotEqualNegativeZero() {
      assertNotEquals(new TSElement(1L, 0.0), new TSElement(1L, -0.0));
    }

    @Test
    public void notEqualToMultiValueWithDifferentSize() {
      TSElement single = new TSElement(1000L, 1.5);
      TSElement.MultiValueTSElement multi = new TSElement.MultiValueTSElement(1000L,
          Arrays.asList(1.5, 2.5));

      assertNotEquals(single, multi);
      assertNotEquals(multi, single);
    }

    @Test
    public void equalToSingletonMultiValueWithSameContent() {
      // MultiValueTSElement is an internal performance variant; whether a sample is
      // wrapped in it or not should not be observable through equals/hashCode.
      TSElement single = new TSElement(1000L, 1.5);
      TSElement.MultiValueTSElement multi = new TSElement.MultiValueTSElement(1000L,
          Collections.singletonList(1.5));

      assertEquals(single, multi);
      assertEquals(multi, single);
      assertEquals(single.hashCode(), multi.hashCode());
    }
  }

  @Nested
  class MultiValue {

    @Test
    public void reflexive() {
      TSElement.MultiValueTSElement e = new TSElement.MultiValueTSElement(1000L,
          Arrays.asList(1.5, 2.5));
      assertEquals(e, e);
    }

    @Test
    public void equalSameTimestampAndValues() {
      TSElement.MultiValueTSElement a = new TSElement.MultiValueTSElement(1000L,
          Arrays.asList(1.5, 2.5, 3.5));
      TSElement.MultiValueTSElement b = new TSElement.MultiValueTSElement(1000L,
          Arrays.asList(1.5, 2.5, 3.5));
      assertEquals(a, b);
      assertEquals(b, a);
      assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void notEqualDifferentTimestamp() {
      TSElement.MultiValueTSElement a = new TSElement.MultiValueTSElement(1000L,
          Arrays.asList(1.5, 2.5));
      TSElement.MultiValueTSElement b = new TSElement.MultiValueTSElement(2000L,
          Arrays.asList(1.5, 2.5));
      assertNotEquals(a, b);
    }

    @Test
    public void notEqualDifferentValues() {
      TSElement.MultiValueTSElement a = new TSElement.MultiValueTSElement(1000L,
          Arrays.asList(1.5, 2.5));
      TSElement.MultiValueTSElement b = new TSElement.MultiValueTSElement(1000L,
          Arrays.asList(1.5, 9.9));
      assertNotEquals(a, b);
    }

    @Test
    public void notEqualDifferentSize() {
      TSElement.MultiValueTSElement a = new TSElement.MultiValueTSElement(1000L,
          Arrays.asList(1.5, 2.5));
      TSElement.MultiValueTSElement b = new TSElement.MultiValueTSElement(1000L,
          Arrays.asList(1.5, 2.5, 3.5));
      assertNotEquals(a, b);
    }

    @Test
    public void notEqualDifferentOrder() {
      TSElement.MultiValueTSElement a = new TSElement.MultiValueTSElement(1000L,
          Arrays.asList(1.5, 2.5));
      TSElement.MultiValueTSElement b = new TSElement.MultiValueTSElement(1000L,
          Arrays.asList(2.5, 1.5));
      assertNotEquals(a, b);
    }

    @Test
    public void notEqualNull() {
      TSElement.MultiValueTSElement a = new TSElement.MultiValueTSElement(1000L,
          Arrays.asList(1.5, 2.5));
      assertNotEquals(a, null);
    }

    @Test
    public void notEqualUnrelatedType() {
      TSElement.MultiValueTSElement a = new TSElement.MultiValueTSElement(1000L,
          Arrays.asList(1.5, 2.5));
      assertNotEquals(a, "not an element");
    }
  }

  @Nested
  class CrossClass {

    @Test
    public void getValueReturnsFirstValue() {
      List<Double> values = Arrays.asList(7.0, 8.0, 9.0);
      TSElement.MultiValueTSElement multi = new TSElement.MultiValueTSElement(1L, values);
      assertEquals(7.0, multi.getValue(), 0.0);
    }

    @Test
    public void getValuesReturnsSingletonForBaseClass() {
      TSElement single = new TSElement(1L, 3.14);
      List<Double> values = single.getValues();
      assertEquals(1, values.size());
      assertEquals(3.14, values.get(0), 0.0);
    }

    @Test
    public void getValuesReturnsUnderlyingListForSubclass() {
      List<Double> values = Arrays.asList(1.0, 2.0, 3.0);
      TSElement.MultiValueTSElement multi = new TSElement.MultiValueTSElement(1L, values);
      assertEquals(values, multi.getValues());
    }

    @Test
    public void baseAndSubclassEqualWhenContentMatches() {
      TSElement single = new TSElement(1L, 1.5);
      TSElement.MultiValueTSElement multi = new TSElement.MultiValueTSElement(1L,
          Collections.singletonList(1.5));
      // Symmetric.
      assertEquals(single, multi);
      assertEquals(multi, single);
      assertEquals(single.hashCode(), multi.hashCode());
    }

    @Test
    public void baseAndSubclassNotEqualWhenSubclassHasExtraValues() {
      TSElement single = new TSElement(1L, 1.5);
      TSElement.MultiValueTSElement multi = new TSElement.MultiValueTSElement(1L,
          Arrays.asList(1.5, 2.5));
      // Symmetric.
      assertNotEquals(single, multi);
      assertNotEquals(multi, single);
    }

    @Test
    public void multiValueElementIsAssignableToTSElement() {
      TSElement multi = new TSElement.MultiValueTSElement(1L, Arrays.asList(1.5, 2.5));
      assertTrue(multi instanceof TSElement);
    }
  }
}