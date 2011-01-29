package com.googlecode.jedis;

import static com.googlecode.jedis.PairImpl.newPair;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.testng.annotations.Test;

public class PairImplTest {

    @Test
    public void equalsAndHashcodeTest() {
	final Pair<String, Double> p1 = newPair("a", 1.);
	final Pair<String, Double> p2 = newPair("a", 1.);
	final Pair<String, Double> p3 = newPair("a", 2.);

	assertThat(p1.equals(p2), is(true));
	assertThat((p1.hashCode() == p2.hashCode()), is(true));
	assertThat((p1 == p2), is(false));

	assertThat(p1.equals(p3), is(false));
	assertThat((p1.hashCode() == p3.hashCode()), is(false));
    }

}
