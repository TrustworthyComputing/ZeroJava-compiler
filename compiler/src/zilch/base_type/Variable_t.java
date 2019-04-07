package base_type;

public class Variable_t extends BaseType {
	public String type;
	public int var_num;
	public String var_temp;

	public Variable_t(String type, String name) {
	    super(name);
	    this.type = type;
	    this.var_temp = null;
	}

	public String getType() {
        return this.type;
    }

    public String getTemp() {
        return this.var_temp;
    }

    public int getVarNum() {
        return this.var_num;
    }

    public void printVar() {
		System.out.print(var_temp + ") " + this.type + " " + this.getName() + " " + var_num);
	}
}