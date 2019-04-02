package symbol_table;

import base_type.*;
import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;

public class SymbolTable {
    public int glob_temp_cnt_;
    public Map<String, Class_t> st_;

    public SymbolTable(LinkedList<Class_t> classes) {
        glob_temp_cnt_ = 0;
        
        st_ = new HashMap<>();
        for (Class_t c : classes) {
            st_.put(c.getName(), c);
        }
    }
    
    public SymbolTable(Map<String, Class_t> classMap) {
        glob_temp_cnt_ = 0;
        st_ = classMap;
    }

    public Map<String, Class_t> getST() {
        return st_;
    }

    public Class_t contains(String cname) { // returns a class if exists in ST else null
        if (st_.containsKey(cname)) {
            return st_.get(cname);
        }
        return null;
    }

    public void printST() {
        int i = 0, j = 0;
        System.out.println("\n________Symbol-Table________");
        System.out.println("\tClasses:");
        
        for (Map.Entry<String, Class_t> st_entry : st_.entrySet()) {
            Class_t cl = st_entry.getValue();
            System.out.println("___________________________________________________");
            if (cl.getDad() == null) {
                System.out.println(cl.getName() + ":\n\t" +  cl.var_cnt+ " Vars:");
            } else {
                System.out.println(cl.getName() + " extends " + cl.getDad() + ":\n\t" +  cl.var_cnt+ " Vars:");
            }
            
            
            for (Map.Entry<String, Variable_t> cl_entry : cl.classVarsMap_.entrySet()) {
                Variable_t var = cl_entry.getValue();
                System.out.print("\t\t");
                var.printVar();
                System.out.println("");
            }
            
            System.out.println("\t" + cl.meth_cnt + " Methods:");
            for (Map.Entry<String, Method_t> cl_entry : cl.classMethodsMap_.entrySet()) {
                Method_t meth = cl_entry.getValue();
                System.out.print("\t\t");
                meth.printMethod();
            }
            
        }
                
    }

}