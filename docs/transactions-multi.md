# Transactions/Multi

## Overview

Transactions guarantee that all the included commands will execute to completion without being interrupted by commands from other clients. See the [Transactions](https://redis.io/docs/latest/develop/interact/transactions/) page for more information.

To execute commands in a transaction, create a transaction object with the `multi()` command, call command methods on that object, and then call the transaction object's `exec()` method to execute it. You can access the results from commands in the transaction using `Response` objects. The `exec()` method also returns a `List<Object>` value that contains all the result values in the order the commands were executed.


## Immediate Transaction Start

The simplest way to use transactions is to call `multi()` on your Jedis client, which immediately starts a transaction by sending the `MULTI` command to Redis. All subsequent commands are queued until `exec()` is called.

```java
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.AbstractTransaction;
import redis.clients.jedis.Response;
import java.util.List;

RedisClient jedis = RedisClient.create("redis://localhost:6379");

// Create a transaction that immediately sends MULTI
try (AbstractTransaction tx = jedis.multi()) {
    // Commands are queued
    Response<String> set1 = tx.set("counter:1", "0");
    Response<Long> incr1 = tx.incrBy("counter:1", 1);
    Response<Long> incr2 = tx.incrBy("counter:1", 2);
    
    // Execute the transaction
    List<Object> results = tx.exec();
    
    // Access results via Response objects
    System.out.println(incr1.get()); // 1
    System.out.println(incr2.get()); // 3
    
    // Or via the results list
    System.out.println(results.get(0)); // OK
    System.out.println(results.get(1)); // 1
    System.out.println(results.get(2)); // 3
}

jedis.close();
```

### Response Handling

Commands invoked within a transaction return `Response<T>` objects. These responses become available only after `exec()` is called:

- Before `exec()`: Calling `response.get()` will throw `IllegalStateException` with the message "Please close pipeline or multi block before calling this method."
- After `exec()`: Response objects contain the actual results from Redis

The `exec()` method returns a `List<Object>` containing all command results in the order they were queued.

## Manual Transaction Start

For more control, you can create a transaction without immediately sending `MULTI`. This is useful when you need to:

- Execute commands before starting the transaction
- Use `WATCH` to implement optimistic locking
- Conditionally start the transaction

Create a manual transaction by passing `false` to the `transaction()` method:

```java
RedisClient jedis = RedisClient.create("redis://localhost:6379");

// Create transaction without sending MULTI
try (AbstractTransaction tx = jedis.transaction(false)) {
    
    // Commands before multi() are executed immediately
    Response<String> setBeforeMulti = tx.set("mykey", "initial_value");
    Response<String> getBeforeMulti = tx.get("mykey");
    
    // These responses are available immediately
    System.out.println(setBeforeMulti.get()); // OK
    System.out.println(getBeforeMulti.get()); // initial_value
    
    // Now start the transaction
    tx.multi();
    
    // Commands after multi() are queued
    Response<String> set = tx.set("mykey", "new_value");
    Response<String> get = tx.get("mykey");
    
    // Execute the transaction
    List<Object> results = tx.exec();
    
    // Results from queued commands
    System.out.println(set.get()); // OK
    System.out.println(get.get()); // new_value
}

jedis.close();
```

### Using WATCH for Optimistic Locking

The `WATCH` command monitors keys for changes. If any watched key is modified before `EXEC`, the transaction is aborted and `exec()` returns `null`.

**Important:** `WATCH` must be executed through the transaction object to ensure it uses the transaction's dedicated connection.

```java
try (AbstractTransaction tx = jedis.transaction(false)) {
    // WATCH must be on the transaction's dedicated connection
    tx.watch("counter");

    // Read current value - can use the client directly
    String current = jedis.get("counter");
    int newValue = Integer.parseInt(current) + 1;

    // Start transaction and queue update
    tx.multi();
    tx.set("counter", String.valueOf(newValue));

    // Returns null if key was modified by another client
    List<Object> results = tx.exec();

    if (results == null) {
        System.out.println("Transaction aborted - key was modified");
    }
}
```

## Connection Lifecycle

A transaction acquires a dedicated connection that is held until `close()` is called. Ensure the transaction is closed on error:

```java
try (AbstractTransaction tx = jedis.multi()) {
    tx.set("key", "value");
    tx.exec();
}
```

**Important:** `WATCH` must be executed through the transaction object to ensure it uses the transaction's dedicated connection.



## Transaction Completion

Complete a transaction by calling either:

- **`exec()`** - Executes all queued commands atomically and returns a `List<Object>` with the results
- **`discard()`** - Discards all queued commands without executing them

### Automatic Cleanup

When using try-with-resources, `close()` automatically sends `DISCARD` (if in `MULTI` state) or `UNWATCH` (if in `WATCH` state) to ensure the connection is returned to the pool in a clean state.

## Notes on `MultiDbTransaction`

`MultiDbTransaction` (returned by `multi()` / `transaction(...)` on a multi-database client) is annotated `@Experimental` and behaves slightly differently from the single-endpoint transaction.

### Connection Lifecycle

- When started with `multi()` (i.e. `doMulti=true`), connection acquisition is **deferred** until `exec()` (or `discard()` / `close()`). Commands issued after `MULTI` are buffered in memory only.
- When started with `transaction(false)` (i.e. `doMulti=false`), a connection is acquired on the first command that needs to be sent to the server (e.g. `watch(...)` or any pre-`MULTI` read/write) and held until the transaction is completed.
- A `MultiDbTransaction` is bound to the database that was active **at the time the connection was acquired**, and the connection is borrowed from that database's pool. Subsequent database failovers do not migrate the in-flight transaction to the new endpoint.
- On `exec()`, `discard()` and `close()` the held connection is released back to its originating pool (via `Connection.close()`) in the `finally` block, regardless of success or failure. The next operation after release will use a fresh connection from the currently active database.

### Database-switch Semantics

If a failover changes the active database while a `MultiDbTransaction` is in progress, the transaction will not silently complete against a different endpoint:

- For commands sent **before** `MULTI` (pre-`MULTI` traffic on a manual transaction), the next `appendCommand` call detects the switch and throws `JedisException("Active database has changed since transaction started")` without sending the command.
- On `exec()`, after queueing commands on the server, the active database is re-checked. If it has changed, the transaction sends `DISCARD` to the original connection so the server-side `MULTI` block is rolled back, then throws `JedisException("Active database has changed since transaction started")`. Any error from the `DISCARD` attempt is attached as a suppressed exception.
- In both cases the held connection is still released via the normal `finally`-block path, so it is returned to its (original) pool and subsequent operations go through the now-active database.


