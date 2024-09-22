// EXAMPLE: search_quickstart
package io.redis.examples;

import java.math.BigDecimal;
import java.util.*;

import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.*;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.aggr.*;
import redis.clients.jedis.search.schemafields.*;
// REMOVE_START
import org.junit.Test;

import static org.junit.Assert.assertEquals;

// REMOVE_END

class Bicycle {
  public String brand;
  public String model;
  public BigDecimal price;
  public String description;
  public String condition;

  public Bicycle(String brand, String model, BigDecimal price, String condition, String description) {
    this.brand = brand;
    this.model = model;
    this.price = price;
    this.condition = condition;
    this.description = description;
  }
}

public class SearchQuickstartExample {

  @Test
  public void run() {
    // STEP_START connect
    // UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");
    JedisPooled jedis = new JedisPooled("localhost", 6379);
    // STEP_END
    // REMOVE_START
    try {
      jedis.ftDropIndex("idx:bicycle");
    } catch (JedisDataException e) {
      System.out.println("Can't connect to Redis: " + e.getMessage());
    }
    // REMOVE_END

    // STEP_START create_index
    SchemaField[] schema = {
      TextField.of("$.brand").as("brand"),
      TextField.of("$.model").as("model"),
      TextField.of("$.description").as("description"),
      NumericField.of("$.price").as("price"),
      TagField.of("$.condition").as("condition")
    };

    jedis.ftCreate("idx:bicycle",
      FTCreateParams.createParams()
      .on(IndexDataType.JSON)
      .addPrefix("bicycle:"),
      schema
    );
    // STEP_END

    Bicycle[] bicycles = {
        new Bicycle(
            "Velorim",
            "Jigger",
            new BigDecimal(270),
            "new",
            "Small and powerful, the Jigger is the best ride " +
            "for the smallest of tikes! This is the tiniest " +
            "kids’ pedal bike on the market available without" +
            " a coaster brake, the Jigger is the vehicle of " +
            "choice for the rare tenacious little rider " +
            "raring to go."
        ),
        new Bicycle(
            "Bicyk",
            "Hillcraft",
            new BigDecimal(1200),
            "used",
            "Kids want to ride with as little weight as possible." +
            " Especially on an incline! They may be at the age " +
            "when a 27.5 inch wheel bike is just too clumsy coming " +
            "off a 24 inch bike. The Hillcraft 26 is just the solution" +
            " they need!"
        ),
        new Bicycle(
            "Nord",
            "Chook air 5",
            new BigDecimal(815),
            "used",
            "The Chook Air 5  gives kids aged six years and older " +
            "a durable and uberlight mountain bike for their first" +
            " experience on tracks and easy cruising through forests" +
            " and fields. The lower  top tube makes it easy to mount" +
            " and dismount in any situation, giving your kids greater" +
            " safety on the trails."
        ),
        new Bicycle(
            "Eva",
            "Eva 291",
            new BigDecimal(3400),
            "used",
            "The sister company to Nord, Eva launched in 2005 as the" +
            " first and only women-dedicated bicycle brand. Designed" +
            " by women for women, allEva bikes are optimized for the" +
            " feminine physique using analytics from a body metrics" +
            " database. If you like 29ers, try the Eva 291. It's a " +
            "brand new bike for 2022.. This full-suspension, " +
            "cross-country ride has been designed for velocity. The" +
            " 291 has 100mm of front and rear travel, a superlight " +
            "aluminum frame and fast-rolling 29-inch wheels. Yippee!"
        ),
        new Bicycle(
            "Noka Bikes",
            "Kahuna",
            new BigDecimal(3200),
            "used",
            "Whether you want to try your hand at XC racing or are " +
            "looking for a lively trail bike that's just as inspiring" +
            " on the climbs as it is over rougher ground, the Wilder" +
            " is one heck of a bike built specifically for short women." +
            " Both the frames and components have been tweaked to " +
            "include a women’s saddle, different bars and unique " +
            "colourway."
        ),
        new Bicycle(
            "Breakout",
            "XBN 2.1 Alloy",
            new BigDecimal(810),
            "new",
            "The XBN 2.1 Alloy is our entry-level road bike – but that’s" +
            " not to say that it’s a basic machine. With an internal " +
            "weld aluminium frame, a full carbon fork, and the slick-shifting" +
            " Claris gears from Shimano’s, this is a bike which doesn’t" +
            " break the bank and delivers craved performance."
        ),
        new Bicycle(
            "ScramBikes",
            "WattBike",
            new BigDecimal(2300),
            "new",
            "The WattBike is the best e-bike for people who still feel young" +
            " at heart. It has a Bafang 1000W mid-drive system and a 48V" +
            " 17.5AH Samsung Lithium-Ion battery, allowing you to ride for" +
            " more than 60 miles on one charge. It’s great for tackling hilly" +
            " terrain or if you just fancy a more leisurely ride. With three" +
            " working modes, you can choose between E-bike, assisted bicycle," +
            " and normal bike modes."
        ),
        new Bicycle(
            "Peaknetic",
            "Secto",
            new BigDecimal(430),
            "new",
            "If you struggle with stiff fingers or a kinked neck or back after" +
            " a few minutes on the road, this lightweight, aluminum bike" +
            " alleviates those issues and allows you to enjoy the ride. From" +
            " the ergonomic grips to the lumbar-supporting seat position, the" +
            " Roll Low-Entry offers incredible comfort. The rear-inclined seat" +
            " tube facilitates stability by allowing you to put a foot on the" +
            " ground to balance at a stop, and the low step-over frame makes it" +
            " accessible for all ability and mobility levels. The saddle is" +
            " very soft, with a wide back to support your hip joints and a" +
            " cutout in the center to redistribute that pressure. Rim brakes" +
            " deliver satisfactory braking control, and the wide tires provide" +
            " a smooth, stable ride on paved roads and gravel. Rack and fender" +
            " mounts facilitate setting up the Roll Low-Entry as your preferred" +
            " commuter, and the BMX-like handlebar offers space for mounting a" +
            " flashlight, bell, or phone holder."
        ),
        new Bicycle(
            "nHill",
            "Summit",
            new BigDecimal(1200),
            "new",
            "This budget mountain bike from nHill performs well both on bike" +
            " paths and on the trail. The fork with 100mm of travel absorbs" +
            " rough terrain. Fat Kenda Booster tires give you grip in corners" +
            " and on wet trails. The Shimano Tourney drivetrain offered enough" +
            " gears for finding a comfortable pace to ride uphill, and the" +
            " Tektro hydraulic disc brakes break smoothly. Whether you want an" +
            " affordable bike that you can take to work, but also take trail in" +
            " mountains on the weekends or you’re just after a stable," +
            " comfortable ride for the bike path, the Summit gives a good value" +
            " for money."
        ),
        new Bicycle(
            "ThrillCycle",
            "BikeShind",
            new BigDecimal(815),
            "refurbished",
            "An artsy,  retro-inspired bicycle that’s as functional as it is" +
            " pretty: The ThrillCycle steel frame offers a smooth ride. A" +
            " 9-speed drivetrain has enough gears for coasting in the city, but" +
            " we wouldn’t suggest taking it to the mountains. Fenders protect" +
            " you from mud, and a rear basket lets you transport groceries," +
            " flowers and books. The ThrillCycle comes with a limited lifetime" +
            " warranty, so this little guy will last you long past graduation."
        ),
    };

    // STEP_START add_documents
    for (int i = 0; i < bicycles.length; i++) {
      jedis.jsonSetWithEscape(String.format("bicycle:%d", i), bicycles[i]);
    }
    // STEP_END

    // STEP_START wildcard_query
    Query query1 = new Query("*");
    List<Document> result1 = jedis.ftSearch("idx:bicycle", query1).getDocuments();
    System.out.println("Documents found:" + result1.size());
    // Prints: Documents found: 10
    // STEP_END
    // REMOVE_START
    assertEquals("Validate total results", 10, result1.size());
    // REMOVE_END

    // STEP_START query_single_term
    Query query2 = new Query("@model:Jigger");
    List<Document> result2 = jedis.ftSearch("idx:bicycle", query2).getDocuments();
    System.out.println(result2);
    // Prints: [id:bicycle:0, score: 1.0, payload:null,
    // properties:[$={"brand":"Velorim","model":"Jigger","price":270,"description":"Small and powerful, the Jigger is the best ride for the smallest of tikes! This is the tiniest kids’ pedal bike on the market available without a coaster brake, the Jigger is the vehicle of choice for the rare tenacious little rider raring to go.","condition":"new"}]]
    // STEP_END
    // REMOVE_START
    assertEquals("Validate bike id", "bicycle:0", result2.get(0).getId());
    // REMOVE_END

    // STEP_START query_single_term_limit_fields
    Query query3 = new Query("@model:Jigger").returnFields("price");
    List<Document> result3 = jedis.ftSearch("idx:bicycle", query3).getDocuments();
    System.out.println(result3);
    // Prints: [id:bicycle:0, score: 1.0, payload:null, properties:[price=270]]
    // STEP_END
    // REMOVE_START
    assertEquals("Validate cargo bike id", "bicycle:0", result3.get(0).getId());
    // REMOVE_END

    // STEP_START query_single_term_and_num_range
    Query query4 = new Query("basic @price:[500 1000]");
    List<Document> result4 = jedis.ftSearch("idx:bicycle", query4).getDocuments();
    System.out.println(result4);
    // Prints: [id:bicycle:5, score: 1.0, payload:null,
    // properties:[$={"brand":"Breakout","model":"XBN 2.1 Alloy","price":810,"description":"The XBN 2.1 Alloy is our entry-level road bike – but that’s not to say that it’s a basic machine. With an internal weld aluminium frame, a full carbon fork, and the slick-shifting Claris gears from Shimano’s, this is a bike which doesn’t break the bank and delivers craved performance.","condition":"new"}]]
    // STEP_END
    // REMOVE_START
    assertEquals("Validate bike id", "bicycle:5", result4.get(0).getId());
    // REMOVE_END

    // STEP_START query_exact_matching
    Query query5 = new Query("@brand:\"Noka Bikes\"");
    List<Document> result5 = jedis.ftSearch("idx:bicycle", query5).getDocuments();
    System.out.println(result5);
    // Prints: [id:bicycle:4, score: 1.0, payload:null,
    // properties:[$={"brand":"Noka Bikes","model":"Kahuna","price":3200,"description":"Whether you want to try your hand at XC racing or are looking for a lively trail bike that's just as inspiring on the climbs as it is over rougher ground, the Wilder is one heck of a bike built specifically for short women. Both the frames and components have been tweaked to include a women’s saddle, different bars and unique colourway.","condition":"used"}]]
    // STEP_END
    // REMOVE_START
    assertEquals("Validate bike id", "bicycle:4", result5.get(0).getId());
    // REMOVE_END

    // STEP_START simple_aggregation
    AggregationBuilder ab = new AggregationBuilder("*").groupBy("@condition",
      Reducers.count().as("count"));
    AggregationResult ar = jedis.ftAggregate("idx:bicycle", ab);
    for (int i = 0; i < ar.getTotalResults(); i++) {
      System.out.println(ar.getRow(i).getString("condition") + " - "
          + ar.getRow(i).getString("count"));
    }
    // Prints:
    // refurbished - 1
    // used - 5
    // new - 4
    assertEquals("Validate aggregation results", 3, ar.getTotalResults());
    // STEP_END

    jedis.close();
  }
}