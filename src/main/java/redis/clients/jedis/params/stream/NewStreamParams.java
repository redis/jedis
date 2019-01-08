package redis.clients.jedis.params.stream;

public class NewStreamParams {

    private String key;

    private StreamParams entry;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public StreamParams getEntry() {
        return entry;
    }

    public void setEntry(StreamParams entry) {
        this.entry = entry;
    }

    public NewStreamParams(String key, StreamParams entry){
        setKey(key);
        setEntry(entry);
    }
}
