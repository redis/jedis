package redis.clients.jedis;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseDeprecationTest {

  @Test
  void setDependencyIsDeprecated() throws NoSuchMethodException {
    Method method = Response.class.getMethod("setDependency", Response.class);

    assertTrue(method.isAnnotationPresent(Deprecated.class));
  }

  @Test
  void exceptionFieldIsDeprecated() throws NoSuchFieldException {
    Field field = Response.class.getDeclaredField("exception");

    assertTrue(field.isAnnotationPresent(Deprecated.class));
  }
}
