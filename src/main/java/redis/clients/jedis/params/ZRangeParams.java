package redis.clients.jedis.params;

import java.util.ArrayList;
import java.util.Collections;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.SafeEncoder;

import static redis.clients.jedis.Protocol.Keyword.BYLEX;
import static redis.clients.jedis.Protocol.Keyword.BYSCORE;
import static redis.clients.jedis.Protocol.Keyword.LIMIT;
import static redis.clients.jedis.Protocol.Keyword.REV;
import static redis.clients.jedis.Protocol.Keyword.WITHSCORES;

public class ZRangeParams extends Params {

    private long offset;
    private long count;

    public ZRangeParams() {}

    public static ZRangeParams ZRangeParams() {
        return new ZRangeParams();
    }

    public ZRangeParams byscore() {
        addParam(BYSCORE.name());
        return this;
    }

    public ZRangeParams bylex() {
        addParam(BYLEX.name());
        return this;
    }

    public ZRangeParams rev() {
        addParam(REV.name());
        return this;
    }

    public ZRangeParams limit(final long offset, final long count) {
        addParam(LIMIT.name());
        this.offset = offset;
        this.count = count;
        return this;
    }

    public ZRangeParams withscores() {
        addParam(WITHSCORES.name());
        return this;
    }

    public byte[][] getByteParams(byte[]... args) {
        ArrayList<byte[]> byteParams = new ArrayList<>();
        Collections.addAll(byteParams, args);

        if (contains(BYSCORE.name())) {
            byteParams.add(SafeEncoder.encode(BYSCORE.name()));
        }
        if (contains(BYLEX.name())) {
            byteParams.add(SafeEncoder.encode(BYLEX.name()));
        }
        if (contains(REV.name())) {
            byteParams.add(SafeEncoder.encode(REV.name()));
        }
        if (contains(LIMIT.name())) {
            byteParams.add(SafeEncoder.encode(LIMIT.name()));
            byteParams.add(Protocol.toByteArray(offset));
            byteParams.add(Protocol.toByteArray(count));
        }
        if (contains(WITHSCORES.name())) {
            byteParams.add(SafeEncoder.encode(WITHSCORES.name()));
        }

        return byteParams.toArray(new byte[byteParams.size()][]);
    }
}
