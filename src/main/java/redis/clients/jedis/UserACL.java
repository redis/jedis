package redis.clients.jedis;

import java.util.ArrayList;
import java.util.List;

public class UserACL {

  private List<String> flags = new ArrayList<String>();
  private List<String> keys = new ArrayList<String>();
  private List<String> passwords = new ArrayList<String>();
  private String commands;

  public UserACL() {
  }

  public void addFlag(String flag) {
    flags.add(flag);
  }

  public List<String> getFlags() {
    return flags;
  }

  public void addKey(String key) {
    keys.add(key);
  }

  public List<String> getKeys() {
    return keys;
  }

  public void addPassword(String password) {
    passwords.add(password);
  }

  public List<String> getPassword() {
    return passwords;
  }

  public String getCommands() {
    return commands;
  }

  public void setCommands(String commands) {
    this.commands = commands;
  }

  @Override
  public String toString() {
    return "UserACL{" + "flags=" + flags + ", keys=" + keys + ", passwords=" + passwords
        + ", commands='" + commands + '\'' + '}';
  }
}
