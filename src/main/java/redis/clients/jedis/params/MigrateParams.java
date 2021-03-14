package redis.clients.jedis.params;

import redis.clients.jedis.util.SafeEncoder;

import java.util.ArrayList;
import java.util.List;

public class MigrateParams extends Params {

  private static final String COPY = "COPY";
  private static final String REPLACE = "REPLACE";
  private static final String AUTH = "AUTH";
  private static final String AUTH2 = "AUTH2";

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

  public MigrateParams auth2(String username, String password) {
    addParam(AUTH2, new String[] { username, password });
    return this;
  }

  @Override
  public byte[][] getByteParams() {
    List<byte[]> byteParams = new ArrayList<>();

    if (contains(COPY)) {
      byteParams.add(SafeEncoder.encode(COPY));
    }
    if (contains(REPLACE)) {
      byteParams.add(SafeEncoder.encode(REPLACE));
    }
    if (contains(AUTH)) {
      byteParams.add(SafeEncoder.encode(AUTH));
      byteParams.add(SafeEncoder.encode((String) getParam(AUTH)));
    } else if (contains(AUTH2)) {
      byteParams.add(SafeEncoder.encode(AUTH2));
      String[] nameAndPass = (String[]) getParam(AUTH2);
      byteParams.add(SafeEncoder.encode(nameAndPass[0]));
      byteParams.add(SafeEncoder.encode(nameAndPass[1]));
    }

    return byteParams.toArray(new byte[byteParams.size()][]);
  }
}
