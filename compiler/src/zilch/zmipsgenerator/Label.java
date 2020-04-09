package zmipsgenerator;

public class Label {
	private int cnt;

	public Label() { this.cnt = 1; }

	public String new_label() { return new String("L" + this.cnt++); }

	public String newClassLabel(String str) { return new String(str + "_vTable"); }

}