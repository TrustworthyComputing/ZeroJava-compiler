package facts_gen;
import java.io.*;

public class Args_t extends dl_t {
	
	public String temp;

	public Args_t(String meth_name, String temp) {
		super(meth_name);
		this.temp = temp;
	}

	public void writerec(PrintWriter writer) {
		String ret = "arg(" + this.meth_name + ", " + this.temp + ").";
		// System.out.println(ret);
		writer.println(ret);
	}

}
