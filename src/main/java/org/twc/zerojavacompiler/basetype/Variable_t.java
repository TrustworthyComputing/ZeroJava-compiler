package org.twc.zerojavacompiler.basetype;

import java.util.List;

public class Variable_t extends Base_t {

    private String type_;
    private int num;
    private String reg;
    private List<String> vars_;
    private boolean has_many_ = false;

    public Variable_t(String type) {
        super(null);
        this.type_ = type;
        this.reg = null;
    }

    public Variable_t(String type, String name) {
        super(name);
        this.type_ = type;
        this.reg = null;
    }

    public Variable_t(String type, List<String> vars) {
        super(null);
        this.type_ = type;
        vars_ = vars;
        has_many_ = true;
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
        if (!this.has_many_) {
            System.out.print(this.type_ + " " + this.getName());
        } else {
            System.out.print(this.type_ + " ");
            for (String var : vars_) {
                System.out.print(var + " ");
            }
            System.out.println();
        }
    }

    public boolean getHasMany() {
        return this.has_many_;
    }

    public List<String> getVarList() {
        return this.vars_;
    }

    public void printVarDetails() {
        System.out.println("Type: " + this.type_ +
                "\nName: " + this.getName() +
                "\nReg: " + this.reg +
                "\nNum: " + this.num + "\n"
        );
    }

}
