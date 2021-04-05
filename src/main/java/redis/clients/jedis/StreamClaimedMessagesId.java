package redis.clients.jedis;

import java.io.Serializable;
import java.util.List;

public class StreamClaimedMessagesId implements Serializable {

    private static final long serialVersionUID = 1L;

    private final StreamEntryID id;
    private final List<StreamEntryID> streamEntryIDs;

    public StreamClaimedMessagesId(StreamEntryID id, List<StreamEntryID> streamEntryIDs) {
        this.id = id;
        this.streamEntryIDs = streamEntryIDs;
    }

    public StreamEntryID getId() {
        return id;
    }

    public List<StreamEntryID> getStreamEntryIDs() {
        return streamEntryIDs;
    }

}
