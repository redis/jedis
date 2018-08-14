package redis.clients.jedis;

import java.util.List;

public interface AdvancedBinaryJedisCommands {

  List<byte[]> configGet(byte[] pattern);

  byte[] configSet(byte[] parameter, byte[] value);

  String slowlogReset();

  Long slowlogLen();

  List<byte[]> slowlogGetBinary();

  List<byte[]> slowlogGetBinary(long entries);

  Long objectRefcount(byte[] key);

  byte[] objectEncoding(byte[] key);

  Long objectIdletime(byte[] key);

  String migrate(String host, int port, byte[] key, int destinationDB, int timeout);

  String clientKill(byte[] ipPort);

  String clientKill(String ip, int port);

  byte[] clientGetnameBinary();

  byte[] clientListBinary();

  String clientSetname(byte[] name);
}
