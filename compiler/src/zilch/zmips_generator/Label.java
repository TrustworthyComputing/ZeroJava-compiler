package zmips_generator;

public class Label {
	private int cnt;

	public Label() { this.cnt = 1; }

	public String new_label() { return new String("__L" + this.cnt++ + "__"); }
	
	public boolean is_label(String str) { return str.startsWith("__") && str.endsWith("__"); }

}
