package org.twc.zerojavacompiler.spigletoptimizer.factsgen;

import java.io.*;

public class InstructionType extends FactType {

    public int ic;
    public String instr;

    public InstructionType(String meth_name, int ic, String instr) {
        super(meth_name);
        this.ic = ic;
        this.instr = "\"" + instr + "\"";
    }

    public void writeRecord(PrintWriter writer, boolean print) {
        String ret = "instruction(" + this.meth_name + ", " + this.ic + ", " + this.instr + ").";
        if (print) System.out.println(ret);
        writer.println(ret);
    }

}
