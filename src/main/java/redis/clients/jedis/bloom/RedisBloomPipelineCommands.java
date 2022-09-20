package redis.clients.jedis.bloom;

public interface RedisBloomPipelineCommands extends BloomFilterPipelineCommands,
    CuckooFilterPipelineCommands, CountMinSketchPipelineCommands, TopKFilterPipelineCommands,
    TDigestSketchPipelineCommands {

}
