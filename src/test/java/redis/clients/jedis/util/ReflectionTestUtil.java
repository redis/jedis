package redis.clients.jedis.util;

import java.lang.reflect.Field;

/**
 * Simple utility for accessing private fields in tests using reflection.
 * <p>
 * This utility is intended for testing purposes only to access internal state that is not exposed
 * through public APIs.
 * </p>
 */
public class ReflectionTestUtil {

  /**
   * Gets the value of a private field from an object.
   * @param target the object containing the field
   * @param fieldName the name of the field to access
   * @param <T> the expected type of the field value
   * @return the value of the field
   * @throws RuntimeException if the field cannot be accessed
   */
  @SuppressWarnings("unchecked")
  public static <T> T getField(Object target, String fieldName) {
    if (target == null) {
      throw new IllegalArgumentException("Target object cannot be null");
    }
    if (fieldName == null || fieldName.isEmpty()) {
      throw new IllegalArgumentException("Field name cannot be null or empty");
    }

    try {
      Field field = findField(target.getClass(), fieldName);
      field.setAccessible(true);
      return (T) field.get(target);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(
          "Field '" + fieldName + "' not found in class " + target.getClass().getName(), e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(
          "Cannot access field '" + fieldName + "' in class " + target.getClass().getName(), e);
    }
  }

  /**
   * Sets the value of a private field in an object.
   * @param target the object containing the field
   * @param fieldName the name of the field to set
   * @param value the value to set
   * @throws RuntimeException if the field cannot be accessed
   */
  public static void setField(Object target, String fieldName, Object value) {
    if (target == null) {
      throw new IllegalArgumentException("Target object cannot be null");
    }
    if (fieldName == null || fieldName.isEmpty()) {
      throw new IllegalArgumentException("Field name cannot be null or empty");
    }

    try {
      Field field = findField(target.getClass(), fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(
          "Field '" + fieldName + "' not found in class " + target.getClass().getName(), e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(
          "Cannot access field '" + fieldName + "' in class " + target.getClass().getName(), e);
    }
  }

  /**
   * Finds a field in the class hierarchy.
   * @param clazz the class to search
   * @param fieldName the name of the field
   * @return the field
   * @throws NoSuchFieldException if the field is not found
   */
  private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    Class<?> current = clazz;
    while (current != null) {
      try {
        return current.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    throw new NoSuchFieldException(
        "Field '" + fieldName + "' not found in class hierarchy of " + clazz.getName());
  }
}
