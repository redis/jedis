package redis.clients.jedis;

class SimpleTimeoutSupplier implements TimeoutSupplier {

    final DefaultTimeoutCard card;

    SimpleTimeoutSupplier(DefaultTimeoutCard defaultCard) {
        card = defaultCard;
    }

    public TimeoutCard get() {
        return card;
    }

    public TimeoutCard push(TimeoutInfo info) {
        throw new UnsupportedOperationException();
    }

    public void remove(TimeoutCard card) {
        throw new UnsupportedOperationException();
    }

    public void setDefaults(int timeout, int blockingTimeout) {
        card.set(timeout, blockingTimeout);
    }
}