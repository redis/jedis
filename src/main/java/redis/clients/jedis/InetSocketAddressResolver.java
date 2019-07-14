package redis.clients.jedis;

import java.net.InetSocketAddress;

public interface InetSocketAddressResolver {
  
  InetSocketAddress resolve();
}
