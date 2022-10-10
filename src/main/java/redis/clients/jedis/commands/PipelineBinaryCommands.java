package redis.clients.jedis.commands;

public interface PipelineBinaryCommands extends KeyPipelineBinaryCommands,
    StringPipelineBinaryCommands, ListPipelineBinaryCommands, HashPipelineBinaryCommands,
    SetPipelineBinaryCommands, SortedSetPipelineBinaryCommands, GeoPipelineBinaryCommands,
    HyperLogLogPipelineBinaryCommands, StreamPipelineBinaryCommands,
    ScriptingKeyPipelineBinaryCommands, SampleBinaryKeyedPipelineCommands, FunctionPipelineBinaryCommands {
}
