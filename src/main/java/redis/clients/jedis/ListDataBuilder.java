package redis.clients.jedis;
import java.util.List;
import java.util.stream.Collectors;
import redis.clients.jedis.util.KeyValue;
public class ListDataBuilder implements DataBuilder {

    @Override
    public Object build(Object data) {
        if (!(data instanceof List)) {
            throw new IllegalArgumentException("Data must be of type List");
        }
        List<?> list = (List<?>) data;
        if (list.isEmpty()) {
            return list;
        }
        if (list.get(0) instanceof KeyValue) {
            return list.stream()
                    .filter(kv -> kv != null && ((KeyValue<?, ?>) kv).getKey() != null)
                    .collect(Collectors.toMap(
                            kv -> BuilderFactory.STRING.build(((KeyValue<?, ?>) kv).getKey()),
                            kv -> BuilderFactory.STRING.build(((KeyValue<?, ?>) kv).getValue())
                    ));
        } else {
            return list.stream()
                    .map(item -> BuilderFactory.STRING.build(item))
                    .collect(Collectors.toList());
        }
    }
}
