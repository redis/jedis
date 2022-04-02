package redis.clients.jedis.graph.entities;

import java.util.List;
import java.util.Objects;

/**
 * This class represents a path in the graph.
 */
public final class Path {

    private final List<Node> nodes;
    private final List<Edge> edges;


    /**
     * Parametrized constructor
     * @param nodes - List of nodes.
     * @param edges - List of edges.
     */
    public Path(List<Node> nodes, List<Edge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    /**
     * Returns the nodes of the path.
     * @return List of nodes.
     */
    public List<Node> getNodes() {
        return nodes;
    }

    /**
     * Returns the edges of the path.
     * @return List of edges.
     */
    public List<Edge> getEdges() {
        return edges;
    }

    /**
     * Returns the length of the path - number of edges.
     * @return Number of edges.
     */
    public int length() {
        return edges.size();
    }

    /**
     * Return the number of nodes in the path.
     * @return Number of nodes.
     */
    public int nodeCount(){
        return nodes.size();
    }

    /**
     * Returns the first node in the path.
     * @return First nodes in the path.
     * @throws IndexOutOfBoundsException if the path is empty.
     */
    public Node firstNode(){
        return nodes.get(0);
    }

    /**
     * Returns the last node in the path.
     * @return Last nodes in the path.
     * @throws IndexOutOfBoundsException if the path is empty.
     */
    public Node lastNode(){
        return nodes.get(nodes.size() - 1);
    }

    /**
     * Returns a node with specified index in the path.
     * @return Node.
     * @throws IndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index >= nodesCount()})
     */
    public Node getNode(int index){
        return nodes.get(index);
    }

    /**
     * Returns an edge with specified index in the path.
     * @return Edge.
     * @throws IndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index >= length()})
     */
    public Edge getEdge(int index){
        return edges.get(index);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Path path = (Path) o;
        return Objects.equals(nodes, path.nodes) &&
                Objects.equals(edges, path.edges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodes, edges);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Path{");
        sb.append("nodes=").append(nodes);
        sb.append(", edges=").append(edges);
        sb.append('}');
        return sb.toString();
    }
}
