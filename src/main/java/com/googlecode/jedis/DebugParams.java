package com.googlecode.jedis;

import static com.google.common.base.Charsets.UTF_8;
import static com.googlecode.jedis.Protocol.Keyword.*;

public class DebugParams {

    public static DebugParams OBJECT(byte[] key) {
	DebugParams debugParams = new DebugParams();
	debugParams.command = new byte[][] { OBJECT.raw, key };
	return debugParams;
    }

    public static DebugParams OBJECT(String key) {
	return OBJECT(key.getBytes(UTF_8));
    }

    public static DebugParams RELOAD() {
	DebugParams debugParams = new DebugParams();
	debugParams.command = new byte[][] { RELOAD.raw };
	return debugParams;
    }

    public static DebugParams SEGFAULT() {
	DebugParams debugParams = new DebugParams();
	debugParams.command = new byte[][] { SEGFAULT.raw };
	return debugParams;
    }

    public static DebugParams SWAPIN(byte[] key) {
	DebugParams debugParams = new DebugParams();
	debugParams.command = new byte[][] { SWAPIN.raw, key };
	return debugParams;
    }

    public static DebugParams SWAPIN(String key) {
	return SWAPIN(key.getBytes(UTF_8));
    }

    public static DebugParams SWAPOUT(byte[] key) {
	DebugParams debugParams = new DebugParams();
	debugParams.command = new byte[][] { SWAPOUT.raw, key };
	return debugParams;
    }

    public static DebugParams SWAPOUT(String key) {
	return SWAPOUT(key.getBytes(UTF_8));
    }

    private byte[][] command;

    private DebugParams() {
    }

    public byte[][] getCommand() {
	return command;
    }
}
