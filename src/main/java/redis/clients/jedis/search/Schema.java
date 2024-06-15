package redis.clients.jedis.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.params.IParams;

/**
 * Schema abstracts the schema definition when creating an index. Documents can contain fields not
 * mentioned in the schema, but the index will only index pre-defined fields
 */
public class Schema {

  public enum FieldType {
    TAG,
    TEXT,
    GEO,
    NUMERIC,
    VECTOR
  }

  // public for CommandObjects
  public final List<Field> fields;

  public Schema() {
    this.fields = new ArrayList<>();
  }

  public static Schema from(Field... fields) {
    Schema schema = new Schema();
    for (Field field : fields) {
      schema.addField(field);
    }
    return schema;
  }

  /**
   * Add a text field to the schema with a given weight
   *
   * @param name the field's name
   * @param weight its weight, a positive floating point number
   * @return the schema object
   */
  public Schema addTextField(String name, double weight) {
    fields.add(new TextField(name, weight));
    return this;
  }

  /**
   * Add a text field that can be sorted on
   *
   * @param name the field's name
   * @param weight its weight, a positive floating point number
   * @return the schema object
   */
  public Schema addSortableTextField(String name, double weight) {
    fields.add(new TextField(name, weight, true));
    return this;
  }

  /**
   * Add a geo filtering field to the schema.
   *
   * @param name the field's name
   * @return the schema object
   */
  public Schema addGeoField(String name) {
    fields.add(new Field(name, FieldType.GEO, false));
    return this;
  }

  /**
   * Add a numeric field to the schema
   *
   * @param name the fields's nam e
   * @return the schema object
   */
  public Schema addNumericField(String name) {
    fields.add(new Field(name, FieldType.NUMERIC, false));
    return this;
  }

  /* Add a numeric field that can be sorted on */
  public Schema addSortableNumericField(String name) {
    fields.add(new Field(name, FieldType.NUMERIC, true));
    return this;
  }

  public Schema addTagField(String name) {
    fields.add(new TagField(name));
    return this;
  }

  public Schema addTagField(String name, String separator) {
    fields.add(new TagField(name, separator));
    return this;
  }

  public Schema addTagField(String name, boolean caseSensitive) {
    fields.add(new TagField(name, caseSensitive, false));
    return this;
  }

  public Schema addTagField(String name, String separator, boolean caseSensitive) {
    fields.add(new TagField(name, separator, caseSensitive, false));
    return this;
  }

  public Schema addSortableTagField(String name, String separator) {
    fields.add(new TagField(name, separator, true));
    return this;
  }

  public Schema addSortableTagField(String name, boolean caseSensitive) {
    fields.add(new TagField(name, caseSensitive, true));
    return this;
  }

  public Schema addSortableTagField(String name, String separator, boolean caseSensitive) {
    fields.add(new TagField(name, separator, caseSensitive, true));
    return this;
  }

  public Schema addVectorField(String name, VectorField.VectorAlgo algorithm, Map<String, Object> attributes) {
    fields.add(new VectorField(name, algorithm, attributes));
    return this;
  }

  public Schema addFlatVectorField(String name, Map<String, Object> attributes) {
    fields.add(new VectorField(name, VectorField.VectorAlgo.FLAT, attributes));
    return this;
  }

  public Schema addHNSWVectorField(String name, Map<String, Object> attributes) {
    fields.add(new VectorField(name, VectorField.VectorAlgo.HNSW, attributes));
    return this;
  }

  public Schema addField(Field field) {
    fields.add(field);
    return this;
  }

  /***
   * Chain as name to the last filed added to the schema
   * @param attribute
   */
  // TODO: Not sure about this pattern. May consider removing later.
  public Schema as(String attribute) {
    fields.get(fields.size() - 1).as(attribute);
    return this;
  }

  @Override
  public String toString() {
    return "Schema{fields=" + fields + "}";
  }

  public static class Field implements IParams {

    protected final FieldName fieldName;
    protected final FieldType type;
    protected final boolean sortable;
    protected final boolean noIndex;

    public Field(String name, FieldType type) {
      this(name, type, false, false);
    }

    public Field(String name, FieldType type, boolean sortable) {
      this(name, type, sortable, false);
    }

    public Field(String name, FieldType type, boolean sortable, boolean noindex) {
      this(FieldName.of(name), type, sortable, noindex);
    }

    public Field(FieldName name, FieldType type) {
      this(name, type, false, false);
    }

    public Field(FieldName name, FieldType type, boolean sortable, boolean noIndex) {
      this.fieldName = name;
      this.type = type;
      this.sortable = sortable;
      this.noIndex = noIndex;
    }

    public void as(String attribute){
      this.fieldName.as(attribute);
    }

