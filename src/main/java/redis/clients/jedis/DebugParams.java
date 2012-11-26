package redis.clients.jedis;

import redis.clients.util.SafeEncoder;

public class DebugParams {
    private byte[][] command;

    public byte[][] getCommand() {
	return command;
    }

    private DebugParams() {

    }

    public static DebugParams SEGFAULT() {
	DebugParams debugParams = new DebugParams();
	debugParams.command = new byte[][] { SafeEncoder.encode("SEGFAULT") };
	return debugParams;
    }

    public static DebugParams OBJECT(String key) {
	DebugParams debugParams = new DebugParams();
	debugParams.command = new byte[][] { SafeEncoder.encode("OBJECT"), SafeEncoder.encode(key) };
	return debugParams;
    }
    
    public static DebugParams OBJECT(byte[] key) {
    DebugParams debugParams = new DebugParams();
    debugParams.command = new byte[][] { SafeEncoder.encode("OBJECT"), key };
    return debugParams;
    }

    public static DebugParams RELOAD() {
	DebugParams debugParams = new DebugParams();
	debugParams.command = new byte[][] { SafeEncoder.encode("RELOAD") };
	return debugParams;
    }
}
