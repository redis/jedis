package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class JedisTelemetryTest {

  @Test
  public void recordsCommandMetrics() {
    InMemoryMetricReader reader = InMemoryMetricReader.create();
    SdkMeterProvider meterProvider = SdkMeterProvider.builder().registerMetricReader(reader).build();
    OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().setMeterProvider(meterProvider).build();

    JedisTelemetry telemetry = JedisTelemetry.create(
      JedisTelemetryConfig.builder().openTelemetry(openTelemetry).build(),
      new HostAndPort("localhost", 6379),
      RedisProtocol.RESP2);

    CommandArguments args = new CommandArguments(Protocol.Command.GET).key("key");
    long startNanos = telemetry.startCommand();
    telemetry.recordCommandSent(args);
    telemetry.recordCommandSuccess(args, startNanos);

    Collection<MetricData> metrics = reader.collectAllMetrics();
    assertEquals(1L, longSumValue(metrics, "jedis.command.sent"));
    assertEquals(1L, histogramCount(metrics, "jedis.command.duration"));

    meterProvider.close();
  }

  @Test
  public void recordsErrorAndConnectionMetrics() {
    InMemoryMetricReader reader = InMemoryMetricReader.create();
    SdkMeterProvider meterProvider = SdkMeterProvider.builder().registerMetricReader(reader).build();
    OpenTelemetry openTelemetry = OpenTelemetrySdk.builder().setMeterProvider(meterProvider).build();

    JedisTelemetry telemetry = JedisTelemetry.create(
      JedisTelemetryConfig.builder().openTelemetry(openTelemetry).build(),
      new HostAndPort("localhost", 6379),
      RedisProtocol.RESP3);

    CommandArguments args = new CommandArguments(Protocol.Command.SET).key("key").add("value");
    long startNanos = telemetry.startCommand();
    telemetry.recordCommandError(args, startNanos, new JedisConnectionException("boom"));
    telemetry.recordConnectionOpened();
    telemetry.recordConnectionClosed();
    telemetry.recordConnectionError(new JedisConnectionException("boom"));

    Collection<MetricData> metrics = reader.collectAllMetrics();
    assertEquals(1L, longSumValue(metrics, "jedis.command.errors"));
    assertEquals(1L, longSumValue(metrics, "jedis.connection.opened"));
    assertEquals(1L, longSumValue(metrics, "jedis.connection.closed"));
    assertEquals(1L, longSumValue(metrics, "jedis.connection.errors"));

    meterProvider.close();
  }

  private static long longSumValue(Collection<MetricData> metrics, String name) {
    return findMetric(metrics, name).getLongSumData().getPoints().iterator().next().getValue();
  }

  private static long histogramCount(Collection<MetricData> metrics, String name) {
    return findMetric(metrics, name).getHistogramData().getPoints().iterator().next().getCount();
  }

  private static MetricData findMetric(Collection<MetricData> metrics, String name) {
    for (MetricData metric : metrics) {
      if (name.equals(metric.getName())) {
        return metric;
      }
    }
    assertTrue(false, "Metric not found: " + name);
    return null;
  }
}
