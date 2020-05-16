package org.twc.zerojavacompiler.spigletoptimizer.factsgen;

import java.io.*;

public class JumpType extends FactType {

    public int ic;
    public String label;

    public JumpType(String meth_name, int ic, String label) {
        super(meth_name);
        this.ic = ic;
        this.label = "\"" + label + "\"";
    }

    public void writeRecord(PrintWriter writer, boolean print) {
        String ret = "jumpInstr(" + this.meth_name + ", " + this.ic + ", " + this.label + ").";
        if (print) System.out.println(ret);
        writer.println(ret);
    }

}
