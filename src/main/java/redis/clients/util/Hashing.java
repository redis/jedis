package redis.clients.util;

import java.util.List;
import java.util.TreeMap;

public abstract class Hashing {
    public static final Hashing MURMUR_HASH = new MurmurHash();
    public static final Hashing MD5 = new MD5Hash();
    public static final Hashing KETAMA = new KetamaHash();

    public abstract long hash(String key);

    public abstract long hash(byte[] key);
    
    protected String createContinuumId(final ShardInfo<?> shardInfo, int shardPosition, int repetition) {
        if (shardInfo.getName() == null) {
          return "SHARD-" + shardPosition + "-NODE-" + repetition ;
        }
        return shardInfo.getName() + "*" + shardInfo.getWeight() + repetition;
    }

    protected <S extends ShardInfo<?>> TreeMap<Long, S> createContinuum(List<S> shards) {
        TreeMap<Long, S> nodes = new TreeMap<Long, S>();
        
        for (int shardPosition = 0; shardPosition != shards.size(); ++shardPosition) {
            final S shardInfo = shards.get(shardPosition);
            for (int repetition = 0; repetition < 160 * shardInfo.getWeight(); repetition++) {
                String continuumid = createContinuumId(shardInfo, shardPosition, repetition);
                nodes.put(this.hash(continuumid), shardInfo);
            }
        }
        return nodes;
    }    

}