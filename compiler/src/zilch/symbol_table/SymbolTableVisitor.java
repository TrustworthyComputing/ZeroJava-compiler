package symbol_table;

import syntaxtree.*;
import visitor.GJNoArguDepthFirst;
import java.util.Map;
import java.util.HashMap;
import base_type.*;

/* Second Visitor Pattern creates the Symbol Table */
public class SymbolTableVisitor extends GJNoArguDepthFirst<BaseType> {
    private Map<String, Method_t> st_;
    private int meth_cnt_;

    
    public SymbolTableVisitor() {
        this.st_ = new HashMap<>();
        meth_cnt_ = 0;
    }

    public Map<String, Method_t> getSymbolTable() {
        return this.st_;
    }
    
    public void printST() {
        int i = 0, j = 0;
        System.out.println("\n_____Symbol-Table_____");
        System.out.println("Methods:");
        for (Map.Entry<String, Method_t> st_entry : this.st_.entrySet()) {
            Method_t meth = st_entry.getValue();
            meth.printMethod();
            System.out.println("___________________________________________________");
        }
    }

    /**
     * f0 -> "void"
     * f1 -> "main"
     * f2 -> "("
     * f3 -> "void"
     * f4 -> ")"
     * f5 -> "{"
     * f6 -> ( VarDeclaration() )*
     * f7 -> ( Statement() )*
     * f8 -> "}"
     */
    public BaseType visit(MainMethodDeclaration n) throws Exception {
        n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(this);
        n.f3.accept(this);
        n.f4.accept(this);
        n.f5.accept(this);
        n.f6.accept(this);
        n.f7.accept(this);
        n.f8.accept(this);
        
        Method_t main = new Method_t("void", "main");
        if (n.f6.present()) {   // add Variables
            for (int i = 0 ; i < n.f6.size() ; i++) {
                if (!main.addVar((Variable_t) n.f6.nodes.get(i).accept(this))) {
                    throw new Exception("Method main: Variable " + n.f6.nodes.get(i).accept(this).getName() + " already exists!");
                }
                String vartype = ((Variable_t) n.f6.nodes.get(i).accept(this)).getType();
                if (!vartype.equals("int") && !vartype.equals("int[]") && !vartype.equals("short") && !vartype.equals("unsigned short")) { //if not classic types
                    throw new Exception("main: Cannot declare type " + vartype + " does not exist!\nUse either int or int[].");
                }
            }
        }
        main.meth_num = this.meth_cnt_++;
        this.st_.put("main", main);
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ( FormalParameterList() )?
     * f4 -> ")"
     * f5 -> "{"
     * f6 -> ( VarDeclaration() )*
     * f7 -> ( Statement() )*
     * f8 -> "return"
     * f9 -> Expression()
     * f10 -> ";"
     * f11 -> "}"
     */
    public BaseType visit(MethodDeclaration n) throws Exception {
        n.f0.accept(this);
        String type = n.f0.accept(this).getName();
        n.f1.accept(this);
        String mname = n.f1.accept(this).getName();
        n.f2.accept(this);
        n.f3.accept(this);
        Method_t m = (Method_t) n.f3.accept(this);        
        n.f4.accept(this);
        n.f5.accept(this);
        n.f6.accept(this);
        n.f7.accept(this);
        n.f8.accept(this);
        n.f9.accept(this);
        n.f10.accept(this);
        n.f11.accept(this);
        Method_t meth = new Method_t(type, mname);
        if (this.st_.containsKey(mname)) {
            throw new Exception("Method " + mname + " already exists!");
        }        
        if (n.f3.present()) {   // add parameters to method
            for (Map.Entry<String, Variable_t> meth_entry : m.methodParamsMap_.entrySet()) {
                Variable_t var = meth_entry.getValue();
                meth.addParam((Variable_t) var);
                String vartype = ((Variable_t) var).getType();
                if (!vartype.equals("int") && !vartype.equals("int[]")) { //if not classic types
                    throw new Exception(mname + ": Cannot declare " + vartype + " does not exist!");
                }
            }
        }
        if (n.f6.present()) {    // add method Variables
            for (int i = 0 ; i < n.f6.size() ; i++) {
                if (!meth.addVar((Variable_t) n.f6.nodes.get(i).accept(this))) {
                    throw new Exception("Method " + mname + ": Variable " + n.f6.nodes.get(i).accept(this).getName() + " already exists!");
                }
                String vartype = ((Variable_t) n.f6.nodes.get(i).accept(this)).getType();
                if (!vartype.equals("int") && !vartype.equals("int[]")) { //if not classic types
                    throw new Exception(mname + ": Cannot declare " + vartype + " does not exist!");
                }
            }
        }
        meth.meth_num = this.meth_cnt_++;
        this.st_.put(mname, meth);
        return null;
    }
    
    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    public BaseType visit(VarDeclaration n) throws Exception {
        String type = n.f0.accept(this).getName();
        String id = n.f1.accept(this).getName();
        n.f2.accept(this);
        return new Variable_t(type, id);
    }

    /**
    * f0 -> FormalParameter()
    * f1 -> FormalParameterTail()
    */
    public BaseType visit(FormalParameterList n) throws Exception {
        Variable_t fp = (Variable_t) n.f0.accept(this);
        Method_t meth = (Method_t) n.f1.accept(this);
        if (!meth.addParam(fp))
            throw new Exception("Parameter " + fp.getName() + " already exists!");
        return meth;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
    public BaseType visit(FormalParameter n) throws Exception {
        String type = n.f0.accept(this).getName();
        String id = n.f1.accept(this).getName();
        return new Variable_t(type, id);
    }

    /**
    * f0 -> ( FormalParameterTerm() )*
    */
    public BaseType visit(FormalParameterTail n) throws Exception {
        Method_t meth = new Method_t(null, null);
        if (n.f0.present())                 // create a linked list of variables. (parameters list)
            for (int i = 0 ; i < n.f0.size() ; i++)
                if (!meth.addParam((Variable_t) n.f0.nodes.get(i).accept(this)))
                    throw new Exception("Parameter " + n.f0.nodes.get(i).accept(this).getName() + " already exists!");
        return meth;
    }

        /**
    * f0 -> ","
    * f1 -> FormalParameter()
    */
    public BaseType visit(FormalParameterTerm n) throws Exception {
        n.f0.accept(this);
        return (Variable_t) n.f1.accept(this);
    }


        /**
    * f0 -> ArrayType()
    *       | IntegerType()
    *       | Identifier()
    */
    public BaseType visit(Type n) throws Exception {
        return n.f0.accept(this);
    }

    /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
    public BaseType visit(ArrayType n) throws Exception {
        return new BaseType("int[]");
    }

    /**
    * f0 -> "int"
    */
    public BaseType visit(IntegerType n) throws Exception {
        return new BaseType("int");
    }
    
    /**
    * f0 -> "short"
    */
    public BaseType visit(ShortType n) throws Exception {
        return new BaseType("short");
    }
    
    /**
    * f0 -> "unsigned short"
    */
    public BaseType visit(UShortType n) throws Exception {
        return new BaseType("unsigned short");
    }


    /**
    * f0 -> <IDENTIFIER>
    */
    public BaseType visit(Identifier n) throws Exception {
        return new BaseType(n.f0.toString());
    }


}
