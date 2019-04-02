package base_type;

import java.util.*;

public class Class_t extends BaseType {
    public LinkedList<Method_t> classMethods;
    public LinkedList<Variable_t> classVars;
    private String dad; // if extended
    public int meth_cnt;
    public int var_cnt;
    public boolean isMain;

    public Class_t(String name, String dad) {
        super(name);
        classMethods = new LinkedList<Method_t>();
        classVars = new LinkedList<Variable_t>();
        this.dad = dad;
        this.meth_cnt = 0;
        this.var_cnt = 0;
        this.isMain = false;
    }

    public boolean addMethod(Method_t meth) {
        for (int i = 0 ; i < classMethods.size() ; i++)
            if (classMethods.get(i).getName().equals(meth.getName()))
                return false;
            else if (meth.getName().equals("main"))
                return false;
        meth.comesFrom = this;
        this.meth_cnt++;
        meth.meth_num = this.meth_cnt;
        classMethods.addLast(meth);
        return true;
    }

    public void copyMethod(Method_t meth) {
        Method_t newMeth = new Method_t(meth.getType(), meth.getName());
        newMeth.methodParams = meth.methodParams;
        newMeth.methodVars = meth.methodVars;
        newMeth.comesFrom = meth.comesFrom;
        newMeth.var_cnt = meth.var_cnt;
        newMeth.par_cnt = meth.par_cnt;
        newMeth.meth_num = meth.meth_num;
        classMethods.addLast(newMeth);
    }

    public void copyVar(Variable_t var) {
        this.var_cnt++;
        var.var_num = this.var_cnt;
        classVars.addLast(var);
    }

    public boolean addVar(Variable_t var) {
        for (int i = 0 ; i < classVars.size() ; i++)
            if (classVars.get(i).getName().equals(var.getName()))
                return false;
        this.var_cnt++;
        var.var_num = this.var_cnt;
        classVars.addLast(var);
        return true;
    }

    public Method_t getMethod(String methName) {
        for (int i = 0 ; i < classMethods.size() ; i++)
            if (classMethods.get(i).getName().equals(methName))
                return classMethods.get(i);
        return null;
    }

    public Variable_t classContainsVar(String varName) {
        for (int i = 0 ; i < classVars.size() ; i++)
            if (classVars.get(i).getName().equals(varName))
                return classVars.get(i);
        return null;
    }

    public Variable_t classContainsVarReverse(String varName) {
        for (int i = classVars.size()-1 ; i >=0  ; i--)
            if (classVars.get(i).getName().equals(varName))
                return classVars.get(i);
        return null;
    }

    public boolean classContainsMeth(String methName) {
        for (int i = 0 ; i < classMethods.size() ; i++)
            if (classMethods.get(i).getName().equals(methName))
                return true;
        return false;
    }

    public boolean checkMethod(Method_t meth) { // check if meth is the same as this.method
        int i = 0;
        while (i < classMethods.size()) {
            if (classMethods.get(i).getName().equals(meth.getName())) //found method meth
                if (classMethods.get(i).getType().equals(meth.getType())) { //if same type
                    LinkedList<Variable_t> parameters = classMethods.get(i).getParams();
                    if (parameters.size() != meth.getParams().size())
                        return false;
                    for (int j = 0 ; j < parameters.size() ; j++) 
                        if (parameters.get(j).getType() != meth.getParams().get(j).getType())
                            return false;
                    return true;
                }
                else
                    return false;
            i++;
        }
        return false;
    }

    public String getDad() {
        return this.dad;
    }

}