package redis.clients.jedis.commands;

import java.util.List;

public interface ControlBinaryCommands extends ACLBinaryCommands, ClientBinaryCommands {

  List<Object> roleBinary();

  Long objectRefcount(byte[] key);

  byte[] objectEncoding(byte[] key);

  Long objectIdletime(byte[] key);

  List<byte[]> objectHelpBinary();

  Long objectFreq(byte[] key);

  byte[] memoryDoctorBinary();

  Long memoryUsage(byte[] key);

  Long memoryUsage(byte[] key, int samples);

}
