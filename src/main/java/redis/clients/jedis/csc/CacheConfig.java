package redis.clients.jedis.csc;

import java.util.Set;
import java.util.function.Supplier;
import java.util.Collections;
import java.util.HashSet;

import redis.clients.jedis.MetadataResolver;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.JedisAsserts;

public class CacheConfig {

    private int maxSize;
    private Cacheable cacheable;
    private Cacheable resolver;
    private EvictionPolicy evictionPolicy;
    private Class cacheClass;
    private Supplier<EvictionPolicy> evictionPolicySupplier;

    public int getMaxSize() {
        return maxSize;
    }

    public Cacheable getCacheable() {
        return resolver == null ? cacheable : resolver;
    }

    /**
     * @return the configured eviction policy, or null when none was set
     * @deprecated Use {@link #getEvictionPolicySupplier()} instead; a supplier yields a fresh
     *             policy per cache instance.
     */
    @Deprecated
    public EvictionPolicy getEvictionPolicy() {
        return evictionPolicy;
    }

    /**
     * @return supplier of the eviction policy for a new cache instance; never null
     * @since 8.1
     */
    public Supplier<EvictionPolicy> getEvictionPolicySupplier() {
        return evictionPolicySupplier;
    }

    /**
     * @return the custom cache class, or null when the default cache is used
     * @deprecated Reflective cache instantiation is being phased out. Supply a {@link CacheFactory} to
     *             the client builder instead.
     */
    @Deprecated
    public Class getCacheClass() {
        return cacheClass;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final int DEFAULT_MAX_SIZE = 10000;
        private int maxSize = DEFAULT_MAX_SIZE;
        private Cacheable cacheable = DefaultCacheable.INSTANCE;
        private Cacheable fallback;
        private Set<ProtocolCommand> excludedCommands = Collections.emptySet();
        private EvictionPolicy evictionPolicy;
        private Class cacheClass;
        private Supplier<EvictionPolicy> evictionPolicySupplier;

        public Builder maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder evictionPolicy(EvictionPolicy policy) {
            this.evictionPolicy = policy;
            return this;
        }

        /**
         * Sets a supplier invoked once per cache instance to create its eviction policy. Takes
         * precedence over {@link #evictionPolicy(EvictionPolicy)} when both are set.
         * @param supplier eviction policy supplier; must not be null
         * @return this builder
         * @since 8.1
         */
        public Builder evictionPolicySupplier(Supplier<EvictionPolicy> supplier) {
            JedisAsserts.notNull(supplier, "supplier cannot be null");
            this.evictionPolicySupplier = supplier;
            return this;
        }

        public Builder cacheable(Cacheable cacheable) {
            this.cacheable = cacheable;
            return this;
        }

        /**
         * Sets the fallback {@link Cacheable} deciding commands that have no metadata verdict.
         * Without a fallback, such commands are not cached. Cannot be combined with
         * {@link #cacheable(Cacheable)}.
         * @param fallback fallback {@link Cacheable}; must not be null
         * @return this builder
         * @since 8.1
         */
        public Builder withFallback(Cacheable fallback) {
            JedisAsserts.notNull(fallback, "fallback cannot be null");
            this.fallback = fallback;
            return this;
        }

        /**
         * Excludes the given commands from client-side caching, narrowing the eligible command set.
         * Exclusions can only narrow: a command that is not cacheable by the default policy cannot
         * be made cacheable. Exclusions apply to the exact command; a container subcommand is
         * excluded individually by its {@code PARENT|CHILD} name, excluding a parent does not
         * exclude its subcommands. Cannot be combined with {@link #cacheable(Cacheable)}.
         * @param commands commands to exclude; copied, later changes to the given set have no
         *            effect
         * @return this builder
         * @since 8.1
         */
        public Builder excludeCommands(Set<ProtocolCommand> commands) {
            JedisAsserts.notNull(commands, "commands cannot be null");
            for (ProtocolCommand command : commands) {
                JedisAsserts.notNull(command, "commands cannot contain null elements");
            }
            this.excludedCommands = new HashSet<>(commands);
            return this;
        }

        /**
         * @param cacheClass a {@link Cache} implementation instantiated reflectively through its
         *            {@code (int, EvictionPolicy)} or {@code (int, EvictionPolicy, Cacheable)}
         *            constructor
         * @return this builder
         * @deprecated Reflective cache instantiation is being phased out. Supply a {@link CacheFactory} 
         *          to the client builder instead.
         */
        @Deprecated
        public Builder cacheClass(Class cacheClass) {
            this.cacheClass = cacheClass;
            return this;
        }

        public CacheConfig build() {
            CacheConfig cacheConfig = new CacheConfig();
            cacheConfig.maxSize = this.maxSize;
            CacheabilityResolver resolver = new CacheabilityResolver(new MetadataResolver(),
                    this.excludedCommands, fallback);
            // check if it is default
            if (this.cacheable != null && this.cacheable.getClass() ==  DefaultCacheable.class) {
                cacheConfig.resolver = resolver;
            } else {
                JedisAsserts.isTrue(excludedCommands.isEmpty(),
                    "Cannot provide both a custom Cacheable and excluded commands. Pick either custom Cacheable or command exclusion.");
                JedisAsserts.isTrue(fallback == null,
                    "Cannot provide both a custom Cacheable and a fallback. Pick either custom Cacheable or fallback.");
                // Combining the user-provided Cacheable with the resolver; with a later major
                // release, we will change the behavior to let the resolver take precedence over
                // the user-provided Cacheable. For now, the user-provided Cacheable always wins;
                // this is only to avoid a behavioural breaking change in a minor release.
                if (this.cacheable != null) {
                    cacheConfig.cacheable = new CustomCacheablePolicy(this.cacheable, resolver);
                }
            }
            cacheConfig.evictionPolicy = this.evictionPolicy;
            if (evictionPolicySupplier != null) {
                cacheConfig.evictionPolicySupplier = evictionPolicySupplier;
            } else {
                cacheConfig.evictionPolicySupplier = cacheConfig.evictionPolicy == null
                        ? () -> new LRUEviction(cacheConfig.maxSize)
                        : () -> cacheConfig.evictionPolicy;
            }
            cacheConfig.cacheClass = this.cacheClass;
            return cacheConfig;
        }

    }
}