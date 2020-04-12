package facts_gen;
import java.io.*;

public class AnswerInstruction_t extends dl_t {

	public int ic;
	public String instr;

	public AnswerInstruction_t(String meth_name, int ic, String instr) {
		super(meth_name);
		this.ic = ic;
		this.instr = instr;
	}

	public void writerec(PrintWriter writer, boolean print) {
		String ret = "answerInstruction(" + this.meth_name + ", " + this.ic + ", " + this.instr + ").";
		if (print) System.out.println(ret);
		writer.println(ret);
	}

}
