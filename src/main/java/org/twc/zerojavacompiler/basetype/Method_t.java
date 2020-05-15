package org.twc.zerojavacompiler.basetype;

import org.twc.zerojavacompiler.spiglet2kanga.FlowGraph;
import org.twc.zerojavacompiler.spiglet2kanga.LiveInterval;

import java.util.*;

public class Method_t extends Base_t {

    public LinkedList<Variable_t> method_params;
    public LinkedList<Variable_t> method_vars;

    private Class_t from_class_;
    private String type_;
    private int num_parameters_;
    private int meth_num_;

    /**
     * For register allocation
     */
    private int stack_num_;
    private int call_param_num_;

    // t0-t9
    public HashMap<String, String> temp_regs_map;
    // s0-s7
    public HashMap<String, String> save_regs_map;
    // SPILLEDARG *
    public HashMap<String, String> spilled_regs_map;
    // tempNo -> Interval
    public HashMap<Integer, LiveInterval> temp_reg_intervals;

    public FlowGraph flowGraph;

    public Method_t(String type, String name) {
        super(name);
        this.type_ = type;
        this.num_parameters_ = 0;
        this.method_params = new LinkedList<>();
        this.method_vars = new LinkedList<>();
        this.from_class_ = null;
    }

    public Method_t(String name, int num_parameters) {
        super(name);
        this.num_parameters_ = num_parameters;
        this.stack_num_ = 0;
        this.call_param_num_ = 0;
        this.temp_regs_map = new HashMap<>();
        this.save_regs_map = new HashMap<>();
        this.spilled_regs_map = new HashMap<>();
        this.temp_reg_intervals = new HashMap<>();
        this.flowGraph = new FlowGraph();
    }

    public Class_t getFrom_class_() {
        return this.from_class_;
    }

    public void setFrom_class_(Class_t from_class) {
        this.from_class_ = from_class;
    }

    public String getType_() {
        return this.type_;
    }

    public void setType_(String type) {
        this.type_ = type;
    }

    public int getNum_parameters_() {
        return this.num_parameters_;
    }

    public int getMeth_num_() {
        return this.meth_num_;
    }

    public void setMeth_num_(int meth_num) {
        this.meth_num_ = meth_num;
    }

    public LinkedList<Variable_t> getMethod_params() {
        return this.method_params;
    }

    public int getStack_num_() {
        return stack_num_;
    }

    public void setStack_num_(int stack_num_) {
        this.stack_num_ = stack_num_;
    }

    public int getCall_param_num_() {
        return call_param_num_;
    }

    public void setCall_param_num_(int call_param_num_) {
        this.call_param_num_ = call_param_num_;
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
        param.setRegister("TEMP " + param.getNum());
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
        System.out.println(") <" + from_class_.getName() + ">\n\t\tMethod Variables:");
        for (Variable_t v : method_vars) {
            System.out.print("\t\t\t");
            v.printVar();
            System.out.println();
        }
    }

}
