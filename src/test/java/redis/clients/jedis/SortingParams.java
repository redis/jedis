package redis.clients.jedis;

import java.util.ArrayList;
import java.util.List;

public class SortingParams {
    private String pattern = null;
    private List<String> params = new ArrayList<String>();

    public SortingParams by(String pattern) {
        this.pattern = pattern;
        params.add("BY");
        params.add(pattern);
        return this;
    }

    public String[] getParams() {
        return params.toArray(new String[params.size()]);
    }
}
