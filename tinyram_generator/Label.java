package tinyram_generator;

public class Label {
	private int cnt;

	public Label() { this.cnt = 1; }

	public String new_label() { return new String("__L" + this.cnt++ + "__"); }

}