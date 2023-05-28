//EXAMPLE: set_and_get
//HIDE_START
package io.redis.examples;

import java.math.BigDecimal;
import java.util.*;

import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.*;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.aggr.*;
import redis.clients.jedis.search.schemafields.*;
//REMOVE_START
import org.junit.Test;

import static org.junit.Assert.assertEquals;
//REMOVE_END

class Bicycle {
    public String brand;
    public String model;
    public BigDecimal price;
    public String description;
    public String condition;

    public Bicycle(String brand, String model, BigDecimal price, String description, String condition) {
        this.brand = brand;
        this.model = model;
        this.price = price;
        this.description = description;
        this.condition = condition;
    }
}

public class SearchQuickstartExample {

    @Test
    public void run() {
        //HIDE_END

        // STEP_START connect
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");
        // STEP_END
        // REMOVE_START
        try {
            jedis.ftDropIndex("idx:bicycle");
        } catch (JedisDataException e) {
            // ignore
        }
        // REMOVE_END

        // STEP_START data_sample
        Bicycle bike1 = new Bicycle(
                "Diaz Ltd",
                "Dealer Sl",
                new BigDecimal(7315.58),
                "The Diaz Ltd Dealer Sl is a reliable choice" +
                        " for urban cycling. The Diaz Ltd Dealer Sl " +
                        "is a comfortable choice for urban cycling.",
                "used"
        );
        // STEP_END

        Bicycle[] bicycles = {
                bike1,
                new Bicycle(
                        "Bridges Group",
                        "Project Pro",
                        new BigDecimal(3610.82),
                        "This mountain bike is perfect for mountain biking. The Bridges" +
                                " Group Project Pro is a responsive choice for mountain biking.",
                        "used"
                ),
                new Bicycle(
                        "Vega, Cole and Miller",
                        "Group Advanced",
                        new BigDecimal(8961.42),
                        "The Vega, Cole and Miller Group Advanced provides a excellent" +
                                " ride. With its fast carbon frame and 24 gears, this bicycle is" +
                                " perfect for any terrain.",
                        "used"
                ),
                new Bicycle(
                        "Powell-Montgomery",
                        "Angle Race",
                        new BigDecimal(4050.27),
                        "The Powell-Montgomery Angle Race is a smooth choice for road" +
                                " cycling. The Powell-Montgomery Angle Race provides a durable" +
                                " ride.",
                        "used"
                ),
                new Bicycle(
                        "Gill-Lewis",
                        "Action Evo",
                        new BigDecimal(283.68),
                        "The Gill-Lewis Action Evo provides a smooth ride. The Gill-Lewis" +
                                " Action Evo provides a excellent ride.",
                        "used"
                ),
                new Bicycle(
                        "Rodriguez-Guerrero",
                        "Drama Comp",
                        new BigDecimal(4462.55),
                        "This kids bike is perfect for young riders. With its excellent" +
                                " aluminum frame and 12 gears, this bicycle is perfect for any" +
                                " terrain.",
                        "new"
                ),
                new Bicycle(
                        "Moore PLC",
                        "Award Race",
                        new BigDecimal(3790.76),

                        "This olive folding bike features a carbon frame and 27.5 inch" +
                                " wheels. This folding bike is perfect for compact storage and" +
                                " transportation.",
                        "new"
                ),
                new Bicycle(
                        "Hall, Haley and Hayes",
                        "Weekend Plus",
                        new BigDecimal(2008.4),
                        "The Hall, Haley and Hayes Weekend Plus provides a comfortable" +
                                " ride. This blue kids bike features a steel frame and 29.0 inch" +
                                " wheels.",
                        "new"
                ),
                new Bicycle(
                        "Peck-Carson",
                        "Sun Hybrid",
                        new BigDecimal(9874.95),
                        "With its comfortable aluminum frame and 25 gears, this bicycle is" +
                                " perfect for any terrain. The Peck-Carson Sun Hybrid provides a" +
                                " comfortable ride.",
                        "new"
                ),
                new Bicycle(
                        "Fowler Ltd",
                        "Weekend Trail",
                        new BigDecimal(3833.71),
                        "The Fowler Ltd Letter Trail is a comfortable choice for" +
                                " transporting cargo. This cargo bike is perfect for transporting" +
                                " cargo.",
                        "refurbished"
                )
        };

        // STEP_START define_index
        SchemaField[] schema = {
                TextField.of("$.brand").as("brand"),
                TextField.of("$.model").as("model"),
                TextField.of("$.description").as("description"),
                NumericField.of("$.price").as("price"),
                TagField.of("$.condition").as("condition")
        };
        // STEP_END

        // STEP_START create_index
        jedis.ftCreate("idx:bicycle",
                FTCreateParams.createParams()
                        .on(IndexDataType.JSON)
                        .addPrefix("bicycle:"),
                schema
        );
        // STEP_END
        // STEP_START add_documents
        for (int i = 0; i < bicycles.length; i++) {
            jedis.jsonSetWithEscape(String.format("bicycle:%d", i), bicycles[i]);
        }
        // STEP_END

        // STEP_START query_single_term_and_num_range
        Query query = new Query("folding @price:[1000 4000]");
        List<Document> result = jedis.ftSearch("idx:bicycle", query).getDocuments();
        System.out.println(result);
        // Prints: [id:bicycle:6, score: 1.0, payload:null, properties:[
        // $={"brand":"Moore PLC","model":"Award Race","price":3790.76,
        // "description":"This olive folding bike features a carbon frame and 27.5 inch wheels.
        // This folding bike is perfect for compact storage and transportation.","condition":"new"}]
        // ]
        // STEP_END
        // REMOVE_START
        assertEquals("Validate folding bike id", "bicycle:6", result.get(0).getId());
        // REMOVE_END

        // STEP_START query_single_term_limit_fields
        Query cargo_query = new Query("cargo").returnFields("price");
        List<Document> cargo_result = jedis.ftSearch(
                "idx:bicycle", cargo_query).getDocuments();
        System.out.println(cargo_result);
        // Prints: [id:bicycle:9, score: 1.0, payload:null, properties:[price=3833.71]]
        // STEP_END
        // REMOVE_START
        assertEquals("Validate cargo bike id", "bicycle:9", cargo_result.get(0).getId());
        // REMOVE_END

        // STEP_START simple_aggregation
        AggregationBuilder ab = new AggregationBuilder("*")
                .groupBy("@condition", Reducers.count().as("count"));
        AggregationResult ar = jedis.ftAggregate("idx:bicycle", ab);

        for (int i = 0; i < ar.getTotalResults(); i++) {
            System.out.println(
                    ar.getRow(i).getString("condition")
                            + " - "
                            + ar.getRow(i).getString("count"));
        }
        // Prints:
        // refurbished - 1
        // used - 5
        // new - 4
        // STEP_END
        // REMOVE_START
        assertEquals("Validate aggregation results", 3, ar.getTotalResults());
        // REMOVE_END
    }
}
//HIDE_END
