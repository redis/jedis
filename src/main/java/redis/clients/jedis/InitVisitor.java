package redis.clients.jedis;

interface InitVisitor {

  void visit(Connection connection);
}
