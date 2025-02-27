# Advanced Usage

## Transactions

To do transactions in Jedis, you have to wrap operations in a transaction block, very similar to pipelining:

```java
jedis.watch (key1, key2, ...);
Transaction t = jedis.multi();
t.set("foo", "bar");
t.exec();
```

Note: when you have any method that returns values, you have to do like this:


```java
Transaction t = jedis.multi();
t.set("fool", "bar"); 
Response<String> result1 = t.get("fool");

t.zadd("foo", 1, "barowitch"); t.zadd("foo", 0, "barinsky"); t.zadd("foo", 0, "barikoviev");
Response<Set<String>> sose = t.zrange("foo", 0, -1);   // get the entire sortedset
t.exec();                                              // dont forget it

String foolbar = result1.get();                       // use Response.get() to retrieve things from a Response
int soseSize = sose.get().size();                      // on sose.get() you can directly call Set methods!

// List<Object> allResults = t.exec();                 // you could still get all results at once, as before
```
Note that a Response Object does not contain the result before t.exec() is called (it is a kind of a Future). Forgetting exec gives you exceptions. In the last lines, you see how transactions/pipelines were dealt with before version 2. You can still do it that way, but then you need to extract objects from a list, which contains also Redis status messages.

Note 2: Redis does not allow to use intermediate results of a transaction within that same transaction. This does not work:

```java
// this does not work! Intra-transaction dependencies are not supported by Redis!
jedis.watch(...);
Transaction t = jedis.multi();
if(t.get("key1").equals("something"))
   t.set("key2", "value2");
else 
   t.set("key", "value");
```

However, there are some commands like setnx, that include such a conditional execution. Those are of course supported within transactions. You can build your own customized commands using EVAL / LUA scripting. 


## Pipelining

Sometimes you need to send a bunch of different commands. A very cool way to do that, and have better performance than doing it the naive way, is to use pipelining. This way you send commands without waiting for response, and you actually read the responses at the end, which is faster. 

Here is how to do it:

```java
Pipeline p = jedis.pipelined();
p.set("fool", "bar"); 
p.zadd("foo", 1, "barowitch");  p.zadd("foo", 0, "barinsky"); p.zadd("foo", 0, "barikoviev");
Response<String> pipeString = p.get("fool");
Response<Set<String>> sose = p.zrange("foo", 0, -1);
p.sync(); 

int soseSize = sose.get().size();
Set<String> setBack = sose.get();
```
For more explanations see code comments in the transaction section.


## Publish/Subscribe

To subscribe to a channel in Redis, create an instance of JedisPubSub and call subscribe on the Jedis instance:

```java
class MyListener extends JedisPubSub {
        public void onMessage(String channel, String message) {
        }

        public void onSubscribe(String channel, int subscribedChannels) {
        }

        public void onUnsubscribe(String channel, int subscribedChannels) {
        }

        public void onPSubscribe(String pattern, int subscribedChannels) {
        }

        public void onPUnsubscribe(String pattern, int subscribedChannels) {
        }

        public void onPMessage(String pattern, String channel, String message) {
        }
}

MyListener l = new MyListener();

jedis.subscribe(l, "foo");
```
Note that subscribe is a blocking operation because it will poll Redis for responses on the thread that calls subscribe.  A single JedisPubSub instance can be used to subscribe to multiple channels.  You can call subscribe or psubscribe on an existing JedisPubSub instance to change your subscriptions.


## Monitoring

To use the monitor command you can do something like the following:

```java
new Thread(new Runnable() {
    public void run() {
        Jedis j = new Jedis("localhost");
        for (int i = 0; i < 100; i++) {
            j.incr("foobared");
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
        }
        j.disconnect();
    }
}).start();

jedis.monitor(new JedisMonitor() {
    public void onCommand(String command) {
        System.out.println(command);
    }
});
```
## Token-Based Authentication

