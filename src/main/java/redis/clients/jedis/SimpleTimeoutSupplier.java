package redis.clients.jedis;

class SimpleTimeoutSupplier implements TimeoutSupplier {

    TimeoutCard card;

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

}