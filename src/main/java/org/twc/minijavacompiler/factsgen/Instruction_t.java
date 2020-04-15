package org.twc.minijavacompiler.factsgen;
import java.io.*;

public class Instruction_t extends Fact_t {

	public int ic;
	public String instr;

	public Instruction_t(String meth_name, int ic, String instr) {
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
