package org.twc.zerojavacompiler.zerojava2spiglet;

public class Label {

	private int labels_;
	private boolean may_have_error_;

	public Label() {
		this.labels_ = 0;
		this.may_have_error_ = false;
	}

	public String newLabel() {
		return "L" + (++this.labels_);
	}

	public String newVTableLabel(String str) {
		return str + "_vTable";
	}

	public String getErrorLabel() {
		this.may_have_error_ = true;
		return "__Runtime_Error__";
	}

	public String getErrorCode() {
		StringBuilder error_code = new StringBuilder();
		if (this.may_have_error_) {
			error_code.append("__Runtime_Error__\n");
			error_code.append("move $r10, 0xffffffffffffffff\t\t# Runtime error\n");
			error_code.append("answer $r10\n");
		}
		return error_code.toString();
	}

}
