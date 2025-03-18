package redis.clients.jedis;

import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * This provides a {@link ClusterPipelineExecutor} used for parallel syncing of {@link MultiNodePipelineBase}
 */
public class PipelineExecutorProvider {

    static final PipelineExecutorProvider DEFAULT = new PipelineExecutorProvider();

    private ClusterPipelineExecutor clusterPipelineExecutor;

    /**
     * Default constructor providing an empty {@link Optional} of {@link ClusterPipelineExecutor}
     */
    private PipelineExecutorProvider() {}

    /**
     * Will provide a {@link ClusterPipelineExecutor} with the specified number of thread. The number of thread
     * should be equal or higher than the number of master nodes in the cluster.
     *
     * @param threadCount
     */
    public PipelineExecutorProvider(int threadCount) {
        this.clusterPipelineExecutor = ClusterPipelineExecutor.from(Executors.newFixedThreadPool(threadCount));;
    }

    /**
     * Allow clients to provide their own implementation of {@link ClusterPipelineExecutor}
     * @param clusterPipelineExecutor
     */
    public PipelineExecutorProvider(ClusterPipelineExecutor clusterPipelineExecutor) {
        this.clusterPipelineExecutor = clusterPipelineExecutor;
    }

    /**
     * @return an empty option by default, otherwise will return the configured value.
     */
    Optional<ClusterPipelineExecutor> getClusteredPipelineExecutor() {
        return Optional.ofNullable(clusterPipelineExecutor);
    }
}
