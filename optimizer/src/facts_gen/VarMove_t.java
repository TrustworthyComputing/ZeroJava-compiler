package facts_gen;
import java.io.*;

public class VarMove_t extends Var_t {

	public int ic;
	public String src;

	public VarMove_t(String meth_name, int ic, String dst, String src) {
		super(meth_name, dst);
		this.ic = ic;
		this.src = "\"" + src + "\"";
	}

	public void writerec(PrintWriter writer, boolean print) {
		String ret = "varMove(" + this.meth_name + ", " + this.ic + ", " + this.var + ", " + this.src + ").";
		if (print) System.out.println(ret);
		writer.println(ret);
	}

}
