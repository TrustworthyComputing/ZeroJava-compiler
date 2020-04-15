package org.twc.minijavacompiler.factsgen;
import java.io.*;

public class BinOpMove_t extends Var_t {

	public int ic;
	public String src1;
	public String src2;

	public BinOpMove_t(String meth_name, int ic, String dst, String src1, String src2) {
		super(meth_name, dst);
		this.ic = ic;
		this.src1 = "\"" + src1 + "\"";
		this.src2 = "\"" + src2 + "\"";
	}

	public void writeRecord(PrintWriter writer, boolean print) {
		String ret = "binOpMove(" + this.meth_name + ", " + this.ic + ", " + this.var + ", " + this.src1 + ", " + this.src2 + ").";
		if (print) System.out.println(ret);
		writer.println(ret);
	}

}
