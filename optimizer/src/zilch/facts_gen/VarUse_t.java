package facts_gen;
import java.io.*;

public class VarUse_t extends Var_t {
	
	public int ic;

	public VarUse_t(String meth_name, int ic, String src) {
		super(meth_name, src);
		this.ic = ic;
	}

	public void writerec(PrintWriter writer, boolean print) {
		String ret = "varUse(" + this.meth_name + ", " + this.ic + ", " + this.var + ").";
		if (print) System.out.println(ret);
		writer.println(ret);
	}

}
