package redis.clients.jedis;

public interface InitializationTracker<T> extends Iterable<T> {
    void add(T target);
    void remove(T target);

    public static InitializationTracker NOOP = new InitializationTracker() {
        @Override
        public void add(Object target) {
        }

        @Override
        public void remove(Object target) {
        }

        @Override
        public java.util.Iterator iterator() {
            return java.util.Collections.emptyIterator();
        }
    };
}
