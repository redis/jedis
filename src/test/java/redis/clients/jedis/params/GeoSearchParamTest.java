package redis.clients.jedis.params;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.args.Rawable;

public class GeoSearchParamTest {

  @Test
  public void countWithAnyFalseDoesNotAddAny() {
    GeoSearchParam param = GeoSearchParam.geoSearchParam()
        .fromLonLat(15, 37).byRadius(200, GeoUnit.KM).count(10, false);
    assertFalse(argStrings(param).contains("ANY"),
        "COUNT 10 false must not append the ANY keyword");
  }

  @Test
  public void countWithAnyTrueAddsAny() {
    GeoSearchParam param = GeoSearchParam.geoSearchParam()
        .fromLonLat(15, 37).byRadius(200, GeoUnit.KM).count(10, true);
    assertTrue(argStrings(param).contains("ANY"),
        "COUNT 10 true must append the ANY keyword");
  }

  private static List<String> argStrings(GeoSearchParam param) {
    CommandArguments args = new CommandArguments(Command.GEOSEARCH);
    param.addParams(args);
    List<String> values = new ArrayList<>();
    for (Rawable raw : args) {
      values.add(raw.toString());
    }
    return values;
  }
}
