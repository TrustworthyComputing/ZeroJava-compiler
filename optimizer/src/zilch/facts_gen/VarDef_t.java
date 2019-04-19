package facts_gen;
import java.io.*;

public class VarDef_t extends Var_t {
	
	public int ic;

	public VarDef_t(String meth_name, int ic, String dst) {
		super(meth_name, dst);
		this.ic = ic;
	}

	public void writerec(PrintWriter writer, boolean print) {
		String ret = "varDef(" + this.meth_name + ", " + this.ic + ", " + this.var + ").";
		if (print) System.out.println(ret);
		writer.println(ret);
	}

}
