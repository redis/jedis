package redis.clients.jedis;

import java.util.Locale;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import redis.clients.jedis.commands.ProtocolCommand;

final class JedisTelemetry {

  private static final String INSTRUMENTATION_NAME = "redis.clients.jedis";
  private static final JedisTelemetry NOOP = new JedisTelemetry();

  private final boolean enabled;
  private final Attributes baseAttributes;
  private final LongCounter commandSentCounter;
  private final LongCounter commandErrorCounter;
  private final DoubleHistogram commandDurationHistogram;
  private final LongCounter connectionOpenedCounter;
  private final LongCounter connectionClosedCounter;
  private final LongCounter connectionErrorCounter;

  private JedisTelemetry() {
    this.enabled = false;
    this.baseAttributes = Attributes.empty();
    this.commandSentCounter = null;
    this.commandErrorCounter = null;
    this.commandDurationHistogram = null;
    this.connectionOpenedCounter = null;
    this.connectionClosedCounter = null;
    this.connectionErrorCounter = null;
  }

  private JedisTelemetry(Meter meter, Attributes baseAttributes) {
    this.enabled = true;
    this.baseAttributes = baseAttributes;
    this.commandSentCounter = meter.counterBuilder("jedis.command.sent")
        .setDescription("Number of Redis commands sent by Jedis.")
        .setUnit("{command}")
        .build();
    this.commandErrorCounter = meter.counterBuilder("jedis.command.errors")
        .setDescription("Number of Redis command executions that failed in Jedis.")
        .setUnit("{error}")
        .build();
    this.commandDurationHistogram = meter.histogramBuilder("jedis.command.duration")
        .setDescription("Redis command execution duration observed by Jedis.")
        .setUnit("ms")
        .build();
    this.connectionOpenedCounter = meter.counterBuilder("jedis.connection.opened")
        .setDescription("Number of Redis connections opened by Jedis.")
        .setUnit("{connection}")
        .build();
    this.connectionClosedCounter = meter.counterBuilder("jedis.connection.closed")
        .setDescription("Number of Redis connections closed by Jedis.")
        .setUnit("{connection}")
        .build();
    this.connectionErrorCounter = meter.counterBuilder("jedis.connection.errors")
        .setDescription("Number of Redis connection failures observed by Jedis.")
        .setUnit("{error}")
        .build();
  }

  /**
   * 获取禁用指标采集的 recorder。
   * @return noop recorder
   */
  static JedisTelemetry noop() {
    return NOOP;
  }

  /**
   * 根据客户端配置和连接信息创建指标 recorder。
   * @param config 指标配置
   * @param hostAndPort Redis 地址
   * @param protocol Redis 协议
   * @return 指标 recorder
   */
  static JedisTelemetry create(JedisTelemetryConfig config, HostAndPort hostAndPort, RedisProtocol protocol) {
    if (config == null || !config.isEnabled()) {
      return NOOP;
    }

    OpenTelemetry openTelemetry = config.getOpenTelemetry();
    String version = JedisMetaInfo.getVersion();
    Meter meter = openTelemetry.meterBuilder(INSTRUMENTATION_NAME)
        .setInstrumentationVersion(version == null ? "unknown" : version)
        .build();

    AttributesBuilder attributes = Attributes.builder()
        .put("db.system", "redis")
        .putAll(config.getCommonAttributes());
    if (hostAndPort != null) {
      attributes.put("server.address", hostAndPort.getHost());
      attributes.put("server.port", hostAndPort.getPort());
    }
    if (protocol != null) {
      attributes.put("redis.protocol", protocol.name());
    }
    return new JedisTelemetry(meter, attributes.build());
  }

  /**
   * 记录命令已发送。
   * @param args 命令参数
   */
  void recordCommandSent(CommandArguments args) {
    if (!enabled) {
      return;
    }
    commandSentCounter.add(1, commandAttributes(args));
  }

  /**
   * 创建命令执行开始时间。
   * @return 纳秒时间戳
   */
  long startCommand() {
    return enabled ? System.nanoTime() : 0L;
  }

  /**
   * 记录命令执行成功。
   * @param args 命令参数
   * @param startNanos 开始时间
   */
  void recordCommandSuccess(CommandArguments args, long startNanos) {
    if (!enabled) {
      return;
    }
    commandDurationHistogram.record(elapsedMillis(startNanos), commandAttributes(args));
  }

  /**
   * 记录命令执行失败。
   * @param args 命令参数
   * @param startNanos 开始时间
   * @param error 异常
   */
  void recordCommandError(CommandArguments args, long startNanos, Throwable error) {
    if (!enabled) {
      return;
    }
    Attributes attributes = commandAttributes(args).toBuilder()
        .put("error.type", error.getClass().getName())
        .build();
    commandErrorCounter.add(1, attributes);
    commandDurationHistogram.record(elapsedMillis(startNanos), attributes);
  }

  /**
   * 记录连接已打开。
   */
  void recordConnectionOpened() {
    if (enabled) {
      connectionOpenedCounter.add(1, baseAttributes);
    }
  }

  /**
   * 记录连接已关闭。
   */
  void recordConnectionClosed() {
    if (enabled) {
      connectionClosedCounter.add(1, baseAttributes);
    }
  }

  /**
   * 记录连接异常。
   * @param error 异常
   */
  void recordConnectionError(Throwable error) {
    if (!enabled) {
      return;
    }
    connectionErrorCounter.add(1, baseAttributes.toBuilder()
        .put("error.type", error.getClass().getName())
        .build());
  }

  private Attributes commandAttributes(CommandArguments args) {
    return baseAttributes.toBuilder()
        .put("redis.command", commandName(args))
        .build();
  }

  private static double elapsedMillis(long startNanos) {
    return (System.nanoTime() - startNanos) / 1_000_000.0d;
  }

  private static String commandName(CommandArguments args) {
    if (args == null) {
      return "UNKNOWN";
    }
    ProtocolCommand command = args.getCommand();
    if (command == null || command.getRaw() == null) {
      return "UNKNOWN";
    }
    return new String(command.getRaw(), Protocol.CHARSET).toUpperCase(Locale.ROOT);
  }
}