Starting with version 5.3.0 GA, Jedis supports token-based authentication. The [redis-authx-entraid](https://github.com/redis/jvm-redis-authx-entraid) repository provides the necessary components that Jedis utilizes to enable this functionality. 

Additionally, support for Microsoft EntraID has been fully implemented and is now available as an extension for Azure Managed Redis (AMR) and Azure Cache for Redis(ACR).

### Using With Custom Identity Provider
Jedis provides a token-based authentication mechanism with a generic identity provider of your choice. 
For custom use of this feature, you will need to provide an implementation of `IdentityProvider` and `IdentityProviderConfig` and configure it in the way Jedis expects. 

You will have the required interfaces from transitive Jedis dependencies;

```
    <dependency>
        <groupId>redis.clients.authentication</groupId>
        <artifactId>redis-authx-core</artifactId>
        <version>${version}</version>
    </dependency>
```

**An example to get started:**
```java
public class YourCustomIdentityProviderConfig implements IdentityProviderConfig {
    ...
}

public class YourCustomIdentityProvider implements IdentityProvider {
    ...
}
```

Then configure Jedis like this:
```java
IdentityProviderConfig yourCustomIdentityProviderConfig = new YourCustomIdentityProviderConfig();
TokenAuthConfig tokenAuthConfig = TokenAuthConfig.builder().identityProviderConfig(yourCustomIdentityProviderConfig);

JedisClientConfig config = DefaultJedisClientConfig.builder()
                .authXManager(new AuthXManager(tokenAuthConfig)).build();
...
```
### Using With Microsoft EntraID

Extension for EntraID is fully integrated and ready to use with [Azure Managed Redis](https://azure.microsoft.com/en-us/products/managed-redis)(AMR) or [Azure Cache for Redis](https://azure.microsoft.com/en-us/products/cache/)(ACR). All you need is to add the EntraID dependency and code for configuration for chosen authentication type with Microsoft EntraID service.

To get started, add the `redis-authx-entraid` extension as dependency;

```
    <dependency>
        <groupId>redis.clients.authentication</groupId>
        <artifactId>redis-authx-entraid</artifactId>
        <version>${version}</version>
    </dependency>
```

After adding the dependency, configure it using `EntraIDTokenAuthConfigBuilder`:

```java
...
    TokenAuthConfig tokenAuthConfig = EntraIDTokenAuthConfigBuilder.builder()
        .expirationRefreshRatio(0.8F)
        .clientId("yourClientId")
        .secret("yourClientSecret")
        .authority("yourAuthority")
        .scopes("yourRedisScopes").build();

    AuthXManager authXManager = new AuthXManager(tokenAuthConfig);

    JedisClientConfig config = DefaultJedisClientConfig.builder()
        .authXManager(authXManager).build();
...
```

Here you will see the `AuthXManager` class that is built into Jedis. Essentially it integrates the extension into Jedis and handles the authentication process.  
For other available configurations, detailed information and usage of Jedis with Microsoft EntraID, please refer to the [official guide](https://redis.io/docs/latest/develop/clients/jedis/amr/)

**Setting Up AMR or ACR with Microsoft EntraID:**

To use Microsoft EntraID with AMR or ACR, for sure you will need to set up and configure your AMR/ACR services as well as Microsoft EntraID. The following resources provide useful information;

[Azure Managed Redis](https://azure.microsoft.com/en-us/products/managed-redis)

[Azure Cache for Redis](https://azure.microsoft.com/en-us/products/cache/)

[Microsoft Entra ID for AMR authentication](https://learn.microsoft.com/en-us/azure/azure-cache-for-redis/managed-redis/managed-redis-entra-for-authentication)

[Microsoft Entra ID for ACR authentication](https://learn.microsoft.com/en-us/azure/azure-cache-for-redis/cache-azure-active-directory-for-authentication)

[Use Microsoft Entra](https://learn.microsoft.com/en-us/azure/app-service/configure-authentication-provider-aad?tabs=workforce-configuration)

## Miscellaneous 

### A note about String and Binary - what is native?

Redis/Jedis talks a lot about Strings. And here [[http://redis.io/topics/internals]] it says Strings are the basic building block of Redis. However, this stress on strings may be misleading. Redis' "String" refer to the C char type (8 bit), which is incompatible with Java Strings (16-bit). Redis sees only 8-bit blocks of data of predefined length, so normally it doesn't interpret the data (it's "binary safe"). Therefore in Java, byte[] data is "native", whereas Strings have to be encoded before being sent, and decoded after being retrieved by the SafeEncoder. This has some minor performance impact.
In short: if you have binary data, don't encode it into String, but use the binary versions.

### A note on Redis' master/slave distribution

A Redis network consists of redis servers, which can be either masters or slaves. Slaves are synchronized to the master (master/slave replication). However, master and slaves look identical to a client, and slaves do accept write requests, but they will not be propagated "up-hill" and could eventually be overwritten by the master. It makes sense to route reads to slaves, and write demands to the master. Furthermore, being a slave doesn't prevent from being considered master by another slave.
