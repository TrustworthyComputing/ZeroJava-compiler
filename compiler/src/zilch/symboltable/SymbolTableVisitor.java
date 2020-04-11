package symboltable;

import syntaxtree.*;
import visitor.GJNoArguDepthFirst;
import java.util.Map;
import basetype.*;

/* Second Visitor Pattern creates the Symbol Table */
public class SymbolTableVisitor extends GJNoArguDepthFirst<BaseType> {

    private Map<String, Class_t> st_;
    private int globals_;

    public SymbolTableVisitor(Map<String, Class_t> st) {
        st_ = st;
        globals_ = 0;
    }

    public Map<String, Class_t> getSymbolTable() {
        return st_;
    }

    public void printSymbolTable() {
        for (Map.Entry<String, Class_t> entry : st_.entrySet()) {
            Class_t clazz = entry.getValue();
            clazz.printClass();
            System.out.println();
        }
    }

    public int getGlobalsNumber() {
        return this.globals_;
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> "public"
    * f4 -> "static"
    * f5 -> "void"
    * f6 -> "main"
    * f7 -> "("
    * f8 -> "String"
    * f9 -> "["
    * f10 -> "]"
    * f11 -> Identifier()
    * f12 -> ")"
    * f13 -> "{"
    * f14 -> ( VarDeclaration() )*
    * f15 -> ( Statement() )*
    * f16 -> "}"
    * f17 -> "}"
    */
    public BaseType visit(MainClass n) throws Exception {
        n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(this);
        n.f3.accept(this);
        n.f4.accept(this);
        n.f5.accept(this);
        n.f6.accept(this);
        n.f7.accept(this);
        n.f8.accept(this);
        n.f9.accept(this);
        n.f10.accept(this);
        n.f11.accept(this);
        n.f12.accept(this);
        n.f13.accept(this);
        n.f14.accept(this);
        n.f15.accept(this);
        n.f16.accept(this);
        n.f17.accept(this);
        Variable_t v = new Variable_t("String[]", n.f11.accept(this).getName());
        Method_t m = new Method_t("void", "main");
        m.addParam(v);
        String classname = n.f1.accept(this).getName();
        Class_t mainclass = st_.get(classname);
        mainclass.setIsMain();
        if (! mainclass.addMethod(m)) {
            throw new Exception("Method " + m.getName() + " already exists!");
        }
        if (n.f14.present()) {
            for (int i = 0 ; i < n.f14.size() ; i++) {
                if (!m.addVar((Variable_t) n.f14.nodes.get(i).accept(this))) {
                    throw new Exception("Class " + classname + ": Variable " + n.f14.nodes.get(i).accept(this).getName() + " already exists!");
                }
                String vartype = ((Variable_t) n.f14.nodes.get(i).accept(this)).getType();
                if (!(vartype.equals("int") || vartype.equals("boolean") || vartype.equals("int[]") || st_.containsKey(vartype))) {
                    throw new Exception(classname + ": Cannot declare " + vartype + " does not exist!");
                }
            }
        }
        return null;
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
    public BaseType visit(ClassDeclaration n) throws Exception {
        n.f0.accept(this);
        String classname = n.f1.accept(this).getName();
        n.f2.accept(this);
        n.f3.accept(this);
        n.f4.accept(this);
        n.f5.accept(this);
        // add Class Variables
        if (n.f3.present()) {
            for (int i = 0 ; i < n.f3.size() ; i++) {   // for every variable
                // if variable isnt unique
                if (!st_.get(classname).addVar((Variable_t)n.f3.nodes.get(i).accept(this))) {
                    throw new Exception("Class " + classname + ": Variable " + n.f3.nodes.get(i).accept(this).getName() + " already exists!");
                }
                String vartype = ((Variable_t) n.f3.nodes.get(i).accept(this)).getType();
                if (! (vartype.equals("int") || vartype.equals("boolean") || vartype.equals("int[]") || st_.containsKey(vartype))) {
                    throw new Exception(classname + ": Cannot declare " + vartype + " does not exist!");
                }
            }
        }
        // add Class Methods
        if (n.f4.present()) {
            for (int i = 0 ; i < n.f4.size() ; i++) {
                // if method isnt unique
                if (! st_.get(classname).addMethod((Method_t) n.f4.nodes.get(i).accept(this)) ) {
                    throw new Exception("Class " + classname + ": Method " + n.f4.nodes.get(i).accept(this).getName() + " already exists!");
                }
                String vartype = ((Method_t) n.f4.nodes.get(i).accept(this)).getType();
                if (! (vartype.equals("int") || vartype.equals("boolean") || vartype.equals("int[]") || st_.containsKey(vartype))) {
                    throw new Exception(classname + ": Cannot declare " + vartype + " does not exist!");
                }
            }
        }
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    public BaseType visit(ClassExtendsDeclaration n) throws Exception {
        n.f0.accept(this);
        String id = n.f1.accept(this).getName();
        n.f2.accept(this);
        String parent_id = n.f3.accept(this).getName();
        n.f4.accept(this);
        n.f5.accept(this);
        n.f6.accept(this);
        n.f7.accept(this);
        Class_t newClass = st_.get(id);
        Class_t parentClass = st_.get(parent_id);
        // add Class Variables
        if (n.f5.present()) {
            for (int i = 0 ; i < n.f5.size() ; i++) {
                Variable_t var = (Variable_t) n.f5.nodes.get(i).accept(this);
                if (! newClass.addVar( var) ) { // if variable isnt unique
                    throw new Exception("Class " + id + ": Variable " + var.getName() + " already exists.");
                }
                String vartype = var.getType();
                if (!(vartype.equals("int") || vartype.equals("boolean") || vartype.equals("int[]") || st_.containsKey(vartype))) {
                    throw new Exception(id + ": Cannot declare " + vartype + " does not exist.");
                }
            }
        }
        //add variables from parent-class
        parentClass.class_vars_map.forEach((k, v) -> newClass.copyVar(v) );
        // add Class Methods
        if (n.f6.present()) {
            for (int i = 0 ; i < n.f6.size() ; i++) {
                Method_t m = (Method_t) n.f6.nodes.get(i).accept(this);

                // if method isnt unique
                if (!newClass.addMethod(m)) {
                    throw new Exception("Class " + id + ": Method " + m.getName() + " already exists!");
                } else if (parentClass.classContainsMeth(m.getName())) { // if the parent-class has a method with this name
                    if (!parentClass.checkMethod(m)) { // and if it isnt exactly the same error
                        throw new Exception("Class " + id + ": Method " + m.getName() + " is extended but has a different prototype from the first one!");
                    }
                }
                String vartype = m.getType();
                if (!(vartype.equals("int") || vartype.equals("boolean") || vartype.equals("int[]") || st_.containsKey(vartype))) {
                    throw new Exception(id + ": Cannot declare " + vartype + " does not exist.");
                }
                // if the parent-class hasnt a method with this classname
                if (!parentClass.classContainsMeth(m.getName())) {
                    newClass.addMethod(m);
                } else { // replace method
                    int k = 0;
                    for (Map.Entry<String, Method_t> entry : newClass.class_methods_map.entrySet()) {
                        if (entry.getKey().equals(m.getName())) {
                            m.setFromClass(newClass);
                            m.setMethNum(k + 1);
                            entry.setValue(m);
                        }
                        k++;
                    }
                }
            }
        }
        //add methods from parent-class
        for (Method_t parentsMeth : parentClass.class_methods_map.values()) {
            if (parentsMeth.getName().equals("main")) {
                continue ;
            }
            newClass.addMethod(parentsMeth);
            newClass.setNumMethods(parentClass.getNumMethods());
        }
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public BaseType visit(VarDeclaration n) throws Exception {
        String type_ = n.f0.accept(this).getName();
        String var_id = n.f1.accept(this).getName();
        n.f2.accept(this);
        return new Variable_t(type_, var_id);
    }

    /**
     * f0 -> "public"
     * f1 -> BaseType()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    public BaseType visit(MethodDeclaration n) throws Exception {
        n.f0.accept(this);
        String type_ = n.f1.accept(this).getName();
        String meth_name = n.f2.accept(this).getName();
        Method_t meth = new Method_t(type_, meth_name);
        n.f3.accept(this);
        // add parameters to method
        if (n.f4.present()) {
            Method_t m = (Method_t) n.f4.accept(this);
            for (int i = 0 ; i < m.method_params.size() ; i++) {
                Variable_t param = m.method_params.get(i);
                meth.addParam(param);
                String vartype = param.getType();
                if (!(vartype.equals("int") || vartype.equals("boolean") || vartype.equals("int[]") || st_.containsKey(vartype))) {
                    throw new Exception(meth_name + ": Cannot declare " + vartype + " does not exist!");
                }
            }
        }
        n.f5.accept(this);
        n.f6.accept(this);
        // add method Variables
        if (n.f7.present()) {
            n.f7.accept(this);
            for (int i = 0 ; i < n.f7.size() ; i++) {
                if (!meth.addVar((Variable_t) n.f7.nodes.get(i).accept(this))) {
                    throw new Exception("Method " + meth_name + ": Variable " + n.f7.nodes.get(i).accept(this).getName() + " already exists!");
                }
                String vartype = ((Variable_t) n.f7.nodes.get(i).accept(this)).getType();
                if (!(vartype.equals("int") || vartype.equals("boolean") || vartype.equals("int[]") || st_.containsKey(vartype))) {
                    throw new Exception(meth_name + ": Cannot declare " + vartype + " does not exist!");
                }
            }
        }
        n.f8.accept(this);
        n.f9.accept(this);
        n.f10.accept(this);
        n.f11.accept(this);
        n.f12.accept(this);
        if (globals_ < meth.getNumParameters()) {
            globals_ = meth.getNumParameters();
        }
        return meth;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public BaseType visit(FormalParameterList n) throws Exception {
        Variable_t fp = (Variable_t) n.f0.accept(this);
        Method_t meth = (Method_t) n.f1.accept(this);
        if (!meth.addParam(fp)) {
            throw new Exception("Parameter " + fp.getName() + " already exists!");
        }
        return meth;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public BaseType visit(FormalParameter n) throws Exception {
        String type_ = n.f0.accept(this).getName();
        String id = n.f1.accept(this).getName();
        return new Variable_t(type_, id);
    }

    /**
     * f0 -> ( FormalParameterTerm() )*
     */
    public BaseType visit(FormalParameterTail n) throws Exception {
        Method_t meth = new Method_t(null, null);
        // create a linked list of variables. (parameters list)
        if (n.f0.present()) {
            for (int i = 0 ; i < n.f0.size() ; i++) {
                if (!meth.addParam((Variable_t) n.f0.nodes.get(i).accept(this))) {
                    throw new Exception("Parameter " + n.f0.nodes.get(i).accept(this).getName() + " already exists!");
                }
            }
        }
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
     *       | BooleanType()
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
     * f0 -> "boolean"
     */
    public BaseType visit(BooleanType n) throws Exception {
        return new BaseType("boolean");
    }

    /**
     * f0 -> "int"
     */
    public BaseType visit(IntegerType n) throws Exception {
        return new BaseType("int");
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public BaseType visit(Identifier n) throws Exception {
        return new BaseType(n.f0.toString());
    }

}
