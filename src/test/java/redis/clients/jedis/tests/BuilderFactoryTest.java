package redis.clients.jedis.tests;

import org.junit.Assert;
import org.junit.Test;

import redis.clients.jedis.BuilderFactory;

public class BuilderFactoryTest extends Assert {
    @Test
    public void buildDouble() {
        Double build = BuilderFactory.DOUBLE.build("1.0".getBytes());
        assertEquals(new Double(1.0), build);
    }

    @Test
    public void buildInfinities() {
        Double build = BuilderFactory.DOUBLE.build("-inf".getBytes());
        assertEquals(new Double(Double.NEGATIVE_INFINITY), build);
        build = BuilderFactory.DOUBLE.build("inf".getBytes());
        assertEquals(new Double(Double.POSITIVE_INFINITY), build);
        build = BuilderFactory.DOUBLE.build("+inf".getBytes());
        assertEquals(new Double(Double.POSITIVE_INFINITY), build);
    }
}