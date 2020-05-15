package org.twc.zerojavacompiler.spiglet2kanga;

import org.twc.zerojavacompiler.basetype.Method_t;

import java.util.*;

public class Temp2Reg {

    HashMap<String, Method_t> method_map_;
    FlowGraph currFlowGraph;
    Method_t currMethod;

    public Temp2Reg(HashMap<String, Method_t> method_map_) {
        this.method_map_ = method_map_;
    }

    private void LiveAnalyze() {
        boolean notOver = true;
        int size = currFlowGraph.vVertex.size();
        // Iterate
        while (notOver) {
            notOver = false;
            for (int currVid = size - 1; currVid >= 0; currVid--) {
                // System.out.println(currVid);
                FlowGraphVertex currVertex = currFlowGraph.mVertex.get(currVid);
                // System.out.println(currVertex.toString());
                for (FlowGraphVertex nextVertex : currVertex.Succ)
                    currVertex.Out.addAll(nextVertex.In);

                HashSet<Integer> newIn = new HashSet<>();
                // 'Out' - 'Def' + 'Use'
                newIn.addAll(currVertex.Out);
                newIn.removeAll(currVertex.Def);
                newIn.addAll(currVertex.Use);
                // 'In' changes, iteration not over
                if (!currVertex.In.equals(newIn)) {
                    currVertex.In = newIn;
                    notOver = true;
                }
            }
        }
    }

    private void GetLiveInterval() {
        int size = currFlowGraph.mVertex.size();

        // update interval 'end'
        for (int vid = 0; vid < size; vid++) {
            FlowGraphVertex currVertex = currFlowGraph.mVertex.get(vid);
            for (Integer tempNo : currVertex.In)
                currMethod.temp_reg_intervals.get(tempNo).end = vid;
            for (Integer tempNo : currVertex.Out)
                currMethod.temp_reg_intervals.get(tempNo).end = vid;
        }

        for (LiveInterval interval : currMethod.temp_reg_intervals.values()) {
            for (int callPos : currFlowGraph.callPos) {
                // across a method call, better use callee-saved regS
                if (interval.begin < callPos && interval.end > callPos) {
                    interval.S = true;
                    break;
                }
            }
        }
    }

    public void LinearScan() {
        for (Method_t method : method_map_.values()) {
            // System.out.println(method.methodName);
            currMethod = method;
            currFlowGraph = currMethod.flowGraph;
            LiveAnalyze();
            GetLiveInterval();

            // sort the intervals by [begin, end]
            ArrayList<LiveInterval> intervals = new ArrayList<>();
            intervals.addAll(currMethod.temp_reg_intervals.values());
            Collections.sort(intervals);

            LiveInterval[] Tinterval = new LiveInterval[10];
            LiveInterval[] Sinterval = new LiveInterval[8];
            for (LiveInterval interval : intervals) {
                // last: the reg contains interval which ends last
                // empty: empty reg
                int lastT = -1, lastS = -1, emptyT = -1, emptyS = -1;
                // analyze t0-t9
                for (int regIdx = 9; regIdx >= 0; regIdx--) {
                    if (Tinterval[regIdx] != null) {
                        // not empty
                        if (Tinterval[regIdx].end <= interval.begin) {
                            // interval already ends
                            currMethod.temp_regs_map.put("TEMP " + Tinterval[regIdx].tempNo, "t" + regIdx);
                            Tinterval[regIdx] = null;
                            emptyT = regIdx;
                        } else {
                            if (lastT == -1 || Tinterval[regIdx].end > Tinterval[lastT].end)
                                lastT = regIdx;
                        }
                    } else {
                        emptyT = regIdx;
                    }
                }
                // analyze s0-s7
                for (int regIdx = 7; regIdx >= 0; regIdx--) {
                    if (Sinterval[regIdx] != null) {
                        if (Sinterval[regIdx].end <= interval.begin) {
                            currMethod.save_regs_map.put("TEMP " + Sinterval[regIdx].tempNo, "s" + regIdx);
                            Sinterval[regIdx] = null;
                            emptyS = regIdx;
                        } else {
                            if (lastS == -1 || Sinterval[regIdx].end > Sinterval[lastS].end)
                                lastS = regIdx;
                        }
                    } else {
                        emptyS = regIdx;
                    }
                }
                // first assign T
                if (!interval.S) {
                    if (emptyT != -1) {
                        // assign empty T to interval
                        Tinterval[emptyT] = interval;
                        interval = null;
                    } else {
                        // swap with the last T
                        if (interval.end < Tinterval[lastT].end) {
                            LiveInterval swapTmp = Tinterval[lastT];
                            Tinterval[lastT] = interval;
                            interval = swapTmp;
                        }
                    }
                }
                // then assign S
                if (interval != null) {
                    if (emptyS != -1) {
                        Sinterval[emptyS] = interval;
                        interval = null;
                    } else {
                        if (interval.end < Sinterval[lastS].end) {
                            LiveInterval swapTmp = Sinterval[lastS];
                            Sinterval[lastS] = interval;
                            interval = swapTmp;
                        }
                    }
                }
                // if not assigned, spill it
                if (interval != null)
                    currMethod.spilled_regs_map.put("TEMP " + interval.tempNo, "");
            }
            for (int idx = 0; idx < 10; idx++) {
                if (Tinterval[idx] != null)
                    currMethod.temp_regs_map.put("TEMP " + Tinterval[idx].tempNo, "t" + idx);
            }
            for (int idx = 0; idx < 8; idx++) {
                if (Sinterval[idx] != null)
                    currMethod.save_regs_map.put("TEMP " + Sinterval[idx].tempNo, "s" + idx);
            }
            // calculate stackNum:
            // contains params(>4), spilled regs, callee-saved S
            int stackIdx = (currMethod.getNum_parameters_() > 4 ? currMethod.getNum_parameters_() - 4 : 0) + currMethod.save_regs_map.size();
            for (String temp : currMethod.spilled_regs_map.keySet()) {
                currMethod.spilled_regs_map.put(temp, "SPILLEDARG " + stackIdx);
                stackIdx++;
            }
            currMethod.setStack_num_(stackIdx);
        }
    }

}