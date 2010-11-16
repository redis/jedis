package redis.clients.jedis;

import java.util.Arrays;

public class Tuple {
    private byte[] element;
    private Double score;

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result;
	if (null != element) {
		for(final byte b : element) {
			result = prime * result + b;
		}
	}
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
	} else if (!Arrays.equals(element, other.element))
	    return false;
	if (Double.doubleToLongBits(score) != Double
		.doubleToLongBits(other.score))
	    return false;
	return true;
    }

    public Tuple(String element, Double score) {
	super();
	this.element = element.getBytes(Protocol.UTF8);
	this.score = score;
    }

    public Tuple(byte[] element, Double score) {
    	super();
    	this.element = element;
    	this.score = score;
        }

    public String getElement() {
    	if(null != element) {
    		return new String(element, Protocol.UTF8);
    	} else {
    		return null;
    	}
    }

    public byte[] getBinaryElement() {
   		return element;
    }

    public double getScore() {
	return score;
    }
    
    public String toString() {
    	return '['+Arrays.toString(element)+','+score+']';
    }
}
