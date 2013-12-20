package redis.clients.util;

import java.util.List;
import java.util.TreeMap;

public class KetamaHash extends MD5Hash {

    @Override
    protected String createContinuumId(ShardInfo<?> shardInfo, int shardPosition, int repetition) {
        return shardInfo.getName() + "-" + repetition;
    }

    /**
     * ketama - a consistent hashing algo
     * libketama compatible implementation
     * http://www.last.fm/user/RJ/journal/2007/04/10/rz_libketama_-_a_consistent_hashing_algo_for_memcache_clients
     * 
     * Notice the difference with {@link #createContinuum(List)}. 
     * The repetition is calculated differently. Evenly weighted shards are for ketama max 40 and not 160
     */
    protected <S extends ShardInfo<?>> TreeMap<Long, S> createContinuum(List<S> shards) {
        TreeMap<Long, S> nodes = new TreeMap<Long, S>();

        int totalWeight = 0;
        for (S shardInfo : shards) {
            totalWeight += shardInfo.getWeight() <= 0 ? ShardInfo.DEFAULT_WEIGHT : shardInfo.getWeight();
        }
        
        for (int i = 0; i != shards.size(); ++i) {
            final S shardInfo = shards.get(i);
            int weight = shardInfo.getWeight() <= 0 ? ShardInfo.DEFAULT_WEIGHT : shardInfo.getWeight();
            double factor = Math.floor(((double) (40 * shards.size() * weight)) / (double) totalWeight);
        
            for (int repetition = 0; repetition < factor; repetition++) {
                String continuumid = createContinuumId(shardInfo, i, repetition);
                byte[] d = hashBytes(SafeEncoder.encode(continuumid));
                // MD% returns 16 bytes. Create 4 continuum points out of these bytes
                for (int h = 0; h < 4; h++) {
                    Long k = ((long) (d[3 + h * 4] & 0xFF) << 24)
                     | ((long) (d[2 + h * 4] & 0xFF) << 16)
                     | ((long) (d[1 + h * 4] & 0xFF) << 8)
                     | ((long) (d[0 + h * 4] & 0xFF));
                    nodes.put(k, shardInfo);
                }
            }
        }
        return nodes;
    }

}
