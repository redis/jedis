package redis.clients.jedis.search;

import static redis.clients.jedis.BuilderFactory.AGGRESSIVE_ENCODED_OBJECT;

import redis.clients.jedis.Builder;

public class ProfilingInfo {

  private final Object profilingInfo;

  private ProfilingInfo(Object profilingInfo) {
    this.profilingInfo = profilingInfo;
  }

  public Object getProfilingInfo() {
    return profilingInfo;
  }

  @Override
  public String toString() {
    return String.valueOf(profilingInfo);
  }

  public static final Builder<ProfilingInfo> PROFILING_INFO_BUILDER
      = new Builder<ProfilingInfo>() {
    @Override
    public ProfilingInfo build(Object data) {
      return new ProfilingInfo(AGGRESSIVE_ENCODED_OBJECT.build(data));
    }
  };
}
