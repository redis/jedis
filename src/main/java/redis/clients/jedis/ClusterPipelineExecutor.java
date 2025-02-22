package redis.clients.jedis;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Executor used for parallel syncing of multinode pipeline when provided in
 * {@link DefaultJedisClientConfig.Builder#pipelineExecutorProvider(PipelineExecutorProvider)}
 */
public interface ClusterPipelineExecutor extends Executor, AutoCloseable {

    /**
     * To avoid following hte {@link JedisCluster} client lifecycle in shutting down the executor service
     * provide your own implementation of this interface to {@link PipelineExecutorProvider}
     */
    default void shutdown() {}

    default void close() {
        shutdown();
    }

    /**
     * Wrap an executor service into a {@link ClusterPipelineExecutor} to allow clients to provide their
     * desired implementation of the {@link ExecutorService} to support parallel syncing of {@link MultiNodePipelineBase}.
     *
     * @param executorService
     * @return ClusterPipelineExecutor that will be shutdown alongside the {@link JedisCluster} client.
     */
    static ClusterPipelineExecutor from(ExecutorService executorService) {
        return new ClusterPipelineExecutor() {
            @Override
            public void execute(Runnable command) {
                executorService.execute(command);
            }

            @Override
            public void shutdown() {
                executorService.shutdown();
            }
        };
    }

}
