package redis.clients.jedis;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;

public class RedisScriptingRoutineBuilder {
  private final MessageDigest sha1Computer;
  public RedisScriptingRoutineBuilder() throws NoSuchAlgorithmException {
    sha1Computer = MessageDigest.getInstance("SHA-1");
  }

  public RedisScriptingRoutine build(String name, String routineBody) {
    byte[] scriptSha = sha1Computer.digest(routineBody.getBytes(StandardCharsets.UTF_8));
    String routineDigest = DatatypeConverter.printHexBinary(scriptSha);
    return new RedisScriptingRoutine(routineDigest, name, routineBody);
  }
}
