package base_type;

import java.util.Map;
import java.util.HashMap;

public class Method_t extends BaseType {
    private String type;
    public Map<String, Variable_t> methodParamsMap_;
    public Map<String, Variable_t> methodVarsMap_;
    public String return_reg;   // the register with the result of the method 
    public int var_cnt;         // number of variables of method
    public int par_cnt;         // the number of parameters
    public int meth_num;        // meth-id number
    
    public String body_;

    public Method_t(String type, String name) {
        super(name);
        this.type = type;
        methodParamsMap_ = new HashMap<>();
        methodVarsMap_ = new HashMap<>();
    }

    public String getType() {
        return this.type;
    }

    public Map<String, Variable_t> getParams() {
        return this.methodParamsMap_;
    }

    public String methContains(String varName) {
        if (methodParamsMap_.containsKey(varName)) {
            return methodParamsMap_.get(varName).getType();
        }
        if (methodVarsMap_.containsKey(varName)) {
            return methodVarsMap_.get(varName).getType();
        }
        return null;
    }

    public Variable_t methContainsVar(String varName) {
        if (methodParamsMap_.containsKey(varName)) {
            return methodParamsMap_.get(varName);
        }
        if (methodVarsMap_.containsKey(varName)) {
            return methodVarsMap_.get(varName);
        }
        return null;
    }

    public boolean addParam(Variable_t param) {
        String pname = param.getName();
        if (methodParamsMap_.containsKey(pname)) {
            return false;
        }
        
        param.var_num = this.par_cnt;
        this.par_cnt++;
        param.var_temp = new String("r" + param.var_num);
        methodParamsMap_.put(pname, param);
        return true;
    }

    public boolean addVar(Variable_t var) {
        String vname = var.getName();
        if (methodVarsMap_.containsKey(vname)) {
            return false;
        }
        if (methodParamsMap_.containsKey(vname)) {
            return false;
        }
        // var.var_num = this.var_cnt;
        // this.var_cnt++;
        methodVarsMap_.put(vname, var);
        
        return true;
    }

    public void printMethod() {
        System.out.print(meth_num + ") " + type + " " + this.getName() + "(");
        for (Map.Entry<String, Variable_t> meth_entry : methodParamsMap_.entrySet()) {
            Variable_t var = meth_entry.getValue();
            var.printVar();
            System.out.print(" ");
        }
        System.out.println(") "+"\n\t\t\tMethod Variables:");
        for (Map.Entry<String, Variable_t> meth_entry : methodVarsMap_.entrySet()) {
            Variable_t var = meth_entry.getValue();
            System.out.print("\t\t\t\t");
            var.printVar();
            System.out.println("");
        }
    }

    public void addTempToVar(String varName, String tempName) {
        if (methodVarsMap_.containsKey(varName)) {
            methodVarsMap_.get(varName).var_temp = tempName;
        }    
    }

}