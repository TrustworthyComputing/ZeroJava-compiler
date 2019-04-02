package base_type;

import java.util.*;

public class Class_t extends BaseType {
    public Map<String, Method_t> classMethodsMap_;
    public Map<String, Variable_t> classVarsMap_;
    private String dad; // if extended
    public int meth_cnt;
    public int var_cnt;
    public boolean isMain;

    public Class_t(String name, String dad) {
        super(name);
        classMethodsMap_ = new HashMap<>();
        classVarsMap_ = new HashMap<>();
        this.dad = dad;
        this.meth_cnt = 0;
        this.var_cnt = 0;
        this.isMain = false;
    }

    public boolean addMethod(Method_t meth) {
        String mname = meth.getName();
        if ((mname.equals("main") && meth_cnt > 0) || classMethodsMap_.containsKey(mname)) {
            return false;
        }
        meth.comesFrom = this;
        this.meth_cnt++;
        meth.meth_num = this.meth_cnt;
        classMethodsMap_.put(mname, meth);
        return true;
    }

    public void copyMethod(Method_t meth) {
        String mname = meth.getName();
        Method_t newMeth = new Method_t(meth.getType(), mname);
        newMeth.methodParamsMap_ = meth.methodParamsMap_;
        newMeth.methodVarsMap_ = meth.methodVarsMap_;
        newMeth.comesFrom = meth.comesFrom;
        newMeth.var_cnt = meth.var_cnt;
        newMeth.par_cnt = meth.par_cnt;
        newMeth.meth_num = meth.meth_num;
        classMethodsMap_.put(mname, newMeth);
    }

    public void copyVar(Variable_t var) {
        String vname = var.getName();
        this.var_cnt++;
        var.var_num = this.var_cnt;
        classVarsMap_.put(vname, var);
    }

    public boolean addVar(Variable_t var) {
        String vname = var.getName();
        if (classVarsMap_.containsKey(vname)) {
            return false;
        }
        this.var_cnt++;
        var.var_num = this.var_cnt;
        classVarsMap_.put(vname, var);
        return true;
    }

    public Method_t getMethod(String methName) {
        if (classMethodsMap_.containsKey(methName)) {
            return classMethodsMap_.get(methName);
        }
        return null;
    }

    public Variable_t classContainsVar(String varName) {
        if (classVarsMap_.containsKey(varName)) {
            return classVarsMap_.get(varName);
        }
        return null;
    }

    public Variable_t classContainsVarReverse(String varName) {
        if (classVarsMap_.containsKey(varName)) {
            return classVarsMap_.get(varName);
        }
        return null;
    }

    public boolean classContainsMeth(String methName) {
        if (classMethodsMap_.containsKey(methName)) {
            return true;
        }
        return false;
    }

    public boolean checkMethod(Method_t meth) { // check if meth is the same as this.method
        String mname = meth.getName();

        if (classMethodsMap_.containsKey(mname)) {
            Method_t m = classMethodsMap_.get(mname);
            if (m.getType().equals(meth.getType())) { // if same type
        
                Map<String, Variable_t> parameters = m.getParams();
                if (parameters.size() != meth.getParams().size()) {
                    return false;        
                }
                for (Map.Entry<String, Variable_t> item : parameters.entrySet()) {
                    String key = item.getKey();
                    Variable_t value = item.getValue();
                    if (value.getType() != meth.getParams().get(key).getType()) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public String getDad() {
        return this.dad;
    }

}