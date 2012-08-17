package redis.clients.jedis;


public abstract class PipelineBlock extends Pipeline {
    public abstract void execute();
}
