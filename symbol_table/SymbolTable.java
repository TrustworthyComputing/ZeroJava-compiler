package symbol_table;

import base_type.*;
import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;

public class SymbolTable {
    public LinkedList<Class_t> ST;
    public int glob_temp_cnt;

    public SymbolTable(LinkedList<Class_t> classes) {
        ST = classes;
        glob_temp_cnt = 0;
    }

    public LinkedList<Class_t> getST() { return ST; }

    public Class_t contains(String nam) { // returns a class if exists in ST else null
        int i = 0, size = ST.size();
        while (i < size)
            if (ST.get(i++).getName().equals(nam))
                return ST.get(i-1);
        return null;
    }

    public void printST() {
        int i = 0, j = 0;
        System.out.println("\n____Symbol Table____\n");
        System.out.println("\n\tClasses:");
        while (i < ST.size()) {
            j = 0; 
            System.out.println("___________________________________________________");
            if (ST.get(i).getDad() == null)
                System.out.println(ST.get(i).getName() + ":\n\t" +  ST.get(i).var_cnt+ " Vars:");
            else
                System.out.println(ST.get(i).getName() + " extends " + ST.get(i).getDad() + ":\n\t" +  ST.get(i).var_cnt+ " Vars:");
            while (j < ST.get(i).classVars.size()) {
                System.out.print("\t\t");
                ST.get(i).classVars.get(j).printVar();
                System.out.println("");
                j++;
            }
            j = 0; 
            System.out.println("\t" + ST.get(i).meth_cnt + " Methods:");
            while (j < ST.get(i).classMethods.size()) {
                System.out.print("\t\t");
                ST.get(i).classMethods.get(j).printMethod();
                j++;
            }
            i++;
        }
    }

}