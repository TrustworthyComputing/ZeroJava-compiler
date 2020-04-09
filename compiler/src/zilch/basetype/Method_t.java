package basetype;

import java.util.*;

public class Method_t extends BaseType {

    private String type;
    public LinkedList<Variable_t> method_params;
    public LinkedList<Variable_t> method_vars;
    public Class_t comesFrom;
    public int var_cnt;
    public int par_cnt;
    public int meth_num;


    public Method_t(String type, String name) {
        super(name);
        this.type = type;
        this.method_params = new LinkedList<Variable_t>();
        this.method_vars = new LinkedList<Variable_t>();
        this.comesFrom = null;
    }

    public String getType() {
        return this.type;
    }

    public LinkedList<Variable_t> getParams() {
        return this.method_params;
    }

    public String methContains(String varName) {
        for (int i = 0 ; i < method_vars.size() ; i++) {
            if (method_vars.get(i).getName().equals(varName)) {
                return method_vars.get(i).getType();
            }
        }
        for (int i = 0 ; i < method_params.size() ; i++) {
            if (method_params.get(i).getName().equals(varName)) {
                return method_params.get(i).getType();
            }
        }
        return null;
    }

    public Variable_t methContainsVar(String varName) {
        for (int i = 0 ; i < method_vars.size() ; i++) {
            if (method_vars.get(i).getName().equals(varName)) {
                return method_vars.get(i);
            }
        }
        for (int i = 0 ; i < method_params.size() ; i++) {
            if (method_params.get(i).getName().equals(varName)) {
                return method_params.get(i);
            }
        }
        return null;
    }

    public boolean addParam(Variable_t param) {
        int i = 0;
        while (i < method_params.size()) {
            if (method_params.get(i++).getName().equals(param.getName())) {
                return false;
            }
        }
        this.par_cnt++;
        param.var_num = this.par_cnt;
        param.var_temp = new String("$r" + param.var_num);
        method_params.addLast(param);
        return true;
    }

    public boolean addVar(Variable_t var) {
        int i = 0;
        while (i < method_vars.size())
            if (method_vars.get(i++).getName().equals(var.getName()))
                return false;
        for (int j = 0 ; j < method_params.size() ; j++)
            if (method_params.get(j).getName().equals(var.getName()))
                return false;
        method_vars.addLast(var);
        return true;
    }

    public void addTempToVar(String varName, String tempName) {
        for (int i = 0 ; i < method_vars.size(); i++) {
            if (method_vars.get(i).getName().equals(varName)) {
                method_vars.get(i).var_temp = tempName;
                return ;
            }
        }
    }


    public void printMethod() {
        System.out.print(meth_num + ") " + type + " " + this.getName() + "(");
        int i = 0;
        while (i < method_params.size()) {
            method_params.get(i++).printVar();
            if (i != method_params.size())
                System.out.print(", ");
        }
        System.out.println(") <"+ comesFrom.getName() +">\n\t\tMethod Variables:");
        i = 0;
        while (i < method_vars.size()) {
            System.out.print("\t\t\t");
            method_vars.get(i++).printVar();
            System.out.println("");
        }
    }
}
