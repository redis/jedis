package redis.clients.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class JedisByteHashMap implements Map<byte[], byte[]>, Cloneable,
	Serializable {
    private static final long serialVersionUID = -6971431362627219416L;
    private Map<ByteArrayWrapper, byte[]> internalMap = new HashMap<ByteArrayWrapper, byte[]>();

    public void clear() {
	internalMap.clear();
    }

    public boolean containsKey(Object key) {
	if (key instanceof byte[])
	    return internalMap.containsKey(new ByteArrayWrapper((byte[]) key));
	return internalMap.containsKey(key);
    }

    public boolean containsValue(Object value) {
	return internalMap.containsValue(value);
    }

    public Set<java.util.Map.Entry<byte[], byte[]>> entrySet() {
	Iterator<java.util.Map.Entry<ByteArrayWrapper, byte[]>> iterator = internalMap
		.entrySet().iterator();
	HashSet<Entry<byte[], byte[]>> hashSet = new HashSet<java.util.Map.Entry<byte[], byte[]>>();
	while (iterator.hasNext()) {
	    Entry<ByteArrayWrapper, byte[]> entry = iterator.next();
	    hashSet.add(new JedisByteEntry(entry.getKey().data, entry
		    .getValue()));
	}
	return hashSet;
    }

    public byte[] get(Object key) {
	if (key instanceof byte[])
	    return internalMap.get(new ByteArrayWrapper((byte[]) key));
	return internalMap.get(key);
    }

    public boolean isEmpty() {
	return internalMap.isEmpty();
    }

    public Set<byte[]> keySet() {
	Set<byte[]> keySet = new HashSet<byte[]>();
	Iterator<ByteArrayWrapper> iterator = internalMap.keySet().iterator();
	while (iterator.hasNext()) {
	    keySet.add(iterator.next().data);
	}
	return keySet;
    }

    public byte[] put(byte[] key, byte[] value) {
	return internalMap.put(new ByteArrayWrapper(key), value);
    }

    @SuppressWarnings("unchecked")
    public void putAll(Map<? extends byte[], ? extends byte[]> m) {
	Iterator<?> iterator = m.entrySet().iterator();
	while (iterator.hasNext()) {
	    Entry<? extends byte[], ? extends byte[]> next = (Entry<? extends byte[], ? extends byte[]>) iterator
		    .next();
	    internalMap.put(new ByteArrayWrapper(next.getKey()),
		    next.getValue());
	}
    }

    public byte[] remove(Object key) {
	if (key instanceof byte[])
	    return internalMap.remove(new ByteArrayWrapper((byte[]) key));
	return internalMap.remove(key);
    }

    public int size() {
	return internalMap.size();
    }

    public Collection<byte[]> values() {
	return internalMap.values();
    }

    private static final class ByteArrayWrapper {
	private final byte[] data;

	public ByteArrayWrapper(byte[] data) {
	    if (data == null) {
		throw new NullPointerException();
	    }
	    this.data = data;
	}

	public boolean equals(Object other) {
	    if (!(other instanceof ByteArrayWrapper)) {
		return false;
	    }
	    return Arrays.equals(data, ((ByteArrayWrapper) other).data);
	}

	public int hashCode() {
	    return Arrays.hashCode(data);
	}
    }

    private static final class JedisByteEntry implements Entry<byte[], byte[]> {
	private byte[] value;
	private byte[] key;

	public JedisByteEntry(byte[] key, byte[] value) {
	    this.key = key;
	    this.value = value;
	}

	public byte[] getKey() {
	    return this.key;
	}

	public byte[] getValue() {
	    return this.value;
	}

	public byte[] setValue(byte[] value) {
	    this.value = value;
	    return value;
	}

    }
}