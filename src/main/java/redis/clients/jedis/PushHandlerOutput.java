package redis.clients.jedis;

public class PushHandlerOutput {
    PushEvent message;
    boolean processed;

    public PushHandlerOutput(PushEvent message, boolean processed) {
        this.message = message;
        this.processed = processed;
    }

    public PushEvent getMessage() {
        return message;
    }

    public boolean isProcessed() {
        return processed;
    }
}
