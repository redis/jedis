package redis.clients.jedis.mcf;

public enum HealthStatus {
    HEALTHY(0x01), UNHEALTHY(0x02), FORCED_UNHEALTHY(0x04);

    private final int value;

    HealthStatus(int val) {
        this.value = val;
    }

    public boolean isHealthy() {
        return (this.value & HEALTHY.value) != 0;
    }

    public boolean isForcedUnhealthy() {
        return (this.value & FORCED_UNHEALTHY.value) != 0;
    }
}
