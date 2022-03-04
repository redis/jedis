package redis.clients.jedis.commands;

public interface PipelineCommands extends KeyPipelineCommands, StringPipelineCommands,
    ListPipelineCommands, HashPipelineCommands, SetPipelineCommands, SortedSetPipelineCommands,
    GeoPipelineCommands, HyperLogLogPipelineCommands, StreamPipelineCommands,
    ScriptingKeyPipelineCommands, SampleKeyedPipelineCommands, FunctionPipelineCommands {
}
