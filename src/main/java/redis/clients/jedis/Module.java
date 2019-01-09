package redis.clients.jedis;

public class Module {
  private String name;
  private int version;

  public Module(String name, int version) {
    this.name = name;
    this.version = version;
  }


  public String getName() {
    return name;
  }

  public int getVersion() {
    return version;
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
