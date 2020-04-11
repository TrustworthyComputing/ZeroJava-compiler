package basetype;

import java.util.*;

public class Class_t extends BaseType {

    public Map<String, Method_t> class_methods_map;
    public Map<String, Variable_t> class_vars_map;

    private String parent; // if extended
    private int num_methods_;
    private int num_vars_;
    private boolean is_main;

    public Class_t(String name, String parent) {
        super(name);
        class_methods_map = new LinkedHashMap<>();
        class_vars_map = new LinkedHashMap<>();
        this.parent = parent;
        this.num_methods_ = 0;
        this.num_vars_ = 0;
        this.is_main = false;
    }

    public void setIsMain() {
        this.is_main = true;
    }

    public boolean isMain() {
        return this.is_main;
    }

    public void setNumMethods(int num_methods) {
        this.num_methods_ = num_methods;
    }

    public int getNumMethods() {
        return this.num_methods_;
    }

    public int getNumVars() {
        return this.num_vars_;
    }

    public boolean addMethod(Method_t meth) {
        if (class_methods_map.containsKey(meth.getName())) {
            return false;
        }
        meth.setFromClass(this);
        this.num_methods_++;
        meth.setMethNum(this.num_methods_);
        class_methods_map.put(meth.getName(), meth);
        return true;
    }

    public void copyMethod(Method_t meth) {
        Method_t newMeth = new Method_t(meth.getType(), meth.getName());
        newMeth.method_params = meth.method_params;
        newMeth.method_vars = meth.method_vars;
        newMeth.setFromClass(meth.getFromClass());
        newMeth.setNumVars(meth.getNumVars());
        newMeth.setNumParameters(meth.getNumParameters());
        newMeth.setMethNum(meth.getMethNum());
        class_methods_map.put(meth.getName(), newMeth);
    }

    public void copyVar(Variable_t var) {
        this.num_vars_++;
        var.setNum(this.num_vars_);
        class_vars_map.put(var.getName(), var);
    }

    public boolean addVar(Variable_t var) {
        if (class_vars_map.containsKey(var.getName())) {
            return false;
        }
        this.num_vars_++;
        var.setNum(this.num_vars_);
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
