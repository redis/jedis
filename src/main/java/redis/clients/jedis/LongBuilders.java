package redis.clients.jedis;

import java.util.List;
public class LongBuilders {

    public static final Builder<Long> LONG = new Builder<Long>() {
        @Override
        public Long build(Object data) {
            return (Long) data;
        }

        @Override
        public String toString() {
            return "Long";
        }
    };

    public static final Builder<List<Long>> LONG_LIST = new Builder<List<Long>>() {
        @Override
        @SuppressWarnings("unchecked")
        public List<Long> build(Object data) {
            if (null == data) {
                return null;
            }
            return (List<Long>) data;
        }

        @Override
        public String toString() {
            return "List<Long>";
        }
    };
}
