// EXAMPLE: java_home_json
// REMOVE_START
package io.redis.examples;

import org.junit.Assert;
import org.junit.Test;

// REMOVE_END
// STEP_START import
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.aggr.*;
import redis.clients.jedis.search.schemafields.*;
import org.json.JSONObject;
import java.util.List;
// STEP_END

// HIDE_START
public class HomeJsonExample {

    @Test
    public void run() {
// HIDE_END
        // STEP_START connect
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");
        // STEP_END

        //REMOVE_START
        // Clear the indexes and keys here before using them in tests.
        try {jedis.ftDropIndex("idx:users");} catch (JedisDataException j){}
        jedis.del("bike", "bike:1", "crashes", "newbike", "riders", "bikes:inventory");
        //REMOVE_END

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
        Assert.assertEquals("OK", createResult);
        // REMOVE_END

        // STEP_START add_data
        String user1Set = jedis.jsonSet("user:1", new Path2("$"), user1);
        String user2Set = jedis.jsonSet("user:2", new Path2("$"), user2);
        String user3Set = jedis.jsonSet("user:3", new Path2("$"), user3);
        // STEP_END
        // REMOVE_START
        Assert.assertEquals("OK", user1Set);
        Assert.assertEquals("OK", user2Set);
        Assert.assertEquals("OK", user3Set);
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
        Assert.assertEquals("user:3", paulDocs.get(0).getId());
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
        Assert.assertArrayEquals(
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
            System.out.println(String.format(
                "%s - %d",
                cityRow.getString("city"), cityRow.getLong("count"))
            );
        }
        // >>> London - 1
        // >>> Tel Aviv - 2
        // STEP_END
        // REMOVE_START
        Assert.assertArrayEquals(
            new String[] {"London - 1", "Tel Aviv - 2"},
            aggResult.getRows().stream()
                    .map(r -> r.getString("city") + " - " + r.getString("count"))
                    .sorted().toArray());
        // REMOVE_END

// HIDE_START
        jedis.close();
    }
}
// HIDE_END

