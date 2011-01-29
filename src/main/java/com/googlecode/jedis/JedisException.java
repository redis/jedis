package com.googlecode.jedis;

import java.io.IOException;

public class JedisException extends RuntimeException {

    private static final long serialVersionUID = -2946266495682282677L;

    public JedisException(final IOException e) {
	super(e);
    }

    public JedisException(final String message) {
	super(message);
    }

    public JedisException(final String message, final Throwable cause) {
	super(message, cause);
    }
}
