package redis.clients.jedis.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ReflectionTestUtils {

  public static <T> T getField(Object targetObject, String name) {

    Class<?>  targetClass = targetObject.getClass();

    Field field = findField(targetClass, name);

    makeAccessible(field);

    try {
      return (T) field.get(targetObject);
    } catch (IllegalAccessException ex) {
      throw new IllegalStateException(
          "Unexpected reflection exception - " + ex.getClass().getName() + ": " + ex.getMessage());
    }
  }

  public static Field findField(Class<?> clazz, String name) {
    Class<?> searchType = clazz;
    while (Object.class != searchType && searchType != null) {
      Field[] fields = searchType.getDeclaredFields();
      for (Field field : fields) {
        if (name.equals(field.getName())) {
          return field;
        }
      }
      searchType = searchType.getSuperclass();
    }
    return null;
  }

  private static void makeAccessible(Field field) {
    if ((!Modifier.isPublic(field.getModifiers()) || !Modifier.isPublic(field.getDeclaringClass().getModifiers())
        || Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
      field.setAccessible(true);
    }
  }
}
