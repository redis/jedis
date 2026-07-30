package redis.clients.jedis;

interface InitVisitor {

  void visitBeforeHandshake(Connection connection);

  void visitAfterHandshake(Connection connection);
}
