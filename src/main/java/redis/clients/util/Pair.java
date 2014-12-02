package redis.clients.util;

public class Pair<A, B> {

    private final A left;
    private final B right;

    public Pair(final A left, final B right) {
        this.left = left;
        this.right = right;
    }

    public A getLeft() {
        return left;
    }

    public B getRight() {
        return right;
    }

    @Override
    public boolean equals(final Object object) {
        return (object.getClass().equals(Pair.class) && ((Pair) object).getLeft().equals(this.left) && ((Pair) object).getRight().equals(this.right));
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.left != null ? this.left.hashCode() : 0);
        hash = 97 * hash + (this.right != null ? this.right.hashCode() : 0);
        return hash;
    }
}