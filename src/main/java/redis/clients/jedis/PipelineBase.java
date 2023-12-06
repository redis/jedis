package redis.clients.jedis;

/**
 * @deprecated Use {@link AbstractPipeline}.
 */
@Deprecated
public abstract class PipelineBase extends AbstractPipeline {

  protected PipelineBase(CommandObjects commandObjects) {
    super(commandObjects);
  }
}
