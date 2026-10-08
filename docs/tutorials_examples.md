# Tutorials and Examples

## General

* Redis for Java Developers: <https://university.redis.io/learningpath/kllayn0wtd847i>
* Jedis Guide: <https://redis.io/docs/latest/develop/clients/jedis/>
* Connecting to a Redis server: <https://redis.io/docs/latest/develop/clients/jedis/connect/>
* Using Jedis in production: <https://redis.io/docs/latest/develop/clients/jedis/produsage/>

## Client-side Caching

* Client-side Caching: <https://github.com/Redislabs-Solution-Architects/redis-client-side-caching-csc-jedis-demo>

Cached command replies are rebuilt with their response builder on every read;
JSON objects, JSON arrays, `jsonMGet` collections and Gson-mapped POJOs do not
need to implement Java `Serializable`. Builders receive independent copies of
mutable protocol data, protecting the cache even when a builder returns raw
bytes or containers. Custom builders should create independent results on each
call. See the [8.1.0 release notes](release-notes/8.1.0.md#rebuilding-client-side-cached-replies-with-command-builders)
for compatibility details and the JSON.MGET server key-tracking limitation.
On affected servers, use an uncached client for JSON.MGET or exclude the command
with a custom `Cacheable` policy to avoid stale results.

## JSON

* Store, Read and Search JSON: <https://redis.io/kb/doc/1cd7hi2721/learn-to-store-read-and-search-data-in-json-documents-using-jedis>

## Search

* Vector Search: <https://redis.io/kb/doc/13qsrk8xpx/how-to-perform-vector-search-in-java-with-the-jedis-client-library>
* Spring Boot Search: <https://github.com/Redislabs-Solution-Architects/Spring-Boot-RediSearch-Example>
