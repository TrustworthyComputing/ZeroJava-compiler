package org.twc.zerojavacompiler.spiglet2kanga;

public class LiveInterval implements Comparable<LiveInterval> {
    public int begin, end;
    // S or T
    public boolean S;
    public int tempNo;

    public LiveInterval(int tempNo, int begin, int end) {
        this.S = false;
        this.begin = begin;
        this.end = end;
        this.tempNo = tempNo;
    }

    public int compareTo(LiveInterval another) {
        // compare Interval [begin, end]
        if (begin == another.begin) {
            return end - another.end;
        } else {
            return begin - another.begin;
        }
    }
}