    @Override
    public final void addParams(CommandArguments args) {
      this.fieldName.addParams(args);
      args.add(type.name());
      addTypeArgs(args);
      if (sortable) {
        args.add("SORTABLE");
      }
      if (noIndex) {
        args.add("NOINDEX");
      }
    }

    /**
     * Subclasses should override this method.
     *
     * @param args
     */
    protected void addTypeArgs(CommandArguments args) { }

    @Override
    public String toString() {
      return "Field{name='" + fieldName + "', type=" + type + ", sortable=" + sortable + ", noindex=" + noIndex + "}";
    }
  }

  /**
   * FullText field spec.
   */
  public static class TextField extends Field {

    private final double weight;
    private final boolean nostem;
    private final String phonetic;

    public TextField(String name) {
      this(name, 1.0);
    }

    public TextField(FieldName name) {
      this(name, 1.0, false, false, false, null);
    }

    public TextField(String name, double weight) {
      this(name, weight, false);
    }

    public TextField(String name, double weight, boolean sortable) {
      this(name, weight, sortable, false);
    }

    public TextField(String name, double weight, boolean sortable, boolean nostem) {
      this(name, weight, sortable, nostem, false);
    }

    public TextField(String name, double weight, boolean sortable, boolean nostem, boolean noindex) {
      this(name, weight, sortable, nostem, noindex, null);
    }

    public TextField(String name, double weight, boolean sortable, boolean nostem, boolean noindex, String phonetic) {
      super(name, FieldType.TEXT, sortable, noindex);
      this.weight = weight;
      this.nostem = nostem;
      this.phonetic = phonetic;
    }

    public TextField(FieldName name, double weight, boolean sortable, boolean nostem, boolean noindex, String phonetic) {
      super(name, FieldType.TEXT, sortable, noindex);
      this.weight = weight;
      this.nostem = nostem;
      this.phonetic = phonetic;
    }

    @Override
    protected void addTypeArgs(CommandArguments args) {
      if (weight != 1.0) {
        args.add("WEIGHT");
        args.add(Double.toString(weight));
      }
      if (nostem) {
        args.add("NOSTEM");
      }
      if (phonetic != null) {
        args.add("PHONETIC");
        args.add(this.phonetic);
      }
    }

    @Override
    public String toString() {
      return "TextField{name='" + fieldName + "', type=" + type + ", sortable=" + sortable + ", noindex=" + noIndex
          + ", weight=" + weight + ", nostem=" + nostem + ", phonetic='" + phonetic + "'}";
    }
  }

  public static class TagField extends Field {

    private final String separator;
    private final boolean caseSensitive;

    public TagField(String name) {
      this(name, null);
    }

    public TagField(String name, String separator) {
      this(name, separator, false);
    }

    public TagField(String name, boolean sortable) {
      this(name, null, sortable);
    }

    public TagField(String name, String separator, boolean sortable) {
      this(name, separator, false, sortable);
    }

    public TagField(String name, boolean caseSensitive, boolean sortable) {
      this(name, null, caseSensitive, sortable);
    }

    public TagField(String name, String separator, boolean caseSensitive, boolean sortable) {
      super(name, FieldType.TAG, sortable);
      this.separator = separator;
      this.caseSensitive = caseSensitive;
    }

    public TagField(FieldName name, String separator, boolean sortable) {
      this(name, separator, false, sortable);
    }

    public TagField(FieldName name, String separator, boolean caseSensitive, boolean sortable) {
      super(name, FieldType.TAG, sortable, false);
      this.separator = separator;
      this.caseSensitive = caseSensitive;
    }

    @Override
    public void addTypeArgs(CommandArguments args) {
      if (separator != null) {
        args.add("SEPARATOR");
        args.add(separator);
      }
      if (caseSensitive) {
        args.add("CASESENSITIVE");
      }
    }

    @Override
    public String toString() {
      return "TagField{name='" + fieldName + "', type=" + type + ", sortable=" + sortable + ", noindex=" + noIndex
          + ", separator='" + separator + ", caseSensitive='" + caseSensitive + "'}";
    }
  }

  public static class VectorField extends Field {

    public enum VectorAlgo {
      FLAT,
      HNSW
    }

    private final VectorAlgo algorithm;
    private final Map<String, Object> attributes;

    public VectorField(String name, VectorAlgo algorithm, Map<String, Object> attributes) {
      super(name, FieldType.VECTOR);
      this.algorithm = algorithm;
      this.attributes = attributes;
    }

    @Override
    public void addTypeArgs(CommandArguments args) {
      args.add(algorithm);
      args.add(attributes.size() << 1);
      for (Map.Entry<String, Object> entry : attributes.entrySet()) {
        args.add(entry.getKey());
        args.add(entry.getValue());
      }
    }

    @Override
    public String toString() {
      return "VectorField{name='" + fieldName + "', type=" + type + ", algorithm=" + algorithm + ", attributes=" + attributes + "}";
    }
  }
}
