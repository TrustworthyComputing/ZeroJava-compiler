package org.twc.zerojavacompiler.spigletoptimizer.factsgen;

import java.io.*;

public class VarUseType extends VariableType {

    public int ic;

    public VarUseType(String meth_name, int ic, String src) {
        super(meth_name, src);
        this.ic = ic;
    }

    public void writeRecord(PrintWriter writer, boolean print) {
        String ret = "varUse(" + this.meth_name + ", " + this.ic + ", " + this.var + ").";
        if (print) System.out.println(ret);
        writer.println(ret);
    }

}
