package org.twc.zerojavacompiler.spiglet2kanga;

import java.util.*;

public class FlowGraphVertex {
    public int vid;

    // Predecessor, Successor
    public HashSet<FlowGraphVertex> Pred = new HashSet<>();
    public HashSet<FlowGraphVertex> Succ = new HashSet<>();
    // Define x = ...
    // Use ... = x
    public HashSet<Integer> Def = new HashSet<>();
    public HashSet<Integer> Use = new HashSet<>();
    public HashSet<Integer> In = new HashSet<>();
    public HashSet<Integer> Out = new HashSet<>();

    public FlowGraphVertex(int vid) {
        this.vid = vid;
    }

}
