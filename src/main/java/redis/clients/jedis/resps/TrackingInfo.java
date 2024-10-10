package redis.clients.jedis.resps;

import redis.clients.jedis.Builder;
import redis.clients.jedis.util.KeyValue;

import java.util.Collections;
import java.util.List;

import static redis.clients.jedis.BuilderFactory.*;

public class TrackingInfo {

    private final List<String> flags;
    private final long redirect;
    private final List<String> prefixes;

    public TrackingInfo(List<String> flags, long redirect, List<String> prefixes) {
        this.flags = flags;
        this.redirect = redirect;
        this.prefixes = prefixes;
    }


    public List<String> getFlags() {
        return flags;
    }

    public long getRedirect() {
        return redirect;
    }

    public List<String> getPrefixes() {
        return prefixes;
    }

    public static final Builder<TrackingInfo> TRACKING_INFO_BUILDER = new Builder<TrackingInfo>() {
        @Override
        public TrackingInfo build(Object data) {
            List commandData = (List) data;

            if (commandData.get(0) instanceof KeyValue) {
                List<String> flags = Collections.emptyList();
                long redirect = -1;
                List<String> prefixes = Collections.emptyList();

                for (KeyValue kv : (List<KeyValue>) commandData) {
                    switch (STRING.build(kv.getKey())) {
                        case "flags":
                            flags = STRING_LIST.build(kv.getValue());
                            break;
                        case "redirect":
                            redirect = LONG.build(kv.getValue());
                            break;
                        case "prefixes":
                            prefixes = STRING_LIST.build(kv.getValue());
                            break;
                    }
                }

                return new TrackingInfo(flags, redirect, prefixes);
            } else {
                List<String> flags = STRING_LIST.build(commandData.get(1));
                long redirect = LONG.build(commandData.get(3));
                List<String> prefixes = STRING_LIST.build(commandData.get(5));

                return new TrackingInfo(flags, redirect, prefixes);
            }
        }
    };
}
