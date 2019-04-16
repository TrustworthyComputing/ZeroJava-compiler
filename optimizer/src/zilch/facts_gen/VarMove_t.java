package facts_gen;
import java.io.*;

public class VarMove_t extends Var_t {
	
	public int ic;
	public String tempsrc;

	public VarMove_t(String meth_name, int ic, String temp, String tempsrc) {
		super(meth_name, temp);
		this.ic = ic;
		this.tempsrc = tempsrc;
	}

	public void writerec(PrintWriter writer, boolean print) {
		String ret = "varMove(" + this.meth_name + ", " + this.ic + ", " + this.temp + ", " + this.tempsrc + ").";
		if (print) System.out.println(ret);
		writer.println(ret);
	}

}
