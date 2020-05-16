package org.twc.zerojavacompiler.spigletoptimizer.factsgen;

import java.io.*;

public class VarMoveType extends VariableType {

    public int ic;
    public String src;

    public VarMoveType(String meth_name, int ic, String temp, String src) {
        super(meth_name, temp);
        this.ic = ic;
        this.src = "\"" + src + "\"";
    }

    public void writeRecord(PrintWriter writer, boolean print) {
        String ret = "varMove(" + this.meth_name + ", " + this.ic + ", " + this.var + ", " + this.src + ").";
        if (print) System.out.println(ret);
        writer.println(ret);
    }

}
