# Automatic Failover and Failback with Jedis

Jedis supports failover and failback for your Redis deployments. This is useful when:
1. You have more than one Redis deployment. This might include two independent Redis servers or two or more Redis databases replicated across multiple [active-active Redis Enterprise](https://docs.redis.com/latest/rs/databases/active-active/) clusters.
2. You want your application to connect to and use one deployment at a time.
3. You want your application to fail over to the next available deployment if the current deployment becomes unavailable.
4. You want your application to fail back to the original deployment when it becomes available again.

Jedis will fail over to a subsequent Redis deployment after reaching a configurable failure threshold.
This failure threshold is implemented using a [circuit breaker pattern](https://en.wikipedia.org/wiki/Circuit_breaker_design_pattern).

You can also configure Jedis to retry failed calls to Redis.
Once a maximum number of retries have been exhausted, the circuit breaker will record a failure.
When the circuit breaker reaches its failure threshold, a failover will be triggered on the subsequent operation.
In the background, Jedis executes configured health checks to determine when a Redis deployment is available again.
When this occurs, Jedis will fail back to the original deployment after a configurable grace period.

The remainder of this guide describes:

* A basic failover and health check configuration
* Supported retry and circuit breaker settings
* Failback and the cluster selection API

We recommend that you read this guide carefully and understand the configuration settings before enabling Jedis failover
in production.

## Basic usage

To configure Jedis for failover, you specify a weighted list of Redis databases.
Jedis will connect to the Redis database in the list with the highest weight. 
If the highest-weighted database becomes unavailable,
Jedis will attempt to connect to the database with the next highest weight in the list, and so on.

Suppose you run two Redis deployments.
We'll call them `redis-east` and `redis-west`.
You want your application to first connect to `redis-east`.
If `redis-east` becomes unavailable, you want your application to connect to `redis-west`.

Let's look at one way of configuring Jedis for this scenario.

First, create an array of `ClusterConfig` objects, one for each Redis database.

```java
JedisClientConfig config = DefaultJedisClientConfig.builder().user("cache").password("secret")
    .socketTimeoutMillis(5000).connectionTimeoutMillis(5000).build();

ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
poolConfig.setMaxTotal(8);
poolConfig.setMaxIdle(8);
poolConfig.setMinIdle(0);
poolConfig.setBlockWhenExhausted(true);
poolConfig.setMaxWait(Duration.ofSeconds(1));
poolConfig.setTestWhileIdle(true);
poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(1));

MultiClusterClientConfig.ClusterConfig[] clusterConfig = new MultiClusterClientConfig.ClusterConfig[2];
HostAndPort east = new HostAndPort("redis-east.example.com", 14000);
clusterConfig[0] = ClusterConfig.builder(east, config).connectionPoolConfig(poolConfig).weight(1.0f).build();

HostAndPort west = new HostAndPort("redis-west.example.com", 14000);
clusterConfig[1] = ClusterConfig.builder(west, config).connectionPoolConfig(poolConfig).weight(0.5f).build();
```

The configuration above represents your two Redis deployments: `redis-east` and `redis-west`.
You'll use this array of configuration objects to create a connection provider that supports failover.

Use the `MultiClusterClientConfig` builder to set your preferred retry and failover configuration, passing in the client configs you just created.
Then build a `MultiClusterPooledConnectionProvider`.

```java
MultiClusterClientConfig.Builder builder = new MultiClusterClientConfig.Builder(clientConfigs);
builder.circuitBreakerSlidingWindowSize(10); // Sliding window size in number of calls
builder.circuitBreakerSlidingWindowMinCalls(1);
builder.circuitBreakerFailureRateThreshold(50.0f); // percentage of failures to trigger circuit breaker

builder.failbackSupported(true); // Enable failback
builder.failbackCheckInterval(1000); // Check every second the unhealthy cluster to see if it has recovered
builder.gracePeriod(10000); // Keep cluster disabled for 10 seconds after it becomes unhealthy

// Optional: configure retry settings
builder.retryMaxAttempts(3); // Maximum number of retry attempts (including the initial call)
builder.retryWaitDuration(500); // Number of milliseconds to wait between retry attempts
builder.retryWaitDurationExponentialBackoffMultiplier(2); // Exponential backoff factor multiplied against wait duration between retries

// Optional: configure fast failover
builder.fastFailover(true); // Force closing connections to unhealthy cluster on failover
builder.retryOnFailover(false); // Do not retry failed commands during failover

MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(builder.build());
```

Internally, the connection provider uses a [highly configurable circuit breaker and retry implementation](https://resilience4j.readme.io/docs/circuitbreaker) to determine when to fail over.
In the configuration here, we've set a sliding window size of 10 and a failure rate threshold of 50%.
This means that a failover will be triggered if 5 out of any 10 calls to Redis fail.

Once you've configured and created a `MultiClusterPooledConnectionProvider`, instantiate a `UnifiedJedis` instance for your application, passing in the provider you just created:

```java
UnifiedJedis jedis = new UnifiedJedis(provider);
```

You can now use this `UnifiedJedis` instance, and the connection management and failover will be handled transparently.

## Configuration options

Under the hood, Jedis' failover support relies on [resilience4j](https://resilience4j.readme.io/docs/getting-started),
a fault-tolerance library that implements [retry](https://resilience4j.readme.io/docs/retry) and [circuit breakers](https://resilience4j.readme.io/docs/circuitbreaker).

Once you configure Jedis for failover using the `MultiClusterPooledConnectionProvider`, each call to Redis is decorated with a resilience4j retry and circuit breaker.

By default, any call that throws a `JedisConnectionException` will be retried up to 3 times.
If the call continues to fail after the maximum number of retry attempts, then the circuit breaker will record a failure.

The circuit breaker maintains a record of failures in a sliding window data structure.
If the failure rate reaches a configured threshold (e.g., when 50% of the last 10 calls have failed),
then the circuit breaker's state transitions from `CLOSED` to `OPEN`.
When this occurs, Jedis will attempt to connect to the next Redis database with the highest weight in its client configuration list.

The supported retry and circuit breaker settings, and their default values, are described below.
You can configure any of these settings using the `MultiClusterClientConfig.Builder` builder.
Refer the basic usage above for an example of this.

### Retry configuration

Jedis uses the following retry settings:

| Setting                          | Default value              | Description                                                                                                                                                                                                     |
|----------------------------------|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Max retry attempts               | 3                          | Maximum number of retry attempts (including the initial call)                                                                                                                                                   |
| Retry wait duration              | 500 ms                     | Number of milliseconds to wait between retry attempts                                                                                                                                                           |
| Wait duration backoff multiplier | 2                          | Exponential backoff factor multiplied against wait duration between retries. For example, with a wait duration of 1 second and a multiplier of 2, the retries would occur after 1s, 2s, 4s, 8s, 16s, and so on. |
| Retry included exception list    | [JedisConnectionException] | A list of Throwable classes that count as failures and should be retried.                                                                                                                                       |
| Retry ignored exception list     | null                       | A list of Throwable classes to explicitly ignore for the purposes of retry.                                                                                                                                     |

To disable retry, set `maxRetryAttempts` to 1.

### Circuit breaker configuration

Jedis uses the following circuit breaker settings:

| Setting                                 | Default value              | Description                                                                                                                                                                   |
|-----------------------------------------|----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Sliding window type                     | `COUNT_BASED`              | The type of sliding window used to record the outcome of calls. Options are `COUNT_BASED` and `TIME_BASED`.                                                                   |
| Sliding window size                     | 100                        | The size of the sliding window. Units depend on sliding window type. When `COUNT_BASED`, the size represents number of calls. When `TIME_BASED`, the size represents seconds. |
| Sliding window min calls                | 100                        | Minimum number of calls required (per sliding window period) before the CircuitBreaker will start calculating the error rate or slow call rate.                               |
| Failure rate threshold                  | `50.0f`                    | Percentage of calls within the sliding window that must fail before the circuit breaker transitions to the `OPEN` state.                                                      |
| Slow call duration threshold            | 60000 ms                   | Duration threshold above which calls are classified as slow and added to the sliding window.                                                                                  |
| Slow call rate threshold                | `100.0f`                   | Percentage of calls within the sliding window that exceed the slow call duration threshold before circuit breaker transitions to the `OPEN` state.                            |
| Circuit breaker included exception list | [JedisConnectionException] | A list of Throwable classes that count as failures and add to the failure rate.                                                                                               |
| Circuit breaker ignored exception list  | null                       | A list of Throwable classes to explicitly ignore for failure rate calculations.                                                                                               |                                                                                                               |

### Health Check Configuration and Customization

The `MultiClusterPooledConnectionProvider` includes a comprehensive health check system that continuously monitors the availability of Redis clusters to enable automatic failover and failback.

The health check system serves several critical purposes in the failover architecture:

1. **Proactive Monitoring**: Continuously monitors passive clusters that aren't currently receiving traffic
2. **Failback Detection**: Determines when a previously failed cluster has recovered and is ready to accept traffic
3. **Circuit Breaker Integration**: Works with the circuit breaker pattern to manage cluster state transitions
4. **Customizable Strategies**: Supports pluggable health check implementations for different deployment scenarios

The health check system operates independently of your application traffic, running background checks at configurable intervals to assess cluster health without impacting performance.

#### Available Health Check Types

##### 1. EchoStrategy (Default)

The `EchoStrategy` is the default health check implementation that uses Redis's `ECHO` command to verify both connectivity and write capability.

**Use Cases:**
- General-purpose health checking for most Redis deployments
- Verifying both read and write operations
- Simple connectivity validation

**How it works:**
- Sends `ECHO "HealthCheck"` command to the Redis server
- Expects exact response `"HealthCheck"` to consider the server healthy
- Any exception or unexpected response marks the server as unhealthy

##### 2. LagAwareStrategy [PREVIEW] (Redis Enterprise)

The `LagAwareStrategy` is designed specifically for Redis Enterprise Active-Active deployments and uses the Redis Enterprise REST API to check database availability and replication lag.

**Use Cases:**
- Redis Enterprise Active-Active (CRDB) deployments
- Scenarios where replication lag tolerance is critical
- Enterprise environments with REST API access

**How it works:**
- Queries Redis Enterprise REST API for database availability
- Optionally validates replication lag against configurable thresholds
- Automatically discovers database IDs based on endpoint hostnames

**Example Configuration:**
```java
BiFunction<HostAndPort, Supplier<RedisCredentials>, MultiClusterClientConfig.StrategySupplier> healthCheckStrategySupplier =
(HostAndPort clusterHostPort, Supplier<RedisCredentials> credentialsSupplier) -> {
  LagAwareStrategy.Config lagConfig = LagAwareStrategy.Config.builder(clusterHostPort, credentialsSupplier)
      .interval(5000)                                          // Check every 5 seconds
      .timeout(3000)                                           // 3 second timeout
      .extendedCheckEnabled(true)
      .build();

  return (hostAndPort, jedisClientConfig) -> new LagAwareStrategy(lagConfig);
};

// Configure REST API endpoint and credentials
Endpoint restEndpoint = new HostAndPort("redis-enterprise-cluster-fqdn", 9443);
Supplier<RedisCredentials> credentialsSupplier = () -> 
    new DefaultRedisCredentials("rest-api-user", "pwd");

MultiClusterClientConfig.StrategySupplier lagawareStrategySupplier = healthCheckStrategySupplier.apply(
    restEndpoint, credentialsSupplier);

MultiClusterClientConfig.ClusterConfig clusterConfig =
    MultiClusterClientConfig.ClusterConfig.builder(hostAndPort, clientConfig)
        .healthCheckStrategySupplier(lagawareStrategySupplier)
        .build();
```

##### 3. Custom Health Check Strategies

You can implement custom health check strategies by implementing the `HealthCheckStrategy` interface.

**Use Cases:**
- Application-specific health validation logic
- Integration with external monitoring systems
- Custom performance or latency-based health checks

Use the `healthCheckStrategySupplier()` method to provide a custom health check implementation:

```java
// Custom strategy supplier
MultiClusterClientConfig.StrategySupplier customStrategy =
    (hostAndPort, jedisClientConfig) -> {
        // Return your custom HealthCheckStrategy implementation
        return new MyCustomHealthCheckStrategy(hostAndPort, jedisClientConfig);
    };

MultiClusterClientConfig.ClusterConfig clusterConfig =
    MultiClusterClientConfig.ClusterConfig.builder(hostAndPort, clientConfig)
        .healthCheckStrategySupplier(customStrategy)
        .weight(1.0f)
        .build();
```

You can implement custom health check strategies by implementing the `HealthCheckStrategy` interface:

```java
MultiClusterClientConfig.StrategySupplier pingStrategy = (hostAndPort, jedisClientConfig) -> {
    return new HealthCheckStrategy() {
        @Override
        public int getInterval() {
            return 1000; // Check every second
        }

        @Override
        public int getTimeout() {
            return 500; // 500ms timeout
        }

        @Override
        public int minConsecutiveSuccessCount() {
            return 1; // Single success required
        }

        @Override
        public HealthStatus doHealthCheck(Endpoint endpoint) {
            try (UnifiedJedis jedis = new UnifiedJedis(hostAndPort, jedisClientConfig)) {
                String result = jedis.ping();
                return "PONG".equals(result) ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
            } catch (Exception e) {
                return HealthStatus.UNHEALTHY;
            }
        }

        @Override
        public void close() {
            // Cleanup resources if needed
        }
    };
};

MultiClusterClientConfig.ClusterConfig clusterConfig =
    MultiClusterClientConfig.ClusterConfig.builder(hostAndPort, clientConfig)
        .healthCheckStrategySupplier(pingStrategy)
        .build();
```

#### Disabling Health Checks

Use the `healthCheckEnabled(false)` method to completely disable health checks:

```java
clusterConfig[0] = ClusterConfig.builder(east, config)
    .healthCheckEnabled(false) // Disable health checks entirely
    .build();
```

### Fallback configuration

Jedis uses the following fallback settings:

| Setting                 | Default value                                         | Description                                        |
|-------------------------|-------------------------------------------------------|----------------------------------------------------|
| Fallback exception list | [CallNotPermittedException, JedisConnectionException] | A list of Throwable classes that trigger fallback. |

### Failover callbacks

In the event that Jedis fails over, you may wish to take some action. This might include logging a warning, recording
a metric, or externally persisting the cluster connection state, to name just a few examples. For this reason,
`MultiPooledConnectionProvider` lets you register a custom callback that will be called whenever Jedis
fails over to a new cluster.

To use this feature, you'll need to design a class that implements `java.util.function.Consumer`.
This class must implement the `accept` method, as you can see below.

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class FailoverReporter implements Consumer<ClusterSwitchEventArgs> {

    @Override
    public void accept(ClusterSwitchEventArgs e) {
        Logger logger = LoggerFactory.getLogger(FailoverReporter.class);
        logger.warn("Jedis failover to cluster: " + e.getClusterName() + " due to " + e.getReason());
    }
}
```

You can then pass an instance of this class to your `MultiPooledConnectionProvider`.

```
FailoverReporter reporter = new FailoverReporter();
provider.setClusterSwitchListener(reporter);
```

The provider will call your `accept` whenever a failover occurs.

## Failing back

Jedis supports automatic failback based on health checks or manual failback using the cluster selection API.

## Failback scenario

When a failover is triggered, Jedis will attempt to connect to the next Redis server based on the weights of server configurations
you provide at setup.

For example, recall the `redis-east` and `redis-west` deployments from the basic usage example above.
Jedis will attempt to connect to `redis-east` first.
If `redis-east` becomes unavailable (and the circuit breaker transitions), then Jedis will attempt to use `redis-west`.

Now suppose that `redis-east` eventually comes back online.
You will likely want to fail your application back to `redis-east`.

### Automatic failback based on health checks

When health checks are enabled, Jedis automatically monitors the health of all configured clusters, including those that are currently inactive due to previous failures. 
The automatic failback process works as follows:

1. **Continuous Monitoring**: Health checks run continuously for all clusters, regardless of their current active status
2. **Recovery Detection**: When a previously failed cluster passes the required number of consecutive health checks, it's marked as healthy
3. **Weight-Based Failback**: If automatic failback is enabled and a recovered cluster has a higher weight than the currently active cluster, Jedis will automatically switch to the recovered cluster
4. **Grace Period Respect**: Failback only occurs after the configured grace period has elapsed since the cluster was marked as unhealthy

## Manual Failback using the cluster selection API

Once you've determined that it's safe to fail back to a previously-unavailable cluster,
you need to decide how to trigger the failback. There are two ways to accomplish this:

`MultiClusterPooledConnectionProvider` exposes a method that you can use to manually select which cluster Jedis should use.
To select a different cluster to use, pass the cluster's `HostAndPort` to `setActiveCluster()`:
```
provider.setActiveCluster(west);
```

This method is thread-safe.

If you decide to implement manual failback, you will need a way for external systems to trigger this method in your
application. For example, if your application exposes a REST API, you might consider creating a REST endpoint
to call `setActiveCluster` and fail back the application.

## Troubleshooting Failover and Failback Issues

#### Health Checks Always Report Unhealthy

**Common causes:**
- Timeout too aggressive for network conditions
- Authentication issues with Redis server
- Network connectivity problems

**Solutions:**
```java
// Increase timeout values
HealthCheckStrategy.Config config = HealthCheckStrategy.Config.builder()
    .timeout(3000)  // Increase from default 1000ms
    .build();
```

#### Intermittent Health Check Failures

**Solutions:**
```java
// Require more consecutive successes for stability
HealthCheckStrategy.Config config = HealthCheckStrategy.Config.builder()
    .interval(5000)                 // Less frequent checks
    .timeout(2000)                  // More generous timeout
    .build();
```

#### Slow Failback After Recovery

**Solutions:**
```java
// Faster recovery configuration
HealthCheckStrategy.Config config = HealthCheckStrategy.Config.builder()
    .interval(1000)                    // More frequent checks
    .build();

// Adjust failback timing
MultiClusterClientConfig multiConfig = new MultiClusterClientConfig.Builder(clusterConfigs)
    .gracePeriod(5000)                 // Shorter grace period
    .build();
```

## Need help or have questions?
For assistance with this automatic failover and failback feature,
[start a discussion](https://github.com/redis/jedis/discussions/new?category=q-a).
