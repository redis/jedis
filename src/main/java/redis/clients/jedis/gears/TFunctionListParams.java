package redis.clients.jedis.gears;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.gears.RedisGearsProtocol.GearsKeyword;
import redis.clients.jedis.params.IParams;

import java.util.Collections;

@Deprecated
public class TFunctionListParams implements IParams {
  private boolean withCode = false;
  private int verbose;
  private String libraryName;

  public static TFunctionListParams listParams() {
    return new TFunctionListParams();
  }

  @Override
  public void addParams(CommandArguments args) {
    if (withCode) {
      args.add(GearsKeyword.WITHCODE);
    }

    if (verbose > 0 && verbose < 4) {
      args.add(String.join("", Collections.nCopies(verbose, "v")));
    } else if (verbose != 0) { // verbose == 0 is the default, so we don't need to throw an error
      throw new IllegalArgumentException("verbose must be between 1 and 3");
    }

    if (libraryName != null) {
      args.add(GearsKeyword.LIBRARY).add(libraryName);
    }
  }

  public TFunctionListParams withCode() {
    this.withCode = true;
    return this;
  }

  public TFunctionListParams verbose(int verbose) {
    this.verbose = verbose;
    return this;
  }

  public TFunctionListParams library(String libraryName) {
    this.libraryName = libraryName;
    return this;
  }
}
