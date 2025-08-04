package redis.clients.jedis.util;

import java.io.Serializable;
import java.util.*;

public class JedisByteMap<T> implements Map<byte[], T>, Cloneable, Serializable {
    private static final long serialVersionUID = -6971431362627219416L;
    private final Map<ByteArrayWrapper, T> internalMap = new HashMap<>();

    @Override
    public void clear() {
        internalMap.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        if (key instanceof byte[]) return internalMap.containsKey(new ByteArrayWrapper((byte[]) key));
        return internalMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return internalMap.containsValue(value);
    }

    @Override
    public Set<Entry<byte[], T>> entrySet() {
        Iterator<Entry<ByteArrayWrapper, T>> iterator = internalMap.entrySet()
                .iterator();
        HashSet<Entry<byte[], T>> hashSet = new HashSet<>();
        while (iterator.hasNext()) {
            Entry<ByteArrayWrapper, T> entry = iterator.next();
            hashSet.add(new JedisByteEntry(entry.getKey().data, entry.getValue()));
        }
        return hashSet;
    }

    @Override
    public T get(Object key) {
        if (key instanceof byte[]) return internalMap.get(new ByteArrayWrapper((byte[]) key));
        return internalMap.get(key);
    }

    @Override
    public boolean isEmpty() {
        return internalMap.isEmpty();
    }

    @Override
    public Set<byte[]> keySet() {
        Set<byte[]> keySet = new HashSet<>();
        Iterator<ByteArrayWrapper> iterator = internalMap.keySet().iterator();
        while (iterator.hasNext()) {
            keySet.add(iterator.next().data);
        }
        return keySet;
    }

    @Override
    public T put(byte[] key, T value) {
        return internalMap.put(new ByteArrayWrapper(key), value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void putAll(Map<? extends byte[], ? extends T> m) {
        Iterator<?> iterator = m.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<? extends byte[], ? extends T> next = (Entry<? extends byte[], ? extends T>) iterator
                    .next();
            internalMap.put(new ByteArrayWrapper(next.getKey()), next.getValue());
        }
    }

    @Override
    public T remove(Object key) {
        if (key instanceof byte[]) return internalMap.remove(new ByteArrayWrapper((byte[]) key));
        return internalMap.remove(key);
    }

    @Override
    public int size() {
        return internalMap.size();
    }

    @Override
    public Collection<T> values() {
        return internalMap.values();
    }

    private static final class ByteArrayWrapper implements Serializable {
        private final byte[] data;

        public ByteArrayWrapper(byte[] data) {
            if (data == null) {
                throw new NullPointerException();
            }
            this.data = data;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) return false;
            if (other == this) return true;
            if (!(other instanceof ByteArrayWrapper)) return false;

            return Arrays.equals(data, ((ByteArrayWrapper) other).data);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(data);
        }
    }

    private static final class JedisByteEntry<T> implements Entry<byte[], T> {
        private final byte[] key;
        private T value;

        public JedisByteEntry(byte[] key, T value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public byte[] getKey() {
            return this.key;
        }

        @Override
        public T getValue() {
            return this.value;
        }

        @Override
        public T setValue(T value) {
            this.value = value;
            return value;
        }

    }
}
