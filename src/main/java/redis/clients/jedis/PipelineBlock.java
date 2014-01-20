package redis.clients.jedis;


public abstract class PipelineBlock extends Pipeline {
	// For shadowing
	@SuppressWarnings("unused")
	private Client client;	
	
    public abstract void execute();
}
