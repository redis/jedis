package redis.clients.jedis.resps;

import java.util.List;

public class CommandDocs {
  private final String name;
  private final String summary;
  private final String since;
  private final String group;
  private final String complexity;
  private final List<String> history;

  public CommandDocs(String name, String summary, String since, String group, String complexity, List<String> history) {
    this.name = name;
    this.summary = summary;
    this.since = since;
    this.group = group;
    this.complexity = complexity;
    this.history = history;
  }

  public String getName() {
    return name;
  }

  public String getSummary() {
    return summary;
  }

  public String getSince() {
    return since;
  }

  public String getGroup() {
    return group;
  }

  public String getComplexity() {
    return complexity;
  }

  public List<String> getHistory() {
    return history;
  }
}
