package redis.clients.jedis;

import static redis.clients.jedis.Protocol.Keyword.COUNT;
import static redis.clients.jedis.Protocol.Keyword.MATCH;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import redis.clients.util.SafeEncoder;

public class ScanParams {
    private List<byte[]> params = new ArrayList<byte[]>();
    public final static String SCAN_POINTER_START = String.valueOf(0);
    public final static byte[] SCAN_POINTER_START_BINARY = SafeEncoder.encode(SCAN_POINTER_START);

    public void match(final byte[] pattern) {
	params.add(MATCH.raw);
	params.add(pattern);
    }
    
    public void match(final String pattern) {
	params.add(MATCH.raw);
	params.add(SafeEncoder.encode(pattern));
    }

    public void count(final int count) {
	params.add(COUNT.raw);
	params.add(Protocol.toByteArray(count));
    }

    public Collection<byte[]> getParams() {
	return Collections.unmodifiableCollection(params);
    }
}
