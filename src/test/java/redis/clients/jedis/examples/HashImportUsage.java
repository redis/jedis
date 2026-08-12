package redis.clients.jedis.examples;

import redis.clients.jedis.HashImport;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.Response;

/**
 * Importing many hashes that share the same field names with {@code HIMPORT} (Redis 8.10),
 * mirroring the examples in {@code docs/hash-import.md}: declare the field names once as a reusable
 * {@link HashImport} template, then send only the values per hash. The
 * {@code PREPARE}/{@code DISCARD} lifecycle is managed by the client.
 */
public class HashImportUsage {

  public static void main(String[] args) {
    String redisUri = args.length > 0 ? args[0] : "redis://localhost:6379";

    try (RedisClient client = RedisClient.create(redisUri)) {

      // declare the field names shared by every hash we are importing; reusable and safe to share
      try (HashImport fields = HashImport.of("name", "email", "age")) {

        // import as many hashes as you like — one command per hash, so the total is unbounded
        client.himportSet("user:1", fields, "alice", "a@example.com", "30");
        client.himportSet("user:2", fields, "bob", "b@example.com", "25");
        client.himportSet("user:3", fields, "carol", "c@example.com", "42");
      }

      // the produced keys are ordinary hashes
      System.out.println(client.hgetAll("user:1")); // {name=alice, email=a@example.com, age=30}

      // for large imports, pipeline the rows to avoid a round trip per hash
      try (HashImport fields = HashImport.of("name", "email", "age");
          Pipeline pipeline = client.pipelined()) {

        Response<String> u1 = pipeline.himportSet("user:1", fields, "alice", "a@example.com", "30");
        Response<String> u2 = pipeline.himportSet("user:2", fields, "bob", "b@example.com", "25");
        Response<String> u3 = pipeline.himportSet("user:3", fields, "carol", "c@example.com", "42");

        pipeline.sync();

        System.out.println(u1.get() + " " + u2.get() + " " + u3.get()); // OK OK OK
      }
    }
  }
}