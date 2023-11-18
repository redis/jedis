package redis.clients.jedis;
import java.util.*;

public class DataBuilderFactory {

    public static DataBuilder getBuilder(Object data) {
        if (data instanceof List) {
            return new ListDataBuilder();
        } else if (data instanceof byte[]) {
            return new ByteArrayDataBuilder();
        } else {
            return new DefaultDataBuilder();
        }
    }
}

