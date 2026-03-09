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



