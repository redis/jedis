package com.googlecode.jedis;

import java.io.ByteArrayInputStream;

/**
 * Test class the fragment a byte array for testing purpose.
 */
public class FragmentedByteArrayInputStream extends ByteArrayInputStream {
    private int readMethodCallCount = 0;

    public FragmentedByteArrayInputStream(final byte[] buf) {
	super(buf);
    }

    public int getReadMethodCallCount() {
	return readMethodCallCount;
    }

    @Override
    public synchronized int read(final byte[] b, final int off, final int len) {
	readMethodCallCount++;
	if (len <= 10) {
	    // if the len <= 10, return as usual ..
	    return super.read(b, off, len);
	} else {
	    // else return the first half ..
	    return super.read(b, off, len / 2);
	}
    }

}
