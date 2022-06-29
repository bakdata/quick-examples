package com.bakdata.quick.profilestore.graph;

public interface WriteableBipartiteGraph extends BipartiteGraph {
    void addEdge(long leftNodeId, long rightNodeId);
}
