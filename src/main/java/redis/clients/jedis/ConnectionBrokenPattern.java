package redis.clients.jedis;

public interface ConnectionBrokenPattern {
  boolean determine(final RuntimeException throwable);
}
