package org.twc.minijavacompiler.factsgen;

public class Fact_t {

	public String meth_name;

	public Fact_t(String meth_name) {
		this.meth_name = "\"" + meth_name + "\"";
	}

	public void writerec() {
		// System.out.println("(" + this.meth_name + ").");
	}

}
