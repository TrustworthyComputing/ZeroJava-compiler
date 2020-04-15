package org.twc.minijavacompiler.factsgen;
import java.io.*;

public class Var_t extends Fact_t {

	public String var;

	public Var_t(String meth_name, String var) {
		super(meth_name);
		this.var = "\"" + var + "\"";
	}

	public void writeRecord(PrintWriter writer, boolean print) {
		String ret = "var(" + this.meth_name + ", " + this.var + ").";
		if (print) System.out.println(ret);
		writer.println(ret);
	}

}
