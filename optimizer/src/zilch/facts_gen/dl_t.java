package facts_gen;

public class dl_t {
	
	public String meth_name;

	public dl_t(String meth_name) {
		this.meth_name = meth_name;
	}

	public void printrec() {
		System.out.println("(" + this.meth_name + ").");
	}

}
