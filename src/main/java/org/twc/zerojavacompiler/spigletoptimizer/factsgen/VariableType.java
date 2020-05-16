package org.twc.zerojavacompiler.spigletoptimizer.factsgen;

import java.io.*;

public class VariableType extends FactType {

    public String var;

    public VariableType(String meth_name, String var) {
        super(meth_name);
        this.var = "\"" + var + "\"";
    }

    public void writeRecord(PrintWriter writer, boolean print) {
        String ret = "var(" + this.meth_name + ", " + this.var + ").";
        if (print) System.out.println(ret);
        writer.println(ret);
    }

}
