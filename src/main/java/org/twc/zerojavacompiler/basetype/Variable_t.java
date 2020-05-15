package org.twc.zerojavacompiler.basetype;

public class Variable_t extends Base_t {

    private String type_;
    private int num;
    private String reg;

    public Variable_t(String type, String name) {
        super(name);
        this.type_ = type;
        this.reg = null;
    }

    public Variable_t(String type, String name, String reg) {
        super(name);
        this.type_ = type;
        this.reg = reg;
    }

    public String getType() {
        return this.type_;
    }

    public void setType(String type) {
        this.type_ = type;
    }

    public int getNum() {
        return this.num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public String getRegister() {
        return this.reg;
    }

    public void setRegister(String reg) {
        this.reg = reg;
    }

    public void printVar() {
        System.out.print(this.type_ + " " + this.getName());
    }

    public void printVarDetails() {
        System.out.println("Type: " + this.type_ +
                "\nName: " + this.getName() +
                "\nReg: " + this.reg +
                "\nNum: " + this.num + "\n"
        );
    }

}
