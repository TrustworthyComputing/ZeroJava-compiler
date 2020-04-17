package org.twc.zerojavacompiler.basetype;

import java.util.*;

public class Method_t extends Base_t {

    public LinkedList<Variable_t> method_params;
    public LinkedList<Variable_t> method_vars;

    private Class_t from_class_;
    private String type_;
    private int num_parameters_;
    private int meth_num_;

    public Method_t(String type, String name) {
        super(name);
        this.type_ = type;
        this.num_parameters_ = 0;
        this.method_params = new LinkedList<>();
        this.method_vars = new LinkedList<>();
        this.from_class_ = null;
    }

    public Class_t getFromClass() {
        return this.from_class_;
    }

    public void setFromClass(Class_t from_class) {
        this.from_class_ = from_class;
    }

    public String getType() {
        return this.type_;
    }

    public void setType(String type) {
        this.type_ = type;
    }

    public int getNumParameters() {
        return this.num_parameters_;
    }

    public int getMethNum() {
        return this.meth_num_;
    }

    public void setMethNum(int meth_num) {
        this.meth_num_ = meth_num;
    }

    public LinkedList<Variable_t> getParams() {
        return this.method_params;
    }

    public String methContains(String varName) {
        for (Variable_t method_var : method_vars) {
            if (method_var.getName().equals(varName)) {
                return method_var.getType();
            }
        }
        for (Variable_t method_param : method_params) {
            if (method_param.getName().equals(varName)) {
                return method_param.getType();
            }
        }
        return null;
    }

    public Variable_t methContainsVar(String varName) {
        Variable_t v = (Variable_t) getByName(method_vars, varName);
        if (v != null) return v;
        return (Variable_t) getByName(method_params, varName);
    }

    public boolean addParam(Variable_t param) {
        if (containsName(method_params, param.getName())) {
            return false;
        }
        param.setNum(++this.num_parameters_);
        param.setRegister("$a" + param.getNum());
        method_params.add(param);
        return true;
    }

    public boolean addVar(Variable_t var) {
        if (containsName(method_vars, var.getName())) {
            return false;
        }
        if (containsName(method_params, var.getName())) {
            return false;
        }
        method_vars.add(var);
        return true;
    }

    public void addRegToVar(String varName, String tempName) {
        for (Variable_t method_var : method_vars) {
            if (method_var.getName().equals(varName)) {
                method_var.setRegister(tempName);
                return;
            }
        }
    }

    public void printMethod() {
        System.out.print(meth_num_ + ") " + type_ + " " + this.getName() + "(");
        int i = 0;
        for (Variable_t v : method_params) {
            v.printVar();
            if (i++ < method_params.size() - 1) {
                System.out.print(", ");
            }
        }
        System.out.println(") <"+ from_class_.getName() +">\n\t\tMethod Variables:");
        for (Variable_t v : method_vars) {
            System.out.print("\t\t\t");
            v.printVar();
            System.out.println();
        }
    }

}
