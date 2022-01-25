package redis.clients.jedis.resps;

import java.util.ArrayList;
import java.util.List;

public class AccessControlUser {

  private final List<String> flags = new ArrayList<>();
  private final List<String> keysList = new ArrayList<>();
  private String keysString;
  private final List<String> passwords = new ArrayList<>();
  private final List<String> channelsList = new ArrayList<>();
  private String channelsString;
  private String commands;

  public AccessControlUser() {
  }

  public void addFlag(String flag) {
    flags.add(flag);
  }

  public List<String> getFlags() {
    return flags;
  }

  public void addKey(String key) {
    keysList.add(key);
  }

  /**
   * @deprecated Use {@link AccessControlUser#getKeysList()}.
   */
  @Deprecated
  public List<String> getKeys() {
    return keysList;
  }

  /**
   * For Redis version below 7.
   */
  public List<String> getKeysList() {
    return keysList;
  }

  /**
   * For Redis version from 7.
   */
  public String getKeysString() {
    return keysString;
  }

  public void setKeys(String keys) {
    this.keysString = keys;
  }

  public void addPassword(String password) {
    passwords.add(password);
  }

  public List<String> getPassword() {
    return passwords;
  }

  public void addChannel(String channel) {
    channelsList.add(channel);
  }

  /**
   * @deprecated Use {@link AccessControlUser#getChannelsList()}.
   */
  @Deprecated
  public List<String> getChannels() {
    return channelsList;
  }

  /**
   * For Redis version below 7.
   */
  public List<String> getChannelsList() {
    return channelsList;
  }

  /**
   * For Redis version from 7.
   */
  public String getChannelsString() {
    return channelsString;
  }

  public void setChannels(String channels) {
    this.channelsString = channels;
  }

  public String getCommands() {
    return commands;
  }

  public void setCommands(String commands) {
    this.commands = commands;
  }

  @Override
  public String toString() {
    return "AccessControlUser{" + "flags=" + flags + ", passwords=" + passwords + ", commands='" + commands 
        + "', keys='" + (keysString != null ? keysString : keysList)
        + "', channels='" + (channelsString != null ? channelsString : channelsList) + "'}";
  }
}
