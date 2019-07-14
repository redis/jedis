package redis.clients.jedis.params;

public class MigrateParams extends Params {

  private static final String COPY = "COPY";
  private static final String REPLACE = "REPLACE";
  private static final String AUTH = "AUTH";

  public MigrateParams() {
  }

  public static MigrateParams migrateParams() {
    return new MigrateParams();
  }

  public MigrateParams copy() {
    addParam(COPY);
    return this;
  }

  public MigrateParams replace() {
    addParam(REPLACE);
    return this;
  }
  
  public MigrateParams auth(String password) {
    addParam(AUTH, password);
    return this;
  }
}
