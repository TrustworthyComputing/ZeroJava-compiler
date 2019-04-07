package facts_gen;
import java.io.*;

public class Var_t extends dl_t {
	
	public String temp;

	public Var_t(String meth_name, String temp) {
		super(meth_name);
		this.temp = temp;
	}

	public void printrec(PrintWriter writer) {
		String ret = "var(" + this.meth_name + ", " + this.temp + ").";
		System.out.println(ret);
		writer.println(ret);
	}

}
