# Tutorials and Examples

## General

* Redis for Java Developers: <https://university.redis.io/learningpath/kllayn0wtd847i>
* Jedis Guide: <https://redis.io/docs/latest/develop/clients/jedis/>
* Connecting to a Redis server: <https://redis.io/docs/latest/develop/clients/jedis/connect/>
* Using Jedis in production: <https://redis.io/docs/latest/develop/clients/jedis/produsage/>

## Client-side Caching

* Client-side Caching: <https://github.com/Redislabs-Solution-Architects/redis-client-side-caching-csc-jedis-demo>

When caching JSON reads, `jsonGet(String, Path2...)` can return `JSONObject` or
`JSONArray` values. Each cache read returns an independent JSON container, so
modifying a returned object or array does not modify the cached value. See the
[8.1.0 release notes](release-notes/8.1.0.md#json-object-and-array-values-in-client-side-caching)
for the supported scope and the limitation for collections containing JSON values.

## JSON

* Store, Read and Search JSON: <https://redis.io/kb/doc/1cd7hi2721/learn-to-store-read-and-search-data-in-json-documents-using-jedis>

## Search

* Vector Search: <https://redis.io/kb/doc/13qsrk8xpx/how-to-perform-vector-search-in-java-with-the-jedis-client-library>
* Spring Boot Search: <https://github.com/Redislabs-Solution-Architects/Spring-Boot-RediSearch-Example>
