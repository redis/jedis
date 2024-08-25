# RediSearch Jedis Quick Start

To use RediSearch features with Jedis, you'll need to use an implementation of RediSearchCommands.

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

### Indexing

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

Alternatively, we can create the same index using FTCreateParams:

```java
client.ftCreate("item-index",

        FTCreateParams.createParams()
                .prefix("item:", "product:")
                .filter("@price>100"),

        TextField.of("title").weight(5.0),
        TextField.of("body"),
        NumericField.of("price")
);
```

### Inserting

Adding documents to the index:

```java
Map<String, Object> fields = new HashMap<>();
fields.put("title", "hello world");
fields.put("state", "NY");
fields.put("body", "lorem ipsum");
fields.put("price", 1337);

client.hset("item:hw", RediSearchUtil.toStringMap(fields));
```

Another way to insert documents:

```java
client.hsetObject("item:hw", fields);
```

### Querying

Searching the index:

```java
Query q = new Query("hello world")
        .addFilter(new Query.NumericFilter("price", 0, 1000))
        .limit(0, 5);

SearchResult sr = client.ftSearch("item-index", q);
```

Alternative searching using FTSearchParams:

```java
SearchResult sr = client.ftSearch("item-index",
        "hello world",
        FTSearchParams.searchParams()
                .filter("price", 0, 1000)
                .limit(0, 5));
```

Aggregation query:

```java
AggregationBuilder ab = new AggregationBuilder("hello")
        .apply("@price/1000", "k")
        .groupBy("@state", Reducers.avg("@k").as("avgprice"))
        .filter("@avgprice>=2")
        .sortBy(10, SortedField.asc("@state"));

AggregationResult ar = client.ftAggregate("item-index", ab);
```
