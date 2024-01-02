package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

public class MigrateParams implements IParams {

  private boolean copy = false;
  private boolean replace = false;
  private String username = null;
  private String password = null;

  public MigrateParams() {
  }

  public static MigrateParams migrateParams() {
    return new MigrateParams();
  }

  public MigrateParams copy() {
    this.copy = true;
    return this;
  }

  public MigrateParams replace() {
    this.replace = true;
    return this;
  }

  public MigrateParams auth(String password) {
    this.password = password;
    return this;
  }

  public MigrateParams auth2(String username, String password) {
    this.username = username;
    this.password = password;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (copy) {
      args.add(Keyword.COPY);
    }
    if (replace) {
      args.add(Keyword.REPLACE);
    }
    if (username != null) {
      args.add(Keyword.AUTH2).add(username).add(password);
    } else if (password != null) {
      args.add(Keyword.AUTH).add(password);
    }
  }
}
