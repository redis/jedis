package redis.clients.jedis;

import java.util.Map;

// TODO: 'resps' package
// TODO: remove
public class Module {

  private final Map<String, Object> info;
  private final String name;
  private final int version;

  @Deprecated
  public Module(String name, int version) {
    this.name = name;
    this.version = version;
    this.info = null;
  }

  public Module(Map<String, Object> map) {
    this.info = map;
    this.name = (String) map.get("name");
    this.version = ((Long) map.get("version")).intValue();
  }

  public String getName() {
    return name;
  }

  public int getVersion() {
    return version;
  }

  /**
   * @return Generic map containing all key-value pairs returned by the server
   */
  public Map<String, Object> getInfo() {
    return info;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) return false;
    if (o == this) return true;
    if (!(o instanceof Module)) return false;

    Module module = (Module) o;

    if (version != module.version) return false;
    return !(name != null ? !name.equals(module.name) : module.name != null);

  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + version;
    return result;
  }

}
