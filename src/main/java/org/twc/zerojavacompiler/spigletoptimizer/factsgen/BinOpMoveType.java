package org.twc.zerojavacompiler.spigletoptimizer.factsgen;

import java.io.*;

public class BinOpMoveType extends VariableType {

    public int ic;
    public String tempsrc;

    public BinOpMoveType(String meth_name, int ic, String temp, String tempsrc) {
        super(meth_name, temp);
        this.ic = ic;
        this.tempsrc = "\"" + tempsrc + "\"";
    }

    public void writeRecord(PrintWriter writer, boolean print) {
        String ret = "binOpMove(" + this.meth_name + ", " + this.ic + ", " + this.var + ", " + this.tempsrc + ").";
        if (print) System.out.println(ret);
        writer.println(ret);
    }

}
