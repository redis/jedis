package redis.clients.jedis;

public class MaintenanceNotificationsConfig {

    public static final MaintenanceNotificationsConfig DEFAULT;

    static {
        DEFAULT = new MaintenanceNotificationsConfig();
        DEFAULT.mode = Mode.DISABLED; // Disabled by default for backward compatibility
        DEFAULT.timeoutOptions = TimeoutOptions.create(); // Default timeout options
    }

    /**
     * Endpoint types for maintenance event notifications.
     * <p>
     * Determines the format of endpoint addresses returned in MOVING notifications.
     *
     * @since 8.0
     */
    public enum EndpointType {
        /** Internal IP address (for private network connections) */
        INTERNAL_IP,
        /** Internal fully qualified domain name (for private network connections with TLS) */
        INTERNAL_FQDN,
        /** External IP address (for public network connections) */
        EXTERNAL_IP,
        /** External fully qualified domain name (for public network connections with TLS) */
        EXTERNAL_FQDN,
        /**
         * none indicates that the MOVING message doesn’t need to contain an endpoint. In such a case, the client is expected to
         * schedule a graceful reconnect to its currently configured endpoint after half of the grace period that was
         * communicated by the server is over.
         */
        NONE
    }

    /**
     * Mode for maintenance event notifications.
     * <ul>
     * <li>ENABLED - Maintenance notifications are explicitly enabled. Both timeout relaxation and
     * proactive rebind are activated. Server must support the feature.</li>
     * <li>DISABLED - Maintenance notifications are explicitly disabled.</li>
     * <li>AUTO - Maintenance notifications are automatically enabled if the server supports them.
     * Silently falls back if not supported. Both timeout relaxation and proactive rebind are
     * activated when successful.</li>
     * </ul>
     */
    public enum Mode {
        ENABLED, DISABLED, AUTO
    }

    // default to EndpointTypeExternalIP
    EndpointType endpointType = EndpointType.EXTERNAL_IP;

    TimeoutOptions timeoutOptions;

    // default to AUTO
    Mode mode = Mode.AUTO;

    public EndpointType getEndpointType(){
        return endpointType;
    }

    public EndpointType getMovingTargetEndpointType(){
        return endpointType;
    }

    public TimeoutOptions getTimeoutOptions(){
        return timeoutOptions;
    }

    public Mode getMode() {
        return mode;
    }

    /**
     * Returns whether maintenance event notifications are enabled. When enabled, both timeout
     * relaxation and proactive rebind features are activated.
     * @return true if mode is ENABLED or AUTO, false if DISABLED
     */
    public boolean isEnabled() {
        return mode == Mode.ENABLED || mode == Mode.AUTO;
    }

    public static MaintenanceNotificationsConfig create() {
        return new MaintenanceNotificationsConfig();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private EndpointType endpointType = EndpointType.EXTERNAL_IP;
        private TimeoutOptions timeoutOptions = TimeoutOptions.create(); // Default timeout options
        private Mode mode = Mode.AUTO;

        public Builder endpointType(EndpointType endpointType) {
            this.endpointType = endpointType;
            return this;
        }

        public Builder timeoutOptions(TimeoutOptions timeoutOptions) {
            this.timeoutOptions = timeoutOptions;
            return this;
        }

        public Builder mode(Mode mode) {
            this.mode = mode;
            return this;
        }

        public MaintenanceNotificationsConfig build() {
            MaintenanceNotificationsConfig config = new MaintenanceNotificationsConfig();
            config.endpointType = this.endpointType;
            config.timeoutOptions = this.timeoutOptions;
            config.mode = this.mode;
            return config;
        }
    }

}
