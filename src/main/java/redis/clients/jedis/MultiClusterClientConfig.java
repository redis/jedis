package redis.clients.jedis;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisValidationException;


/**
 * @author Allen Terleto (aterleto)
 * <p>
 * Config which supports multiple cluster/database endpoint configurations
 * that all share retry and circuit breaker configuration settings.
 * <p>
 * With this Config users can seamlessly failover to Disaster Recovery (DR), Backup, and Active-Active cluster(s)
 * by using simple configuration which is passed through from Resilience4j - https://resilience4j.readme.io/docs
 * <p>
 * Configuration options related to automatic failback (e.g. HALF_OPENED state) are not supported and therefore
 * not passed through to Jedis users.
 * <p>
 */
// TODO: move
@Experimental
public final class MultiClusterClientConfig {

    private static final int RETRY_MAX_ATTEMPTS_DEFAULT = 3;
    private static final int RETRY_WAIT_DURATION_DEFAULT = 500;  // measured in milliseconds
    private static final int RETRY_WAIT_DURATION_EXPONENTIAL_BACKOFF_MULTIPLIER_DEFAULT = 2;
    private static final List<Class> RETRY_INCLUDED_EXCEPTIONS_DEFAULT = Arrays.asList(JedisConnectionException.class);

    private static final float CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD_DEFAULT = 50.0f; // measured as percentage
    private static final int CIRCUIT_BREAKER_SLIDING_WINDOW_MIN_CALLS_DEFAULT = 100;
    private static final SlidingWindowType CIRCUIT_BREAKER_SLIDING_WINDOW_TYPE_DEFAULT = SlidingWindowType.COUNT_BASED;
    private static final int CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE_DEFAULT = 100;
    private static final int CIRCUIT_BREAKER_SLOW_CALL_DURATION_THRESHOLD_DEFAULT = 60000; // measured in milliseconds
    private static final float CIRCUIT_BREAKER_SLOW_CALL_RATE_THRESHOLD_DEFAULT = 100.0f; // measured as percentage
    private static final List<Class> CIRCUIT_BREAKER_INCLUDED_EXCEPTIONS_DEFAULT = Arrays.asList(JedisConnectionException.class);

    private static final List<Class<? extends Throwable>> FALLBACK_EXCEPTIONS_DEFAULT = Arrays.asList(CallNotPermittedException.class);

    private final ClusterConfig[] clusterConfigs;

    //////////// Retry Config - https://resilience4j.readme.io/docs/retry ////////////

    /** Maximum number of attempts (including the initial call as the first attempt) */
    private int retryMaxAttempts;

    /** Fixed wait duration between retry attempt */
    private Duration retryWaitDuration;

    /**  Wait duration increases exponentially between attempts due to the multiplier.
     * For example, if we specified an initial wait time of 1s and a multiplier of 2,
     * the retries would be done after 1s, 2s, 4s, 8s, 16s, and so on */
    private int retryWaitDurationExponentialBackoffMultiplier;

    /**  Configures a list of Throwable classes that are recorded as a failure and thus are retried.
     * This parameter supports subtyping. */
    private List<Class> retryIncludedExceptionList;

    /**  Configures a list of Throwable classes that are ignored and thus are not retried.
     * This parameter supports subtyping. */
    private List<Class> retryIgnoreExceptionList;

    //////////// Circuit Breaker Config - https://resilience4j.readme.io/docs/circuitbreaker ////////////

    /**  When the failure rate is equal or greater than the threshold the CircuitBreaker transitions
     * to open and starts short-circuiting calls */
    private float circuitBreakerFailureRateThreshold;

    /**  Minimum number of calls required (per sliding window period) before the CircuitBreaker
     * can calculate the error rate or slow call rate. For example, if the value is 10,
     * then at least 10 calls must be recorded, before the failure rate can be calculated. However, if
     * only 9 calls have been recorded, the CircuitBreaker will not transition to open even if all 9  have failed */
    private int circuitBreakerSlidingWindowMinCalls;

