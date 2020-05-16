package org.twc.zerojavacompiler.spigletoptimizer.factsgen;

import java.io.*;

public class ConstMoveType extends VariableType {

    public int ic;
    public int constsrc;

    public ConstMoveType(String meth_name, int ic, String dst, int constsrc) {
        super(meth_name, dst);
        this.ic = ic;
        this.constsrc = constsrc;
    }

    public void writeRecord(PrintWriter writer, boolean print) {
        String ret = "constMove(" + this.meth_name + ", " + this.ic + ", " + this.var + ", " + this.constsrc + ").";
        if (print) System.out.println(ret);
        writer.println(ret);
    }

}
