package redis.clients.jedis.params.stream;

import redis.clients.jedis.params.Params;

public class StreamParams extends Params {

    private String entryId;

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public void addPair(String field, String value) {
        addParam(field, value);
    }
}