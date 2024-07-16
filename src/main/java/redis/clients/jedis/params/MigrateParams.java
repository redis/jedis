package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MigrateParams that = (MigrateParams) o;
    return copy == that.copy && replace == that.replace && Objects.equals(username, that.username) && Objects.equals(password, that.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(copy, replace, username, password);
  }
}
