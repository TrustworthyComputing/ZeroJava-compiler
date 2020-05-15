package org.twc.zerojavacompiler.spiglet2kanga;

import java.util.*;

public class FlowGraph {
    public Vector<FlowGraphVertex> vVertex = new Vector<>();
    public HashMap<Integer, FlowGraphVertex> mVertex = new HashMap<>();
    public HashSet<Integer> callPos = new HashSet<>();

    public FlowGraphVertex getVertex(int vid) {
        return mVertex.get(vid);
    }

    public void addVertex(int vid) {
        FlowGraphVertex v = new FlowGraphVertex(vid);
        this.vVertex.add(v);
        this.mVertex.put(vid, v);
    }

    public void addEdge(int src_id, int dst_id) {
        FlowGraphVertex src = mVertex.get(src_id);
        FlowGraphVertex dst = mVertex.get(dst_id);
        src.Succ.add(dst);
        dst.Pred.add(src);
    }

}
