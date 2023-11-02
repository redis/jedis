package redis.clients.jedis;

import org.junit.Test;
import static org.junit.Assert.*;
public class ClusterCommandObjectsTest {

    @Test
    public void dbSizeUnsupportedTest() {
        ClusterCommandObjects commandObjects = new ClusterCommandObjects();
        try {
            commandObjects.dbSize();
            fail("Expected Exception: UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertEquals("Not supported in cluster mode.", e.getMessage());
        }
    }

    @Test
    public void keysPatternUnsupportedTest() {
        ClusterCommandObjects commandObjects = new ClusterCommandObjects();
        try {
            commandObjects.keys("pattern");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Cluster mode only supports KEYS command with pattern containing hash-tag ( curly-brackets enclosed string )", e.getMessage());
        }
    }

    @Test
    public void scanPatternUnsupportedTest() {
        ClusterCommandObjects commandObjects = new ClusterCommandObjects();
        try {
            commandObjects.scan("0");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Cluster mode only supports SCAN command with MATCH pattern containing hash-tag ( curly-brackets enclosed string )", e.getMessage());
        }
    }

    @Test
    public void waitReplicasUnsupportedTest() {
        ClusterCommandObjects commandObjects = new ClusterCommandObjects();
        try {
            commandObjects.waitReplicas(1, 1000);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertEquals("Not supported in cluster mode.", e.getMessage());
        }
    }

    @Test
    public void waitAOFUnsupportedTest() {
        ClusterCommandObjects commandObjects = new ClusterCommandObjects();
        try {
            commandObjects.waitAOF(1, 1, 1000);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertEquals("Not supported in cluster mode.", e.getMessage());
        }
    }

}
