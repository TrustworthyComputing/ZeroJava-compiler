package facts_gen;
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

	public void writerec(PrintWriter writer, boolean print) {
		String ret = "binOpmove(" + this.meth_name + ", " + this.ic + ", " + this.var + ", " + this.src1 + ", " + this.src2 + ").";
		if (print) System.out.println(ret);
		writer.println(ret);
	}

}
