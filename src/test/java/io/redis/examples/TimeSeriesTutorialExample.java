// EXAMPLE: time_series_tutorial
// REMOVE_START
package io.redis.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
// REMOVE_END
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.timeseries.*;
import redis.clients.jedis.timeseries.TSElement;

import java.util.*;

public class TimeSeriesTutorialExample {

    @Test
    public void run() {
        UnifiedJedis jedis = new UnifiedJedis("redis://localhost:6379");
        // REMOVE_START
        // Clear any keys before using them in tests
        jedis.del(
            "thermometer:1", "thermometer:2", "thermometer:3",
            "rg:1", "rg:2", "rg:3", "rg:4",
            "sensor3",
            "wind:1", "wind:2", "wind:3", "wind:4",
            "hyg:1", "hyg:compacted"
        );
        // REMOVE_END

        // STEP_START create
        String res1 = jedis.tsCreate("thermometer:1");
        System.out.println(res1); // >>> OK

        String res2 = jedis.type("thermometer:1");
        System.out.println(res2); // >>> TSDB-TYPE

        TSInfo res3 = jedis.tsInfo("thermometer:1");
        System.out.println(res3.getProperty("totalSamples")); // >>> 0
        // STEP_END
        // REMOVE_START
        assertEquals("OK", res1);
        assertEquals("TSDB-TYPE", res2);
        assertEquals((Long) 0L, res3.getProperty("totalSamples"));
        // REMOVE_END

        // STEP_START create_retention
        long res4 = jedis.tsAdd("thermometer:2", 1L, 10.8, 
            TSCreateParams.createParams().retention(100));
        System.out.println(res4); // >>> 1

        TSInfo res5 = jedis.tsInfo("thermometer:2");
        System.out.println(res5.getProperty("retentionTime")); // >>> 100
        // STEP_END
        // REMOVE_START
        assertEquals(1L, res4);
        assertEquals((Long) 100L, res5.getProperty("retentionTime"));
        // REMOVE_END

        // STEP_START create_labels
        Map<String, String> labels = new HashMap<>();
        labels.put("location", "UK");
        labels.put("type", "Mercury");
        
        long res6 = jedis.tsAdd("thermometer:3", 1L, 10.4,
            TSCreateParams.createParams().labels(labels));
        System.out.println(res6); // >>> 1

        TSInfo res7 = jedis.tsInfo("thermometer:3");
        System.out.println("Labels: " + res7.getLabels());
        // >>> Labels: {location=UK, type=Mercury}
        // STEP_END
        // REMOVE_START
        assertEquals(1L, res6);
        assertEquals(labels, res7.getLabels());
        // REMOVE_END

        // STEP_START madd
        List<Long> res8 = jedis.tsMAdd(
            new AbstractMap.SimpleEntry<>("thermometer:1", new TSElement(1L, 9.2)),
            new AbstractMap.SimpleEntry<>("thermometer:1", new TSElement(2L, 9.9)),
            new AbstractMap.SimpleEntry<>("thermometer:2", new TSElement(2L, 10.3))
        );
        System.out.println(res8); // >>> [1, 2, 2]
        // STEP_END
        // REMOVE_START
        assertEquals(Arrays.asList(1L, 2L, 2L), res8);
        // REMOVE_END

        // STEP_START get
        // The last recorded temperature for thermometer:2
        // was 10.3 at time 2.
        TSElement res9 = jedis.tsGet("thermometer:2");
        System.out.println("(" + res9.getTimestamp() + ", " + res9.getValue() + ")");
        // >>> (2, 10.3)
        // STEP_END
        // REMOVE_START
        assertEquals(2L, res9.getTimestamp());
        assertEquals(10.3, res9.getValue(), 0.001);
        // REMOVE_END

        // STEP_START range
        // Add 5 data points to a time series named "rg:1"
        String res10 = jedis.tsCreate("rg:1");
        System.out.println(res10); // >>> OK

        List<Long> res11 = jedis.tsMAdd(
            new AbstractMap.SimpleEntry<>("rg:1", new TSElement(0L, 18.0)),
            new AbstractMap.SimpleEntry<>("rg:1", new TSElement(1L, 14.0)),
            new AbstractMap.SimpleEntry<>("rg:1", new TSElement(2L, 22.0)),
            new AbstractMap.SimpleEntry<>("rg:1", new TSElement(3L, 18.0)),
            new AbstractMap.SimpleEntry<>("rg:1", new TSElement(4L, 24.0))
        );
        System.out.println(res11); // >>> [0, 1, 2, 3, 4]

        // Retrieve all the data points in ascending order
        List<TSElement> res12 = jedis.tsRange("rg:1", 0L, 4L);
        System.out.println(res12);
        // >>> [(0:18.0), (1:14.0), (2:22.0), (3:18.0), (4:24.0)]

        // Retrieve data points up to time 1 (inclusive)
        List<TSElement> res13 = jedis.tsRange("rg:1", 0L, 1L);
        System.out.println(res13);
        // >>> [(0:18.0), (1:14.0)]

        // Retrieve data points from time 3 onwards
        List<TSElement> res14 = jedis.tsRange("rg:1", 3L, 4L);
        System.out.println(res14);
        // >>> [(3:18.0), (4:24.0)]

        // Retrieve all the data points in descending order
        List<TSElement> res15 = jedis.tsRevRange("rg:1", 0L, 4L);
        System.out.println(res15);
        // >>> [(4:24.0), (3:18.0), (2:22.0), (1:14.0), (0:18.0)]

        // Retrieve data points up to time 1 (inclusive), in descending order
        List<TSElement> res16 = jedis.tsRevRange("rg:1", 0L, 1L);
        System.out.println(res16);
        // >>> [(1:14.0), (0:18.0)]
        // STEP_END
        // REMOVE_START
        assertEquals("OK", res10);
        assertEquals(Arrays.asList(0L, 1L, 2L, 3L, 4L), res11);
        assertEquals(Arrays.asList(
            new TSElement(0L, 18.0), new TSElement(1L, 14.0), new TSElement(2L, 22.0), 
            new TSElement(3L, 18.0), new TSElement(4L, 24.0)), res12);
        assertEquals(Arrays.asList(new TSElement(0L, 18.0), new TSElement(1L, 14.0)), res13);
        assertEquals(Arrays.asList(new TSElement(3L, 18.0), new TSElement(4L, 24.0)), res14);
        assertEquals(Arrays.asList(
            new TSElement(4L, 24.0), new TSElement(3L, 18.0), new TSElement(2L, 22.0), 
            new TSElement(1L, 14.0), new TSElement(0L, 18.0)), res15);
        assertEquals(Arrays.asList(new TSElement(1L, 14.0), new TSElement(0L, 18.0)), res16);
        // REMOVE_END

        // STEP_START range_filter
        List<TSElement> res17 = jedis.tsRange("rg:1",  
            TSRangeParams.rangeParams()
                .fromTimestamp(0L)
                .toTimestamp(4L)
                .filterByTS(0L, 2L, 4L)
        );
        System.out.println(res17);
        // >>> [(0:18.0), (2:22.0), (4:24.0)]

        List<TSElement> res18 = jedis.tsRevRange("rg:1",
            TSRangeParams.rangeParams()
                .fromTimestamp(0L)
                .toTimestamp(4L)
                .filterByTS(0L, 2L, 4L)
                .filterByValues(20.0, 25.0)
        );
        System.out.println(res18);
        // >>> [(4:24.0), (2:22.0)]

        List<TSElement> res19 = jedis.tsRevRange("rg:1",
            TSRangeParams.rangeParams()
                .fromTimestamp(0L)
                .toTimestamp(4L)
                .filterByTS(0L, 2L, 4L)
                .filterByValues(22.0, 22.0)
                .count(1)
        );
        System.out.println(res19);
        // >>> [(2:22.0)]
        // STEP_END
        // REMOVE_START
        assertEquals(Arrays.asList(
            new TSElement(0L, 18.0), new TSElement(2L, 22.0), new TSElement(4L, 24.0)), res17);
        assertEquals(Arrays.asList(new TSElement(4L, 24.0), new TSElement(2L, 22.0)), res18);
        assertEquals(Arrays.asList(new TSElement(2L, 22.0)), res19);
        // REMOVE_END

        // STEP_START query_multi
        // Create three new "rg:" time series (two in the US
        // and one in the UK, with different units) and add some
        // data points.
        Map<String, String> usLabels1 = new HashMap<>();
        usLabels1.put("location", "us");
        usLabels1.put("unit", "cm");
        
        Map<String, String> usLabels2 = new HashMap<>();
        usLabels2.put("location", "us");
        usLabels2.put("unit", "in");
        
        Map<String, String> ukLabels = new HashMap<>();
        ukLabels.put("location", "uk");
        ukLabels.put("unit", "mm");

        String res20 = jedis.tsCreate("rg:2",
            TSCreateParams.createParams().labels(usLabels1));
        System.out.println(res20); // >>> OK

        String res21 = jedis.tsCreate("rg:3",
            TSCreateParams.createParams().labels(usLabels2));
        System.out.println(res21); // >>> OK

        String res22 = jedis.tsCreate("rg:4",
            TSCreateParams.createParams().labels(ukLabels));
        System.out.println(res22); // >>> OK

        List<Long> res23 = jedis.tsMAdd(
            new AbstractMap.SimpleEntry<>("rg:2", new TSElement(0L, 1.8)),
            new AbstractMap.SimpleEntry<>("rg:3", new TSElement(0L, 0.9)),
            new AbstractMap.SimpleEntry<>("rg:4", new TSElement(0L, 25.0))
        );
        System.out.println(res23); // >>> [0, 0, 0]

        List<Long> res24 = jedis.tsMAdd(
            new AbstractMap.SimpleEntry<>("rg:2", new TSElement(1L, 2.1)),
            new AbstractMap.SimpleEntry<>("rg:3", new TSElement(1L, 0.77)),
            new AbstractMap.SimpleEntry<>("rg:4", new TSElement(1L, 18.0))
        );
        System.out.println(res24); // >>> [1, 1, 1]

        List<Long> res25 = jedis.tsMAdd(
            new AbstractMap.SimpleEntry<>("rg:2", new TSElement(2L, 2.3)),
            new AbstractMap.SimpleEntry<>("rg:3", new TSElement(2L, 1.1)),
            new AbstractMap.SimpleEntry<>("rg:4", new TSElement(2L, 21.0))
        );
        System.out.println(res25); // >>> [2, 2, 2]
        
        List<Long> res26 = jedis.tsMAdd(
            new AbstractMap.SimpleEntry<>("rg:2", new TSElement(3L, 1.9)),
            new AbstractMap.SimpleEntry<>("rg:3", new TSElement(3L, 0.81)),
            new AbstractMap.SimpleEntry<>("rg:4", new TSElement(3L, 19.0))
        );
        System.out.println(res26); // >>> [3, 3, 3]

        List<Long> res27 = jedis.tsMAdd(
            new AbstractMap.SimpleEntry<>("rg:2", new TSElement(4L, 1.78)),
            new AbstractMap.SimpleEntry<>("rg:3", new TSElement(4L, 0.74)),
            new AbstractMap.SimpleEntry<>("rg:4", new TSElement(4L, 23.0))
        );
        System.out.println(res27); // >>> [4, 4, 4]

        // Retrieve the last data point from each US time series.
        Map<String, TSMGetElement> res28 = jedis.tsMGet(
            TSMGetParams.multiGetParams().latest(),
             "location=us"
        );
        System.out.println(res28);
        // >>> {rg:2=TSMGetElement{key=rg:2, labels={}, element=(4:1.78)}...

        // Retrieve the same data points, but include the `unit`
        // label in the results.
        Map<String, TSMGetElement> res29 = jedis.tsMGet(
            TSMGetParams.multiGetParams().selectedLabels("unit"), 
            "location=us"
        );
        System.out.println(res29);
        // >>> {rg:2=TSMGetElement{key=rg:2, labels={unit=cm}, element=(4:1.78)}...

        // Retrieve data points up to time 2 (inclusive) from all
        // time series that use millimeters as the unit. Include all
        // labels in the results.
        Map<String, TSMRangeElements> res30 = jedis.tsMRange(
            TSMRangeParams.multiRangeParams(0L, 2L)
                .withLabels()
                .filter("unit=mm")
        );
        System.out.println(res30);
        // >>> {rg:4=TSMRangeElements{key=rg:4, labels={location=uk, unit=mm}, value=[(0:25.0), (1:18.0), (2:21.0)]}}

        // Retrieve data points from time 1 to time 3 (inclusive) from
        // all time series that use centimeters or millimeters as the unit,
        // but only return the `location` label. Return the results
        // in descending order of timestamp.
        Map<String, TSMRangeElements> res31 = jedis.tsMRevRange(
            TSMRangeParams.multiRangeParams(1L, 3L)
                .selectedLabels("location")
                .filter("unit=(cm,mm)")
        );
        System.out.println(res31);
        // >>> {rg:2=TSMRangeElements{key=rg:2, labels={location=us, unit=cm}, value=[(1:2.1)...
        // STEP_END
        // REMOVE_START
        assertEquals("OK", res20);
        assertEquals("OK", res21);
        assertEquals("OK", res22);
        assertEquals(Arrays.asList(0L, 0L, 0L), res23);
        assertEquals(Arrays.asList(1L, 1L, 1L), res24);
        assertEquals(Arrays.asList(2L, 2L, 2L), res25);
        assertEquals(Arrays.asList(3L, 3L, 3L), res26);
        assertEquals(Arrays.asList(4L, 4L, 4L), res27);
        assertEquals(2, res28.size());

        assertTrue(res28.containsKey("rg:2"));
        TSMGetElement res28rg2 = res28.get("rg:2");
        assertEquals("rg:2", res28rg2.getKey());
        assertEquals(0, res28rg2.getLabels().size());
        assertEquals(4L, res28rg2.getElement().getTimestamp());
        assertEquals(1.78, res28rg2.getElement().getValue(), 0.001);

        assertTrue(res28.containsKey("rg:3"));
        TSMGetElement res28rg3 = res28.get("rg:3");
        assertEquals("rg:3", res28rg3.getKey());
        assertEquals(0, res28rg3.getLabels().size());
        assertEquals(4L, res28rg3.getElement().getTimestamp());
        assertEquals(0.74, res28rg3.getElement().getValue(), 0.001);
        
        assertEquals(2, res29.size());
        assertTrue(res29.containsKey("rg:2"));
        TSMGetElement res29rg2 = res29.get("rg:2");
        assertEquals("rg:2", res29rg2.getKey());
        assertEquals(1, res29rg2.getLabels().size());
        assertEquals("cm", res29rg2.getLabels().get("unit"));
        assertEquals(4L, res29rg2.getElement().getTimestamp());
        assertEquals(1.78, res29rg2.getElement().getValue(), 0.001);

        assertEquals(1, res30.size());
        assertTrue(res30.containsKey("rg:4"));
        TSMRangeElements res30rg4 = res30.get("rg:4");
        assertEquals("rg:4", res30rg4.getKey());
        assertEquals(2, res30rg4.getLabels().size());
        assertEquals("uk", res30rg4.getLabels().get("location"));
        assertEquals("mm", res30rg4.getLabels().get("unit"));
        assertEquals(3, res30rg4.getElements().size());
        assertEquals(0L, res30rg4.getElements().get(0).getTimestamp());
        assertEquals(25.0, res30rg4.getElements().get(0).getValue(), 0.001);
        assertEquals(1L, res30rg4.getElements().get(1).getTimestamp());
        assertEquals(18.0, res30rg4.getElements().get(1).getValue(), 0.001);
        assertEquals(2L, res30rg4.getElements().get(2).getTimestamp());
        assertEquals(21.0, res30rg4.getElements().get(2).getValue(), 0.001);

        assertEquals(2, res31.size());
        assertTrue(res31.containsKey("rg:2"));
        TSMRangeElements res31rg2 = res31.get("rg:2");
        assertEquals("rg:2", res31rg2.getKey());
        assertEquals(1, res31rg2.getLabels().size());
        assertEquals("us", res31rg2.getLabels().get("location"));
        assertEquals(3, res31rg2.getElements().size());
        assertEquals(3L, res31rg2.getElements().get(0).getTimestamp());
        assertEquals(1.9, res31rg2.getElements().get(0).getValue(), 0.001);
        assertEquals(2L, res31rg2.getElements().get(1).getTimestamp());
        assertEquals(2.3, res31rg2.getElements().get(1).getValue(), 0.001);
        assertEquals(1L, res31rg2.getElements().get(2).getTimestamp());
        assertEquals(2.1, res31rg2.getElements().get(2).getValue(), 0.001);
        
        // REMOVE_END

        // STEP_START agg
        List<TSElement> res32 = jedis.tsRange("rg:2",
            TSRangeParams.rangeParams()
                .fromTimestamp(0L)
                .toTimestamp(4L)
                .aggregation(AggregationType.AVG, 2)
        );
        System.out.println(res32);
        // >>> [(0:1.9500000000000002), (2:2.0999999999999996), (4:1.78)]
        // STEP_END
        // REMOVE_START
        assertEquals(
            Arrays.asList(
                new TSElement(0L, 1.9500000000000002),
                new TSElement(2L, 2.0999999999999996),
                new TSElement(4L, 1.78)
            ),
            res32
        );
        // REMOVE_END

        // STEP_START agg_bucket
        String res33 = jedis.tsCreate("sensor3");
        System.out.println(res33); // >>> OK

        List<Long> res34 = jedis.tsMAdd(
            new AbstractMap.SimpleEntry<>("sensor3", new TSElement(10L, 1000.0)),
            new AbstractMap.SimpleEntry<>("sensor3", new TSElement(20L, 2000.0)),
            new AbstractMap.SimpleEntry<>("sensor3", new TSElement(30L, 3000.0)),
            new AbstractMap.SimpleEntry<>("sensor3", new TSElement(40L, 4000.0)),
            new AbstractMap.SimpleEntry<>("sensor3", new TSElement(50L, 5000.0)),
            new AbstractMap.SimpleEntry<>("sensor3", new TSElement(60L, 6000.0)),
            new AbstractMap.SimpleEntry<>("sensor3", new TSElement(70L, 7000.0))
        );
        System.out.println(res34); // >>> [10, 20, 30, 40, 50, 60, 70]

        List<TSElement> res35 = jedis.tsRange("sensor3",
            TSRangeParams.rangeParams()
                .fromTimestamp(10L)
                .toTimestamp(70L)
                .aggregation(AggregationType.MIN, 25)
        );
        System.out.println(res35);
        // >>> [(0:1000.0), (25:3000.0), (50:5000.0)]
        // STEP_END
        // REMOVE_START
        assertEquals("OK", res33);
        assertEquals(Arrays.asList(10L, 20L, 30L, 40L, 50L, 60L, 70L), res34);
        assertEquals(
            Arrays.asList(
                new TSElement(0L, 1000.0),
                new TSElement(25L, 3000.0),
                new TSElement(50L, 5000.0)
            ),
            res35
        );
        // REMOVE_END

        // STEP_START agg_align
        List<TSElement> res36 = jedis.tsRange("sensor3",
            TSRangeParams.rangeParams()
                .fromTimestamp(10L)
                .toTimestamp(70L)
                .aggregation(AggregationType.MIN, 25)
                .alignStart()
        );
        System.out.println(res36);
        // >>> [(10:1000.0), (35:4000.0), (60:6000.0)]
        // STEP_END
        // REMOVE_START
        assertEquals(
            Arrays.asList(
                new TSElement(10L, 1000.0),
                new TSElement(35L, 4000.0),
                new TSElement(60L, 6000.0)
            ),
            res36
        );
        // REMOVE_END

        // STEP_START agg_multi
        Map<String, String> ukCountry = new HashMap<>();
        ukCountry.put("country", "uk");
        
        Map<String, String> usCountry = new HashMap<>();
        usCountry.put("country", "us");

        jedis.tsCreate("wind:1", TSCreateParams.createParams().labels(ukCountry));
        jedis.tsCreate("wind:2", TSCreateParams.createParams().labels(ukCountry));
        jedis.tsCreate("wind:3", TSCreateParams.createParams().labels(usCountry));
        jedis.tsCreate("wind:4", TSCreateParams.createParams().labels(usCountry));

        jedis.tsMAdd(
            new AbstractMap.SimpleEntry<>("wind:1", new TSElement(0L, 10.0)),
            new AbstractMap.SimpleEntry<>("wind:2", new TSElement(0L, 12.0)),
            new AbstractMap.SimpleEntry<>("wind:3", new TSElement(0L, 8.0)),
            new AbstractMap.SimpleEntry<>("wind:4", new TSElement(0L, 15.0))
        );

        jedis.tsMAdd(
            new AbstractMap.SimpleEntry<>("wind:1", new TSElement(1L, 11.0)),
            new AbstractMap.SimpleEntry<>("wind:2", new TSElement(1L, 13.0)),
            new AbstractMap.SimpleEntry<>("wind:3", new TSElement(1L, 9.0)),
            new AbstractMap.SimpleEntry<>("wind:4", new TSElement(1L, 16.0))
        );

        jedis.tsMAdd(
            new AbstractMap.SimpleEntry<>("wind:1", new TSElement(2L, 9.0)),
            new AbstractMap.SimpleEntry<>("wind:2", new TSElement(2L, 11.0)),
            new AbstractMap.SimpleEntry<>("wind:3", new TSElement(2L, 7.0)),
            new AbstractMap.SimpleEntry<>("wind:4", new TSElement(2L, 14.0))
        );

        jedis.tsMAdd(
            new AbstractMap.SimpleEntry<>("wind:1", new TSElement(3L, 12.0)),
            new AbstractMap.SimpleEntry<>("wind:2", new TSElement(3L, 14.0)),
            new AbstractMap.SimpleEntry<>("wind:3", new TSElement(3L, 10.0)),
            new AbstractMap.SimpleEntry<>("wind:4", new TSElement(3L, 17.0))
        );

        jedis.tsMAdd(
            new AbstractMap.SimpleEntry<>("wind:1", new TSElement(4L, 8.0)),
            new AbstractMap.SimpleEntry<>("wind:2", new TSElement(4L, 10.0)),
            new AbstractMap.SimpleEntry<>("wind:3", new TSElement(4L, 6.0)),
            new AbstractMap.SimpleEntry<>("wind:4", new TSElement(4L, 13.0))
        );

        // Group by country with max reduction
        Map<String, TSMRangeElements> res44 = jedis.tsMRange(
            TSMRangeParams.multiRangeParams(0L, 4L)
                .filter("country=(us,uk)")
                .groupBy("country", "max"));
        System.out.println(res44);
        // >>> {country=uk=TSMRangeElements{key=country=uk, labels={}, value=[(0:12.0)...

        // Group by country with avg reduction
        Map<String, TSMRangeElements> res45 = jedis.tsMRange(
            TSMRangeParams.multiRangeParams(0L, 4L)
                .filter("country=(us,uk)")
                .groupBy("country", "avg"));
        System.out.println(res45);
        // >>> {country=uk=TSMRangeElements{key=country=uk, labels={}, value=[(0:11.0)...
        // STEP_END
        // REMOVE_START
        assertEquals(2, res44.size());
        assertTrue(res44.containsKey("country=uk"));
        TSMRangeElements res44uk = res44.get("country=uk");
        assertEquals("country=uk", res44uk.getKey());
        assertEquals(0, res44uk.getLabels().size());
        assertEquals(5, res44uk.getElements().size());
        assertEquals(0L, res44uk.getElements().get(0).getTimestamp());
        assertEquals(12.0, res44uk.getElements().get(0).getValue(), 0.001);
        assertEquals(1L, res44uk.getElements().get(1).getTimestamp());
        assertEquals(13.0, res44uk.getElements().get(1).getValue(), 0.001);
        assertEquals(2L, res44uk.getElements().get(2).getTimestamp());
        assertEquals(11.0, res44uk.getElements().get(2).getValue(), 0.001);
        assertEquals(3L, res44uk.getElements().get(3).getTimestamp());
        assertEquals(14.0, res44uk.getElements().get(3).getValue(), 0.001);
        assertEquals(4L, res44uk.getElements().get(4).getTimestamp());
        assertEquals(10.0, res44uk.getElements().get(4).getValue(), 0.001);

        assertTrue(res44.containsKey("country=us"));
        TSMRangeElements res44us = res44.get("country=us");
        assertEquals("country=us", res44us.getKey());
        assertEquals(0, res44us.getLabels().size());
        assertEquals(5, res44us.getElements().size());
        assertEquals(0L, res44us.getElements().get(0).getTimestamp());
        assertEquals(15.0, res44us.getElements().get(0).getValue(), 0.001);
        assertEquals(1L, res44us.getElements().get(1).getTimestamp());
        assertEquals(16.0, res44us.getElements().get(1).getValue(), 0.001);
        assertEquals(2L, res44us.getElements().get(2).getTimestamp());
        assertEquals(14.0, res44us.getElements().get(2).getValue(), 0.001);
        assertEquals(3L, res44us.getElements().get(3).getTimestamp());
        assertEquals(17.0, res44us.getElements().get(3).getValue(), 0.001);
        assertEquals(4L, res44us.getElements().get(4).getTimestamp());
        assertEquals(13.0, res44us.getElements().get(4).getValue(), 0.001);

        assertEquals(2, res45.size());
        assertTrue(res45.containsKey("country=uk"));
        TSMRangeElements res45uk = res45.get("country=uk");
        assertEquals("country=uk", res45uk.getKey());
        assertEquals(0, res45uk.getLabels().size());
        assertEquals(5, res45uk.getElements().size());
        assertEquals(0L, res45uk.getElements().get(0).getTimestamp());
        assertEquals(11.0, res45uk.getElements().get(0).getValue(), 0.001);
        assertEquals(1L, res45uk.getElements().get(1).getTimestamp());
        assertEquals(12.0, res45uk.getElements().get(1).getValue(), 0.001);
        assertEquals(2L, res45uk.getElements().get(2).getTimestamp());
        assertEquals(10.0, res45uk.getElements().get(2).getValue(), 0.001);
        assertEquals(3L, res45uk.getElements().get(3).getTimestamp());
        assertEquals(13.0, res45uk.getElements().get(3).getValue(), 0.001);
        assertEquals(4L, res45uk.getElements().get(4).getTimestamp());
        assertEquals(9.0, res45uk.getElements().get(4).getValue(), 0.001);

        assertTrue(res45.containsKey("country=us"));
        TSMRangeElements res45us = res45.get("country=us");
        assertEquals("country=us", res45us.getKey());
        assertEquals(0, res45us.getLabels().size());
        assertEquals(5, res45us.getElements().size());
        assertEquals(0L, res45us.getElements().get(0).getTimestamp());
        assertEquals(11.5, res45us.getElements().get(0).getValue(), 0.001);
        assertEquals(1L, res45us.getElements().get(1).getTimestamp());
        assertEquals(12.5, res45us.getElements().get(1).getValue(), 0.001);
        assertEquals(2L, res45us.getElements().get(2).getTimestamp());
        assertEquals(10.5, res45us.getElements().get(2).getValue(), 0.001);
        assertEquals(3L, res45us.getElements().get(3).getTimestamp());
        assertEquals(13.5, res45us.getElements().get(3).getValue(), 0.001);
        assertEquals(4L, res45us.getElements().get(4).getTimestamp());
        assertEquals(9.5, res45us.getElements().get(4).getValue(), 0.001);
        // REMOVE_END

        // STEP_START create_compaction
        String res46 = jedis.tsCreate("hyg:1");
        System.out.println(res46); // >>> OK

        String res47 = jedis.tsCreate("hyg:compacted");
        System.out.println(res47); // >>> OK

        String res48 = jedis.tsCreateRule("hyg:1", "hyg:compacted", AggregationType.MIN, 3);
        System.out.println(res48); // >>> OK

        TSInfo res49 = jedis.tsInfo("hyg:1");
        System.out.println("Rules: " + res49.getProperty("rules"));
        // >>> Rules: [{compactionKey=hyg:compacted, bucketDuration=3, aggregationType=MIN, alignmentTimestamp=0}]

        TSInfo res50 = jedis.tsInfo("hyg:compacted");
        System.out.println("Source key: " + res50.getProperty("sourceKey"));
        // >>> Source key: hyg:1
        // STEP_END
        // REMOVE_START
        assertEquals("OK", res46);
        assertEquals("OK", res47);
        assertEquals("OK", res48);
        assertEquals("hyg:1", res50.getProperty("sourceKey"));
        // REMOVE_END

        // STEP_START comp_add
        List<Long> res51 = jedis.tsMAdd(
            new AbstractMap.SimpleEntry<>("hyg:1", new TSElement(0L, 75.0)),
            new AbstractMap.SimpleEntry<>("hyg:1", new TSElement(1L, 77.0)),
            new AbstractMap.SimpleEntry<>("hyg:1", new TSElement(2L, 78.0))
        );
        System.out.println(res51); // >>> [0, 1, 2]

        List<TSElement> res52 = jedis.tsRange("hyg:compacted", 0L, 10L);
        System.out.println(res52); // >>> []

        long res53 = jedis.tsAdd("hyg:1", 3L, 79.0);
        System.out.println(res53); // >>> 3

        List<TSElement> res54 = jedis.tsRange("hyg:compacted", 0L, 10L);
        System.out.println(res54); // >>> [(0:75.0)]
        // STEP_END
        // REMOVE_START
        assertEquals(Arrays.asList(0L, 1L, 2L), res51);
        assertEquals(Arrays.asList(), res52);
        assertEquals(3L, res53);
        assertEquals(Arrays.asList(new TSElement(0L, 75.0)), res54);
        // REMOVE_END

        // STEP_START del
        TSInfo res55 = jedis.tsInfo("thermometer:1");
        System.out.println(res55.getProperty("totalSamples")); // >>> 2
        System.out.println(res55.getProperty("firstTimestamp")); // >>> 1
        System.out.println(res55.getProperty("lastTimestamp")); // >>> 2

        long res56 = jedis.tsAdd("thermometer:1", 3L, 9.7);
        System.out.println(res56); // >>> 3

        TSInfo res57 = jedis.tsInfo("thermometer:1");
        System.out.println(res57.getProperty("totalSamples")); // >>> 3

        long res58 = jedis.tsDel("thermometer:1", 1L, 2L);
        System.out.println(res58); // >>> 2

        TSInfo res59 = jedis.tsInfo("thermometer:1");
        System.out.println(res59.getProperty("totalSamples")); // >>> 1

        long res60 = jedis.tsDel("thermometer:1", 3L, 3L);
        System.out.println(res60); // >>> 1

        TSInfo res61 = jedis.tsInfo("thermometer:1");
        System.out.println(res61.getProperty("totalSamples")); // >>> 0
        // STEP_END
        // REMOVE_START
        assertEquals((Long) 2L, res55.getProperty("totalSamples"));
        assertEquals((Long) 1L, res55.getProperty("firstTimestamp"));
        assertEquals((Long) 2L, res55.getProperty("lastTimestamp"));
        assertEquals(3L, res56);
        assertEquals((Long) 3L, res57.getProperty("totalSamples"));
        assertEquals(2L, res58);
        assertEquals((Long) 1L, res59.getProperty("totalSamples"));
        assertEquals(1L, res60);
        assertEquals((Long) 0L, res61.getProperty("totalSamples"));
        // REMOVE_END

        //HIDE_START
        jedis.close();
        //HIDE_END
    }
}
