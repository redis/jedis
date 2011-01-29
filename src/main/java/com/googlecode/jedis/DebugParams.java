package com.googlecode.jedis;

import static com.google.common.base.Charsets.UTF_8;
import static com.googlecode.jedis.Protocol.Keyword.*;

/**
 * Helper class for use with {@link Jedis#debug(DebugParams)}.
 * 
 * @author Jonathan Leibiusky
 * @author Moritz Heuser <moritz.heuser@gmail.com>
 * 
 */
public final class DebugParams {

    public static DebugParams OBJECT(final byte[] key) {
	final DebugParams debugParams = new DebugParams();
	debugParams.command = new byte[][] { OBJECT.raw, key };
	return debugParams;
    }

    public static DebugParams OBJECT(final String key) {
	return OBJECT(key.getBytes(UTF_8));
    }

    public static DebugParams RELOAD() {
	final DebugParams debugParams = new DebugParams();
	debugParams.command = new byte[][] { RELOAD.raw };
	return debugParams;
    }

    public static DebugParams SEGFAULT() {
	final DebugParams debugParams = new DebugParams();
	debugParams.command = new byte[][] { SEGFAULT.raw };
	return debugParams;
    }

    public static DebugParams SWAPIN(final byte[] key) {
	final DebugParams debugParams = new DebugParams();
	debugParams.command = new byte[][] { SWAPIN.raw, key };
	return debugParams;
    }

    public static DebugParams SWAPIN(final String key) {
	return SWAPIN(key.getBytes(UTF_8));
    }

    public static DebugParams SWAPOUT(final byte[] key) {
	final DebugParams debugParams = new DebugParams();
	debugParams.command = new byte[][] { SWAPOUT.raw, key };
	return debugParams;
    }

    public static DebugParams SWAPOUT(final String key) {
	return SWAPOUT(key.getBytes(UTF_8));
    }

    private byte[][] command;

    private DebugParams() {
    }

    public byte[][] getCommand() {
	return command;
    }
}
