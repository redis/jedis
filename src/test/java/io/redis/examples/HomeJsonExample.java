// EXAMPLE: java_home_json
// BINDER_ID jedis-java_home_json
// REMOVE_START
package io.redis.examples;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
// REMOVE_END
// STEP_START import
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.aggr.*;
import redis.clients.jedis.search.schemafields.*;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
// STEP_END

public class HomeJsonExample {

    @Test
    public void run() {

        // STEP_START create_data
        JSONObject user1 = new JSONObject()
                .put("name", "Paul John")
                .put("email", "paul.john@example.com")
                .put("age", 42)
                .put("city", "London");
        
        JSONObject user2 = new JSONObject()
                .put("name", "Eden Zamir")
                .put("email", "eden.zamir@example.com")
                .put("age", 29)
                .put("city", "Tel Aviv");
        
        JSONObject user3 = new JSONObject()
                .put("name", "Paul Zamir")
                .put("email", "paul.zamir@example.com")
                .put("age", 35)
                .put("city", "Tel Aviv");
        // STEP_END

        // STEP_START connect
        RedisClient jedis = new RedisClient("redis://localhost:6379");
        // STEP_END

        // STEP_START cleanup_json
        try {jedis.ftDropIndex("idx:users");} catch (JedisDataException j){}
        jedis.del("user:1", "user:2", "user:3");
        // STEP_END

        // STEP_START make_index
        SchemaField[] schema = {
            TextField.of("$.name").as("name"),
            TextField.of("$.city").as("city"),
            NumericField.of("$.age").as("age")
        };

        String createResult = jedis.ftCreate("idx:users",
            FTCreateParams.createParams()
                .on(IndexDataType.JSON)
                .addPrefix("user:"),
                schema
        );

        System.out.println(createResult); // >>> OK
        // STEP_END
        // REMOVE_START
        assertEquals("OK", createResult);
        // REMOVE_END

        // STEP_START add_data
        String user1Set = jedis.jsonSet("user:1", new Path2("$"), user1);
        String user2Set = jedis.jsonSet("user:2", new Path2("$"), user2);
        String user3Set = jedis.jsonSet("user:3", new Path2("$"), user3);
        // STEP_END
        // REMOVE_START
        assertEquals("OK", user1Set);
        assertEquals("OK", user2Set);
        assertEquals("OK", user3Set);
        // REMOVE_END

        // STEP_START query1
        SearchResult findPaulResult = jedis.ftSearch("idx:users",
             "Paul @age:[30 40]"
        );
        
        System.out.println(findPaulResult.getTotalResults()); // >>> 1

        List<Document> paulDocs = findPaulResult.getDocuments();

        for (Document doc: paulDocs) {
            System.out.println(doc.getId());
        }
        // >>> user:3
        // STEP_END
        // REMOVE_START
        assertEquals("user:3", paulDocs.get(0).getId());
        // REMOVE_END

        // STEP_START query2
        SearchResult citiesResult = jedis.ftSearch("idx:users",
            "Paul",
            FTSearchParams.searchParams()
                .returnFields("city")
        );

        System.out.println(citiesResult.getTotalResults()); // >>> 2

        for (Document doc: citiesResult.getDocuments()) {
            System.out.println(doc.getId());
        }
        // >>> user:1
        // >>> user:3
        // STEP_END
        // REMOVE_START
        assertArrayEquals(
            new String[] {"user:1", "user:3"},
            citiesResult.getDocuments().stream().map(Document::getId).sorted().toArray()
        );
        // REMOVE_END

        // STEP_START query3
        AggregationResult aggResult = jedis.ftAggregate("idx:users",
            new AggregationBuilder("*")
                .groupBy("@city", Reducers.count().as("count"))
        );

        System.out.println(aggResult.getTotalResults()); // >>> 2

        for (Row cityRow: aggResult.getRows()) {
            System.out.printf("%s - %d%n",
                cityRow.getString("city"), cityRow.getLong("count"));
        }
        // >>> London - 1
        // >>> Tel Aviv - 2
        // STEP_END
        // REMOVE_START
        assertArrayEquals(
            new String[] {"London - 1", "Tel Aviv - 2"},
            aggResult.getRows().stream()
                    .map(r -> r.getString("city") + " - " + r.getString("count"))
                    .sorted().toArray());
        // REMOVE_END

        // STEP_START cleanup_hash
        try {jedis.ftDropIndex("hash-idx:users");} catch (JedisDataException j){}
        jedis.del("huser:1", "huser:2", "huser:3");
        // STEP_END

        // STEP_START make_hash_index
        SchemaField[] hashSchema = {
            TextField.of("name"),
            TextField.of("city"),
            NumericField.of("age")
        };

        String hashCreateResult = jedis.ftCreate("hash-idx:users",
            FTCreateParams.createParams()
                .on(IndexDataType.HASH)
                .addPrefix("huser:"),
                hashSchema
        );

        System.out.println(hashCreateResult); // >>> OK
        // STEP_END
        // REMOVE_START
        assertEquals("OK", hashCreateResult);
        // REMOVE_END

        // STEP_START add_hash_data
        Map<String, String> user1Info = new HashMap<>();
        user1Info.put("name", "Paul John");
        user1Info.put("email", "paul.john@example.com");
        user1Info.put("age", "42");
        user1Info.put("city", "London");
        long huser1Set = jedis.hset("huser:1", user1Info);
        
        System.out.println(huser1Set); // >>> 4

        Map<String, String> user2Info = new HashMap<>();
        user2Info.put("name", "Eden Zamir");
        user2Info.put("email", "eden.zamir@example.com");
        user2Info.put("age", "29");
        user2Info.put("city", "Tel Aviv");
        long huser2Set = jedis.hset("huser:2", user2Info);
        
        System.out.println(huser2Set); // >>> 4

        Map<String, String> user3Info = new HashMap<>();
        user3Info.put("name", "Paul Zamir");
        user3Info.put("email", "paul.zamir@example.com");
        user3Info.put("age", "35");
        user3Info.put("city", "Tel Aviv");
        long huser3Set = jedis.hset("huser:3", user3Info);
        
        System.out.println(huser3Set); // >>> 4
        // STEP_END
        // REMOVE_START
        assertEquals(4, huser1Set);
        assertEquals(4, huser2Set);
        assertEquals(4, huser3Set);
        // REMOVE_END
        
        // STEP_START query1_hash
        SearchResult findPaulHashResult = jedis.ftSearch("hash-idx:users",
             "Paul @age:[30 40]"
        );
        
        System.out.println(findPaulHashResult.getTotalResults()); // >>> 1

        List<Document> paulHashDocs = findPaulHashResult.getDocuments();

        for (Document doc: paulHashDocs) {
            System.out.println(doc.getId());
        }
        // >>> user:3
        // STEP_END
        // REMOVE_START
        assertEquals("huser:3", paulHashDocs.get(0).getId());
        // REMOVE_END

        // STEP_START close
        jedis.close();
        // STEP_END
    }
}

