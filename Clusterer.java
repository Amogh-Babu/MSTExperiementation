import java.util.*;

public class Clusterer {
    private List<List<WeightedEdge<Integer, Double>>> adjList; // the adjacency list of the original graph
    private List<List<WeightedEdge<Integer, Double>>> mstAdjList; // the adjacency list of the minimum spanning tree
    private List<List<Integer>> clusters; // a list of k points, each representing one of the clusters.
    private final int size;
    private double cost; // the distance between the closest pair of clusters

    public Clusterer(double[][] distances, int k) {
        this.size = distances.length;

        // Populating AdjList
        adjList = new ArrayList<>();
        for (int i = 0; i < distances.length; i++) {
            List<WeightedEdge<Integer, Double>> toAdd = new ArrayList<>();
            for (int j = 0; j < distances[i].length; j++) {
                toAdd.add(new WeightedEdge<>(i, j, distances[i][j]));
            }
            adjList.add(toAdd);
        }

        prims(0);
        makeKCluster(k);
    }

    // implement Prim's algorithm to find a MST of the graph.
    // in my implementation I used the mstAdjList field to store this.
    private void prims(int start) {

        // Keeping track of visited nodes using an array list
        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(start, -1, 0, true)); // Add start node

        // Populating tracker with initial distances
        for (int i = 0; i < size; i++) {
            if (i != start) nodes.add(new Node(i, start, adjList.get(start).get(i).weight, false));
        }

        // Sorting to get the next node to analyze
        Collections.sort(nodes);

        // Initializing mstAdjList
        mstAdjList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            mstAdjList.add(new ArrayList<>());
        }

        // Prim's. Maintains this loop until all nodes are known. Tracks using the loop variable
        for (int known = 1; known < size; known ++) {
            Node cur = nodes.get(0);
            cur.known = true;

            // Since this is undirected, both "directions" are added
            mstAdjList.get(cur.prev).add(new WeightedEdge<>(cur.prev, cur.node, cur.dist));
            mstAdjList.get(cur.node).add(new WeightedEdge<>(cur.node, cur.prev, cur.dist));

            // Updating all curs neighbors
            for (Node update : nodes) {
                if (!update.known && adjList.get(cur.node).get(update.node).weight < update.dist) {
                    update.dist = adjList.get(cur.node).get(update.node).weight;
                    update.prev = cur.node;
                }
            }
            Collections.sort(nodes);
        }
    }


    // After making the minimum spanning tree, use this method to
    // remove its k-1 heaviest edges, then assign integers
    // to clusters based on which nodes are still connected by
    // the remaining MST edges.
    private void makeKCluster(int k) {

        // Getting the k-1 edges to remove
        Set<Edge> seenEdges = new HashSet<>();
        PriorityQueue<Edge> topEdges = new PriorityQueue<>();

        // Iterating through all mst edges to get a non-directional edge
        for (int i = 0; i < mstAdjList.size(); i++) {
            for (int j = 0; j < mstAdjList.get(i).size(); j++) {
                WeightedEdge<Integer, Double> weightedEdge = mstAdjList.get(i).get(j);

                // Normalizes the edge/combines 2 into 1. Smaller of the 2 nodes to the bigger of the 2 nodes.
                Edge edge = new Edge(Math.min(weightedEdge.source, weightedEdge.destination),
                                     Math.max(weightedEdge.source, weightedEdge.destination),
                                     weightedEdge.weight);

                // Only update if this edge (or its counterpart) has not been seen
                if (seenEdges.add(edge)) {
                    // Directly adds if the size of the priority queue is less than k-1. If not, removes the lowest
                    // of the existing top k-1 and adds the new edge
                    if (topEdges.size() < k-1) {
                        topEdges.add(edge);
                    } else if (edge.weight > topEdges.peek().weight) {
                        topEdges.poll();
                        topEdges.add(edge);
                    }
                }
            }
        }

        // Convert to hash set for better contains checking
        HashSet<Edge> toRemove = new HashSet<>(topEdges);
        cost = Double.MAX_VALUE;

        // Removing the top k-1 edges from mstAdjList
        for (List<WeightedEdge<Integer, Double>> weightedEdges : mstAdjList) {
            // Goes backwards to account for removals
            for (int j = weightedEdges.size() - 1; j >= 0; j--) {
                WeightedEdge<Integer, Double> weightedEdge = weightedEdges.get(j);

                // Normalizes in the same way
                Edge normalizedEdge = new Edge(Math.min(weightedEdge.source, weightedEdge.destination),
                                               Math.max(weightedEdge.source, weightedEdge.destination),
                                               weightedEdge.weight);

                // If the normalized edge is in toRemove, cost is updated and it is removed
                if (toRemove.contains(normalizedEdge)) {
                    cost = Math.min(cost, weightedEdges.get(j).weight);
                    weightedEdges.remove(j);
                }
            }
        }

        // Index corresponds to node
        boolean[] visited = new boolean[size];
        clusters = new ArrayList<>();

        // Goes through every starting node mstAdjList
        for (int i = 0; i < adjList.size(); i++) {

            // Only does something if it hasn't been visited
            if (!visited[i]) {

                // If this node is by itself (no connections), simply add it to its own list. If not, BFS
                ArrayList<Integer> cluster = new ArrayList<>();
                if (adjList.get(i).isEmpty()) {
                    cluster.add(i);
                } else {
                    // Add initial node to the queue
                    Queue<Integer> edges = new LinkedList<>();
                    edges.add(i);
                    while (!edges.isEmpty()) {
                        int cur = edges.poll();
                        cluster.add(cur);
                        visited[i] = true;

                        // Adds all the neighbors to be analyzed
                        for (WeightedEdge<Integer, Double> neighbor : mstAdjList.get(cur)) {
                            if (!visited[neighbor.destination]) {
                                visited[neighbor.destination] = true;
                                edges.add(neighbor.destination);
                            }
                        }
                    }
                }
                clusters.add(cluster);
            }
        }
    }

    public List<List<Integer>> getClusters() {
        return clusters;
    }

    public double getCost() {
        return cost;
    }

    // Class to represent a Node for the purposes of tracking if it has been visited for Prim's algorithm
    public static class Node implements Comparable<Node> {
        int node;
        int prev;
        double dist;
        boolean known;

        public Node (int node, int prev, double dist, boolean known) {
            this.node = node;
            this.prev = prev;
            this.dist = dist;
            this.known = known;
        }

        public int compareTo(Node other) {
            if (this.known && !other.known) return 1;
            if (!this.known && other.known) return -1;
            return Double.compare(this.dist, other.dist);
        }
    }

    // Class to represent a undirected normalized node.
    public static class Edge implements Comparable<Edge> {
        int node1;
        int node2;
        double weight;

        public Edge (int v1, int v2, double weight) {
            this.node1 = v1;
            this.node2 = v2;
            this.weight = weight;
        }

        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Edge edge)) return false;
            return node1 == edge.node1 &&
                    node2 == edge.node2 &&
                    Double.compare(edge.weight, weight) == 0;
        }

        public int compareTo(Edge other) {
            return Double.compare(this.weight, other.weight);
        }

        public int hashCode() {
            return Objects.hash(node1, node2, weight);
        }
    }

}
