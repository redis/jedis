package redis.clients.jedis.modules.bloom;

import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TDIGESTTest extends RedisModuleCommandsTestBase {

    @BeforeClass
    public static void prepare() {
        RedisModuleCommandsTestBase.prepare();
    }

    @Test
    public void create() {
        String res = client.tdigestCreate("alice", 100);
        assertEquals("OK", res);

        res = client.tdigestCreate("alice", 100);
        assertEquals("OK", res);

        res = client.tdigestCreate("bob", 200);
        assertEquals("OK", res);

        res = client.tdigestCreate("alicebob");
        assertEquals("OK", res);
    }

    @Test
    public void info() {
        client.tdigestCreate("alice", 200);
        Map<String, Object> res = client.tdigestInfo("alice");
        assertNotNull(res);
        assertEquals(200L, res.get("Compression"));
        assertEquals(1210L, res.get("Capacity"));

        client.tdigestCreate("bob", 100);
        res = client.tdigestInfo("bob");
        assertNotNull(res);
        assertEquals(100L, res.get("Compression"));
        assertEquals(610L, res.get("Capacity"));

        client.tdigestCreate("fitz", 1000);
        res = client.tdigestInfo("fitz");
        assertNotNull(res);
        assertEquals(1000L, res.get("Compression"));
        assertEquals(6010L, res.get("Capacity"));
    }

    @Test
    public void reset() {
        client.tdigestCreate("alice", 200);

        HashMap<Double, Double> map = new HashMap<>();
        map.put(42.0, 1.0);
        map.put(194.0, 0.3);
        client.tdigestAdd("alice", map);

        String res = client.tdigestReset("alice");
        assertEquals("OK", res);
    }

    @Test
    public void add() {
        client.tdigestCreate("alice", 200);
        client.tdigestCreate("bob", 100);

        HashMap<Double, Double> map = new HashMap<>();
        map.put(42.0, 1.0);
        map.put(194.0, 0.3);
        String res = client.tdigestAdd("alice", map);
        assertEquals("OK", res);

        map = new HashMap<>();
        map.put(4.0, 10.0);
        res = client.tdigestAdd("bob", map);
        assertEquals("OK", res);
    }

    @Test
    public void max() {
        client.tdigestCreate("alice", 200);

        HashMap<Double, Double> map = new HashMap<>();
        map.put(42.0, 1.0);
        map.put(194.0, 0.3);
        client.tdigestAdd("alice", map);

        String res = client.tdigestMax("alice");
        assertEquals("194", res);
    }

    @Test
    public void min() {
        client.tdigestCreate("alice", 200);

        HashMap<Double, Double> map = new HashMap<>();
        map.put(42.0, 1.0);
        map.put(194.0, 0.3);
        client.tdigestAdd("alice", map);

        String res = client.tdigestMin("alice");
        assertEquals("42", res);
    }

    @Test
    public void merge() {
        client.tdigestCreate("alice", 200);

        HashMap<Double, Double> map = new HashMap<>();
        map.put(42.0, 1.0);
        map.put(194.0, 0.3);
        client.tdigestAdd("alice", map);

        client.tdigestCreate("bob", 100);

        map = new HashMap<>();
        map.put(4.0, 0.1);
        map.put(14.0, 0.3);
        client.tdigestAdd("bob", map);

        String res = client.tdigestMerge("bob", "alice");

        assertEquals("OK", res);
    }

    @Test
    public void cdf() {
        client.tdigestCreate("alice", 300);

        HashMap<Double, Double> map = new HashMap<>();
        map.put(42.0, 1.0);
        map.put(194.0, 0.3);
        client.tdigestAdd("alice", map);

        String res = client.tdigestCdf("alice", 43);
        assertEquals("0.76998987854251011", res);
    }

    @Test
    public void quantile() {
        client.tdigestCreate("alice", 200);

        HashMap<Double, Double> map = new HashMap<>();
        map.put(42.0, 1.0);
        map.put(194.0, 0.3);
        client.tdigestAdd("alice", map);

        Map<String, String> res = client.tdigestQuantile("alice", 1, 0.9);
        assertEquals("194", res.get("0.90000000000000002"));
        assertEquals("194", res.get("1"));

        res = client.tdigestQuantile("alice", 1, 0.9, 0);
        assertEquals("42", res.get("0"));
        assertEquals("194", res.get("0.90000000000000002"));
        assertEquals("194", res.get("1"));

        client.tdigestQuantile("alice", 0);
        assertEquals("42", res.get("0"));
    }

    @Test
    public void trimmedMean() {
        client.tdigestCreate("alice", 200);

        HashMap<Double, Double> map = new HashMap<>();
        map.put(42.0, 1.0);
        map.put(194.0, 0.3);
        client.tdigestAdd("alice", map);

        String res = client.tdigestTrimmedMean("alice", 0.5, 1);
        assertEquals("77.076923076923066", res);

        res = client.tdigestTrimmedMean("alice", 0.9, 1);
        assertEquals("194", res);

        res = client.tdigestTrimmedMean("alice", 0.1, 0.2);
        assertEquals("42", res);
    }

    @Test
    public void mergeStore() {
        client.tdigestCreate("alice", 200);

        HashMap<Double, Double> map = new HashMap<>();
        map.put(42.0, 1.0);
        map.put(194.0, 0.3);
        client.tdigestAdd("alice", map);

        client.tdigestCreate("bob", 100);

        map = new HashMap<>();
        map.put(4.0, 0.1);
        map.put(14.0, 0.3);
        client.tdigestAdd("bob", map);

        client.tdigestCreate("alicebob1", 100);
        client.tdigestCreate("alicebob2");

        List<String> from = new ArrayList<>();
        from.add("alice");
        from.add("bob");

        String res = client.tdigestMergeStore("alicebob", from);
        assertEquals("OK", res);

        res = client.tdigestMergeStore("alicebob", from, 200);
        assertEquals("OK", res);
    }
}
