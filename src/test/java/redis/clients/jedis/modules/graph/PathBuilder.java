package redis.clients.jedis.modules.graph;

import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.graph.entities.Edge;
import redis.clients.jedis.graph.entities.Node;
import redis.clients.jedis.graph.entities.Path;

public final class PathBuilder {

  private final List<Node> nodes;
  private final List<Edge> edges;
  private Class<?> currentAppendClass;

  public PathBuilder() {
    this.nodes = new ArrayList<>(0);
    this.edges = new ArrayList<>(0);
    currentAppendClass = Node.class;
  }

  public PathBuilder(int nodesCount) {
    nodes = new ArrayList<>(nodesCount);
    edges = new ArrayList<>(nodesCount - 1 >= 0 ? nodesCount - 1 : 0);
    currentAppendClass = Node.class;
  }

  public PathBuilder append(Object object) {
    Class<? extends Object> c = object.getClass();
    if (!currentAppendClass.equals(c)) {
      throw new IllegalArgumentException("Path Builder expected " + currentAppendClass.getSimpleName() + " but was " + c.getSimpleName());
    }
    if (c.equals(Node.class)) {
      return appendNode((Node) object);
    } else {
      return appendEdge((Edge) object);
    }
  }

  private PathBuilder appendEdge(Edge edge) {
    edges.add(edge);
    currentAppendClass = Node.class;
    return this;
  }

  private PathBuilder appendNode(Node node) {
    nodes.add(node);
    currentAppendClass = Edge.class;
    return this;
  }

  public Path build() {
    if (nodes.size() != edges.size() + 1) {
      throw new IllegalArgumentException("Path builder nodes count should be edge count + 1");
    }
    return new Path(nodes, edges);
  }
}
