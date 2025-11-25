import java.util.*;

public class Clusterer {
    private List<List<WeightedEdge<Integer, Double>>> adjList; // the adjacency list of the original graph
    private List<List<WeightedEdge<Integer, Double>>> mstAdjList; // the adjacency list of the minimum spanning tree
    private List<List<Integer>> clusters; // a list of k points, each representing one of the clusters.
    private final int size;
    private double cost; // the distance between the closest pair of clusters

    public Clusterer(double[][] distances, int k) {
        this.size = distances.length;

        adjList = new ArrayList<>();
        for (int i = 0; i < distances.length; i++) {
            List<WeightedEdge<Integer, Double>> toAdd = new ArrayList<>();
            for (int j = 0; j < distances[i].length; j++) {
                toAdd.add(new WeightedEdge<>(i, j, distances[i][j]));
            }
            adjList.add(toAdd);
        }
        prims(0);
        System.out.println(mstAdjList);

        makeKCluster(k);
    }

    // implement Prim's algorithm to find a MST of the graph.
    // in my implementation I used the mstAdjList field to store this.
    private void prims(int start) {

        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(start, -1, 0, true)); // Add start node

        for (int i = 0; i < size; i++) {
            if (i != start) nodes.add(new Node(i, start, adjList.get(start).get(i).weight, false));
        }
        Collections.sort(nodes);

        mstAdjList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            mstAdjList.add(new ArrayList<>());
        }

        for (int known = 1; known < size; known ++) {
            Node cur = nodes.get(0);
            cur.known = true;

            mstAdjList.get(cur.prev).add(new WeightedEdge<>(cur.prev, cur.node, cur.dist));
            mstAdjList.get(cur.node).add(new WeightedEdge<>(cur.node, cur.prev, cur.dist));


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

        PriorityQueue<WeightedEdge> topEdges = new PriorityQueue<>();

        System.out.println(mstAdjList);

        for (int i = 0; i < mstAdjList.size(); i++) {
            for (int j = 0; j < mstAdjList.get(i).size(); j++) {
                if (topEdges.size() < k-1) {
                    topEdges.add(mstAdjList.get(i).get(j));
                } else if (mstAdjList.get(i).get(j).compareTo(topEdges.peek()) > 0) {
                    topEdges.poll();
                    topEdges.add(mstAdjList.get(i).get(j));
                }
            }
        }


        HashSet<WeightedEdge> toRemove = new HashSet<>(topEdges);
        cost = Double.MAX_VALUE;
        for (List<WeightedEdge<Integer, Double>> weightedEdges : mstAdjList) {
            for (int j = weightedEdges.size() - 1; j >= 0; j--) {
                if (toRemove.contains(weightedEdges.get(j))) {
                    cost = Math.min(cost, weightedEdges.get(j).weight);
                    weightedEdges.remove(j);
                }
            }
        }

        System.out.println(mstAdjList);


        boolean[] visited = new boolean[size];
        int visitedCounter = 0;
        ArrayList<Integer> cluster = new ArrayList<>();
        Queue<WeightedEdge<Integer, Double>> edges = new LinkedList<>();



        while(visitedCounter < size) {
            if (visited[visitedCounter] )

                while (!edges.isEmpty()) {
                    WeightedEdge<Integer, Double> cur = edges.poll();
                    cluster.add(cur.source);

                    for (WeightedEdge<Integer, Double> neighbor : mstAdjList.get(cur.source)) {
                        if (!visited[neighbor.source]) {
                            visited[neighbor.source] = true;
                            visitedCounter++;
                            edges.add(neighbor);
                        }
                    }
                }
        }
    }

    public List<List<Integer>> getClusters() {
        return clusters;
    }

    public double getCost() {
        return cost;
    }

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

        public String toString() {
            return "Node " + node + " from " + prev + " over a dist of " + dist + " is known? " + known;
        }
    }

}
