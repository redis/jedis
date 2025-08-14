package redis.clients.jedis.csc;

import java.util.Collections;
import java.util.List;

public final class TrackingConfig {

    /**
     * tracking mode(true:default; false:broadcast)
     */
    private final boolean trackingModeOnDefault;

    /**
     * tracking prefix list(only broadcast mode)
     */
    private final List<String> trackingPrefixList;

    public TrackingConfig(List<String> trackingPrefixList, boolean trackingModeOnDefault) {
        this.trackingPrefixList = trackingPrefixList;
        this.trackingModeOnDefault = trackingModeOnDefault;
    }

    public boolean isTrackingModeOnDefault() {
        return trackingModeOnDefault;
    }

    public List<String> getTrackingPrefixList() {
        return trackingPrefixList;
    }

    public static final TrackingConfig DEFAULT = new TrackingConfig(null, true);

    /**
     * prefix: ""
     */
    public static final TrackingConfig BROADCAST = new TrackingConfig(Collections.emptyList(), false);
}
