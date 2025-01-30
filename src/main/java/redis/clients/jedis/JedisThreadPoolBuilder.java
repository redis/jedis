package redis.clients.jedis;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to build a thread pool for Jedis.
 */
public class JedisThreadPoolBuilder {
    private static final Logger log = LoggerFactory.getLogger(JedisThreadPoolBuilder.class);

    private static final RejectedExecutionHandler defaultRejectHandler = new AbortPolicy();

    public static PoolBuilder pool() {
        return new PoolBuilder();
    }

    /**
     * Custom thread factory or use default
     * @param threadNamePrefix the thread name prefix
     * @param daemon daemon
     * @return ThreadFactory
     */
    private static ThreadFactory createThreadFactory(String threadNamePrefix, boolean daemon) {
        if (threadNamePrefix != null) {
            return new JedisThreadFactoryBuilder().setNamePrefix(threadNamePrefix).setDaemon(daemon)
                .setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        log.error(String.format("Thread %s threw exception %s", t.getName(), e.getMessage()));
                    }
                }).build();
        }

        return Executors.defaultThreadFactory();
    }

    /**
     * This class is used to build a thread pool.
     */
    public static class PoolBuilder {
        private int coreSize = 0;
        private int maxSize = Integer.MAX_VALUE;
        private long keepAliveMillSecs = 10;
        private ThreadFactory threadFactory;
        private String threadNamePrefix;
        private boolean daemon;
        private RejectedExecutionHandler rejectHandler;
        private BlockingQueue<Runnable> workQueue;

        public PoolBuilder setCoreSize(int coreSize) {
            this.coreSize = coreSize;
            return this;
        }

        public PoolBuilder setMaxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public PoolBuilder setKeepAliveMillSecs(long keepAliveMillSecs) {
            this.keepAliveMillSecs = keepAliveMillSecs;
            return this;
        }

        public PoolBuilder setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
            return this;
        }

        public PoolBuilder setDaemon(boolean daemon) {
            this.daemon = daemon;
            return this;
        }

        public PoolBuilder setThreadFactory(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        public PoolBuilder setRejectHandler(RejectedExecutionHandler rejectHandler) {
            this.rejectHandler = rejectHandler;
            return this;
        }

        public PoolBuilder setWorkQueue(BlockingQueue<Runnable> workQueue) {
            this.workQueue = workQueue;
            return this;
        }

        public ExecutorService build() {
            if (threadFactory == null) {
                threadFactory = createThreadFactory(threadNamePrefix, daemon);
            }

            if (workQueue == null) {
                throw new IllegalArgumentException("workQueue can't be null");
            }

            if (rejectHandler == null) {
                rejectHandler = defaultRejectHandler;
            }

            ExecutorService executorService = new ThreadPoolExecutor(coreSize, maxSize, keepAliveMillSecs,
                TimeUnit.MILLISECONDS, workQueue, threadFactory, rejectHandler);

            return executorService;
        }
    }
}
