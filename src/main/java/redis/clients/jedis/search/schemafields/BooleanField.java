package redis.clients.jedis.search.schemafields;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.search.FieldName;

public class BooleanField extends SchemaField {

  private boolean isMissing;
  private boolean isEmpty;
  private boolean isNull;

  private List<String> trueFlags;
  private List<String> falseFlags;

  public BooleanField(String fieldName) {
    super(fieldName);
  }

  public BooleanField(FieldName fieldName) {
    super(fieldName);
  }

  public static BooleanField of(String fieldName) {
    return new BooleanField(fieldName);
  }

  public static BooleanField of(FieldName fieldName) {
    return new BooleanField(fieldName);
  }

  @Override
  public BooleanField as(String attribute) {
    super.as(attribute);
    return this;
  }

  public BooleanField isMissing() {
    this.isMissing = true;
    return this;
  }

  public BooleanField isEmpty() {
    this.isEmpty = true;
    return this;
  }

  public BooleanField isNull() {
    this.isNull = true;
    return this;
  }

  public BooleanField trueFlags(String... trueFlags) {
    this.trueFlags = new ArrayList<>(Arrays.asList(trueFlags));
    return this;
  }

  public BooleanField addTrueFlag(String trueFlag) {
    if (this.trueFlags == null) this.trueFlags = new LinkedList<>();
    this.trueFlags.add(trueFlag);
    return this;
  }

  public BooleanField falseFlags(String... falseFlags) {
    this.falseFlags = new ArrayList<>(Arrays.asList(falseFlags));
    return this;
  }

  public BooleanField addFalseFlag(String falseFlag) {
    if (this.falseFlags == null) this.falseFlags = new LinkedList<>();
    this.falseFlags.add(falseFlag);
    return this;
  }

  @Override
  public void addParams(CommandArguments args) {
    args.addParams(fieldName);
    args.add(BOOLEAN);

    if (isMissing) args.add(ISMISSING);
    if (isEmpty) args.add(ISEMPTY);
    if (isNull) args.add(ISNULL);

    if (trueFlags == null && falseFlags == null) {
      args.add(0); // count
    } else {
      if (trueFlags == null) trueFlags = Collections.emptyList();
      if (falseFlags == null) falseFlags = Collections.emptyList();

      args.add(4 + trueFlags.size() + falseFlags.size()); // count

      args.add(TRUEFLAG).add(trueFlags.size());
      trueFlags.forEach(args::add);

      args.add(FALSEFLAG).add(falseFlags.size());
      falseFlags.forEach(args::add);
    }
  }
}
