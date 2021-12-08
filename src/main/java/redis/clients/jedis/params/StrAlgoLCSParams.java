package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;

public class StrAlgoLCSParams extends Params implements IParams {

    private static final String IDX = "idx";
    private static final String LEN = "len";
    private static final String WITHMATCHLEN = "withmatchlen";
    private static final String MINMATCHLEN = "minmatchlen";

    public StrAlgoLCSParams() {
    }

    public static StrAlgoLCSParams StrAlgoLCSParams() {
        return new StrAlgoLCSParams();
    }

    /**
     * When IDX is given the command returns an array with the LCS length
     * and all the ranges in both the strings, start and end offset for
     * each string, where there are matches.
     * @return StrAlgoParams
     */
    public StrAlgoLCSParams idx() {
        addParam(IDX);
        return this;
    }

    /**
     * When LEN is given the command returns the length of the longest common substring.
     * @return StrAlgoParams
     */
    public StrAlgoLCSParams len() {
        addParam(LEN);
        return this;
    }

    /**
     * When WITHMATCHLEN is given each array representing a match will also have the length of the match.
     * @return StrAlgoParams
     */
    public StrAlgoLCSParams withMatchLen() {
        addParam(WITHMATCHLEN);
        return this;
    }

    /**
     * Specify the minimum match length.
     * @return StrAlgoParams
     */
    public StrAlgoLCSParams minMatchLen(long minMatchLen) {
        addParam(MINMATCHLEN, minMatchLen);
        return this;
    }

    @Override
    public void addParams(CommandArguments args) {
        if (contains(IDX)) {
            args.add(IDX);
        }
        if (contains(LEN)) {
            args.add(LEN);
        }
        if (contains(WITHMATCHLEN)) {
            args.add(WITHMATCHLEN);
        }

        if (contains(MINMATCHLEN)) {
            args.add(MINMATCHLEN);
            args.add(Protocol.toByteArray((long) getParam(MINMATCHLEN)));
        }
    }
}
