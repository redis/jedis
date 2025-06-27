package redis.clients.jedis.mcf;

class NoOpStrategy implements HealthCheckStrategy {

    @Override
    public int getInterval() {
        return 1000;
    }

    @Override
    public int getTimeout() {
        return 1000;
    }

    @Override
    public HealthStatus doHealthCheck(Endpoint endpoint) {
        return HealthStatus.HEALTHY;
    }

}
