package redis.clients.jedis.executors.aggregators;

import redis.clients.jedis.CommandFlagsRegistry;

interface Aggregator<I, R> {

  void add(I input);

  R getResult();
}