package redis.clients.jedis.timeseries;

import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.LATEST;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.SELECTED_LABELS;
import static redis.clients.jedis.timeseries.TimeSeriesProtocol.TimeSeriesKeyword.WITHLABELS;

import java.util.Arrays;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;

/**
 * Represents optional arguments of TS.MGET command.
 */
public class TSMGetParams implements IParams {

  private boolean latest;

  private boolean withLabels;
  private String[] selectedLabels;

  public static TSMGetParams multiGetParams() {
    return new TSMGetParams();
  }

  public TSMGetParams latest() {
    this.latest = true;
    return this;
  }

  public TSMGetParams withLabels(boolean withLabels) {
    this.withLabels = withLabels;
    return this;
  }

  public TSMGetParams withLabels() {
    return withLabels(true);
  }

  public TSMGetParams selectedLabels(String... labels) {
    this.selectedLabels = labels;
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    if (latest) {
      args.add(LATEST);
    }

    if (withLabels) {
      args.add(WITHLABELS);
    } else if (selectedLabels != null) {
      args.add(SELECTED_LABELS);
      for (String label : selectedLabels) {
        args.add(label);
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TSMGetParams that = (TSMGetParams) o;
    return latest == that.latest && withLabels == that.withLabels &&
        Arrays.equals(selectedLabels, that.selectedLabels);
  }

  @Override
  public int hashCode() {
    int result = Boolean.hashCode(latest);
    result = 31 * result + Boolean.hashCode(withLabels);
    result = 31 * result + Arrays.hashCode(selectedLabels);
    return result;
  }
}
