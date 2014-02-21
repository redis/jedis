package redis.clients.jedis.tests;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

class Closer implements Closeable {
    private final Set<Closeable> elements = new HashSet<Closeable>();

    synchronized <T extends Closeable> T register(T element) {
	if (element != null) {
	    elements.add(element);
	}
	return element;
    }

    public synchronized void close() throws IOException {
	Throwable caught = null;

	for (Closeable element : elements) {
	    try {
		element.close();
	    }
	    catch (Throwable t) {
		caught = t;
	    }
	}

	elements.clear();

	if (caught != null) {
	    if (caught instanceof IOException) {
		throw (IOException) caught;
	    }
	    throw (RuntimeException) caught;
	}
    }
}
