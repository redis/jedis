package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

public enum LatencyEvent implements Rawable {

    ACTIVE_DEFRAG_CYCLE("active-defrag-cycle"), AOF_FSYNC_ALWAYS("aof-fsync-always"), AOF_STAT("aof-stat"),
    AOF_REWRITE_DIFF_WRITE("aof-rewrite-diff-write"), AOF_RENAME("aof-rename"), AOF_WRITE("aof-write"),
    AOF_WRITE_ACTIVE_CHILD("aof-write-active-child"), AOF_WRITE_ALONE("aof-write-alone"),
    AOF_WRITE_PENDING_FSYNC("aof-write-pending-fsync"), COMMAND("command"), EXPIRE_CYCLE("expire-cycle"),
    EVICTION_CYCLE("eviction-cycle"), EVICTION_DEL("eviction-del"), FAST_COMMAND("fast-command"),
    FORK("fork"), RDB_UNLINK_TEMP_FILE("rdb-unlink-temp-file");

    private final byte[] raw;

    private LatencyEvent(String s) {
        raw = SafeEncoder.encode(s);
    }

    @Override
    public byte[] getRaw() {
        return raw;
    }
}
