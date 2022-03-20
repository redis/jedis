package redis.clients.jedis.modules.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import redis.clients.jedis.graph.entities.Edge;
import redis.clients.jedis.graph.entities.Node;
import redis.clients.jedis.graph.entities.Path;

public class PathTest {

  private Node buildNode(int id) {
    Node n = new Node();
    n.setId(0);
    return n;
  }

  private Edge buildEdge(int id, int src, int dst) {
    Edge e = new Edge();
    e.setId(id);
    e.setSource(src);
    e.setDestination(dst);
    return e;
  }

  private List<Node> buildNodeArray(int size) {
    return IntStream.range(0, size).mapToObj(i -> buildNode(i)).collect(Collectors.toList());
  }

  private List<Edge> buildEdgeArray(int size) {
    return IntStream.range(0, size).mapToObj(i -> buildEdge(i, i, i + 1)).collect(Collectors.toList());
  }

  private Path buildPath(int nodeCount) {
    return new Path(buildNodeArray(nodeCount), buildEdgeArray(nodeCount - 1));
  }

  @Test
  public void testEmptyPath() {
    Path path = buildPath(0);
    assertEquals(0, path.length());
    assertEquals(0, path.nodeCount());
    assertThrows(IndexOutOfBoundsException.class, () -> path.getNode(0));
    assertThrows(IndexOutOfBoundsException.class, () -> path.getEdge(0));
  }

  @Test
  public void testSingleNodePath() {
    Path path = buildPath(1);
    assertEquals(0, path.length());
    assertEquals(1, path.nodeCount());
    Node n = new Node();
    n.setId(0);
    assertEquals(n, path.firstNode());
    assertEquals(n, path.lastNode());
    assertEquals(n, path.getNode(0));
  }

  @Test
  public void testRandomLengthPath() {
    int nodeCount = ThreadLocalRandom.current().nextInt(2, 100 + 1);
    Path path = buildPath(nodeCount);
    assertEquals(buildNodeArray(nodeCount), path.getNodes());
    assertEquals(buildEdgeArray(nodeCount - 1), path.getEdges());
    path.getEdge(0);
  }
}
