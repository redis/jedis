// EXAMPLE: geoindex
// REMOVE_START
package io.redis.examples;

import org.junit.Assert;
import org.junit.Test;
// REMOVE_END

// HIDE_START
import org.json.JSONObject;

import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.json.Path2;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.FTCreateParams;
import redis.clients.jedis.search.FTSearchParams;
import redis.clients.jedis.search.IndexDataType;
import redis.clients.jedis.search.schemafields.*;
import redis.clients.jedis.search.schemafields.GeoShapeField.CoordinateSystem;
import redis.clients.jedis.search.SearchResult;
import redis.clients.jedis.exceptions.JedisDataException;
// HIDE_END

// HIDE_START
public class GeoIndexExample {

    @Test
    public void run() {
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");
        //REMOVE_START
        // Clear any keys here before using them in tests.
        try {
            jedis.ftDropIndex("productidx");
        } catch (JedisDataException j) {}
        
        try {
            jedis.ftDropIndex("geomidx");
        } catch (JedisDataException j) {}
        
        jedis.del("product:46885", "product:46886", "shape:1", "shape:2", "shape:3", "shape:4");
        //REMOVE_END
// HIDE_END

        // STEP_START create_geo_idx
        SchemaField[] geoSchema = {
            GeoField.of("$.location").as("location")
        };

        String geoIdxCreateResult = jedis.ftCreate("productidx",
            FTCreateParams.createParams()
                    .on(IndexDataType.JSON)
                    .addPrefix("product:"),
            geoSchema
        );
        // STEP_END
        // REMOVE_START
        Assert.assertEquals("OK", geoIdxCreateResult);
        // REMOVE_END
        
        // STEP_START add_geo_json
        JSONObject prd46885 = new JSONObject()
                .put("description", "Navy Blue Slippers")
                .put("price", 45.99)
                .put("city", "Denver")
                .put("location", "-104.991531, 39.742043");
        
        String jsonAddResult1 = jedis.jsonSet("product:46885", new Path2("$"), prd46885);
        System.out.println(jsonAddResult1); // >>> OK

        JSONObject prd46886 = new JSONObject()
                .put("description", "Bright Green Socks")
                .put("price", 25.50)
                .put("city", "Fort Collins")
                .put("location", "-105.0618814,40.5150098");
        
        String jsonAddResult2 = jedis.jsonSet("product:46886", new Path2("$"), prd46886);
        System.out.println(jsonAddResult2); // >>> OK
        // STEP_END
        // REMOVE_START
        Assert.assertEquals("OK", jsonAddResult1);
        Assert.assertEquals("OK", jsonAddResult2);
        // REMOVE_END

        // STEP_START geo_query
        SearchResult geoResult = jedis.ftSearch("productidx",
            "@location:[-104.800644 38.846127 100 mi]"
        );

        System.out.println(geoResult.getTotalResults()); // >>> 1

        for (Document doc: geoResult.getDocuments()) {
            System.out.println(doc.getId());
        }
        // >>> product:46885
        // STEP_END
        // REMOVE_START
        Assert.assertEquals("OK", jsonAddResult1);
        Assert.assertEquals("OK", jsonAddResult2);
        Assert.assertEquals("product:46885", geoResult.getDocuments().get(0).getId());
        // REMOVE_END

        // STEP_START create_gshape_idx
        SchemaField[] geomSchema = {
            TextField.of("$.name").as("name"),
            GeoShapeField.of("$.geom", CoordinateSystem.FLAT).as("geom")
        };

        String geomIndexCreateResult = jedis.ftCreate("geomidx",
            FTCreateParams.createParams()
                    .on(IndexDataType.JSON)
                    .addPrefix("shape"),
            geomSchema
        );
        System.out.println(geomIndexCreateResult); // >>> OK
        // STEP_END
        // REMOVE_START
        Assert.assertEquals("OK", geomIndexCreateResult);
        // REMOVE_END

        // STEP_START add_gshape_json
        JSONObject shape1 = new JSONObject()
                .put("name", "Green Square")
                .put("geom", "POLYGON ((1 1, 1 3, 3 3, 3 1, 1 1))");
        
        String gmJsonRes1 = jedis.jsonSet("shape:1", new Path2("$"), shape1);
        System.out.println(gmJsonRes1); // >>> OK

        JSONObject shape2 = new JSONObject()
                .put("name", "Red Rectangle")
                .put("geom", "POLYGON ((2 2.5, 2 3.5, 3.5 3.5, 3.5 2.5, 2 2.5))");
        
        String gmJsonRes2 = jedis.jsonSet("shape:2", new Path2("$"), shape2);
        System.out.println(gmJsonRes2); // >>> OK

        JSONObject shape3 = new JSONObject()
                .put("name", "Blue Triangle")
                .put("geom", "POLYGON ((3.5 1, 3.75 2, 4 1, 3.5 1))");
        
        String gmJsonRes3 = jedis.jsonSet("shape:3", new Path2("$"), shape3);
        System.out.println(gmJsonRes3); // >>> OK

        JSONObject shape4 = new JSONObject()
                .put("name", "Purple Point")
                .put("geom", "POINT (2 2)");
        
        String gmJsonRes4 = jedis.jsonSet("shape:4", new Path2("$"), shape4);
        System.out.println(gmJsonRes4); // >>> OK
        // STEP_END
        // REMOVE_START
        Assert.assertEquals("OK", gmJsonRes1);
        Assert.assertEquals("OK", gmJsonRes2);
        Assert.assertEquals("OK", gmJsonRes3);
        Assert.assertEquals("OK", gmJsonRes4);
        // REMOVE_END

        // STEP_START gshape_query
        SearchResult geomResult = jedis.ftSearch("geomidx",
            "(-@name:(Green Square) @geom:[WITHIN $qshape])",
            FTSearchParams.searchParams()
                    .addParam("qshape", "POLYGON ((1 1, 1 3, 3 3, 3 1, 1 1))")
                    .dialect(4)
                    .limit(0, 1)
        );
        System.out.println(geomResult.getTotalResults()); // >>> 1

        for (Document doc: geomResult.getDocuments()) {
            System.out.println(doc.getId());
        }
        // shape:4
        // STEP_END
        // REMOVE_START
        Assert.assertEquals(1, geomResult.getTotalResults());
        Assert.assertEquals("shape:4", geomResult.getDocuments().get(0).getId());
        // REMOVE_END
        // HIDE_START

        jedis.close();
    }
}
// HIDE_END
