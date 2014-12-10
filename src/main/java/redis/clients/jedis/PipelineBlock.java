package redis.clients.jedis;

@Deprecated
/**
 * This method is deprecated due to its error prone with multi
 * and will be removed on next major release
 * @see https://github.com/xetorthio/jedis/pull/498
 */
public abstract class PipelineBlock extends Pipeline {
  public abstract void execute();
}
