package facts_gen;
import java.io.*;

public class Next_t extends dl_t {
	
	public int instr_now;
	public int instr_next;

	public Next_t(String meth_name, int instr_now, int instr_next) {
		super(meth_name);
		this.instr_now = instr_now;
		this.instr_next = instr_next;
	}

	public void writerec(PrintWriter writer) {
		String ret = "next(" + this.meth_name + ", " + this.instr_now + ", " + this.instr_next + ").";
		// System.out.println(ret);
		writer.println(ret);
	}

}
