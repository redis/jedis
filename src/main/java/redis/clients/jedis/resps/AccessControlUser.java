package redis.clients.jedis.resps;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

// TODO: remove
public class AccessControlUser {

  private final Map<String, Object> userInfo;
  private final List<String> flags;
  private final List<String> passwords;
  private final String commands;
  private final List<String> keysList;
  private final String keys;
  private final List<String> channelsList;
  private final String channels;
  private final List<String> selectors;

  public AccessControlUser(Map<String, Object> map) {
    this.userInfo = map;

    this.flags = (List<String>) map.get("flags");

    this.passwords = (List<String>) map.get("passwords");

    this.commands = (String) map.get("commands");

    Object localKeys = map.get("keys");
    if (localKeys == null) {
      this.keys = null;
      this.keysList = null;
    } else if (localKeys instanceof List) {
      this.keysList = (List<String>) localKeys;
      this.keys = joinStrings(this.keysList);
    } else {
      this.keys = (String) localKeys;
      this.keysList = Arrays.asList(this.keys.split(" "));
    }

    Object localChannels = map.get("channels");
    if (localChannels == null) {
      this.channels = null;
      this.channelsList = null;
    } else if (localChannels instanceof List) {
      this.channelsList = (List<String>) localChannels;
      this.channels = joinStrings(this.channelsList);
    } else {
      this.channels = (String) localChannels;
      this.channelsList = Arrays.asList(this.channels.split(" "));
    }

    this.selectors = (List<String>) map.get("selectors");
  }

  private static String joinStrings(List<String> list) {
    StringJoiner joiner = new StringJoiner(" ");
    list.forEach(s -> joiner.add(s));
    return joiner.toString();
  }

  public List<String> getFlags() {
    return flags;
  }

  /**
   * @deprecated Use {@link AccessControlUser#getPasswords()}.
   */
  @Deprecated
  public List<String> getPassword() {
    return passwords;
  }

  public List<String> getPasswords() {
    return passwords;
  }

  public String getCommands() {
    return commands;
  }

  /**
   * @return Generic map containing all key-value pairs returned by the server
   */
  public Map<String, Object> getUserInfo() {
    return userInfo;
  }

  public String getKeys() {
    return keys;
  }

  public List<String> getKeysList() {
    return keysList;
  }

  public List<String> getChannelsList() {
    return channelsList;
  }

  public String getChannels() {
    return channels;
  }

  public List<String> getSelectors() {
    return selectors;
  }

  @Override
  public String toString() {
    return "AccessControlUser{" + "flags=" + flags + ", passwords=" + passwords
        + ", commands='" + commands + "', keys='" + keys + "', channels='" + channels
        + "', selectors=" + selectors + "}";
  }
}
