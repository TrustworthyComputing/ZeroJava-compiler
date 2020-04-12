package facts_gen;
import java.io.*;

public class ConstMove_t extends Var_t {
	
	public int ic;
	public int constsrc;

	public ConstMove_t(String meth_name, int ic, String dst, int constsrc) {
		super(meth_name, dst);
		this.ic = ic;
		this.constsrc = constsrc;
	}

	public void writerec(PrintWriter writer, boolean print) {
		String ret = "constmove(" + this.meth_name + ", " + this.ic + ", " + this.var + ", " + this.constsrc + ").";
		if (print) System.out.println(ret);
		writer.println(ret);
	}

}
