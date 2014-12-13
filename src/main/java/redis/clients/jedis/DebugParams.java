package redis.clients.jedis;

public class DebugParams {
  private String[] command;

  public String[] getCommand() {
    return command;
  }

  private DebugParams() {

  }

  public static DebugParams SEGFAULT() {
    DebugParams debugParams = new DebugParams();
    debugParams.command = new String[] { "SEGFAULT" };
    return debugParams;
  }

  public static DebugParams OBJECT(String key) {
    DebugParams debugParams = new DebugParams();
    debugParams.command = new String[] { "OBJECT", key };
    return debugParams;
  }

  public static DebugParams RELOAD() {
    DebugParams debugParams = new DebugParams();
    debugParams.command = new String[] { "RELOAD" };
    return debugParams;
  }
}
