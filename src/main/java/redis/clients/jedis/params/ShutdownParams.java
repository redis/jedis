package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.SaveMode;

public class ShutdownParams implements IParams {

  private SaveMode saveMode;
  private boolean now;
  private boolean force;

  public static ShutdownParams shutdownParams() {
    return new ShutdownParams();
  }

  public ShutdownParams saveMode(SaveMode saveMode) {
    this.saveMode = saveMode;
    return this;
  }

  public ShutdownParams nosave() {
    return this.saveMode(SaveMode.NOSAVE);
  }

  public ShutdownParams save() {
    return this.saveMode(SaveMode.SAVE);
  }

  public ShutdownParams now() {
    this.now = true;
    return this;
  }

  public ShutdownParams force() {
    this.force = true;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (this.saveMode != null) {
      args.add(saveMode);
    }
    if (this.now) {
      args.add(Keyword.NOW);
    }
    if (this.force) {
      args.add(Keyword.FORCE);
    }
  }
}
