package redis.clients.jedis.graph.entities;

import java.util.Objects;

/**
 * A class represent an edge (graph entity). In addition to the base class id and properties, an edge shows its source,
 * destination and relationship type
 */
public class Edge extends GraphEntity {

    //members
    private String relationshipType;
    private long source;
    private long destination;

    public Edge() {
        super();
    }

    /**
     * Use this constructor to reduce memory allocations
     * when properties are added to the edge
     * @param propertiesCapacity preallocate the capacity for the properties
     */
    public Edge(int propertiesCapacity) {
        super(propertiesCapacity);
    }
    //getters & setters

    /**
     * @return the edge relationship type
     */
    public String getRelationshipType() {
        return relationshipType;
    }

    /**
     * @param relationshipType - the relationship type to be set.
     */
    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }


    /**
     * @return The id of the source node
     */
    public long getSource() {
        return source;
    }

    /**
     * @param source - The id of the source node to be set
     */
    public void setSource(long source) {
        this.source = source;
    }

    /**
     *
     * @return the id of the destination node
     */
    public long getDestination() {
        return destination;
    }

    /**
     *
     * @param destination - The id of the destination node to be set
     */
    public void setDestination(long destination) {
        this.destination = destination;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Edge)) return false;
        if (!super.equals(o)) return false;
        Edge edge = (Edge) o;
        return source == edge.source &&
                destination == edge.destination &&
                Objects.equals(relationshipType, edge.relationshipType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), relationshipType, source, destination);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Edge{");
        sb.append("relationshipType='").append(relationshipType).append('\'');
        sb.append(", source=").append(source);
        sb.append(", destination=").append(destination);
        sb.append(", id=").append(id);
        sb.append(", propertyMap=").append(propertyMap);
        sb.append('}');
        return sb.toString();
    }
}
