package redis.clients.jedis.executors.aggregators;

import redis.clients.jedis.CommandFlagsRegistry;

interface Aggregator<I, O> {

  void add(I input);

  O getResult();
}