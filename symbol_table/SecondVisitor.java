package symbol_table;

import syntaxtree.*;
import visitor.GJNoArguDepthFirst;
import java.util.*;
import base_type.*;

/* Second Visitor Pattern creates the Symbol Table */
public class SecondVisitor extends GJNoArguDepthFirst<BaseType> {
    private SymbolTable st;

    public SecondVisitor(LinkedList<Class_t> classes) { 
        st = new SymbolTable(classes);
    }

    public SymbolTable getSymbolTable() {
        return st;
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
        String name = n.f1.accept(this).getName();
        m.addParam(v);
        if (n.f14.present())    // add MainClass Variables
            for (int i = 0 ; i < n.f14.size() ; i++) {
                m.addVar((Variable_t)n.f14.nodes.get(i).accept(this));
            }            
        Class_t main = st.contains(name);
        main.addMethod(m);
        main.isMain = true;
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
        String name = n.f1.accept(this).getName();
        n.f2.accept(this);
        n.f3.accept(this);
        n.f4.accept(this);
        n.f5.accept(this);
        if (n.f3.present())     // add Class Variables
            for (int i = 0 ; i < n.f3.size() ; i++) {   // for every variable
                boolean found = false;
                if (!st.contains(name).addVar((Variable_t)n.f3.nodes.get(i).accept(this))) // if variable isnt unique
                    throw new Exception("Class " + name + ": Variable " + n.f3.nodes.get(i).accept(this).getName() + " already exists!");
                String vartype = ((Variable_t) n.f3.nodes.get(i).accept(this)).getType();
                if (!vartype.equals("int") && !vartype.equals("boolean") && !vartype.equals("int[]")) { //if not classic types
                    for (int j = 0 ; j < st.getST().size() ; j++)
                        if (vartype.equals(st.getST().get(j).getName()))
                            found = true;
                } else 
                    found = true;
                if (!found)
                    throw new Exception(name + ": Cannot declare " + vartype + " does not exist!");
            }
        if (n.f4.present())     // add Class Methods
            for (int i = 0 ; i < n.f4.size() ; i++) {
                boolean found = false;
                if (st.contains(name).addMethod((Method_t)n.f4.nodes.get(i).accept(this)) == false) // if method isnt unique
                    throw new Exception("Class " + name + ": Method " + n.f4.nodes.get(i).accept(this).getName() + " already exists!");
                String vartype = ((Method_t) n.f4.nodes.get(i).accept(this)).getType();
                if (!vartype.equals("int") && !vartype.equals("boolean") && !vartype.equals("int[]")) { //if not classic types
                    for (int j = 0 ; j < st.getST().size() ; j++)
                        if (vartype.equals(st.getST().get(j).getName()))
                            found = true;
                } else 
                    found = true;
                if (!found)
                    throw new Exception(name + ": Cannot declare " + vartype + " does not exist!");
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
        String did = n.f3.accept(this).getName();
        n.f4.accept(this);
        n.f5.accept(this);
        n.f6.accept(this);
        n.f7.accept(this);
        Class_t newClass = st.contains(id);
        Class_t dadClass = st.contains(did);
        for (int i = 0 ; i < dadClass.classVars.size() ; i++) //add variables from father-class
            newClass.copyVar((Variable_t) dadClass.classVars.get(i));
        if (n.f5.present())    // add Class Variables
            for (int i = 0 ; i < n.f5.size() ; i++)
                newClass.copyVar((Variable_t)n.f5.nodes.get(i).accept(this)); // if variable isnt unique
        for (int i = 0 ; i < dadClass.classMethods.size() ; i++) { //add methods from father-class 
            Method_t dadsMeth = (Method_t) dadClass.classMethods.get(i);
            if (dadsMeth.getName().equals("main"))
                continue ;
            newClass.copyMethod(dadsMeth);
            newClass.meth_cnt =dadClass.meth_cnt;
        }
        if (n.f6.present()) {     // add Class Methods
            for (int i = 0 ; i < n.f6.size() ; i++) {
                Method_t m = (Method_t) n.f6.nodes.get(i).accept(this);
                if (!dadClass.classContainsMeth(m.getName())) {  // if the father-class hasnt a method with this name
                    newClass.addMethod(m);
                }
                else { // replace method
                    for (int k = 0 ; k < newClass.classMethods.size() ; k++) {
                        if (newClass.classMethods.get(k).getName().equals( m.getName() ) ) {
                            m.comesFrom = newClass;
                            m.meth_num = k+1;
                            newClass.classMethods.set(k, m);
                        }
                    }
                }
            }
        }
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
        String type = n.f1.accept(this).getName();
        String id = n.f2.accept(this).getName();
        n.f3.accept(this);
        Method_t m = (Method_t) n.f4.accept(this);
        n.f5.accept(this);
        n.f6.accept(this);
        n.f7.accept(this);
        n.f8.accept(this);
        n.f9.accept(this);
        n.f10.accept(this);
        n.f11.accept(this);
        n.f12.accept(this);
        Method_t meth = new Method_t(type, id);
        if (n.f4.present())    // add parameters to method
            for (int i = 0 ; i < m.methodParams.size() ; i++) {
                boolean found = false;
                meth.addParam((Variable_t) m.methodParams.get(i));
                String vartype = ((Variable_t) m.methodParams.get(i)).getType();
                if (!vartype.equals("int") && !vartype.equals("boolean") && !vartype.equals("int[]")) { //if not classic types
                    for (int j = 0 ; j < st.getST().size() ; j++)
                        if (vartype.equals(st.getST().get(j).getName()))
                            found = true;
                } else 
                    found = true;
                if (!found)
                    throw new Exception(id + ": Cannot declare " + vartype + " does not exist!");
            }
        if (n.f7.present())     // add method Variables
            for (int i = 0 ; i < n.f7.size() ; i++) {
                if (!meth.addVar((Variable_t) n.f7.nodes.get(i).accept(this)))
                    throw new Exception("Method " + id + ": Variable " + n.f7.nodes.get(i).accept(this).getName() + " already exists!");
                boolean found = false;
                String vartype = ((Variable_t) n.f7.nodes.get(i).accept(this)).getType();
                if (!vartype.equals("int") && !vartype.equals("boolean") && !vartype.equals("int[]")) { //if not classic types
                    for (int j = 0 ; j < st.getST().size() ; j++)
                        if (vartype.equals(st.getST().get(j).getName()))
                            found = true;
                } else 
                    found = true;
                if (!found)
                    throw new Exception(id + ": Cannot declare " + vartype + " does not exist!");
            }
        if (st.glob_temp_cnt < meth.par_cnt)
            st.glob_temp_cnt = meth.par_cnt;
        return meth;
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
