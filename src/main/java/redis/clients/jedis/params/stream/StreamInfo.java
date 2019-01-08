package redis.clients.jedis.params.stream;

public class StreamInfo {

    private long length;

    private long radixTreeKeys;

    private long radixTreeNodes;

    private long groups;

    private String lastGeneratedId;

    private StreamParams firstEntry;

    private StreamParams lastEntry;

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getRadixTreeKeys() {
        return radixTreeKeys;
    }

    public void setRadixTreeKeys(long radixTreeKeys) {
        this.radixTreeKeys = radixTreeKeys;
    }

    public long getRadixTreeNodes() {
        return radixTreeNodes;
    }

    public void setRadixTreeNodes(long radixTreeNodes) {
        this.radixTreeNodes = radixTreeNodes;
    }

    public long getGroups() {
        return groups;
    }

    public void setGroups(long groups) {
        this.groups = groups;
    }

    public String getLastGeneratedId() {
        return lastGeneratedId;
    }

    public void setLastGeneratedId(String lastGeneratedId) {
        this.lastGeneratedId = lastGeneratedId;
    }

    public StreamParams getFirstEntry() {
        return firstEntry;
    }

    public void setFirstEntry(StreamParams firstEntry) {
        this.firstEntry = firstEntry;
    }

    public StreamParams getLastEntry() {
        return lastEntry;
    }

    public void setLastEntry(StreamParams lastEntry) {
        this.lastEntry = lastEntry;
    }
}
