package redis.clients.jedis;

public class Tuple {
    private String element;
    private Double score;

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((element == null) ? 0 : element.hashCode());
	long temp;
	temp = Double.doubleToLongBits(score);
	result = prime * result + (int) (temp ^ (temp >>> 32));
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	Tuple other = (Tuple) obj;
	if (element == null) {
	    if (other.element != null)
		return false;
	} else if (!element.equals(other.element))
	    return false;
	if (Double.doubleToLongBits(score) != Double
		.doubleToLongBits(other.score))
	    return false;
	return true;
    }

    public Tuple(String element, Double score) {
	super();
	this.element = element;
	this.score = score;
    }

    public String getElement() {
	return element;
    }

    public double getScore() {
	return score;
    }
}
