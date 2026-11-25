package redis.clients.jedis.params;

import static org.hamcrest.MatcherAssert.assertThat;
import static redis.clients.jedis.util.CommandArgumentsMatchers.hasArguments;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.Protocol.Keyword;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.args.RawableFactory;

public class GeoSearchParamTest {

  @Nested
  class AddParamsTests {

    @Test
    public void countWithAnyFalseOmitsAny() {
      GeoSearchParam params = GeoSearchParam.geoSearchParam().fromMember("m")
          .byRadius(1, GeoUnit.KM).count(10, false);
      CommandArguments args = new CommandArguments(Command.GEOSEARCH);
      params.addParams(args);

      // Expected: GEOSEARCH FROMMEMBER m BYRADIUS 1.0 km COUNT 10
      assertThat(args,
        hasArguments(Command.GEOSEARCH, Keyword.FROMMEMBER, RawableFactory.from("m"),
          Keyword.BYRADIUS, RawableFactory.from(1d), GeoUnit.KM, Keyword.COUNT,
          RawableFactory.from(10)));
    }

    @Test
    public void countWithAnyTrueAddsAny() {
      GeoSearchParam params = GeoSearchParam.geoSearchParam().fromMember("m")
          .byRadius(1, GeoUnit.KM).count(10, true);
      CommandArguments args = new CommandArguments(Command.GEOSEARCH);
      params.addParams(args);

      // Expected: GEOSEARCH FROMMEMBER m BYRADIUS 1.0 km COUNT 10 ANY
      assertThat(args,
        hasArguments(Command.GEOSEARCH, Keyword.FROMMEMBER, RawableFactory.from("m"),
          Keyword.BYRADIUS, RawableFactory.from(1d), GeoUnit.KM, Keyword.COUNT,
          RawableFactory.from(10), Keyword.ANY));
    }
  }
}
