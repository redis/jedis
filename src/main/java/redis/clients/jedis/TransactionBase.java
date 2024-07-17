package redis.clients.jedis;

/**
 * @deprecated Use {@link AbstractTransaction}.
 */
@Deprecated
public abstract class TransactionBase extends AbstractTransaction {

  @Deprecated
  protected TransactionBase() {
    super();
  }

  protected TransactionBase(CommandObjects commandObjects) {
    super(commandObjects);
  }
}
