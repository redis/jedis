package redis.clients.jedis.misc;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.ClientSetInfoConfig;
import redis.clients.jedis.DriverInfo;
import redis.clients.jedis.exceptions.JedisValidationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClientSetInfoConfigTest {

  @Test
  public void defaultConfig() {
    ClientSetInfoConfig config = ClientSetInfoConfig.DEFAULT;
    assertFalse(config.isDisabled());
    assertNull(config.getUpstreamDrivers());
    assertNotNull(config.getDriverInfo());
    assertEquals("jedis", config.getDriverInfo().getName());
    assertEquals("jedis", config.getDriverInfo().getFormattedName());
  }

  @Test
  public void disabledConfig() {
    ClientSetInfoConfig config = ClientSetInfoConfig.DISABLED;
    assertTrue(config.isDisabled());
    assertNull(config.getUpstreamDrivers());
  }

  @Test
  public void constructorWithNullDriverInfoThrows() {
    assertThrows(JedisValidationException.class, () -> new ClientSetInfoConfig((DriverInfo) null));
  }

  @Test
  public void withLibNameSuffixFormatsCorrectly() {
    ClientSetInfoConfig config = ClientSetInfoConfig.withLibNameSuffix("my-suffix");
    assertEquals("my-suffix", config.getUpstreamDrivers());
    assertEquals("jedis(my-suffix)", config.getDriverInfo().getFormattedName());
  }

  @Test
  public void withLibNameSuffixThenAddUpstreamDriverPrepends() {
    // Start with legacy suffix
    ClientSetInfoConfig config = ClientSetInfoConfig.withLibNameSuffix("my-suffix");
    assertEquals("jedis(my-suffix)", config.getDriverInfo().getFormattedName());

    // Add upstream driver - should prepend to the suffix
    DriverInfo driverInfo = DriverInfo.builder(config.getDriverInfo())
        .addUpstreamDriver("spring-data-redis", "3.2.0").build();
    config = new ClientSetInfoConfig(driverInfo);
    assertEquals("spring-data-redis_v3.2.0;my-suffix", config.getUpstreamDrivers());
    assertEquals("jedis(spring-data-redis_v3.2.0;my-suffix)",
      config.getDriverInfo().getFormattedName());
  }

  @Test
  public void defaultNameWithUpstreamDriversKeepsJedisName() {
    // When using default name, adding upstream drivers should keep "jedis" as the base name
    DriverInfo driverInfo = DriverInfo.builder().addUpstreamDriver("spring-data-redis", "3.2.0")
        .build();
    assertEquals("jedis", driverInfo.getName());
    assertEquals("jedis(spring-data-redis_v3.2.0)", driverInfo.getFormattedName());
  }

  @Test
  public void chainingDriverInfoFromExistingConfig() {
    // First library (e.g., spring-data-redis) creates its config
    DriverInfo firstDriverInfo = DriverInfo.builder()
        .addUpstreamDriver("spring-data-redis", "3.2.0").build();
    ClientSetInfoConfig firstConfig = new ClientSetInfoConfig(firstDriverInfo);
    assertEquals("jedis(spring-data-redis_v3.2.0)", firstConfig.getDriverInfo().getFormattedName());

    // Second library builds on top of the first config
    DriverInfo secondDriverInfo = DriverInfo.builder(firstConfig.getDriverInfo())
        .addUpstreamDriver("upstream-library", "1.0.0").build();
    ClientSetInfoConfig secondConfig = new ClientSetInfoConfig(secondDriverInfo);
    assertEquals("upstream-library_v1.0.0;spring-data-redis_v3.2.0",
      secondConfig.getUpstreamDrivers());
    assertEquals("jedis(upstream-library_v1.0.0;spring-data-redis_v3.2.0)",
      secondConfig.getDriverInfo().getFormattedName());
  }

  @Test
  public void withLibNameSuffixErrorForBraces() {
    Arrays.asList('(', ')', '[', ']', '{', '}')
        .forEach(brace -> assertThrows(JedisValidationException.class,
          () -> ClientSetInfoConfig.withLibNameSuffix("" + brace)));
  }

  @Test
  public void builderWithNullDriverInfoThrows() {
    assertThrows(JedisValidationException.class, () -> DriverInfo.builder(null));
  }

  @Test
  public void builderNameNullThrows() {
    assertThrows(JedisValidationException.class, () -> DriverInfo.builder().name(null));
  }

  @Test
  public void builderCustomName() {
    DriverInfo driverInfo = DriverInfo.builder().name("my-custom-client").build();
    assertEquals("my-custom-client", driverInfo.getName());
    assertEquals("my-custom-client", driverInfo.getFormattedName());
    assertNull(driverInfo.getUpstreamDrivers());
  }

  @Test
  public void builderCustomNameWithUpstreamDrivers() {
    DriverInfo driverInfo = DriverInfo.builder().name("my-custom-client")
        .addUpstreamDriver("spring-data-redis", "3.2.0").build();
    assertEquals("my-custom-client", driverInfo.getName());
    assertEquals("my-custom-client(spring-data-redis_v3.2.0)", driverInfo.getFormattedName());
    assertEquals("spring-data-redis_v3.2.0", driverInfo.getUpstreamDrivers());
  }

  @Test
  public void builderCopiesExistingDriverInfo() {
    DriverInfo original = DriverInfo.builder().name("custom-name")
        .addUpstreamDriver("driver1", "1.0.0").build();

    DriverInfo copied = DriverInfo.builder(original).addUpstreamDriver("driver2", "2.0.0").build();

    assertEquals("custom-name", copied.getName());
    assertEquals("driver2_v2.0.0;driver1_v1.0.0", copied.getUpstreamDrivers());
  }

  @Test
  public void addUpstreamDriverSingle() {
    DriverInfo driverInfo = DriverInfo.builder().addUpstreamDriver("spring-data-redis", "3.2.0")
        .build();
    ClientSetInfoConfig config = new ClientSetInfoConfig(driverInfo);
    assertEquals("spring-data-redis_v3.2.0", config.getUpstreamDrivers());
  }

  @Test
  public void addUpstreamDriverMultiple() {
    DriverInfo driverInfo = DriverInfo.builder().addUpstreamDriver("driver1", "1.0.0")
        .addUpstreamDriver("driver2", "2.0.0").addUpstreamDriver("driver3", "3.0.0").build();
    ClientSetInfoConfig config = new ClientSetInfoConfig(driverInfo);
    assertEquals("driver3_v3.0.0;driver2_v2.0.0;driver1_v1.0.0", config.getUpstreamDrivers());
  }

  @Test
  public void addUpstreamDriverPrepends() {
    DriverInfo driverInfo = DriverInfo.builder().addUpstreamDriver("first", "1.0.0").build();
    ClientSetInfoConfig config = new ClientSetInfoConfig(driverInfo);
    assertEquals("first_v1.0.0", config.getUpstreamDrivers());

    driverInfo = DriverInfo.builder(config.getDriverInfo()).addUpstreamDriver("second", "2.0.0")
        .build();
    config = new ClientSetInfoConfig(driverInfo);
    assertEquals("second_v2.0.0;first_v1.0.0", config.getUpstreamDrivers());

    driverInfo = DriverInfo.builder(config.getDriverInfo()).addUpstreamDriver("third", "3.0.0")
        .build();
    config = new ClientSetInfoConfig(driverInfo);
    assertEquals("third_v3.0.0;second_v2.0.0;first_v1.0.0", config.getUpstreamDrivers());
  }

  @Test
  public void formattedNameWithNoUpstreamDrivers() {
    DriverInfo driverInfo = DriverInfo.builder().build();
    assertEquals("jedis", driverInfo.getFormattedName());
  }

  @Test
  public void formattedNameWithSingleUpstreamDriver() {
    DriverInfo driverInfo = DriverInfo.builder().addUpstreamDriver("spring-data-redis", "3.2.0")
        .build();
    assertEquals("jedis(spring-data-redis_v3.2.0)", driverInfo.getFormattedName());
  }

  @Test
  public void formattedNameWithMultipleUpstreamDrivers() {
    DriverInfo driverInfo = DriverInfo.builder().addUpstreamDriver("driver1", "1.0.0")
        .addUpstreamDriver("driver2", "2.0.0").build();
    assertEquals("jedis(driver2_v2.0.0;driver1_v1.0.0)", driverInfo.getFormattedName());
  }

  @Test
  public void toStringReturnsFormattedName() {
    DriverInfo driverInfo = DriverInfo.builder().addUpstreamDriver("spring-data-redis", "3.2.0")
        .build();
    assertEquals(driverInfo.getFormattedName(), driverInfo.toString());
  }

  @Test
  public void driverNameValidation() {
    // Valid names
    DriverInfo.builder().addUpstreamDriver("spring-data-redis", "1.0.0");
    DriverInfo.builder().addUpstreamDriver("lettuce-core", "1.0.0");
    DriverInfo.builder().addUpstreamDriver("akka-redis_2.13", "1.0.0");
    DriverInfo.builder().addUpstreamDriver("jedis", "1.0.0");
    DriverInfo.builder().addUpstreamDriver("redis-client", "1.0.0");
    DriverInfo.builder().addUpstreamDriver("my_driver", "1.0.0");
    DriverInfo.builder().addUpstreamDriver("driver123", "1.0.0");

    // Invalid names
    assertThrows(JedisValidationException.class,
      () -> DriverInfo.builder().addUpstreamDriver("Spring-Data", "1.0.0")); // uppercase
    assertThrows(JedisValidationException.class,
      () -> DriverInfo.builder().addUpstreamDriver("123driver", "1.0.0")); // starts with digit
    assertThrows(JedisValidationException.class,
      () -> DriverInfo.builder().addUpstreamDriver("driver@name", "1.0.0")); // special char
    assertThrows(JedisValidationException.class,
      () -> DriverInfo.builder().addUpstreamDriver("driver.name", "1.0.0")); // dot
    assertThrows(JedisValidationException.class,
      () -> DriverInfo.builder().addUpstreamDriver("driver name", "1.0.0")); // space
  }

  @Test
  public void driverNameNullOrEmpty() {
    assertThrows(JedisValidationException.class,
      () -> DriverInfo.builder().addUpstreamDriver(null, "3.2.0"));
    assertThrows(JedisValidationException.class,
      () -> DriverInfo.builder().addUpstreamDriver("", "3.2.0"));
  }

  @Test
  public void driverVersionNullOrEmpty() {
    assertThrows(JedisValidationException.class,
      () -> DriverInfo.builder().addUpstreamDriver("spring-data-redis", null));
    assertThrows(JedisValidationException.class,
      () -> DriverInfo.builder().addUpstreamDriver("spring-data-redis", ""));
  }
}
