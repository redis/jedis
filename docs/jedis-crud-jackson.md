# Guide using methods CRUD cache Jackson data in Maven Spring projects

### Necessary libraries

#### Jedis
```xml
<dependency>
  <groupId>redis.clients</groupId>
  <artifactId>jedis</artifactId>
  <version>5.0.0</version>
</dependency>
```

#### Jackson
```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-core</artifactId>
    <version>2.16.1</version>
</dependency>
```

### Environment variables
To establish a connection to a Redis server, various connection methods provided by Jedis can be utilized. One effective way is to initialize a URI for connection. This allows for a concise and flexible approach in specifying connection details.

```java
# Redis Configuration
davis.redis.uri=<url_connection>
```

### Redis Configuration

Project configuration connecting to the initially initialized URI.

```java
@Configuration
public class RedisConfig {

	@Value("${davis.redis.uri}")
	private String redisURI;

	@Bean
	public Jedis jedisConnectionFactory() {
		Jedis jedis = new Jedis(redisURI);
		System.out.println(jedis.isConnected());
		return jedis;
	}

}
```

### Constant (time, process)

Initialize constants such as cache expiration time, returned results, etc. Feel free to add more if needed.

```java
package com.davisy.constant;

public class Cache {
	public static final String DONE = "SUCCESS";
	public static final String ERROR = "ERROR";
	
	public static final int TimeUnit_SECONDS = 1;
	public static final int TimeUnit_MINUTE	= 60;
	public static final int TimeUnit_HOUR = 3600;
}
```

### Cache service

Build utility methods using Jedis and Jackson.

```java
@Service
@RequiredArgsConstructor
public class CacheService {

	@Autowired
	private Jedis jedisConnectionFactory;

	private final ObjectMapper objectMapper;

	public String getByKey(String key) {
		return jedisConnectionFactory.get(key);
	}

	public String writeCache(String key, Object result) {
		try {
			String json = objectMapper.writeValueAsString(result);
			jedisConnectionFactory.set(key, json);
		} catch (Exception e) {
			return Cache.ERROR;
		}
		return Cache.DONE;
	}

	public String destroyCache(String key) {
		try {
			jedisConnectionFactory.del(key);
		} catch (Exception e) {
			return Cache.ERROR;
		}
		return Cache.DONE;
	}

	public String writeCacheAtTime(String key, Object result, long time, int timeUnit) {
		if (timeUnit == Cache.TimeUnit_SECONDS) {
			// đơn vị giây
		} else if (timeUnit == Cache.TimeUnit_MINUTE) {
			// đơn vị phút
			time = time * Cache.TimeUnit_MINUTE;
		} else if (timeUnit == Cache.TimeUnit_HOUR) {
			// đơn vị giờ
			time = time * Cache.TimeUnit_HOUR;
		}
		try {
			String json = objectMapper.writeValueAsString(result);
			System.out.println("Writing "+json);
			jedisConnectionFactory.set(key, json);
			jedisConnectionFactory.expire(key, time);
			
			System.out.println("Write success! key: "+key);
		} catch (Exception e) {
			return Cache.ERROR;
		}
		return Cache.DONE;
	}

}
```


