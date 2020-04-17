package org.twc.zerojavacompiler.factsgen;
import java.io.*;

public class Cjump_t extends Fact_t {

	public int ic;
	public String label;

	public Cjump_t(String meth_name, int ic, String label) {
		super(meth_name);
		this.ic = ic;
		this.label = "\"" + label + "\"";
	}

	public void writeRecord(PrintWriter writer, boolean print) {
		String ret = "cjumpInstr(" + this.meth_name + ", " + this.ic + ", " + this.label + ").";
		if (print) System.out.println(ret);
		writer.println(ret);
	}

}
