package facts_gen;
import java.io.*;

public class Constmovee_t extends Var_t {
	
	public int ic;
	public int constsrc;

	public Constmovee_t(String meth_name, int ic, String temp, int constsrc) {
		super(meth_name, temp);
		this.ic = ic;
		this.constsrc = constsrc;
	}

	public void writerec(PrintWriter writer) {
		String ret = "constmove(" + this.meth_name + ", " + this.ic + ", " + this.temp + ", " + this.constsrc + ").";
		// System.out.println(ret);
		writer.println(ret);
	}

}
