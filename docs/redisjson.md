# RedisJSON Jedis Quick Start

Jedis supports [RedisJSON](https://oss.redis.com/redisjson/) and [RediSearch](https://oss.redis.com/redisearch/).

The latest versions of RedisJSON let you store, manipulate, index, and query JSON.
 To 
use these features with Jedis, you'll need to use the `UnifiedJedis` interface. 
See the [RedisJSON Jedis Quick Start](docs/redisjson.md) for details.

Let's see how this works.

## Connecting with UnifiedJedis

First, let's create a `UnifiedJedis` instance:

```java
  HostAndPort config = new HostAndPort(Protocol.DEFAULT_HOST, 6479);
  PooledJedisConnectionProvider provider = new PooledJedisConnectionProvider(config);
  UnifiedJedis client = new UnifiedJedis(provider);
```
Now we can start working with JSON. For these examples, we'll be using the `org.json` [JSON-Java](https://docs.oracle.com/javaee/7/api/javax/json/JsonObject.html) library.

## Creating JSON documents

Suppose we're building an online learning platform, and we want to represent students. Here's how we can represent a couple of students using JSON:

```java
JSONObject maya = new org.json.JSONObject();
maya.put("firstName", "Maya");
maya.put("lastName", "Jayavant");

JSONObject oliwia = new org.json.JSONObject();
oliwia.put("firstName", "Oliwia");
oliwia.put("lastName", "Jagoda");
```
Now we can store this JSON in Redis:

```java
client.jsonSet("student:111", maya);
client.jsonSet("student:112", oliwia);
```

## Querying and indexing JSON

If we want to be able to query this JSON, we'll need to create an index. Let's 
create an index on the "firstName" and "lastName" fields. 

1. We define which fields to index ("firstName" and "lastName").
2. We set up the index definition to recognize JSON and include only those 
documents 
whose key starts with "student:".
3. Then we actually create the index, called "student-index", by calling `ftCreate
()`.

```java
Schema schema = new Schema().addTextField("$.firstName", 1.0).addTextField("$" +
            ".lastName", 1.0);
IndexDefinition rule = new IndexDefinition(IndexDefinition.Type.JSON)
        .setPrefixes(new String[]{"student:"});
client.ftCreate("student-index",
            IndexOptions.defaultOptions().setDefinition(rule),
            schema);
```

With an index now defined, we can query our JSON. Let's find all students whose 
name begins with "maya":

```java
Query q = new Query("@\\$\\" + ".firstName:maya*");
SearchResult mayaSearch = client.ftSearch("student-index", q);
```

We can then iterate over our search results:
    
```java
List<Document> docs = mayaSearch.getDocuments();
for (Document doc : docs) {
   System.out.println(doc);
}
```

This example just scratches the surface. You can atomically manipulate JSON documents and query them in a variety of ways. See the [RedisJSON docs](https://oss.redis.com/redisjson/), the [RediSearch](https://oss.redis.com/redisearch/) docs, and our course, ["Querying, Indexing, and Full-text Search in Redis"](https://university.redis.com/courses/ru203/), for a lot more examples. 