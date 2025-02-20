package redis.clients.jedis.params;

import java.util.Objects;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol.Keyword;

public class HSetExParams extends BaseSetExParams<HSetExParams> {

    private Keyword existance;

    public static HSetExParams hSetExParams() {
        return new HSetExParams();
    }

    /**
     * Only set the key if it does not already exist.
     * @return HSetExParams
     */
    public HSetExParams fnx() {
        this.existance = Keyword.FNX;
        return this;
    }

    /**
     * Only set the key if it already exist.
     * @return HSetExParams
     */
    public HSetExParams fxx() {
        this.existance = Keyword.FXX;
        return this;
    }

    @Override
    public void addParams(CommandArguments args) {
        if (existance != null) {
            args.add(existance);
        }

        super.addParams(args);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HSetExParams setParams = (HSetExParams) o;
        return Objects.equals(existance, setParams.existance) && super.equals((BaseSetExParams) o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(existance, super.hashCode());
    }

}