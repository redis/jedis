package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.args.SaveMode;
import redis.clients.jedis.args.ShutdownMode;
import redis.clients.jedis.util.SafeEncoder;

import static redis.clients.jedis.args.ShutdownMode.NOW;
import static redis.clients.jedis.args.ShutdownMode.FORCE;
import static redis.clients.jedis.args.ShutdownMode.ABORT;

public class ShutdownParams implements IParams {

  private SaveMode saveMode;
  private ShutdownMode shutdownMode;

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
    return shutdownMode(NOW);
  }

  public ShutdownParams force() {
    return shutdownMode(FORCE);
  }
  
  public ShutdownParams abort(){
    return shutdownMode(ABORT);
  }
  
  public ShutdownParams shutdownMode(ShutdownMode shutdownMode){
    this.shutdownMode = shutdownMode;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {

    if (this.saveMode != null) {
      args.add(SafeEncoder.encode(saveMode.getRaw()));
    }

    if (this.shutdownMode != null) {
      args.add(this.shutdownMode);
    }
  }
}
