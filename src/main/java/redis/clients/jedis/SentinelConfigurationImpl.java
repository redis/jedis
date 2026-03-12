package redis.clients.jedis;

import java.util.Set;

class SentinelConfigurationImpl implements SentinelConfiguration {

  private String masterName;
  private Set<HostAndPort> sentinels;
  private JedisClientConfig sentinelClientConfig;

  private SentinelConfigurationImpl(Builder builder) {
    this.masterName = builder.masterName;
    this.sentinels = builder.sentinels;
    this.sentinelClientConfig = builder.sentinelClientConfig;
  }

  @Override
  public String getMasterName(){
    return masterName;
  }

  @Override
  public Set<HostAndPort> getSentinels(){
    return sentinels;
  }

  @Override
  public JedisClientConfig getSentinelClientConfig(){
    return sentinelClientConfig;
  }

  public static Builder builder() {
    return new Builder();
  }

  static class Builder  {

    private String masterName;
    private Set<HostAndPort> sentinels;
    private JedisClientConfig sentinelClientConfig;

    public Builder masterName(String masterName) {
      this.masterName = masterName;
      return this;
    }

    public Builder sentinels(Set<HostAndPort> sentinels) {
      this.sentinels = sentinels;
      return this;
    }

    public Builder sentinelClientConfig(JedisClientConfig sentinelClientConfig) {
      this.sentinelClientConfig = sentinelClientConfig;
      return this;
    }

    public SentinelConfiguration build() {
      return new SentinelConfigurationImpl(this);
    }
  }
}

