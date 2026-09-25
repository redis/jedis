# Automatic Failover and Failback with Jedis

> API was significantly changed in 7.0.0. Please follow the migration guide below.
> 
> This feature is experimental and may change in future versions.

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
* Supported retry, circuit breaker, failover and failback settings
* Initialization policies that decide when the client is ready (since 7.3.0)
* Failback and the database selection API
* Dynamic weight management for runtime priority adjustments (since 7.4.0)
* Dynamic database management for adding and removing databases at runtime

We recommend that you read this guide carefully and understand the configuration settings before enabling Jedis failover
in production.

## Migration from 6.x to 7.x

In Jedis 6.x, failover was supported using special constructor for `UnifiedJedis`.
In Jedis 7.x, failover is supported using `MultiDbClient` and `MultiDbConfig.builder`:
```java
// Jedis 6.x
JedisClientConfig config = DefaultJedisClientConfig.builder().user("cache").password("secret").build();

ClusterConfig[] clientConfigs = new ClusterConfig[2];
clientConfigs[0] = new ClusterConfig(new HostAndPort("redis-east.example.com", 14000), config);
clientConfigs[1] = new ClusterConfig(new HostAndPort("redis-west.example.com", 14000), config);

MultiClusterClientConfig.Builder builder = new MultiClusterClientConfig.Builder(clientConfigs);
// ...
MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(builder.build());
UnifiedJedis client = new UnifiedJedis(provider);

// Jedis 7.x
// MultiClusterClientConfig was renamed to MultiDbConfig and MultiDbClient with convenient builder was added
MultiDbConfig multiConfig = MultiDbConfig.builder()
        .database(DatabaseConfig.builder(east, config).weight(1.0f).build())
        .database(DatabaseConfig.builder(west, config).weight(0.5f).build())
        .build();
// Use MultiDbClient instead of UnifiedJedis
MultiDbClient multiDbClient = MultiDbClient.builder().multiDbConfig(multiConfig).build();
```
For more details on configuration options see sections below.

## Installing optional dependencies

Jedis failover support is provided by optional dependencies.
To use failover, add the following dependencies to your project:
```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-all</artifactId>
    <version>1.7.1</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-circuitbreaker</artifactId>
    <version>1.7.1</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-retry</artifactId>
    <version>1.7.1</version>
</dependency>
```

## Basic usage

To configure Jedis for failover, you specify a weighted list of Redis databases.
Jedis will connect to the Redis database in the list with the highest weight. 
If the highest-weighted database becomes unavailable,
Jedis will attempt to connect to the database with the next highest weight in the list, and so on.

Database weights determine the priority for selecting which database becomes active. Weights can be configured at initialization and can also be changed dynamically at runtime (introduced in version 7.4.0), allowing you to adjust active database selection priorities without recreating the client.

Suppose you run two Redis deployments.
We'll call them `redis-east` and `redis-west`.
You want your application to first connect to `redis-east`.
If `redis-east` becomes unavailable, you want your application to connect to `redis-west`.

Let's look at one way of configuring Jedis for this scenario.

First, start by defining the initial configuration for each Redis database available and prioritize them using weights.

```java
JedisClientConfig config = DefaultJedisClientConfig.builder()
        .user("cache").password("secret")
        .socketTimeoutMillis(5000).connectionTimeoutMillis(5000).build();

// Custom pool config per database can be provided
ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
poolConfig.setMaxTotal(8);
poolConfig.setMaxIdle(8);
poolConfig.setMinIdle(0);
poolConfig.setBlockWhenExhausted(true);
poolConfig.setMaxWait(Duration.ofSeconds(1));
poolConfig.setTestWhileIdle(true);
poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(1));

HostAndPort east = new HostAndPort("redis-east.example.com", 14000);
HostAndPort west = new HostAndPort("redis-west.example.com", 14000);

MultiDbConfig.Builder multiConfig = MultiDbConfig.builder()
        .database(DatabaseConfig.builder(east, config).connectionPoolConfig(poolConfig).weight(1.0f).build())
        .database(DatabaseConfig.builder(west, config).connectionPoolConfig(poolConfig).weight(0.5f).build());
```

The configuration above represents your two Redis deployments: `redis-east` and `redis-west`.

If you do not need a custom pool configuration or health check strategy, the shorter
`database(endpoint, weight, clientConfig)` overload builds the `DatabaseConfig` for you:

```java
MultiDbConfig.Builder multiConfig = MultiDbConfig.builder()
        .database(east, 1.0f, config)
        .database(west, 0.5f, config);
```

Continue using the `MultiDbConfig.Builder` builder to set your preferred retry and failover configuration.
Then build a `MultiDbClient`:

