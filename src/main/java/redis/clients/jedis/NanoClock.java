package redis.clients.jedis;

import java.util.function.LongSupplier;

class NanoClock {
  public static LongSupplier INSTANCE = System::nanoTime;
}