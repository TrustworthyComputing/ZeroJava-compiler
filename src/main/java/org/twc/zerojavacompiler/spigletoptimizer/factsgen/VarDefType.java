package org.twc.zerojavacompiler.spigletoptimizer.factsgen;

import java.io.*;

public class VarDefType extends VariableType {

    public int ic;

    public VarDefType(String meth_name, int ic, String dst) {
        super(meth_name, dst);
        this.ic = ic;
    }

    public void writeRecord(PrintWriter writer, boolean print) {
        String ret = "varDef(" + this.meth_name + ", " + this.ic + ", " + this.var + ").";
        if (print) System.out.println(ret);
        writer.println(ret);
    }

}
