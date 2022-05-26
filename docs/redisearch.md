# RediSearch Jedis Quick Start

To use RediSearch features with Jedis, you'll need to use and implementation of RediSearchCommands.

## Creating the RediSearch client

Initializing the client with JedisPooled:

```java
JedisPooled client = new JedisPooled("localhost", 6379);
```

Initializing the client with JedisCluster:

```java
Set<HostAndPort> nodes = new HashSet<>();
nodes.add(new HostAndPort("127.0.0.1", 7379));
nodes.add(new HostAndPort("127.0.0.1", 7380));

JedisCluster client = new JedisCluster(nodes);
```

## Indexing and querying

Defining a schema for an index and creating it:

```java
Schema sc = new Schema()
        .addTextField("title", 5.0)
        .addTextField("body", 1.0)
        .addNumericField("price");

IndexDefinition def = new IndexDefinition()
        .setPrefixes(new String[]{"item:", "product:"})
        .setFilter("@price>100");

client.ftCreate("item-index", IndexOptions.defaultOptions().setDefinition(def), sc);
```

Adding documents to the index:

```java
Map<String, Object> fields = new HashMap<>();
fields.put("title", "hello world");
fields.put("state", "NY");
fields.put("body", "lorem ipsum");
fields.put("price", 1337);

client.hset("item:hw", RediSearchUtil.toStringMap(fields));
```

Searching the index:

```java
// creating a complex query
Query q = new Query("hello world")
        .addFilter(new Query.NumericFilter("price", 0, 1000))
        .limit(0, 5);

// actual search
SearchResult sr = client.ftSearch("item-index", q);

// aggregation query
AggregationBuilder ab = new AggregationBuilder("hello")
        .apply("@price/1000", "k")
        .groupBy("@state", Reducers.avg("@k").as("avgprice"))
        .filter("@avgprice>=2")
        .sortBy(10, SortedField.asc("@state"));

AggregationResult ar = client.ftAggregate("item-index", ab);
```
