// EXAMPLE: json_tutorial
// REMOVE_START
package io.redis.examples;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

// REMOVE_END
// HIDE_START
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.json.Path2;

import org.json.JSONArray;
import org.json.JSONObject;

// HIDE_END

// HIDE_START
public class JsonExample {
    @Test
    public void run() {
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");
// HIDE_END

        //REMOVE_START
        // Clear any keys here before using them in tests.
        jedis.del("bike", "bike:1", "crashes", "newbike", "riders", "bikes:inventory");
        //REMOVE_END

        // STEP_START set_get
        String res1 = jedis.jsonSet("bike", new Path2("$"), "\"Hyperion\"");
        System.out.println(res1);   // >>> OK

        Object res2 = jedis.jsonGet("bike", new Path2("$"));
        System.out.println(res2);   // >>> ["Hyperion"]

        List<Class<?>> res3 = jedis.jsonType("bike", new Path2("$"));
        System.out.println(res3);   // >>> [class java.lang.String]
        // STEP_END

        // Tests for 'set_get' step.
        // REMOVE_START
        Assert.assertEquals("OK", res1);
        Assert.assertEquals("[\"Hyperion\"]", res2.toString());
        Assert.assertEquals("[class java.lang.String]", res3.toString());
        // REMOVE_END


        // STEP_START str
        List<Long> res4 = jedis.jsonStrLen("bike", new Path2("$"));
        System.out.println(res4);   // >>> [8]

        List<Long> res5 = jedis.jsonStrAppend("bike", new Path2("$"), " (Enduro bikes)");
        System.out.println(res5);   // >>> [23]

        Object res6 = jedis.jsonGet("bike", new Path2("$"));
        System.out.println(res6);   // >>> ["Hyperion (Enduro bikes)"]
        // STEP_END

        // Tests for 'str' step.
        // REMOVE_START
        Assert.assertEquals("[8]", res4.toString());
        Assert.assertEquals("[23]", res5.toString());
        Assert.assertEquals("[\"Hyperion (Enduro bikes)\"]", res6.toString());
        // REMOVE_END


        // STEP_START num
        String res7 = jedis.jsonSet("crashes", new Path2("$"), 0);
        System.out.println(res7);   // >>> OK

        Object res8 = jedis.jsonNumIncrBy("crashes", new Path2("$"), 1);
        System.out.println(res8);   // >>> [1]

        Object res9 = jedis.jsonNumIncrBy("crashes", new Path2("$"), 1.5);
        System.out.println(res9);   // >>> [2.5]

        Object res10 = jedis.jsonNumIncrBy("crashes", new Path2("$"), -0.75);
        System.out.println(res10);   // >>> [1.75]
        // STEP_END

        // Tests for 'num' step.
        // REMOVE_START
        Assert.assertEquals("OK", res7);
        Assert.assertEquals("[1]", res8.toString());
        Assert.assertEquals("[2.5]", res9.toString());
        Assert.assertEquals("[1.75]", res10.toString());
        // REMOVE_END


        // STEP_START arr
        String res11 = jedis.jsonSet("newbike", new Path2("$"),
            new JSONArray()
                .put("Deimos")
                .put(new JSONObject().put("crashes", 0))
                .put((Object) null)
        );
        System.out.println(res11);  // >>> OK
        
        Object res12 = jedis.jsonGet("newbike", new Path2("$"));
        System.out.println(res12);  // >>> [["Deimos",{"crashes":0},null]]

        Object res13 = jedis.jsonGet("newbike", new Path2("$[1].crashes"));
        System.out.println(res13);  // >>> [0]

        long res14 = jedis.jsonDel("newbike", new Path2("$.[-1]"));
        System.out.println(res14);  // >>> 1

        Object res15 = jedis.jsonGet("newbike", new Path2("$"));
        System.out.println(res15);  // >>> [["Deimos",{"crashes":0}]]
        // STEP_END

        // Tests for 'arr' step.
        // REMOVE_START
        Assert.assertEquals("OK", res11);
        Assert.assertEquals("[[\"Deimos\",{\"crashes\":0},null]]", res12.toString());
        Assert.assertEquals("[0]", res13.toString());
        Assert.assertEquals(1, res14);
        Assert.assertEquals("[[\"Deimos\",{\"crashes\":0}]]", res15.toString());
        // REMOVE_END


        // STEP_START arr2
        String res16 = jedis.jsonSet("riders", new Path2("$"), new JSONArray());
        System.out.println(res16);  // >>> OK

        List<Long> res17 = jedis.jsonArrAppendWithEscape("riders", new Path2("$"), "Norem");
        System.out.println(res17);  // >>> [1]

        Object res18 = jedis.jsonGet("riders", new Path2("$"));
        System.out.println(res18);  // >>> [["Norem"]]

        List<Long> res19 = jedis.jsonArrInsertWithEscape(
            "riders", new Path2("$"), 1, "Prickett", "Royce", "Castilla"
        );
        System.out.println(res19);  // >>> [4]

        Object res20 = jedis.jsonGet("riders", new Path2("$"));
        System.out.println(res20);
        // >>> [["Norem","Prickett","Royce","Castilla"]]
        
        List<Long> res21 = jedis.jsonArrTrim("riders", new Path2("$"), 1, 1);
        System.out.println(res21);  // >>> [1]

        Object res22 = jedis.jsonGet("riders", new Path2("$"));
        System.out.println(res22);  // >>> [["Prickett"]]

        Object res23 = jedis.jsonArrPop("riders", new Path2("$"));
        System.out.println(res23);  // >>> [Prickett]

        Object res24 = jedis.jsonArrPop("riders", new Path2("$"));
        System.out.println(res24);  // >>> [null]
        // STEP_END

        // Tests for 'arr2' step.
        // REMOVE_START
        Assert.assertEquals("OK", res16);
        Assert.assertEquals("[1]", res17.toString());
        Assert.assertEquals("[[\"Norem\"]]", res18.toString());
        Assert.assertEquals("[4]", res19.toString());
        Assert.assertEquals("[[\"Norem\",\"Prickett\",\"Royce\",\"Castilla\"]]", res20.toString());
        Assert.assertEquals("[1]", res21.toString());
        Assert.assertEquals("[[\"Prickett\"]]", res22.toString());
        Assert.assertEquals("[Prickett]", res23.toString());
        Assert.assertEquals("[null]", res24.toString());
        // REMOVE_END


        // STEP_START obj
        String res25 = jedis.jsonSet("bike:1", new Path2("$"),
            new JSONObject()
                .put("model", "Deimos")
                .put("brand", "Ergonom")
                .put("price", 4972)
        );
        System.out.println(res25);  // >>> OK

        List<Long> res26 = jedis.jsonObjLen("bike:1", new Path2("$"));
        System.out.println(res26);  // >>> [3]

        List<List<String>> res27 = jedis.jsonObjKeys("bike:1", new Path2("$"));
        System.out.println(res27);  // >>> [[price, model, brand]]
        // STEP_END

        // Tests for 'obj' step.
        // REMOVE_START
        Assert.assertEquals("OK", res25);
        Assert.assertEquals("[3]", res26.toString());
        Assert.assertEquals("[[price, model, brand]]", res27.toString());
        // REMOVE_END

        // STEP_START set_bikes
        String inventory_json = "{"
        + "    \"inventory\": {"
        + "        \"mountain_bikes\": ["
        + "            {"
        + "                \"id\": \"bike:1\","
        + "                \"model\": \"Phoebe\","
        + "                \"description\": \"This is a mid-travel trail slayer that is a "
        + "fantastic daily driver or one bike quiver. The Shimano Claris 8-speed groupset "
        + "gives plenty of gear range to tackle hills and there\u2019s room for mudguards "
        + "and a rack too.  This is the bike for the rider who wants trail manners with "
        + "low fuss ownership.\","
        + "                \"price\": 1920,"
        + "                \"specs\": {\"material\": \"carbon\", \"weight\": 13.1},"
        + "                \"colors\": [\"black\", \"silver\"]"
        + "            },"
        + "            {"
        + "                \"id\": \"bike:2\","
        + "                \"model\": \"Quaoar\","
        + "                \"description\": \"Redesigned for the 2020 model year, this "
        + "bike impressed our testers and is the best all-around trail bike we've ever "
        + "tested. The Shimano gear system effectively does away with an external cassette, "
        + "so is super low maintenance in terms of wear and tear. All in all it's an "
        + "impressive package for the price, making it very competitive.\","
        + "                \"price\": 2072,"
        + "                \"specs\": {\"material\": \"aluminium\", \"weight\": 7.9},"
        + "                \"colors\": [\"black\", \"white\"]"
        + "            },"
        + "            {"
        + "                \"id\": \"bike:3\","
        + "                \"model\": \"Weywot\","
        + "                \"description\": \"This bike gives kids aged six years and older "
        + "a durable and uberlight mountain bike for their first experience on tracks and easy "
        + "cruising through forests and fields. A set of powerful Shimano hydraulic disc brakes "
        + "provide ample stopping ability. If you're after a budget option, this is one of the "
        + "best bikes you could get.\","
        + "                \"price\": 3264,"
        + "                \"specs\": {\"material\": \"alloy\", \"weight\": 13.8}"
        + "            }"
        + "        ],"
        + "        \"commuter_bikes\": ["
        + "            {"
        + "                \"id\": \"bike:4\","
        + "                \"model\": \"Salacia\","
        + "                \"description\": \"This bike is a great option for anyone who just "
        + "wants a bike to get about on With a slick-shifting Claris gears from Shimano\u2019s, "
        + "this is a bike which doesn\u2019t break the bank and delivers craved performance.  "
        + "It\u2019s for the rider who wants both efficiency and capability.\","
        + "                \"price\": 1475,"
        + "                \"specs\": {\"material\": \"aluminium\", \"weight\": 16.6},"
        + "                \"colors\": [\"black\", \"silver\"]"
        + "            },"
        + "            {"
        + "                \"id\": \"bike:5\","
        + "                \"model\": \"Mimas\","
        + "                \"description\": \"A real joy to ride, this bike got very high scores "
        + "in last years Bike of the year report. The carefully crafted 50-34 tooth chainset "
        + "and 11-32 tooth cassette give an easy-on-the-legs bottom gear for climbing, and the "
        + "high-quality Vittoria Zaffiro tires give balance and grip.It includes a low-step "
        + "frame , our memory foam seat, bump-resistant shocks and conveniently placed thumb "
        + "throttle. Put it all together and you get a bike that helps redefine what can be "
        + "done for this price.\","
        + "                \"price\": 3941,"
        + "                \"specs\": {\"material\": \"alloy\", \"weight\": 11.6}"
        + "            }"
        + "        ]"
        + "    }"
        + "}";

        String res28 = jedis.jsonSet("bikes:inventory", new Path2("$"), inventory_json);
        System.out.println(res28);  // >>> OK
        // STEP_END

        // Tests for 'set_bikes' step.
        // REMOVE_START
        Assert.assertEquals("OK", res28);
        // REMOVE_END


        // STEP_START get_bikes
        Object res29 = jedis.jsonGet("bikes:inventory", new Path2("$.inventory.*"));
        System.out.println(res29);
        // >>> [[{"specs":{"material":"carbon","weight":13.1},"price":1920, ...
        // STEP_END

        // Tests for 'get_bikes' step.
        // REMOVE_START
        Assert.assertEquals(
            "[[{\"specs\":{\"material\":\"carbon\",\"weight\":13.1},\"price\":1920,"
            + "\"description\":\"This is a mid-travel trail slayer that is a "
            + "fantastic daily driver or one bike quiver. The Shimano Claris 8-speed "
            + "groupset gives plenty of gear range to tackle hills and "
            + "there\\u2019s room for mudguards and a rack too.  This is the bike "
            + "for the rider who wants trail manners with low fuss "
            + "ownership.\",\"model\":\"Phoebe\",\"id\":\"bike:1\",\"colors\":"
            + "[\"black\",\"silver\"]},{\"specs\":{\"material\":\"aluminium\",\"weight\":7.9},"
            + "\"price\":2072,\"description\":\"Redesigned for the 2020 model year, this "
            + "bike impressed our testers and is the best all-around trail bike we've "
            + "ever tested. The Shimano gear system effectively does away with an "
            + "external cassette, so is super low maintenance in terms of wear and tear. "
            + "All in all it's an impressive package for the price, making it very "
            + "competitive.\",\"model\":\"Quaoar\",\"id\":\"bike:2\",\"colors\":"
            + "[\"black\",\"white\"]},{\"specs\":{\"material\":\"alloy\",\"weight\":13.8},"
            + "\"price\":3264,\"description\":\"This bike gives kids aged six years and "
            + "older a durable and uberlight mountain bike for their first experience "
            + "on tracks and easy cruising through forests and fields. A set of "
            + "powerful Shimano hydraulic disc brakes provide ample stopping ability. "
            + "If you're after a budget option, this is one of the best bikes you could "
            + "get.\",\"model\":\"Weywot\",\"id\":\"bike:3\"}],[{\"specs\":"
            + "{\"material\":\"aluminium\",\"weight\":16.6},\"price\":1475,\"description\":"
            + "\"This bike is a great option for anyone who just wants a bike to get about "
            + "on With a slick-shifting Claris gears from Shimano\\u2019s, this is a bike "
            + "which doesn\\u2019t break the bank and delivers craved performance.  "
            + "It\\u2019s for the rider who wants both efficiency and "
            + "capability.\",\"model\":\"Salacia\",\"id\":\"bike:4\",\"colors\":"
            + "[\"black\",\"silver\"]},{\"specs\":{\"material\":\"alloy\",\"weight\":11.6},"
            + "\"price\":3941,\"description\":\"A real joy to ride, this bike got very "
            + "high scores in last years Bike of the year report. The carefully crafted "
            + "50-34 tooth chainset and 11-32 tooth cassette give an easy-on-the-legs "
            + "bottom gear for climbing, and the high-quality Vittoria Zaffiro tires "
            + "give balance and grip.It includes a low-step frame , our memory foam "
            + "seat, bump-resistant shocks and conveniently placed thumb throttle. Put "
            + "it all together and you get a bike that helps redefine what can be done "
            + "for this price.\",\"model\":\"Mimas\",\"id\":\"bike:5\"}]]",
             res29.toString()
        );
        // REMOVE_END


        // STEP_START get_mtnbikes
        Object res30 = jedis.jsonGet(
            "bikes:inventory", new Path2("$.inventory.mountain_bikes[*].model")
        );
        System.out.println(res30);  // >>> ["Phoebe","Quaoar","Weywot"]

        Object res31 = jedis.jsonGet(
            "bikes:inventory", new Path2("$.inventory[\"mountain_bikes\"][*].model")
        );
        System.out.println(res31);  // >>> ["Phoebe","Quaoar","Weywot"]

        Object res32 = jedis.jsonGet(
            "bikes:inventory", new Path2("$..mountain_bikes[*].model")
        );
        System.out.println(res32);  // >>> ["Phoebe","Quaoar","Weywot"]
        // STEP_END

        // Tests for 'get_mtnbikes' step.
        // REMOVE_START
        Assert.assertEquals("[\"Phoebe\",\"Quaoar\",\"Weywot\"]", res30.toString());
        Assert.assertEquals("[\"Phoebe\",\"Quaoar\",\"Weywot\"]", res31.toString());
        Assert.assertEquals("[\"Phoebe\",\"Quaoar\",\"Weywot\"]", res32.toString());
        // REMOVE_END


        // STEP_START get_models
        Object res33 = jedis.jsonGet("bikes:inventory", new Path2("$..model"));
        System.out.println(res33);
        // >>> ["Phoebe","Quaoar","Weywot","Salacia","Mimas"]
        // STEP_END

        // Tests for 'get_models' step.
        // REMOVE_START
        Assert.assertEquals("[\"Phoebe\",\"Quaoar\",\"Weywot\",\"Salacia\",\"Mimas\"]", res33.toString());
        // REMOVE_END


        // STEP_START get2mtnbikes
        Object res34 = jedis.jsonGet(
            "bikes:inventory", new Path2("$..mountain_bikes[0:2].model")
        );
        System.out.println(res34);  // >>> ["Phoebe","Quaoar"]
        // STEP_END

        // Tests for 'get2mtnbikes' step.
        // REMOVE_START
        Assert.assertEquals("[\"Phoebe\",\"Quaoar\"]", res34.toString());
        // REMOVE_END


        // STEP_START filter1
        Object res35 = jedis.jsonGet(
            "bikes:inventory",
            new Path2("$..mountain_bikes[?(@.price < 3000 && @.specs.weight < 10)]")
        );
        System.out.println(res35);
        // >>> [{"specs":{"material":"aluminium","weight":7.9},"price":2072,...
        // STEP_END

        // Tests for 'filter1' step.
        // REMOVE_START
        Assert.assertEquals(
            "[{\"specs\":{\"material\":\"aluminium\",\"weight\":7.9},\"price\":2072,"
            + "\"description\":\"Redesigned for the 2020 model year, this bike impressed "
            + "our testers and is the best all-around trail bike we've ever tested. The "
            + "Shimano gear system effectively does away with an external cassette, "
            + "so is super low maintenance in terms of wear and tear. All in all it's an "
            + "impressive package for the price, making it very competitive.\",\"model\":"
            + "\"Quaoar\",\"id\":\"bike:2\",\"colors\":[\"black\",\"white\"]}]",
            res35.toString()
        );
        // REMOVE_END


        // STEP_START filter2
        Object res36 = jedis.jsonGet(
            "bikes:inventory", new Path2("$..[?(@.specs.material == 'alloy')].model")
        );
        System.out.println(res36);  // >>> ["Weywot","Mimas"]
        // STEP_END

        // Tests for 'filter2' step.
        // REMOVE_START
        Assert.assertEquals("[\"Weywot\",\"Mimas\"]", res36.toString());
        // REMOVE_END


        // STEP_START filter3
        Object res37 = jedis.jsonGet(
            "bikes:inventory", new Path2("$..[?(@.specs.material =~ '(?i)al')].model")
        );
        System.out.println(res37);
        // >>> ["Quaoar","Weywot","Salacia","Mimas"]
        // STEP_END

        // Tests for 'filter3' step.
        // REMOVE_START
        Assert.assertEquals("[\"Quaoar\",\"Weywot\",\"Salacia\",\"Mimas\"]", res37.toString());
        // REMOVE_END


        // STEP_START filter4
        jedis.jsonSet(
            "bikes:inventory", new Path2("$.inventory.mountain_bikes[0].regex_pat"),
            "\"(?i)al\""
        );
        jedis.jsonSet(
            "bikes:inventory", new Path2("$.inventory.mountain_bikes[1].regex_pat"),
            "\"(?i)al\""
        );
        jedis.jsonSet(
            "bikes:inventory", new Path2("$.inventory.mountain_bikes[2].regex_pat"),
            "\"(?i)al\""
        );
        
        Object res38 = jedis.jsonGet(
            "bikes:inventory",
            new Path2("$.inventory.mountain_bikes[?(@.specs.material =~ @.regex_pat)].model")
        );
        System.out.println(res38);  // >>> ["Quaoar","Weywot"]
        // STEP_END

        // Tests for 'filter4' step.
        // REMOVE_START
        Assert.assertEquals("[\"Quaoar\",\"Weywot\"]", res38.toString());
        // REMOVE_END


        // STEP_START update_bikes
        Object res39 = jedis.jsonGet("bikes:inventory", new Path2("$..price"));
        System.out.println(res39);
        // >>> [1920,2072,3264,1475,3941]

        Object res40 = jedis.jsonNumIncrBy("bikes:inventory", new Path2("$..price"), -100);
        System.out.println(res40);  // >>> [1820,1972,3164,1375,3841]

        Object res41 = jedis.jsonNumIncrBy("bikes:inventory", new Path2("$..price"), 100);
        System.out.println(res41);  // >>> [1920,2072,3264,1475,3941]
        // STEP_END

        // Tests for 'update_bikes' step.
        // REMOVE_START
        Assert.assertEquals("[1920,2072,3264,1475,3941]", res39.toString());
        Assert.assertEquals("[1820,1972,3164,1375,3841]", res40.toString());
        Assert.assertEquals("[1920,2072,3264,1475,3941]", res41.toString());
        // REMOVE_END


        // STEP_START update_filters1
        jedis.jsonSet("bikes:inventory", new Path2("$.inventory.*[?(@.price<2000)].price"), 1500);
        Object res42 = jedis.jsonGet("bikes:inventory", new Path2("$..price"));
        System.out.println(res42);  // >>> [1500,2072,3264,1500,3941]
        // STEP_END

        // Tests for 'update_filters1' step.
        // REMOVE_START
        Assert.assertEquals("[1500,2072,3264,1500,3941]", res42.toString());
        // REMOVE_END


        // STEP_START update_filters2
        List<Long> res43 = jedis.jsonArrAppendWithEscape(
            "bikes:inventory", new Path2("$.inventory.*[?(@.price<2000)].colors"),
            "\"pink\""
        );
        System.out.println(res43);  // >>> [3, 3]

        Object res44 = jedis.jsonGet("bikes:inventory", new Path2("$..[*].colors"));
        System.out.println(res44);
        // >>> [["black","silver","\"pink\""],["black","white"],["black","silver","\"pink\""]]
        // STEP_END

        // Tests for 'update_filters2' step.
        // REMOVE_START
        Assert.assertEquals("[3, 3]", res43.toString());
        Assert.assertEquals(
            "[[\"black\",\"silver\",\"\\\"pink\\\"\"],[\"black\",\"white\"],"
            + "[\"black\",\"silver\",\"\\\"pink\\\"\"]]",
            res44.toString());
        // REMOVE_END

// HIDE_START
        jedis.close();
    }
}
// HIDE_END

