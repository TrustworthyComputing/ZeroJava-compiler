package basetype;

import java.util.*;

public class Class_t extends BaseType {
    public Map<String, Method_t> class_methods_map;
    public Map<String, Variable_t> class_vars_map;

    private String parent; // if extended
    public int meth_cnt;
    public int var_cnt;
    public boolean isMain;

    public Class_t(String name, String parent) {
        super(name);
        class_methods_map = new LinkedHashMap<>();
        class_vars_map = new LinkedHashMap<>();
        this.parent = parent;
        this.meth_cnt = 0;
        this.var_cnt = 0;
        this.isMain = false;
    }

    public boolean addMethod(Method_t meth) {
        if (class_methods_map.containsKey(meth.getName())) {
            return false;
        }
        meth.comesFrom = this;
        this.meth_cnt++;
        meth.meth_num = this.meth_cnt;
        class_methods_map.put(meth.getName(), meth);
        return true;
    }

    public void copyMethod(Method_t meth) {
        Method_t newMeth = new Method_t(meth.getType(), meth.getName());
        newMeth.method_params = meth.method_params;
        newMeth.method_vars = meth.method_vars;
        newMeth.comesFrom = meth.comesFrom;
        newMeth.var_cnt = meth.var_cnt;
        newMeth.par_cnt = meth.par_cnt;
        newMeth.meth_num = meth.meth_num;
        class_methods_map.put(meth.getName(), newMeth);
    }

    public void copyVar(Variable_t var) {
        this.var_cnt++;
        var.var_num = this.var_cnt;
        class_vars_map.put(var.getName(), var);
    }

    public boolean addVar(Variable_t var) {
        if (class_vars_map.containsKey(var.getName())) {
            return false;
        }
        this.var_cnt++;
        var.var_num = this.var_cnt;
        class_vars_map.put(var.getName(), var);
        return true;
    }

    public Method_t getMethod(String methName) {
        return class_methods_map.get(methName);
    }

    public Variable_t classContainsVar(String varName) {
        return class_vars_map.get(varName);
    }

    public boolean classContainsMeth(String methName) {
        return class_methods_map.containsKey(methName);
    }

    public boolean checkMethod(Method_t meth) { // check if meth is the same as this.method
        int i = 0;
        if (class_methods_map.containsKey(meth.getName())) {
            Method_t m_from_class = class_methods_map.get(meth.getName());
            if (m_from_class.getType().equals(meth.getType())) {
                LinkedList<Variable_t> parameters = m_from_class.getParams();
                if (parameters.size() != meth.getParams().size()) {
                    return false;
                }
                for (int j = 0 ; j < parameters.size() ; j++) {
                    if (parameters.get(j).getType() != meth.getParams().get(j).getType()) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public String getParent() {
        return this.parent;
    }

    public void printClass() {
        System.out.println("Class: " + getName());
        System.out.println("\tMethods:");
        for (Map.Entry<String, Method_t> entry : class_methods_map.entrySet()) {
            Method_t meth = entry.getValue();
            System.out.print("\t");
            meth.printMethod();
            System.out.println();
        }
        System.out.println("\tVars:");
        for (Map.Entry<String, Variable_t> entry : class_vars_map.entrySet()) {
            Variable_t var = entry.getValue();
            System.out.print("\t\t");
            var.printVar();
            System.out.println();
        }
        System.out.println();
    }

}