    /**  Used to record the outcome of calls when the CircuitBreaker is closed.
     * If the type is COUNT_BASED, the last slidingWindowSize calls are recorded and aggregated.
     * If the type is TIME_BASED, the calls of the last slidingWindowSize seconds are recorded and aggregated */
    private SlidingWindowType circuitBreakerSlidingWindowType;

    /**  Size of the sliding window which is used to record the outcome of calls when the CircuitBreaker is closed */
    private int circuitBreakerSlidingWindowSize;

    /**  Duration threshold above which calls are considered as slow and increase the rate of slow calls */
    private Duration circuitBreakerSlowCallDurationThreshold;

    /**  When the percentage of slow calls is equal or greater the threshold,
     * the CircuitBreaker transitions to open and starts short-circuiting calls.
     * CircuitBreaker considers a call as slow when the call duration is greater than slowCallDurationThreshold */
    private float circuitBreakerSlowCallRateThreshold;

    /**  A list of exceptions that are recorded as a failure and thus increase the failure rate.
     * Any exception matching or inheriting from one of the list counts as a failure, unless explicitly
     * ignored via ignoreExceptions. If you specify a list of exceptions, all other exceptions count as
     * a success, unless they are explicitly ignored by ignoreExceptions */
    private List<Class> circuitBreakerIncludedExceptionList;

    /**  A list of exceptions that are ignored and neither count as a failure nor success.
     * Any exception matching or inheriting from one of the list will not count as a
     * failure nor success, even if the exceptions is part of recordExceptions */
    private List<Class> circuitBreakerIgnoreExceptionList;

    private List<Class<? extends Throwable>> fallbackExceptionList;

    public MultiClusterClientConfig(ClusterConfig[] clusterConfigs) {
        this.clusterConfigs = clusterConfigs;
    }

    public ClusterConfig[] getClusterConfigs() {
        return clusterConfigs;
    }

    public int getRetryMaxAttempts() {
        return retryMaxAttempts;
    }

    public Duration getRetryWaitDuration() {
        return retryWaitDuration;
    }

    public int getRetryWaitDurationExponentialBackoffMultiplier() {
        return retryWaitDurationExponentialBackoffMultiplier;
    }

    public float getCircuitBreakerFailureRateThreshold() {
        return circuitBreakerFailureRateThreshold;
    }

    public int getCircuitBreakerSlidingWindowMinCalls() {
        return circuitBreakerSlidingWindowMinCalls;
    }

    public int getCircuitBreakerSlidingWindowSize() {
        return circuitBreakerSlidingWindowSize;
    }

    public Duration getCircuitBreakerSlowCallDurationThreshold() {
        return circuitBreakerSlowCallDurationThreshold;
    }

    public float getCircuitBreakerSlowCallRateThreshold() {
        return circuitBreakerSlowCallRateThreshold;
    }

    public List<Class> getRetryIncludedExceptionList() {
        return retryIncludedExceptionList;
    }

    public List<Class> getRetryIgnoreExceptionList() {
        return retryIgnoreExceptionList;
    }

    public List<Class> getCircuitBreakerIncludedExceptionList() {
        return circuitBreakerIncludedExceptionList;
    }

    public List<Class> getCircuitBreakerIgnoreExceptionList() {
        return circuitBreakerIgnoreExceptionList;
    }

    public SlidingWindowType getCircuitBreakerSlidingWindowType() {
        return circuitBreakerSlidingWindowType;
    }

    public List<Class<? extends Throwable>> getFallbackExceptionList() {
        return fallbackExceptionList;
    }

    public static class ClusterConfig {

        private int priority;
        private HostAndPort hostAndPort;
        private JedisClientConfig clientConfig;
        private GenericObjectPoolConfig<Connection> connectionPoolConfig;

        public ClusterConfig(HostAndPort hostAndPort, JedisClientConfig clientConfig) {
            this.hostAndPort = hostAndPort;
            this.clientConfig = clientConfig;
        }

