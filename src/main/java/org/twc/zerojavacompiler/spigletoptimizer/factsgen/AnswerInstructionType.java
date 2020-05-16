package org.twc.zerojavacompiler.spigletoptimizer.factsgen;

import java.io.*;

public class AnswerInstructionType extends FactType {

    public int ic;
    public String instr;

    public AnswerInstructionType(String meth_name, int ic, String instr) {
        super(meth_name);
        this.ic = ic;
        this.instr = "\"" + instr + "\"";
    }

    public void writeRecord(PrintWriter writer, boolean print) {
        String ret = "answerInstruction(" + this.meth_name + ", " + this.ic + ", " + this.instr + ").";
        if (print) System.out.println(ret);
        writer.println(ret);
    }

}
