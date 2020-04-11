package zmipsgenerator;

public class Label {

	private int cnt;

	public Label() {
		this.cnt = 0;
	}

	public String new_label() {
		return new String("__L_" + (++this.cnt) + "__");
	}

	public String newClassLabel(String str) {
		return new String("__" + str + "_vTable__");
	}

}