```java
// Configure circuit breaker for failure detection
multiConfig
        .failureDetector(MultiDbConfig.CircuitBreakerConfig.builder()
                .slidingWindowSize(10)          // Time-based sliding window, in seconds
                .failureRateThreshold(50.0f)    // Percentage of failed calls within the window
                .minNumOfFailures(100)          // Minimum number of failed calls within the window
                .build())
        .failbackSupported(true)                // Enable automatic failback (default: true)
        .failbackCheckInterval(1000)            // Check every second whether a higher-weight database has recovered
        .gracePeriod(10000)                     // Keep a database disabled for 10 seconds after it becomes unhealthy
        // Optional: configure retry settings
        .commandRetry(MultiDbConfig.RetryConfig.builder()
                .maxAttempts(3)                  // Maximum number of attempts (including the initial call)
                .waitDuration(500)               // Number of milliseconds to wait between retry attempts
                .exponentialBackoffMultiplier(2) // Exponential backoff factor multiplied against wait duration between retries
                .build())
        // Optional: configure fast failover
        .fastFailover(true)                       // Force closing connections to the unhealthy database on failover
        .retryOnFailover(false);                  // Do not re-run commands that failed during a failover on the new database

MultiDbClient multiDbClient = MultiDbClient.builder()
        .multiDbConfig(multiConfig.build())
        .build();
```

In the configuration here, we've set a sliding window of 10 seconds, a failure rate threshold of 50% and a minimum of 100 failures.
This means that a failover will be triggered only when, within the last 10 seconds, at least 100 calls to Redis have failed
**and** the failed calls make up at least 50% of all calls in that window. Both conditions must be met.

