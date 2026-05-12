package redis.clients.jedis;

import java.util.Objects;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;

public final class JedisTelemetryConfig {

  private static final JedisTelemetryConfig DISABLED
      = new JedisTelemetryConfig(false, OpenTelemetry.noop(), Attributes.empty());

  private final boolean enabled;
  private final OpenTelemetry openTelemetry;
  private final Attributes commonAttributes;

  private JedisTelemetryConfig(boolean enabled, OpenTelemetry openTelemetry, Attributes commonAttributes) {
    this.enabled = enabled;
    this.openTelemetry = openTelemetry;
    this.commonAttributes = commonAttributes;
  }

  /**
   * 获取禁用 OpenTelemetry 指标采集的配置。
   * @return 禁用状态的配置
   */
  public static JedisTelemetryConfig disabled() {
    return DISABLED;
  }

  /**
   * 创建 OpenTelemetry 指标配置构建器。
   * @return 配置构建器
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * 判断是否启用指标采集。
   * @return true 表示启用
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * 获取用于创建 Meter 的 OpenTelemetry 实例。
   * @return OpenTelemetry 实例
   */
  public OpenTelemetry getOpenTelemetry() {
    return openTelemetry;
  }

  /**
   * 获取附加到所有 Jedis 指标上的公共属性。
   * @return 公共属性
   */
  public Attributes getCommonAttributes() {
    return commonAttributes;
  }

  public static final class Builder {

    private boolean enabled = true;
    private OpenTelemetry openTelemetry = OpenTelemetry.noop();
    private Attributes commonAttributes = Attributes.empty();

    private Builder() {
    }

    /**
     * 设置是否启用指标采集。
     * @param enabled true 表示启用
     * @return 当前构建器
     */
    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /**
     * 设置 OpenTelemetry 实例。
     * @param openTelemetry OpenTelemetry 实例
     * @return 当前构建器
     */
    public Builder openTelemetry(OpenTelemetry openTelemetry) {
      this.openTelemetry = Objects.requireNonNull(openTelemetry, "openTelemetry");
      return this;
    }

    /**
     * 设置公共指标属性。
     * @param commonAttributes 公共属性
     * @return 当前构建器
     */
    public Builder commonAttributes(Attributes commonAttributes) {
      this.commonAttributes = Objects.requireNonNull(commonAttributes, "commonAttributes");
      return this;
    }

    /**
     * 构建 Jedis OpenTelemetry 指标配置。
     * @return 指标配置
     */
    public JedisTelemetryConfig build() {
      if (!enabled) {
        return DISABLED;
      }
      return new JedisTelemetryConfig(true, openTelemetry, commonAttributes);
    }
  }
}
