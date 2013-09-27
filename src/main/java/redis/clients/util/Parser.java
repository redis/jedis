package redis.clients.util;

public class Parser {
    /**
     * Returns a double parsed from a redis string representation of a double. Handles parsing +inf and -inf as
     * POSITIVE_INFINITY and NEGATIVE_INFINITY respectively, which Double.valueOf doesn't handle.
     *
     * @throws NumberFormatException if the string isn't parsable as a double
     */
    public static Double parseRedisDouble(String redisDouble) {
        if (redisDouble.equals("+inf") || redisDouble.equals("inf")) {
            return Double.POSITIVE_INFINITY;
        } else if (redisDouble.equals("-inf")) {
            return Double.NEGATIVE_INFINITY;
        } else {
            return Double.valueOf(redisDouble);
        }
    }
}
