package redis.clients.jedis.util;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.resps.GeoRadiusResponse;

public class GeoRadiusResponseMatcher extends TypeSafeMatcher<GeoRadiusResponse> {
    private final GeoRadiusResponse expected;
    private final double coordinateTolerance;

    public static Matcher<GeoRadiusResponse> isEqualToGeoRadiusResponse(GeoRadiusResponse expected) {
        return new GeoRadiusResponseMatcher(expected, GeoCoordinateMatcher.DEFAULT_TOLERANCE);
    }

    public static Matcher<GeoRadiusResponse> isEqualToGeoRadiusResponse(GeoRadiusResponse expected, double tolerance) {
        return new GeoRadiusResponseMatcher(expected, tolerance);
    }

    public GeoRadiusResponseMatcher(GeoRadiusResponse expected, double coordinateTolerance) {
        this.expected = expected;
        this.coordinateTolerance = coordinateTolerance;
    }

    @Override
    protected boolean matchesSafely(GeoRadiusResponse actual) {
        // Check if coordinates match within the tolerance
        GeoCoordinate expectedCoord = expected.getCoordinate();
        GeoCoordinate actualCoord = actual.getCoordinate();
        if (!GeoCoordinateMatcher.isEqualWithTolerance(expectedCoord, coordinateTolerance).matches(actualCoord)) {
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
        mismatchDescription.appendText("was ")
                .appendValue(actual);
    }

}