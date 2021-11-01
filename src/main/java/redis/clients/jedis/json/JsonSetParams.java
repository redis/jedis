package redis.clients.jedis.json;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;


public class JsonSetParams implements IParams {
    private boolean nx = false;
    private boolean xx = false;

    public JsonSetParams() { }

    public JsonSetParams setNX() {
        this.nx = true;
        this.xx = false;
        return this;
    }

    public JsonSetParams setXX() {
        this.nx = false;
        this.xx = true;
        return this;
    }

    @Override
    public void addParams(CommandArguments args) {
        if (this.nx) {
            args.add("NX");
        }
        if (this.xx) {
            args.add("XX");
        }
    }
}
