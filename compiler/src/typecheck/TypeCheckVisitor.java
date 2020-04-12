package typecheck;

import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;
import symboltable.*;
import basetype.*;
import java.util.Map;
import java.util.HashMap;

public class TypeCheckVisitor extends GJDepthFirst<BaseType, BaseType> {

    private Map<String, Class_t> st_;

    public TypeCheckVisitor(Map<String, Class_t> st_) {
        this.st_ = st_;
    }

    public static Variable_t findType(Variable_t var, Method_t meth) throws Exception {
        if (var.getType() == null) {
            String inMethod = meth.methContains(var.getName());
            if (inMethod == null) {   // if not found in the function, we should seek in the class
                Variable_t inclassvar = meth.getFromClass().classContainsVar(var.getName());
                if (inclassvar == null) {
                    throw new Exception("Undecleared Variable " + var.getName());
                }
                inMethod = inclassvar.getType();
            }
            return new Variable_t(inMethod, var.getName());
        }
        return var;
    }

    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public BaseType visit(Goal n, BaseType argu) throws Exception {
        Variable_t _ret = null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
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
     * f8 -> "Variable_t"
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
    public BaseType visit(MainClass n, BaseType argu) throws Exception {
        Variable_t _ret = null;
        n.f0.accept(this, argu);
        String id = n.f1.accept(this, argu).getName();
        Class_t mainclazz = st_.get(id);
        Method_t meth = mainclazz.getMethod("main");
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        n.f7.accept(this, argu);
        n.f8.accept(this, argu);
        n.f9.accept(this, argu);
        n.f10.accept(this, argu);
        n.f11.accept(this, argu);
        n.f12.accept(this, argu);
        n.f13.accept(this, argu);
        n.f14.accept(this, argu);
        n.f15.accept(this, meth);
        n.f16.accept(this, argu);
        n.f17.accept(this, argu);
        return _ret;
    }

    /**
    * f0 -> ClassDeclaration()
    *       | ClassExtendsDeclaration()
    */
    public BaseType visit(TypeDeclaration n, BaseType argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
    public BaseType visit(ClassDeclaration n, BaseType argu) throws Exception {
        n.f0.accept(this, argu);
        String s = n.f1.accept(this, argu).getName();
        Class_t class_id = st_.get(s);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, class_id);
        n.f5.accept(this, argu);
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
    public BaseType visit(ClassExtendsDeclaration n, BaseType argu) throws Exception {
        n.f0.accept(this, argu);
        String s = n.f1.accept(this, argu).getName();
        Class_t class_id = st_.get(s);
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, class_id);
        n.f7.accept(this, argu);
        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    public BaseType visit(VarDeclaration n, BaseType argu) throws Exception {
        n.f2.accept(this, argu);
        return new Variable_t(((Variable_t)n.f0.accept(this, argu)).getType(), n.f1.accept(this, argu).getName());
    }

    /**
    * f0 -> "public"
    * f1 -> Type()
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
    public BaseType visit(MethodDeclaration n, BaseType argu) throws Exception {
        String methName = n.f2.accept(this, argu).getName();
        Method_t meth = ((Class_t) argu).getMethod(methName);
        n.f0.accept(this, meth);
        String methType = ((Variable_t)n.f1.accept(this, argu)).getType();
        n.f8.accept(this, meth);
        Variable_t retType = (Variable_t) n.f10.accept(this, meth);
        retType = findType(retType, meth);
        if (!meth.getType().equals(retType.getType())) {
            throw new Exception("Error at " + methName + " declaration, type_ is " + methType + " and return type_ is "+ retType.getType() + ", meth " +meth.getFromClass().getName());
        }
        return null;
    }

    /**
    * f0 -> FormalParameter()
    * f1 -> FormalParameterTail()
    */
    public BaseType visit(FormalParameterList n, BaseType argu) throws Exception {
        Variable_t _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
    public BaseType visit(FormalParameter n, BaseType argu) throws Exception {
        return new Variable_t(((Variable_t)n.f0.accept(this, argu)).getType(), n.f1.accept(this, argu).getName());
    }

    /**
    * f0 -> ( FormalParameterTerm() )*
    */
    public BaseType visit(FormalParameterTail n, BaseType argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> ","
    * f1 -> FormalParameter()
    */
    public BaseType visit(FormalParameterTerm n, BaseType argu) throws Exception {
        Variable_t _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
    public BaseType visit(Type n, BaseType argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
    public BaseType visit(ArrayType n, BaseType argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return new Variable_t("int[]", null);
    }

    /**
    * f0 -> "Variable_t"
    */
    public BaseType visit(BooleanType n, BaseType argu) throws Exception {
        return new Variable_t("boolean", null);
    }

    /**
    * f0 -> "int"
    */
    public BaseType visit(IntegerType n, BaseType argu) throws Exception {
        return new Variable_t("int", null);
    }

    /**
    * f0 -> Block()
    *       | AssignmentStatement()
    *       | ArrayAssignmentStatement()
    *       | IfStatement()
    *       | WhileStatement()
    *       | PrintStatement()
    */
    public BaseType visit(Statement n, BaseType argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> "{"
    * f1 -> ( Statement() )*
    * f2 -> "}"
    */
    public BaseType visit(Block n, BaseType argu) throws Exception {
        n.f1.accept(this, argu);
        return null;
    }

    /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    public BaseType visit(AssignmentStatement n, BaseType argu) throws Exception {
        Variable_t t1 = (Variable_t) n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        Variable_t t2 = (Variable_t) n.f2.accept(this, argu);
        t1 = findType(t1, (Method_t) argu);
        t2 = findType(t2, (Method_t) argu);
        // check for methods (extended or not) and for non-extended vars
        if (t1.getType().equals(t2.getType())) {
            return null;
        }
        // now for extended vars
        Class_t parent = st_.get(t2.getType());
        if (parent != null) {
            while (!parent.getName().equals(t1.getType())) {
                if (parent == null || parent.getParent() == null) {
                    throw new Exception("Error assignment between different types: " + t1.getType() + " " + t2.getType());
                }
                parent = st_.get(parent.getParent());
            }
            return null;
        }
        throw new Exception("Error assignment between different types: " + t1.getType() + " " + t2.getType());
    }

    /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
    public BaseType visit(ArrayAssignmentStatement n, BaseType argu) throws Exception {
        Variable_t t1 = (Variable_t) n.f2.accept(this, argu);
        n.f1.accept(this, argu);
        Variable_t t2 = (Variable_t) n.f5.accept(this, argu);
        t1 = findType(t1, (Method_t) argu);
        t2 = findType(t2, (Method_t) argu);
        if (t1.getType().equals("int") && t2.getType().equals("int")) {
            return null;
        }
        throw new Exception("Error assignment between different types: " + t1.getType() + " " + t2.getType());
    }

    /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> ( "else" Statement() )?
    */
    public BaseType visit(IfStatement n, BaseType argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        Variable_t expr = (Variable_t) n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        if (expr.getType() == null) {
            expr = findType(expr, (Method_t)argu);
        }
        if (expr.getType().equals("boolean")) {
            return null;
        }
        throw new Exception("If-condition is not a boolean Expression!");
    }

    /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    public BaseType visit(WhileStatement n, BaseType argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        Variable_t expr = (Variable_t) n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        if (expr.getType() == null) {
            expr = findType(expr, (Method_t)argu);
        }
        if (expr.getType().equals("boolean")) {
            return null;
        }
        throw new Exception("while-condition is not a boolean Expression!");
    }

    /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    public BaseType visit(PrintStatement n, BaseType argu) throws Exception { //is int
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        Variable_t expr = (Variable_t) n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        if (expr.getType() == null) {
            expr = findType(expr, (Method_t)argu);
        }
        if (expr.getType().equals("boolean") || expr.getType().equals("int")) {
            return null;
        }
        throw new Exception("Print Statement not boolean or int!");
    }

    /**
    * f0 -> AndExpression()
    *       | CompareExpression()
    *       | PlusExpression()
    *       | MinusExpression()
    *       | TimesExpression()
    *       | ArrayLookup()
    *       | ArrayLength()
    *       | MessageSend()
    *       | Clause()
    */
    public BaseType visit(Expression n, BaseType argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
    public BaseType visit(AndExpression n, BaseType argu) throws Exception {
        Variable_t t1 = (Variable_t) n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        Variable_t t2 = (Variable_t) n.f2.accept(this, argu);
        t1 = findType(t1, (Method_t) argu);
        t2 = findType(t2, (Method_t) argu);
        if (t1.getType().equals("boolean") && t2.getType().equals("boolean")) {
            return new Variable_t("boolean", null);
        }
        throw new Exception("Logical And between different types: " + t1.getType() + " " + t2.getType());
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    public BaseType visit(CompareExpression n, BaseType argu) throws Exception {
        Variable_t t1 = (Variable_t) n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        Variable_t t2 = (Variable_t) n.f2.accept(this, argu);
        t1 = findType(t1, (Method_t) argu);
        t2 = findType(t2, (Method_t) argu);
        if (t1.getType().equals("int") && t2.getType().equals("int")) {
            return new Variable_t("boolean", null);
        }
        throw new Exception("Compare between different types: " + t1.getType() + " " + t2.getType());
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    public BaseType visit(PlusExpression n, BaseType argu) throws Exception {
        Variable_t t1 = (Variable_t) n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        Variable_t t2 = (Variable_t) n.f2.accept(this, argu);
        t1 = findType(t1, (Method_t) argu);
        t2 = findType(t2, (Method_t) argu);
        if (t1.getType().equals("int") && t2.getType().equals("int")) {
            return new Variable_t("int", null);
        }
        throw new Exception("Addition between different types: " + t1.getType() + " " + t2.getType());
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    public BaseType visit(MinusExpression n, BaseType argu) throws Exception {
        Variable_t t1 = (Variable_t) n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        Variable_t t2 = (Variable_t) n.f2.accept(this, argu);
        t1 = findType(t1, (Method_t) argu);
        t2 = findType(t2, (Method_t) argu);
        if (t1.getType().equals("int") && t2.getType().equals("int")) {
            return new Variable_t("int", null);
        }
        throw new Exception("Substraction between different types: " + t1.getType() + " " + t2.getType());
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    public BaseType visit(TimesExpression n, BaseType argu) throws Exception {
        Variable_t t1 = (Variable_t) n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        Variable_t t2 = (Variable_t) n.f2.accept(this, argu);
        t1 = findType(t1, (Method_t) argu);
        t2 = findType(t2, (Method_t) argu);
        if (t1.getType().equals("int") && t2.getType().equals("int")) {
            return new Variable_t("int", null);
        }
        throw new Exception("Multiply between different types: " + t1.getType() + " " + t2.getType());
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    public BaseType visit(ArrayLookup n, BaseType argu) throws Exception {
        Variable_t t1 = (Variable_t) n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        Variable_t t2 = (Variable_t) n.f2.accept(this, argu);
        t1 = findType(t1, (Method_t) argu);
        t2 = findType(t2, (Method_t) argu);
        if (t1.getType().equals("int[]") && t2.getType().equals("int")) {
            return new Variable_t("int", null);
        }
        throw new Exception("ArrayLookup between different types: " + t1.getType() + " " + t2.getType());
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    public BaseType visit(ArrayLength n, BaseType argu) throws Exception {
        Variable_t t1 = (Variable_t) n.f0.accept(this, argu);
        t1 = findType(t1, (Method_t) argu);
        if (t1.getType().equals("int[]")) {
            return new Variable_t("int", null);
        }
        throw new Exception("ArrayLength in something not int[]: " + t1.getType());
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    public BaseType visit(MessageSend n, BaseType argu) throws Exception {
        Variable_t clazz = (Variable_t) n.f0.accept(this, argu);
        clazz = findType(clazz, (Method_t) argu);                       // now clazz.type_() is the type_ of PrimaryExp
        Variable_t id = (Variable_t) n.f2.accept(this, argu);
        Class_t cl = st_.get(clazz.getType());
        if (!cl.classContainsMeth(id.getName()))                        // check if class primary expr type_ contains method identifier
            throw new Exception("Method " + id.getName() + " is not declared in class " + clazz.getType());
        Method_t existingmeth = cl.getMethod(id.getName());             // get method identifier (also parameters etc)
        Method_t keepParams = (Method_t) n.f4.accept(this, argu);
        if (n.f4.present()) {   // add parameters to method
            if (existingmeth.method_params.size() != keepParams.method_params.size()) {
                throw new Exception("Number of parameters error." +
                    "\n" + existingmeth.getName() + ": " + existingmeth.method_params.size() +
                    "\n" + keepParams.getName() + ": " + keepParams.method_params.size());
            }
            for (int i = 0 ; i < existingmeth.method_params.size() ; i++) { // for each parameter
                String vartype = ((Variable_t) keepParams.method_params.get(i)).getType();
                if (vartype == null) {
                    Variable_t tmpvar = new Variable_t(null, keepParams.method_params.get(i).getName());
                    tmpvar = findType(tmpvar, (Method_t) argu);
                    vartype = tmpvar.getType();
                }
                if (vartype.equals(existingmeth.method_params.get(i).getType())) {
                    continue;
                } else {
                    Class_t parent = st_.get(vartype);
                    if (parent != null) {
                        while (!parent.getName().equals(existingmeth.method_params.get(i).getType())) {
                            if (parent == null || parent.getParent() == null) {
                                throw new Exception("Error assignment between different types " + vartype);
                            }
                            parent = st_.get(parent.getParent());
                        }
                        continue;
                    }
                    throw new Exception("Error assignment between different types " + vartype);
                }
            }
        }
        return new Variable_t(existingmeth.getType(), null);
    }

    /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public BaseType visit(ExpressionList n, BaseType argu) throws Exception {
        Variable_t expr =  (Variable_t) n.f0.accept(this, argu);
        Method_t meth = (Method_t) n.f1.accept(this, argu);
        meth.method_params.add(expr);
        return meth;
    }

    /**
    * f0 -> ( ExpressionTerm() )*
    */
    public BaseType visit(ExpressionTail n, BaseType argu) throws Exception {
        Method_t meth = new Method_t(null, null);
        // create a linked list of variables. (parameters list)
        if (n.f0.present()) {
            for (int i = 0 ; i < n.f0.size() ; i++) {
                meth.method_params.add( (Variable_t)n.f0.nodes.get(i).accept(this, argu) );
            }
        }
        return meth;
    }

    /**
    * f0 -> ","
    * f1 -> Expression()
    */
    public BaseType visit(ExpressionTerm n, BaseType argu) throws Exception {
        n.f0.accept(this, argu);
        return (Variable_t) n.f1.accept(this, argu);
    }

    /**
    * f0 -> NotExpression()
    *       | PrimaryExpression()
    */
    public BaseType visit(Clause n, BaseType argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | BracketExpression()
    */
    public BaseType visit(PrimaryExpression n, BaseType argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> <INTEGER_LITERAL>
    */
    public BaseType visit(IntegerLiteral n, BaseType argu) throws Exception {
        return new Variable_t("int", null);
    }

    /**
    * f0 -> "true"
    */
    public BaseType visit(TrueLiteral n, BaseType argu) throws Exception {
        return new Variable_t("boolean", null);
    }

    /**
    * f0 -> "false"
    */
    public BaseType visit(FalseLiteral n, BaseType argu) throws Exception {
        return new Variable_t("boolean", null);
    }

    /**
    * f0 -> <IDENTIFIER>
    */
    public BaseType visit(Identifier n, BaseType argu) throws Exception {
        return new Variable_t(null, n.f0.toString());
    }

    /**
    * f0 -> "this"
    */
    public BaseType visit(ThisExpression n, BaseType argu) throws Exception {
        n.f0.accept(this, argu);
        Variable_t var = new Variable_t(((Method_t) argu).getFromClass().getName(), null);
        return var;
    }

    /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public BaseType visit(ArrayAllocationExpression n, BaseType argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        Variable_t t = (Variable_t) n.f3.accept(this, argu);
        t = findType(t, (Method_t) argu);
        if (!t.getType().equals("int")) {
            throw new Exception("Error: new int[" + t.getType() + "], " + t.getType() + " should be int!");
        }
        n.f4.accept(this, argu);
        return new Variable_t("int[]", null);
    }

    /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    public BaseType visit(AllocationExpression n, BaseType argu) throws Exception {
        n.f0.accept(this, argu);
        Variable_t classname = new Variable_t(n.f1.accept(this, argu).getName(), null);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        // if class does not exist
        if (st_.get(classname.getType()) == null) {
            throw new Exception("Cannot declare " + classname + " type_. This class does not exist!");
        }
        return classname;
    }

    /**
    * f0 -> "!"
    * f1 -> Clause()
    */
    public BaseType visit(NotExpression n, BaseType argu) throws Exception {
        n.f0.accept(this, argu);
        Variable_t t = (Variable_t) n.f1.accept(this, argu);
        t = findType(t, (Method_t) argu);
        if (t.getType().equals("boolean")) {
            return new Variable_t("boolean", null);
        }
        throw new Exception("Error: Not Clause, " + t + " type_ given. Can apply only to boolean!");
    }

    /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    public BaseType visit(BracketExpression n, BaseType argu) throws Exception {
        n.f0.accept(this, argu);
        n.f2.accept(this, argu);
        return n.f1.accept(this, argu);
    }

}
