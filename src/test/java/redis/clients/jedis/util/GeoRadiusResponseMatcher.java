package redis.clients.jedis.util;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import redis.clients.jedis.resps.GeoRadiusResponse;

public class GeoRadiusResponseMatcher extends TypeSafeMatcher<GeoRadiusResponse> {

    public static GeoRadiusResponseMatcher from(GeoRadiusResponse expected) {
        return new GeoRadiusResponseMatcher(expected);
    }

    private final GeoRadiusResponse expected;

    public GeoRadiusResponseMatcher(GeoRadiusResponse expected) {
        this.expected = expected;
    }

    @Override
    protected boolean matchesSafely(GeoRadiusResponse actual) {
        // Check if coordinates match within the tolerance
        if (!GeoCoordinateMatcher.atCoordinates(expected.getCoordinate()).matches(actual.getCoordinate())) {
            return false;
        }

        // Check if distance and rawScore match exactly
        if (Double.compare(expected.getDistance(), actual.getDistance()) != 0) {
            return false;
        }
        return expected.getRawScore() == actual.getRawScore();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a GeoRadiusResponse with coordinate ")
                .appendValue(expected.getCoordinate())
                .appendText(", distance ")
                .appendValue(expected.getDistance())
                .appendText(", and rawScore ")
                .appendValue(expected.getRawScore());
    }

    @Override
    protected void describeMismatchSafely(GeoRadiusResponse actual, Description mismatchDescription) {
        mismatchDescription.appendText("was ").appendValue(actual);
    }

}