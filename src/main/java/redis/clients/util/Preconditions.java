package redis.clients.util;

public class Preconditions {

  public static void checkArgument(boolean expression, String message, Object... values) {
    if(!expression) {
      throw new IllegalArgumentException(getMessage(message, values));
    }
  }
  
  private static String getMessage(String message, Object... values) {
    if(values != null && values.length > 0) {
      return String.format(message, values);
    } else {
      return message;
    }
  }
  
}
