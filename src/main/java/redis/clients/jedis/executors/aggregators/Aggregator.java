package redis.clients.jedis.executors.aggregators;

interface Aggregator<I, R> {

  void add(I input);

  R getResult();
}