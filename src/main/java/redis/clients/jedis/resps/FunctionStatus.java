package redis.clients.jedis.resps;

import java.util.List;

public class FunctionStatus {

    public static class ScriptInfo {
        private final String name;
        private final List<String> command;
        private final long duration;

        public ScriptInfo(String name, List<String> command, long duration) {
            this.name = name;
            this.command = command;
            this.duration = duration;
        }

        public String getName() {
            return name;
        }

        public List<String> getCommand() {
            return command;
        }

        public long getDurationMs() {
            return duration;
        }
    }

    private final ScriptInfo script;
    private final List<String> engines;

    public FunctionStatus(ScriptInfo script, List<String> engines) {
        this.script = script;
        this.engines = engines;
    }

    public ScriptInfo getScript() {
        return script;
    }

    public List<String> getEngines() {
        return engines;
    }
}