        public ClusterConfig(HostAndPort hostAndPort, JedisClientConfig clientConfig,
                             GenericObjectPoolConfig<Connection> connectionPoolConfig) {
            this.hostAndPort = hostAndPort;
            this.clientConfig = clientConfig;
            this.connectionPoolConfig = connectionPoolConfig;
        }

        public int getPriority() {
            return priority;
        }

        private void setPriority(int priority) {
            this.priority = priority;
        }

        public HostAndPort getHostAndPort() {
            return hostAndPort;
        }

        public JedisClientConfig getJedisClientConfig() {
            return clientConfig;
        }

        public GenericObjectPoolConfig<Connection> getConnectionPoolConfig() {
            return connectionPoolConfig;
        }
    }

    public static class Builder {

        private ClusterConfig[] clusterConfigs;

        private int retryMaxAttempts = RETRY_MAX_ATTEMPTS_DEFAULT;
        private int retryWaitDuration = RETRY_WAIT_DURATION_DEFAULT;
        private int retryWaitDurationExponentialBackoffMultiplier = RETRY_WAIT_DURATION_EXPONENTIAL_BACKOFF_MULTIPLIER_DEFAULT;
        private List<Class> retryIncludedExceptionList = RETRY_INCLUDED_EXCEPTIONS_DEFAULT;
        private List<Class> retryIgnoreExceptionList = null;

        private float circuitBreakerFailureRateThreshold = CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD_DEFAULT;
        private int circuitBreakerSlidingWindowMinCalls = CIRCUIT_BREAKER_SLIDING_WINDOW_MIN_CALLS_DEFAULT;
        private SlidingWindowType circuitBreakerSlidingWindowType = CIRCUIT_BREAKER_SLIDING_WINDOW_TYPE_DEFAULT;
        private int circuitBreakerSlidingWindowSize = CIRCUIT_BREAKER_SLIDING_WINDOW_SIZE_DEFAULT;
        private int circuitBreakerSlowCallDurationThreshold = CIRCUIT_BREAKER_SLOW_CALL_DURATION_THRESHOLD_DEFAULT;
        private float circuitBreakerSlowCallRateThreshold = CIRCUIT_BREAKER_SLOW_CALL_RATE_THRESHOLD_DEFAULT;
        private List<Class> circuitBreakerIncludedExceptionList = CIRCUIT_BREAKER_INCLUDED_EXCEPTIONS_DEFAULT;
        private List<Class> circuitBreakerIgnoreExceptionList = null;
        private List<Class<? extends Throwable>> fallbackExceptionList = FALLBACK_EXCEPTIONS_DEFAULT;

        public Builder(ClusterConfig[] clusterConfigs) {

            if (clusterConfigs == null || clusterConfigs.length < 1)
                throw new JedisValidationException("ClusterClientConfigs are required for MultiClusterPooledConnectionProvider");

            for (int i = 0; i < clusterConfigs.length; i++)
                clusterConfigs[i].setPriority(i + 1);

            this.clusterConfigs = clusterConfigs;
        }

        public Builder(List<ClusterConfig> clusterConfigs) {
            this(clusterConfigs.toArray(new ClusterConfig[0]));
        }

        public Builder retryMaxAttempts(int retryMaxAttempts) {
            this.retryMaxAttempts = retryMaxAttempts;
            return this;
        }

        public Builder retryWaitDuration(int retryWaitDuration) {
            this.retryWaitDuration = retryWaitDuration;
            return this;
        }

        public Builder retryWaitDurationExponentialBackoffMultiplier(int retryWaitDurationExponentialBackoffMultiplier) {
            this.retryWaitDurationExponentialBackoffMultiplier = retryWaitDurationExponentialBackoffMultiplier;
            return this;
        }

        public Builder retryIncludedExceptionList(List<Class> retryIncludedExceptionList) {
            this.retryIncludedExceptionList = retryIncludedExceptionList;
            return this;
        }

        public Builder retryIgnoreExceptionList(List<Class> retryIgnoreExceptionList) {
            this.retryIgnoreExceptionList = retryIgnoreExceptionList;
            return this;
        }

