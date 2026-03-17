package redis.clients.jedis.executors.aggregators;

import redis.clients.jedis.CommandFlagsRegistry;
import redis.clients.jedis.annots.Experimental;

@Experimental
public interface Aggregator<I, O> {

    void add(I input);

    O getResult();

    static <T> Aggregator<T,T> replyAggregator(CommandFlagsRegistry.ResponsePolicy policy){
        return new ReplyAggregator<T>(policy);
    }
}