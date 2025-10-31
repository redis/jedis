package redis.clients.jedis.util;

import java.util.Arrays;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import redis.clients.jedis.resps.GeoRadiusResponse;

public class GeoRadiusResponseMatcher extends TypeSafeMatcher<GeoRadiusResponse> {

    public static GeoRadiusResponseMatcher ofResponse(GeoRadiusResponse expected) {
        return new GeoRadiusResponseMatcher(expected);
    }

    private final GeoRadiusResponse expected;

    public GeoRadiusResponseMatcher(GeoRadiusResponse expected) {
        this.expected = expected;
    }

    @Override
    protected boolean matchesSafely(GeoRadiusResponse actual) {
        // Check if coordinates match within the tolerance
        if (!GeoCoordinateMatcher.atCoordinates(expected.getCoordinate())
                .matches(actual.getCoordinate())) {
            return false;
        }

        // Check if other attributes match exactly
        if (Double.compare(expected.getDistance(), actual.getDistance()) != 0) {
            return false;
        }
        if (Long.compare(expected.getRawScore(), actual.getRawScore()) != 0) {
            return false;
        }
        return Arrays.equals(expected.getMember(), actual.getMember());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a GeoRadiusResponse with coordinate ")
                .appendValue(expected.getCoordinate())
                .appendText(", distance ")
                .appendValue(expected.getDistance())
                .appendText(", rawScore ")
                .appendValue(expected.getRawScore())
                .appendText("and member ")
                .appendValue(expected.getMemberByString());
    }

    @Override
    protected void describeMismatchSafely(GeoRadiusResponse actual, Description mismatchDescription) {
        mismatchDescription.appendText("was ").appendValue(actual);
    }

}