package facts_gen;
import java.io.*;

public class Cjump_t extends dl_t {
	
	public int ic;
	public String label;

	public Cjump_t(String meth_name, int ic, String label) {
		super(meth_name);
		this.ic = ic;
		this.label = label;
	}

	public void printrec(PrintWriter writer) {
		String ret = "cjumpInstr(" + this.meth_name + ", " + this.ic + ", " + this.label + ").";
		System.out.println(ret);
		writer.println(ret);
	}

}
