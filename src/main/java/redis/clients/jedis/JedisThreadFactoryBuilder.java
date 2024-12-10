package redis.clients.jedis;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JedisThreadFactoryBuilder is a class that builds a ThreadFactory for Jedis.
 */
public class JedisThreadFactoryBuilder {
    private String namePrefix = null;
    private boolean daemon = false;
    private int priority = Thread.NORM_PRIORITY;
    private ThreadFactory backingThreadFactory = null;
    private UncaughtExceptionHandler uncaughtExceptionHandler = null;

    /**
     * Sets the name prefix for the threads created by the ThreadFactory.
     *
     * @param namePrefix the name prefix for the threads
     * @return the JedisThreadFactoryBuilder instance
     * @throws NullPointerException if namePrefix is null
     */
    public JedisThreadFactoryBuilder setNamePrefix(String namePrefix) {
        if (namePrefix == null) {
            throw new NullPointerException();
        }
        this.namePrefix = namePrefix;
        return this;
    }

    /**
     * Sets whether the threads created by the ThreadFactory are daemon threads.
     *
     * @param daemon true if the threads are daemon threads, false otherwise
     * @return the JedisThreadFactoryBuilder instance
     */
    public JedisThreadFactoryBuilder setDaemon(boolean daemon) {
        this.daemon = daemon;
        return this;
    }

    /**
     * Sets the priority for the threads created by the ThreadFactory.
     *
     * @param priority the priority for the threads
     * @return the JedisThreadFactoryBuilder instance
     * @throws IllegalArgumentException if priority is not in the range of Thread.MIN_PRIORITY to Thread.MAX_PRIORITY
     */
    public JedisThreadFactoryBuilder setPriority(int priority) {
        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException(String.format(
                "Thread priority (%s) must be in %d ~ %d", priority,
                Thread.MIN_PRIORITY, Thread.MAX_PRIORITY));
        }

        this.priority = priority;
        return this;
    }

    /**
     * Sets the UncaughtExceptionHandler for the threads created by the ThreadFactory.
     *
     * @param uncaughtExceptionHandler the UncaughtExceptionHandler for the threads
     * @return the JedisThreadFactoryBuilder instance
     * @throws NullPointerException if uncaughtExceptionHandler is null
     */
    public JedisThreadFactoryBuilder setUncaughtExceptionHandler(
        UncaughtExceptionHandler uncaughtExceptionHandler) {
        if (uncaughtExceptionHandler == null) {
            throw new NullPointerException(
                "UncaughtExceptionHandler cannot be null");
        }
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        return this;
    }

    /**
     * Sets the backing ThreadFactory for the JedisThreadFactoryBuilder.
     *
     * @param backingThreadFactory the backing ThreadFactory
     * @return the JedisThreadFactoryBuilder instance
     * @throws NullPointerException if backingThreadFactory is null
     */
    public JedisThreadFactoryBuilder setThreadFactory(
        ThreadFactory backingThreadFactory) {
        if (uncaughtExceptionHandler == null) {
            throw new NullPointerException(
                "BackingThreadFactory cannot be null");
        }
        this.backingThreadFactory = backingThreadFactory;
        return this;
    }

    /**
     * Builds a ThreadFactory using the JedisThreadFactoryBuilder instance.
     *
     * @return the ThreadFactory
     */
    public ThreadFactory build() {
        return build(this);
    }

    /**
     * Builds a ThreadFactory by JedisThreadFactoryBuilder.
     *
     * @param builder JedisThreadFactoryBuilder
     * @return ThreadFactory
     */
    private static ThreadFactory build(JedisThreadFactoryBuilder builder) {
        final String namePrefix = builder.namePrefix;
        final Boolean daemon = builder.daemon;
        final Integer priority = builder.priority;
        final UncaughtExceptionHandler uncaughtExceptionHandler = builder.uncaughtExceptionHandler;
        final ThreadFactory backingThreadFactory = (builder.backingThreadFactory != null) ? builder.backingThreadFactory
            : Executors.defaultThreadFactory();
        final AtomicLong count = new AtomicLong(0);

        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = backingThreadFactory.newThread(runnable);
                if (daemon) {
                    thread.setDaemon(daemon);
                }
                if (priority != Thread.NORM_PRIORITY) {
                    thread.setPriority(priority);
                }
                if (namePrefix != null) {
                    thread.setName(namePrefix + "-" + count.getAndIncrement());
                }
                if (uncaughtExceptionHandler != null) {
                    thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
                }
                return thread;
            }
        };
    }
}