        public Builder circuitBreakerFailureRateThreshold(float circuitBreakerFailureRateThreshold) {
            this.circuitBreakerFailureRateThreshold = circuitBreakerFailureRateThreshold;
            return this;
        }

        public Builder circuitBreakerSlidingWindowMinCalls(int circuitBreakerSlidingWindowMinCalls) {
            this.circuitBreakerSlidingWindowMinCalls = circuitBreakerSlidingWindowMinCalls;
            return this;
        }

        public Builder circuitBreakerSlidingWindowType(SlidingWindowType circuitBreakerSlidingWindowType) {
            this.circuitBreakerSlidingWindowType = circuitBreakerSlidingWindowType;
            return this;
        }

        public Builder circuitBreakerSlidingWindowSize(int circuitBreakerSlidingWindowSize) {
            this.circuitBreakerSlidingWindowSize = circuitBreakerSlidingWindowSize;
            return this;
        }

        public Builder circuitBreakerSlowCallDurationThreshold(int circuitBreakerSlowCallDurationThreshold) {
            this.circuitBreakerSlowCallDurationThreshold = circuitBreakerSlowCallDurationThreshold;
            return this;
        }

        public Builder circuitBreakerSlowCallRateThreshold(float circuitBreakerSlowCallRateThreshold) {
            this.circuitBreakerSlowCallRateThreshold = circuitBreakerSlowCallRateThreshold;
            return this;
        }

        public Builder circuitBreakerIncludedExceptionList(List<Class> circuitBreakerIncludedExceptionList) {
            this.circuitBreakerIncludedExceptionList = circuitBreakerIncludedExceptionList;
            return this;
        }

        public Builder circuitBreakerIgnoreExceptionList(List<Class> circuitBreakerIgnoreExceptionList) {
            this.circuitBreakerIgnoreExceptionList = circuitBreakerIgnoreExceptionList;
            return this;
        }

        /**
         * @deprecated Use {@link #fallbackExceptionList(java.util.List)}.
         */
        @Deprecated
        public Builder circuitBreakerFallbackExceptionList(List<Class<? extends Throwable>> circuitBreakerFallbackExceptionList) {
            return fallbackExceptionList(circuitBreakerFallbackExceptionList);
        }

        public Builder fallbackExceptionList(List<Class<? extends Throwable>> fallbackExceptionList) {
            this.fallbackExceptionList = fallbackExceptionList;
            return this;
        }

        public MultiClusterClientConfig build() {
            MultiClusterClientConfig config = new MultiClusterClientConfig(this.clusterConfigs);

            config.retryMaxAttempts = this.retryMaxAttempts;
            config.retryWaitDuration = Duration.ofMillis(this.retryWaitDuration);
            config.retryWaitDurationExponentialBackoffMultiplier = this.retryWaitDurationExponentialBackoffMultiplier;

            config.retryIncludedExceptionList = this.retryIncludedExceptionList;

            config.retryIgnoreExceptionList = this.retryIgnoreExceptionList;

            config.circuitBreakerFailureRateThreshold = this.circuitBreakerFailureRateThreshold;
            config.circuitBreakerSlidingWindowMinCalls = this.circuitBreakerSlidingWindowMinCalls;
            config.circuitBreakerSlidingWindowType = this.circuitBreakerSlidingWindowType;
            config.circuitBreakerSlidingWindowSize = this.circuitBreakerSlidingWindowSize;
            config.circuitBreakerSlowCallDurationThreshold = Duration.ofMillis(this.circuitBreakerSlowCallDurationThreshold);
            config.circuitBreakerSlowCallRateThreshold = this.circuitBreakerSlowCallRateThreshold;

            config.circuitBreakerIncludedExceptionList = this.circuitBreakerIncludedExceptionList;

            config.circuitBreakerIgnoreExceptionList = this.circuitBreakerIgnoreExceptionList;

            config.fallbackExceptionList = this.fallbackExceptionList;

            return config;
        }
    }

}