package redis.clients.jedis;

public class PushHandlerContext {
    private final PushEvent message;
    private boolean processed = false;

    public PushHandlerContext(PushEvent message) {
        this.message = message;
    }

    public PushEvent getMessage() {
        return message;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

}