You can now use this `MultiDbClient` instance in your application to execute Redis commands.
`MultiDbClient` also provides failover-aware pipelines and transactions through `pipelined()` and `multi()`;
see the [Transactions](transactions-multi.md#notes-on-multidbtransaction) page for the `MultiDbTransaction` specifics.

## Configuration options

Under the hood, Jedis' failover support relies on [resilience4j](https://resilience4j.readme.io/docs/getting-started),
a fault-tolerance library that implements [retry](https://resilience4j.readme.io/docs/retry) and [circuit breakers](https://resilience4j.readme.io/docs/circuitbreaker).

Once you configure a `MultiDbClient`, each call to Redis is decorated with a resilience4j retry and circuit breaker.

By default, any call that throws a `JedisConnectionException` will be attempted up to 3 times (the initial call plus 2 retries).
If all attempts fail, the circuit breaker records a failure.

The circuit breaker keeps the outcome of recent calls in a time-based sliding window.
Jedis evaluates two thresholds against that window: a minimum number of failed calls and a failure rate.
When both are reached (e.g., at least 1000 calls failed within the last 2 seconds, and they make up at least 10% of all calls in that window),
the circuit breaker transitions from `CLOSED` to `OPEN`.
When this occurs, Jedis will switch to the healthy Redis database with the highest weight in its configuration.

The supported settings, and their default values, are described below.
You can configure any of these settings using the `MultiDbConfig.Builder` builder.
Refer the basic usage above for an example of this.

### Retry configuration
Configuration for command retry behavior is encapsulated in `MultiDbConfig.RetryConfig` and provided using `MultiDbConfig.Builder.commandRetry()`.
Jedis uses the following retry settings:

| Setting                          | Builder method                 | Default value              | Description                                                                                                                                                                                                     |
|----------------------------------|--------------------------------|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Max attempts                     | `maxAttempts`                  | 3                          | Maximum number of attempts, including the initial call                                                                                                                                                          |
| Retry wait duration              | `waitDuration`                 | 500 ms                     | Number of milliseconds to wait between retry attempts                                                                                                                                                           |
| Wait duration backoff multiplier | `exponentialBackoffMultiplier` | 2                          | Exponential backoff factor multiplied against wait duration between retries. For example, with a wait duration of 1 second and a multiplier of 2, the retries would occur after 1s, 2s, 4s, 8s, 16s, and so on. |
| Retry included exception list    | `includedExceptionList`        | [JedisConnectionException] | A list of Throwable classes that count as failures and should be retried.                                                                                                                                       |
| Retry ignored exception list     | `ignoreExceptionList`          | null                       | A list of Throwable classes to explicitly ignore for the purposes of retry.                                                                                                                                     |

To disable retry, set `maxAttempts` to 1.

### Circuit breaker configuration
For failover, Jedis uses a circuit breaker to detect when a Redis database has failed.
Failover configuration is encapsulated in `MultiDbConfig.CircuitBreakerConfig` and can be provided using the `MultiDbConfig.Builder.failureDetector()`.
Jedis uses the following circuit breaker settings:

| Setting                                 | Builder method          | Default value              | Description                                                                                                                                                              |
|-----------------------------------------|-------------------------|----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Sliding window size                     | `slidingWindowSize`     | 2                          | Size of the time-based sliding window, in **seconds**, over which call outcomes are recorded.                                                                            |
| Threshold min number of failures        | `minNumOfFailures`      | 1000                       | Minimum number of failed calls within the sliding window before the circuit breaker can trip. `0` disables this check so that only the failure rate is considered.       |
| Failure rate threshold                  | `failureRateThreshold`  | `10.0f`                    | Percentage of calls within the sliding window that must fail before the circuit breaker transitions to the `OPEN` state. `0.0f` disables this check so that only the minimum number of failures is considered. |
| Circuit breaker included exception list | `includedExceptionList` | [JedisConnectionException] | A list of Throwable classes that count as failures and add to the failure rate.                                                                                          |
| Circuit breaker ignored exception list  | `ignoreExceptionList`   | null                       | A list of Throwable classes to explicitly ignore for failure rate calculations.                                                                                          |

The circuit breaker opens only when **both** the minimum number of failures and the failure rate threshold are reached within the sliding window.
Once a failover has been triggered, the failed database's circuit breaker is kept open for the configured grace period
(see [Failover and failback configuration](#failover-and-failback-configuration)) so that traffic does not return to it prematurely.

### Failover and failback configuration

The following settings on `MultiDbConfig.Builder` control what happens once a failure has been detected,
and how Jedis returns to a preferred database once it recovers:

| Setting                              | Builder method                   | Default value        | Description                                                                                                                                                                                                                                              |
|--------------------------------------|----------------------------------|----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Failback supported                   | `failbackSupported`              | `true`               | Enables the periodic check that switches back to a healthy database with a higher weight than the active one.                                                                                                                                             |
| Failback check interval              | `failbackCheckInterval`          | 120000 ms (2 min)    | How often the periodic failback check runs.                                                                                                                                                                                                              |
| Grace period                         | `gracePeriod`                    | 60000 ms (1 min)     | How long a database stays disabled after it is marked unhealthy or its circuit breaker trips. During this period it is not considered for failover or failback, even if health checks report it healthy.                                                  |
| Fast failover                        | `fastFailover`                   | `false`              | When enabled, all open connections to the previously active database are forcefully closed on a switch, so in-flight operations fail immediately instead of waiting for their socket timeouts.                                                            |
| Retry on failover                    | `retryOnFailover`                | `false`              | When enabled, a command that fails on a database that has just been switched away from is re-executed on the new active database instead of surfacing the failure to the caller.                                                                        |
| Max number of failover attempts      | `maxNumFailoverAttempts`         | 10                   | How many times Jedis reports the "no healthy database" condition as temporary before it is treated as permanent. See [When no database is available](#when-no-database-is-available).                                                                     |
| Delay in between failover attempts   | `delayInBetweenFailoverAttempts` | 12000 ms             | Minimum time between two counted failover attempts while no healthy database is available.                                                                                                                                                              |
| Initialization policy                | `initializationPolicy`           | `MAJORITY_AVAILABLE` | Decides when `MultiDbClient.builder().build()` may return based on the initial health check results. See [Initialization policy](#initialization-policy).                                                                                                 |

### Initialization policy

> Introduced in version 7.3.0

When a `MultiDbClient` is created, health checks are started for every database that has them enabled,
and the client waits for their first results before selecting the initial active database.
The `InitializationPolicy` decides how many databases must be reachable before the client is considered ready,
and when initialization should fail instead. The built-in policies live in `InitializationPolicy.BuiltIn`:

| Policy               | Client is ready when...                        | Initialization fails when...                                  |
|----------------------|------------------------------------------------|---------------------------------------------------------------|
| `ALL_AVAILABLE`      | every database passed its initial health check | any database fails its initial health check                   |
| `MAJORITY_AVAILABLE` | more than half of the databases are healthy    | a majority can no longer be reached (default)                 |
| `ONE_AVAILABLE`      | at least one database is healthy               | every database failed its initial health check                |

Databases with health checks disabled are counted as available.
If the policy fails, `build()` throws a `JedisConnectionException`.

```java
MultiDbConfig config = MultiDbConfig.builder()
        .database(east, 1.0f, clientConfig)
        .database(west, 0.5f, clientConfig)
        .initializationPolicy(InitializationPolicy.BuiltIn.ONE_AVAILABLE) // start as soon as one database is reachable
        .build();
```

You can also implement `InitializationPolicy` yourself. The `evaluate` method receives the number of available,
failed and pending databases and returns `CONTINUE`, `SUCCESS` or `FAIL`.

### When no database is available

If a failover is triggered but no other healthy database exists, the command fails with one of two
`JedisConnectionException` subclasses from the `redis.clients.jedis.mcf` package:

- `JedisFailoverException.JedisTemporarilyNotAvailableException` while the number of counted failover attempts is
  at or below `maxNumFailoverAttempts`. Attempts are counted at most once per `delayInBetweenFailoverAttempts`,
  so with the defaults the condition is reported as temporary for roughly two minutes.
- `JedisFailoverException.JedisPermanentlyNotAvailableException` once that limit has been exceeded.

The attempt counter resets as soon as a switch to a healthy database succeeds.
Applications can catch the temporary variant to back off and retry, and treat the permanent variant as a signal
to alert or shut down.

### Health Check Configuration and Customization

The `MultiDbClient` includes a comprehensive health check system that continuously monitors the availability of Redis databases to enable automatic failover and failback.

The health check system serves several critical purposes in the failover architecture:

1. **Proactive Monitoring**: Continuously monitors all databases, including passive ones that aren't currently receiving traffic
2. **Failback Detection**: Determines when a previously failed database has recovered and is ready to accept traffic
3. **Circuit Breaker Integration**: Works with the circuit breaker pattern to manage database state transitions
4. **Customizable Strategies**: Supports pluggable health check implementations for different deployment scenarios

The health check system operates independently of your application traffic, running background checks at configurable intervals to assess database health without impacting performance.

#### How a health check run works

Every `HealthCheckStrategy` exposes a small set of parameters that drive the background check:

| Parameter               | Accessor                  | `HealthCheckStrategy.Config` default | Description                                                                                            |
|-------------------------|---------------------------|--------------------------------------|--------------------------------------------------------------------------------------------------------|
| Interval                | `getInterval()`           | 5000 ms                              | Time between two health check runs                                                                     |
| Timeout                 | `getTimeout()`            | 1000 ms                              | Maximum time a single probe may take; a timed out probe counts as a failure                            |
| Number of probes        | `getNumProbes()`          | 3                                    | How many times `doHealthCheck` is invoked (at most) within one run                                     |
| Delay in between probes | `getDelayInBetweenProbes()` | 500 ms                             | Pause between two probes of the same run                                                               |
| Probing policy          | `getPolicy()`             | `ProbingPolicy.BuiltIn.ALL_SUCCESS`  | Decides how the individual probe results are combined into one `HEALTHY` / `UNHEALTHY` result          |

The built-in probing policies in `ProbingPolicy.BuiltIn` are:

- `ALL_SUCCESS` — every probe must succeed; the run stops at the first failed probe.
- `ANY_SUCCESS` — one successful probe is enough; the run stops at the first successful probe.
- `MAJORITY_SUCCESS` — more than half of the probes must succeed; the run stops as soon as the outcome is decided.

The result of a run becomes the database's health status. When the active database turns unhealthy, Jedis puts it into
the grace period and switches to the healthy database with the highest weight.

#### Available Health Check Types

##### 1. PingStrategy (Default)

The `PingStrategy` is the default health check implementation. It keeps a small dedicated connection pool to the database
and sends the Redis `PING` command to verify connectivity and that the server is responding.

**Use Cases:**
- General-purpose health checking for most Redis deployments
- Simple connectivity validation

**How it works:**
- Sends `PING` command to the Redis server using the database's `JedisClientConfig` (so the same credentials, TLS settings, and timeouts apply)
- Expects exact response `"PONG"` to consider the probe successful
- Any exception, timeout, or unexpected response marks the probe as failed
- Probe results are combined according to the `HealthCheckStrategy.Config` shown above (defaults: 3 probes, all must succeed)

`PingStrategy.DEFAULT` is used when no strategy supplier is configured. To tune its interval, timeout, or probing behavior,
pass a `HealthCheckStrategy.Config` through your own supplier:

```java
HealthCheckStrategy.Config pingConfig = HealthCheckStrategy.Config.builder()
        .interval(2000)                                   // Check every 2 seconds
        .timeout(500)                                     // 500ms timeout per probe
        .numProbes(2)
        .policy(ProbingPolicy.BuiltIn.ANY_SUCCESS)
        .build();

MultiDbConfig.StrategySupplier pingSupplier =
        (hostAndPort, jedisClientConfig) -> new PingStrategy(hostAndPort, jedisClientConfig, pingConfig);

MultiDbConfig.DatabaseConfig dbConfig =
        MultiDbConfig.DatabaseConfig.builder(east, config)
                .healthCheckStrategySupplier(pingSupplier)
                .build();
```

##### 2. LagAwareStrategy [PREVIEW] (Redis Enterprise)

The `LagAwareStrategy` is designed specifically for Redis Enterprise Active-Active deployments and uses the Redis Enterprise REST API to check database availability and replication lag.

**Use Cases:**
- Redis Enterprise Active-Active (CRDB) deployments
- Scenarios where replication lag tolerance is critical
- Enterprise environments with REST API access

**How it works:**
- Queries Redis Enterprise REST API for database availability
- Optionally validates replication lag against a configurable tolerance (`extendedCheckEnabled`, default `true`; `availabilityLagTolerance`, default 5 seconds)
- Automatically discovers the database ID by matching the database endpoint's hostname against the databases exposed by the REST API

**Example Configuration:**
```java
// REST API endpoint and credentials of the Redis Enterprise cluster
HostAndPort restEndpoint = new HostAndPort("redis-enterprise-cluster-fqdn", 9443);
Supplier<RedisCredentials> restCredentials = () ->
        new DefaultRedisCredentials("rest-api-user", "pwd");

LagAwareStrategy.Config lagConfig = LagAwareStrategy.Config.builder(restEndpoint, restCredentials)
        .interval(5000)                                  // Check every 5 seconds
        .timeout(3000)                                   // 3 second timeout per REST call
        .extendedCheckEnabled(true)                      // Also validate replication lag
        .availabilityLagTolerance(Duration.ofSeconds(2)) // Tolerate up to 2 seconds of lag
        // .sslOptions(sslOptions)                       // Optional: custom truststore for the HTTPS REST API
        .build();

MultiDbConfig.StrategySupplier lagAwareSupplier =
        (hostAndPort, jedisClientConfig) -> new LagAwareStrategy(lagConfig);

MultiDbConfig.DatabaseConfig dbConfig =
        MultiDbConfig.DatabaseConfig.builder(east, config)
                .healthCheckStrategySupplier(lagAwareSupplier)
                .build();
```

`LagAwareStrategy.Config` also offers the shortcuts `databaseAvailability(...)` (availability only, no lag check),
`lagAware(...)` (default tolerance) and `lagAwareWithTolerance(...)` for the common configurations.

##### 3. Custom Health Check Strategies

You can implement custom health check strategies by implementing the `HealthCheckStrategy` interface.

**Use Cases:**
- Application-specific health validation logic
- Integration with external monitoring systems
- Custom performance or latency-based health checks

Use the `healthCheckStrategySupplier()` method to provide a custom health check implementation.
The supplier receives the database's `HostAndPort` and `JedisClientConfig`, so one supplier can serve several databases:

```java
// Custom strategy supplier
MultiDbConfig.StrategySupplier customStrategy =
        (hostAndPort, jedisClientConfig) -> {
            // Return your custom HealthCheckStrategy implementation
            return new MyCustomHealthCheckStrategy(hostAndPort, jedisClientConfig);
        };

MultiDbConfig.DatabaseConfig dbConfig =
        MultiDbConfig.DatabaseConfig.builder(east, config)
                .healthCheckStrategySupplier(customStrategy)
                .weight(1.0f)
                .build();
```

If you already have a configured strategy instance, `healthCheckStrategy(HealthCheckStrategy)` wraps it in a supplier for you.
The same instance is then reused for every database that shares that `DatabaseConfig`, so make sure it is thread-safe.

A minimal `HealthCheckStrategy` looks like this. Note that `doHealthCheck` receives the `Endpoint` being checked,
and that `close()` is called when the database is removed or the client is closed, so resources should be created once
and released there rather than per probe:

```java
public class MyCustomHealthCheckStrategy implements HealthCheckStrategy {

    private final RedisClient client;

    public MyCustomHealthCheckStrategy(HostAndPort hostAndPort, JedisClientConfig jedisClientConfig) {
        this.client = RedisClient.builder().hostAndPort(hostAndPort).clientConfig(jedisClientConfig).build();
    }

    @Override
    public int getInterval() {
        return 1000; // Check every second
    }

    @Override
    public int getTimeout() {
        return 500; // 500ms timeout per probe
    }

    @Override
    public int getNumProbes() {
        return 1;
    }

    @Override
    public ProbingPolicy getPolicy() {
        return ProbingPolicy.BuiltIn.ANY_SUCCESS;
    }

    @Override
    public int getDelayInBetweenProbes() {
        return 100;
    }

    @Override
    public HealthStatus doHealthCheck(Endpoint endpoint) {
        try {
            String result = client.ping();
            return "PONG".equals(result) ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
        } catch (Exception e) {
            return HealthStatus.UNHEALTHY;
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
```

#### Disabling Health Checks

Use the `healthCheckEnabled(false)` method to completely disable health checks for a database:

```java
MultiDbConfig.DatabaseConfig dbConfig = MultiDbConfig.DatabaseConfig.builder(east, config)
    .healthCheckEnabled(false) // Disable health checks entirely
    .build();
```

A database without health checks is always assumed healthy, so it is only taken out of rotation by its circuit breaker
(for the duration of the grace period). Failures are then detected exclusively through the commands your application executes,
and the database is considered available again as soon as the grace period ends.

### Fallback configuration

Fallback is what makes a command that hits an open circuit breaker, or that fails while a failover is in progress,
switch to the next database and run there instead of surfacing the error.
Jedis uses the following fallback settings:

| Setting                 | Builder method          | Default value                                          | Description                                        |
|-------------------------|-------------------------|--------------------------------------------------------|----------------------------------------------------|
| Fallback exception list | `fallbackExceptionList` | [CallNotPermittedException, ConnectionFailoverException] | A list of Throwable classes that trigger fallback. |

`CallNotPermittedException` is thrown by resilience4j when the circuit breaker is open.
`ConnectionFailoverException` wraps a command failure that happened on a database which is no longer active,
and is only produced when `retryOnFailover` is enabled.

### Failover callbacks

In the event that Jedis fails over, you may wish to take some action. This might include logging a warning, recording
a metric, or externally persisting the database connection state, to name just a few examples. For this reason,
`MultiDbClient` lets you register a custom callback that will be called whenever Jedis
switches to a new database.

To use this feature, you'll need to design a class that implements `java.util.function.Consumer<DatabaseSwitchEvent>`.
This class must implement the `accept` method, as you can see below.

```java
public class FailoverReporter implements Consumer<DatabaseSwitchEvent> {
    
    @Override
    public void accept(DatabaseSwitchEvent e) {
        System.out.println("Jedis switched to database: " + e.getDatabaseName() + " due to " + e.getReason());
    }
}
```

The `DatabaseSwitchEvent` consumer is registered on the client builder, next to the `MultiDbConfig`:

```java
FailoverReporter reporter = new FailoverReporter();
MultiDbClient client = MultiDbClient.builder()
        .multiDbConfig(multiConfig)
        .databaseSwitchListener(reporter)
        .build();
```
The client will call your `accept` whenever the active database changes.
You can also use a lambda expression:
```java
MultiDbClient client = MultiDbClient.builder()
        .multiDbConfig(multiConfig)
        .databaseSwitchListener(event -> System.out.println("Switched to: " + event.getEndpoint()))
        .build();
```

`DatabaseSwitchEvent.getReason()` returns a `SwitchReason` telling you why the switch happened:

| `SwitchReason`    | Triggered by                                                                                   |
|-------------------|------------------------------------------------------------------------------------------------|
| `CIRCUIT_BREAKER` | The active database's circuit breaker tripped                                                  |
| `HEALTH_CHECK`    | A health check reported the active database as unhealthy                                       |
| `FAILBACK`        | The periodic failback check found a healthy database with a higher weight                      |
| `FORCED`          | `setActiveDatabase()`, `forceActiveDatabase()`, or removal of the active database via `removeDatabase()` |

## Failing back

Jedis supports automatic failback based on health checks or manual failback using the database selection API.

### Failback scenario

When a failover is triggered, Jedis will attempt to connect to the next Redis server based on the weights of server configurations
you provide at setup.

For example, recall the `redis-east` and `redis-west` deployments from the basic usage example above.
Jedis will attempt to connect to `redis-east` first.
If `redis-east` becomes unavailable (and the circuit breaker transitions), then Jedis will attempt to use `redis-west`.

Now suppose that `redis-east` eventually comes back online.
You will likely want to fail your application back to `redis-east`.

### Automatic failback based on health checks

When health checks are enabled, Jedis automatically monitors the health of all configured databases, including those that are currently inactive due to previous failures. 
The automatic failback process works as follows:

1. **Continuous Monitoring**: Health checks run continuously for all databases, regardless of their current active status
2. **Recovery Detection**: When a health check run on a previously failed database succeeds (according to the strategy's probing policy), the database is marked as healthy
3. **Grace Period Respect**: A database is not eligible for failback until the configured grace period (`gracePeriod`, default 1 minute) has elapsed since it was marked unhealthy
4. **Weight-Based Failback**: Every `failbackCheckInterval` (default 2 minutes), if `failbackSupported` is enabled and a healthy, eligible database has a higher weight than the currently active database, Jedis switches to it and emits a `FAILBACK` switch event

With the default settings, failback can therefore take up to a few minutes after the database recovers.
Lower `failbackCheckInterval` and `gracePeriod` if you need a faster failback, keeping in mind that a short grace period
increases the risk of oscillating between databases during intermittent failures.

### Manual Failback using the database selection API

Once you've determined that it's safe to fail back to a previously-unavailable database,
you need to decide how to trigger the failback. There are two ways to accomplish this:

**`setActiveDatabase(endpoint)`** switches to the given database right away.
Jedis first validates the target by opening a connection and sending `PING`; if that fails, a `JedisValidationException`
is thrown and the active database is left unchanged. On success a `FORCED` switch event is emitted.

```java
Endpoint endpoint = new HostAndPort("redis-east.example.com", 14000);
client.setActiveDatabase(endpoint);
```

Note that automatic failback keeps running: if a healthy database with a higher weight exists, the next periodic failback
check may switch away from the database you selected. Use `forceActiveDatabase` if you want the choice to stick.

**`forceActiveDatabase(endpoint, forcedActiveDurationMs)`** switches to the given database and puts every other database
into a grace period for the given duration, so neither failover nor failback will move traffic away from it during that time.
Because the other databases are in their grace period, commands fail with the exceptions described in
[When no database is available](#when-no-database-is-available) if the pinned database itself goes down.
The target must be healthy, otherwise a `JedisValidationException` is thrown.

```java
// Pin redis-east as the active database for the next 10 minutes
client.forceActiveDatabase(endpoint, Duration.ofMinutes(10).toMillis());
```

Both methods are thread-safe.

If you decide to implement manual failback, you will need a way for external systems to trigger this method in your
application. For example, if your application exposes a REST API, you might consider creating a REST endpoint
to call `setActiveDatabase` and fail back the application.

## Dynamic Weight Management

> Introduced in version 7.4.0

Jedis allows you to dynamically adjust database weights at runtime without recreating the `MultiDbClient`.

**Important**: Weight determines the **priority for selecting which database becomes the active database**. At any given time, only ONE database is active and receives all traffic. Weight does not distribute load across databases - it determines which single database Jedis will prefer to use as the active connection.

This is useful for scenarios where you need to change the active database selection priority based on operational conditions, such as:

- Changing which database should be preferred during planned maintenance
- Adjusting selection priority based on database performance or regional preferences
- Implementing controlled switchover between databases
- Responding to changing infrastructure conditions

### Getting and Setting Weights

The `MultiDbClient` provides methods to query and modify database weights at runtime:

```java
// Get the current weight of a database
HostAndPort east = new HostAndPort("redis-east.example.com", 14000);
float currentWeight = client.getWeight(east);
System.out.println("Current weight: " + currentWeight);

// Set a new weight for a database
client.setWeight(east, 2.0f);
```

### Weight Constraints

When setting weights dynamically, the following constraints apply:

- **Weight must be greater than 0**: Attempting to set a weight of 0 or negative values will throw an `IllegalArgumentException`
- **Endpoint must exist**: The endpoint must be part of the configured databases, otherwise a `JedisValidationException` is thrown

### Runtime Behavior

When you change a database's weight at runtime:

1. **Immediate Effect on Selection**: The weight change takes effect immediately for future active database selection decisions during failover or failback
2. **Automatic Failback Trigger**: If automatic failback is enabled and you increase a database's weight above the currently active database, Jedis will automatically switch to the higher-weight database during the next periodic failback check (if the database is healthy and the grace period has elapsed)
3. **No Disruption**: Changing weights does not interrupt ongoing operations or force an immediate switch
4. **Single Active Database**: Remember that only one database is active at any time - all traffic goes to that single database

### How Weight Affects Database Selection

During failover or failback, Jedis selects the active database by:

1. Filtering for healthy databases (passing health checks, not in grace period, circuit breaker not open)
2. Sorting the healthy databases by weight in descending order (highest weight first)
3. Selecting the first database from this sorted list as the active database

### Example: Changing Active Database Priority

Here's a practical example of dynamically adjusting weights to control which database should be active:

```java
// Initial configuration - primary has higher weight, so it will be selected as active
HostAndPort primary = new HostAndPort("redis-primary.example.com", 6379);
HostAndPort secondary = new HostAndPort("redis-secondary.example.com", 6379);

MultiDbConfig config = MultiDbConfig.builder()
        .database(DatabaseConfig.builder(primary, clientConfig).weight(2.0f).build())
        .database(DatabaseConfig.builder(secondary, clientConfig).weight(1.0f).build())
        .failbackSupported(true)
        .failbackCheckInterval(1000)
        .build();

MultiDbClient client = MultiDbClient.builder()
        .multiDbConfig(config)
        .build();

// At this point, 'primary' is the active database (weight 2.0 > 1.0)

// Before planned maintenance on primary, make secondary the preferred database
client.setWeight(secondary, 3.0f);  // Now secondary has highest weight
// During the next failback check, Jedis will switch to secondary as the active database

// After maintenance, restore primary as the preferred database
client.setWeight(primary, 4.0f);  // Now primary has highest weight again
// During the next failback check, Jedis will switch back to primary as the active database
```

### Monitoring Database Switches

You can combine weight changes with failover callbacks to monitor when the active database switches due to weight adjustments:

```java
MultiDbClient client = MultiDbClient.builder()
        .multiDbConfig(config)
        .databaseSwitchListener(event -> {
            System.out.println("Active database switched to: " + event.getEndpoint() +
                             " due to: " + event.getReason());
        })
        .build();

// Change weight - may trigger automatic failback to switch active database
client.setWeight(secondary, 5.0f);
```

## Dynamic Database Management

Jedis allows you to dynamically add and remove database endpoints at runtime without recreating the `MultiDbClient`. This provides flexibility for scenarios such as:

- Adding new database replicas or regions as they become available
- Removing databases during planned maintenance or decommissioning
- Scaling your Redis infrastructure dynamically
- Responding to infrastructure changes without application restarts

### Adding Databases at Runtime

The `MultiDbClient` provides two overloaded methods for adding databases dynamically:

#### Method 1: Using DatabaseConfig

This method provides maximum flexibility for advanced configurations including custom health check strategies, connection pool settings, and other database-specific options.

```java
// Create a fully configured DatabaseConfig
HostAndPort newEndpoint = new HostAndPort("redis-new.example.com", 6379);
JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .user("cache").password("secret").build();

DatabaseConfig databaseConfig = DatabaseConfig.builder(newEndpoint, clientConfig)
        .weight(1.5f).connectionPoolConfig(poolConfig).healthCheckEnabled(true).build();

// Add the database to the client
client.addDatabase(databaseConfig);
```

#### Method 2: Using Endpoint, Weight, and ClientConfig

This is a convenience method for simpler configurations when you don't need advanced customization.

```java
HostAndPort newEndpoint = new HostAndPort("redis-new.example.com", 6379);
JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .user("cache").password("secret").build();

// Add the database with basic configuration
client.addDatabase(newEndpoint, 1.5f, clientConfig);
```

### Removing Databases at Runtime

You can remove database endpoints dynamically using the `removeDatabase()` method:

```java
HostAndPort endpointToRemove = new HostAndPort("redis-old.example.com", 6379);

// Remove the database from the client
client.removeDatabase(endpointToRemove);
```

### Behavior and Constraints

When adding or removing databases, the following behavior applies:

#### Adding Databases

- **Immediate Availability**: The new endpoint becomes available for failover operations immediately after being added and, if health checks are enabled, after its first health check run succeeds
- **Health Check Integration**: If health checks are configured, the new database will be monitored according to the configured health check strategy
- **Duplicate Prevention**: Attempting to add an endpoint that already exists will throw a `JedisValidationException`
- **Weight-Based Selection**: The new database participates in weight-based active database selection according to its configured weight. Adding a database does not switch the active database by itself; if the new database has the highest weight, the next periodic failback check will switch to it

#### Removing Databases

- **Automatic Failover**: If the removed endpoint is currently the active database, Jedis will automatically switch to the healthy endpoint with the highest weight and emit a `FORCED` switch event. If no other healthy endpoint exists, the removal is rejected with a `JedisException`
- **Last Database Protection**: You cannot remove the last remaining endpoint - attempting to do so will throw a `JedisValidationException`
- **Non-Existent Endpoint**: Attempting to remove an endpoint that doesn't exist will throw a `JedisValidationException`
- **Resource Cleanup**: The removed database's connections and resources are properly closed and cleaned up
- **Health Check Cleanup**: Health checks for the removed database are automatically stopped and unregistered

### Querying Configured Databases

You can retrieve the set of all currently configured database endpoints, inspect their health, and find out which one is active:

```java
Set<Endpoint> endpoints = client.getDatabaseEndpoints();
System.out.println("Configured databases: " + endpoints);

Endpoint active = client.getActiveDatabaseEndpoint();
System.out.println("Active database: " + active);

for (Endpoint endpoint : endpoints) {
    System.out.println(endpoint + " healthy: " + client.isHealthy(endpoint));
}
```

`isHealthy(endpoint)` reflects the same view Jedis uses for failover decisions: the database's health check status,
whether its circuit breaker is open, and whether it is currently in a grace period.

### Complete Example: Dynamic Database Management

Here's a practical example demonstrating dynamic database management:

```java
// Initial setup with two databases, primary and secondary.
HostAndPort primary = new HostAndPort("redis-primary.example.com", 6379);
HostAndPort secondary = new HostAndPort("redis-secondary.example.com", 6379);

JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .user("cache").password("secret").build();

MultiDbConfig config = MultiDbConfig.builder()
        .database(DatabaseConfig.builder(primary, clientConfig).weight(2.0f).build())
        .database(DatabaseConfig.builder(secondary, clientConfig).weight(1.0f).build())
        .failbackSupported(true)
        .build();

MultiDbClient client = MultiDbClient.builder()
        .multiDbConfig(config)
        .databaseSwitchListener(event -> {
            System.out.println("Switched to: " + event.getEndpoint() +
                             " due to: " + event.getReason());
        })
        .build();

// Add a new database in a different region
HostAndPort newRegion = new HostAndPort("redis-eu.example.com", 6379);
client.addDatabase(newRegion, 1.0f, clientConfig);
System.out.println("Added new database: " + newRegion);

// Verify the database was added
Set<Endpoint> endpoints = client.getDatabaseEndpoints();
System.out.println("Current databases: " + endpoints);
// Output: [redis-primary.example.com:6379, redis-secondary.example.com:6379, redis-eu.example.com:6379]

// Later, remove the secondary database for maintenance
client.removeDatabase(secondary);
System.out.println("Removed database: " + secondary);
// If secondary was active, automatic failover occurs to primary or newRegion

// Verify the database was removed
endpoints = client.getDatabaseEndpoints();
System.out.println("Current databases: " + endpoints);
// Output: [redis-primary.example.com:6379, redis-eu.example.com:6379]

```

### Thread Safety

Both `addDatabase()` and `removeDatabase()` methods are thread-safe and can be called concurrently from multiple threads. The client ensures that database additions and removals are properly synchronized with ongoing operations.

## Troubleshooting Failover and Failback Issues

#### Health Checks Always Report Unhealthy

**Common causes:**
- Timeout too aggressive for network conditions
- Authentication issues with Redis server (the health check uses the database's `JedisClientConfig`)
- Network connectivity problems

**Solutions:**
```java
// Increase timeout values
HealthCheckStrategy.Config hcConfig = HealthCheckStrategy.Config.builder()
    .timeout(3000)  // Increase from default 1000ms
    .build();

DatabaseConfig dbConfig = DatabaseConfig.builder(east, config)
    .healthCheckStrategySupplier((hostAndPort, clientConfig) -> new PingStrategy(hostAndPort, clientConfig, hcConfig))
    .build();
```

#### Intermittent Health Check Failures

**Solutions:**
```java
// Tolerate a single failed probe instead of requiring all probes to succeed
HealthCheckStrategy.Config hcConfig = HealthCheckStrategy.Config.builder()
    .interval(5000)                                  // Less frequent checks
    .timeout(2000)                                   // More generous timeout
    .numProbes(3)
    .policy(ProbingPolicy.BuiltIn.MAJORITY_SUCCESS)  // 2 out of 3 probes must succeed
    .build();
```

#### Slow Failback After Recovery

With the defaults, a recovered database is picked up again only after its 1 minute grace period has passed
**and** the next failback check runs, which happens every 2 minutes.

**Solutions:**
```java
// Faster recovery detection
HealthCheckStrategy.Config hcConfig = HealthCheckStrategy.Config.builder()
    .interval(1000)                    // More frequent checks
    .build();

// Adjust failback timing
MultiDbConfig multiConfig = MultiDbConfig.builder()
        .database(east, 1.0f, config)
        .database(west, 0.5f, config)
        .failbackCheckInterval(5000)       // Look for a better database every 5 seconds
        .gracePeriod(10000)                // Shorter grace period
        .build();
```

## Need help or have questions?
For assistance with this automatic failover and failback feature,
[start a discussion](https://github.com/redis/jedis/discussions/new?category=q-a).
