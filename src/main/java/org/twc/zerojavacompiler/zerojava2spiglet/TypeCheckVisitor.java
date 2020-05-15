package org.twc.zerojavacompiler.zerojava2spiglet;

import org.twc.zerojavacompiler.zerojava2spiglet.zerojavasyntaxtree.*;
import org.twc.zerojavacompiler.zerojava2spiglet.zerojavavisitor.GJDepthFirst;
import org.twc.zerojavacompiler.basetype.*;

import java.util.Map;

public class TypeCheckVisitor extends GJDepthFirst<Base_t, Base_t> {

    private Map<String, Class_t> st_;

    public TypeCheckVisitor(Map<String, Class_t> st_) {
        this.st_ = st_;
    }

    public static Variable_t findType(Variable_t var, Method_t meth) throws Exception {
        if (var.getType() == null) {
            String inMethod = meth.methContains(var.getName());
            if (inMethod == null) {   // if not found in the function, we should seek in the class
                Variable_t inclassvar = meth.getFrom_class_().classContainsVar(var.getName());
                if (inclassvar == null) {
                    throw new Exception("Undeclared variable " + var.getName());
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
    public Base_t visit(Goal n, Base_t argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return null;
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
    public Base_t visit(MainClass n, Base_t argu) throws Exception {
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
        return null;
    }

    /**
     * f0 -> ClassDeclaration()
     * | ClassExtendsDeclaration()
     */
    public Base_t visit(TypeDeclaration n, Base_t argu) throws Exception {
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
    public Base_t visit(ClassDeclaration n, Base_t argu) throws Exception {
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
    public Base_t visit(ClassExtendsDeclaration n, Base_t argu) throws Exception {
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
    public Base_t visit(VarDeclaration n, Base_t argu) throws Exception {
        n.f2.accept(this, argu);
        return new Variable_t(((Variable_t) n.f0.accept(this, argu)).getType(), n.f1.accept(this, argu).getName());
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
    public Base_t visit(MethodDeclaration n, Base_t argu) throws Exception {
        String methName = n.f2.accept(this, argu).getName();
        Method_t meth = ((Class_t) argu).getMethod(methName);
        n.f0.accept(this, meth);
        String methType = ((Variable_t) n.f1.accept(this, argu)).getType();
        n.f8.accept(this, meth);
        Variable_t retType = (Variable_t) n.f10.accept(this, meth);
        retType = findType(retType, meth);
        if (!meth.getType_().equals(retType.getType())) {
            throw new Exception("Error at " + methName + " declaration, type_ is " + methType + " and return type_ is " + retType.getType() + ", meth " + meth.getFrom_class_().getName());
        }
        return null;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public Base_t visit(FormalParameterList n, Base_t argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public Base_t visit(FormalParameter n, Base_t argu) throws Exception {
        return new Variable_t(((Variable_t) n.f0.accept(this, argu)).getType(), n.f1.accept(this, argu).getName());
    }

    /**
     * f0 -> ( FormalParameterTerm() )*
     */
    public Base_t visit(FormalParameterTail n, Base_t argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    public Base_t visit(FormalParameterTerm n, Base_t argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return null;
    }

    /**
     * f0 -> ArrayType()
     * | BooleanType()
     * | IntegerType()
     * | Identifier()
     */
    public Base_t visit(Type n, Base_t argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public Base_t visit(ArrayType n, Base_t argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return new Variable_t("int[]", null);
    }

    /**
     * f0 -> "boolean"
     */
    public Base_t visit(BooleanType n, Base_t argu) throws Exception {
        return new Variable_t("boolean", null);
    }

    /**
     * f0 -> "int"
     */
    public Base_t visit(IntegerType n, Base_t argu) throws Exception {
        return new Variable_t("int", null);
    }

    /**
     * f0 -> Block()
     * | AssignmentStatement()
     * | IncrementAssignmentStatement()
     * | DecrementAssignmentStatement()
     * | CompoundAssignmentStatement()
     * | ArrayAssignmentStatement()
     * | IfStatement()
     * | WhileStatement()
     * | PrintStatement()
     * | PrintLineStatement()
     * | AnswerStatement()
     */
    public Base_t visit(Statement n, Base_t argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "{"
     * f1 -> ( Statement() )*
     * f2 -> "}"
     */
    public Base_t visit(Block n, Base_t argu) throws Exception {
        n.f1.accept(this, argu);
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public Base_t visit(AssignmentStatement n, Base_t argu) throws Exception {
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
                if (parent.getParent() == null) {
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
     * f1 -> "++"
     * f2 -> ";"
     */
    public Base_t visit(IncrementAssignmentStatement n, Base_t argu) throws Exception {
        Variable_t t1 = (Variable_t) n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        t1 = findType(t1, (Method_t) argu);
        if (!t1.getType().equals("int")) {
            throw new Exception("Error increment assignment (++) is only allowed to int type. Found " + t1.getType());
        }
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "--"
     * f2 -> ";"
     */
    public Base_t visit(DecrementAssignmentStatement n, Base_t argu) throws Exception {
        Variable_t t1 = (Variable_t) n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        t1 = findType(t1, (Method_t) argu);
        if (!t1.getType().equals("int")) {
            throw new Exception("Error decrement assignment (--) is only allowed to int type. Found " + t1.getType());
        }
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> CompoundOperator()
     * f2 -> Expression()
     * f3 -> ";"
     */
    public Base_t visit(CompoundAssignmentStatement n, Base_t argu) throws Exception {
        Variable_t t1 = (Variable_t) n.f0.accept(this, argu);
        String operator = n.f1.accept(this, argu).getName();
        Variable_t t2 = (Variable_t) n.f2.accept(this, argu);
        t1 = findType(t1, (Method_t) argu);
        t2 = findType(t2, (Method_t) argu);
        if (t1.getType().equals("int") && t2.getType().equals("int")) {
            return null;
        }
        throw new Exception("Error compound assignment between different types (" + operator + ") : " + t1.getType() + " " + t2.getType());
    }

    /**
     * f0 -> "+="
     * | 	"-="
     * | 	"*="
     * | 	"/="
     * | 	"%="
     * | 	"<<="
     * | 	">>="
     * | 	"&="
     * | 	"|="
     * | 	"^="
     */
    public Base_t visit(CompoundOperator n, Base_t argu) throws Exception {
        String[] _ret = {"+=", "-=", "*=", "/=", "%=", "<<=", ">>=", "&=", "|=", "^="};
        return new Variable_t("int", _ret[n.f0.which]);
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
    public Base_t visit(ArrayAssignmentStatement n, Base_t argu) throws Exception {
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
     * f0 -> IfthenElseStatement()
     * | IfthenStatement()
     */
    public Base_t visit(IfStatement n, Base_t argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public Base_t visit(IfthenStatement n, Base_t argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        Variable_t expr = (Variable_t) n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        if (expr.getType() == null) {
            expr = findType(expr, (Method_t) argu);
        }
        if (expr.getType().equals("boolean")) {
            return null;
        }
        throw new Exception("IfthenStatement is not a boolean expression: " + expr.getType());
    }

    /**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     * f5 -> "else"
     * f6 -> Statement()
     */
    public Base_t visit(IfthenElseStatement n, Base_t argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        Variable_t expr = (Variable_t) n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        if (expr.getType() == null) {
            expr = findType(expr, (Method_t) argu);
        }
        if (expr.getType().equals("boolean")) {
            return null;
        }
        throw new Exception("IfthenElseStatement is not a boolean expression: " + expr.getType());
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public Base_t visit(WhileStatement n, Base_t argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        Variable_t expr = (Variable_t) n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        if (expr.getType() == null) {
            expr = findType(expr, (Method_t) argu);
        }
        if (expr.getType().equals("boolean")) {
            return null;
        }
        throw new Exception("WhileStatement is not a boolean Expression");
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public Base_t visit(PrintStatement n, Base_t argu) throws Exception { //is int
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        Variable_t expr = (Variable_t) n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        if (expr.getType() == null) {
            expr = findType(expr, (Method_t) argu);
        }
        if (expr.getType().equals("boolean") || expr.getType().equals("int")) {
            return null;
        }
        throw new Exception("Print statement not boolean or int.");
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> ")"
     * f3 -> ";"
     */
    public Base_t visit(PrintLineStatement n, Base_t argu) throws Exception { //is int
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        return null;
    }

    /**
     * f0 -> "Prover.answer"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public Base_t visit(AnswerStatement n, Base_t argu) throws Exception { //is int
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        Variable_t expr = (Variable_t) n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        if (expr.getType() == null) {
            expr = findType(expr, (Method_t) argu);
        }
        if (expr.getType().equals("boolean") || expr.getType().equals("int")) {
            return null;
        }
        throw new Exception("Answer statement not boolean or int.");
    }

    /**
     * f0 -> LogicalAndExpression()
     * | LogicalOrExpression()
     * | BinaryExpression()
     * | BinNotExpression()
     * | ArrayLookup()
     * | ArrayLength()
     * | MessageSend()
     * | TernaryExpression()
     * | PublicReadExpression()
     * | PrivateReadExpression()
     * | PublicSeekExpression()
     * | PrivateSeekExpression()
     * | Clause()
     */
    public Base_t visit(Expression n, Base_t argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public Base_t visit(LogicalAndExpression n, Base_t argu) throws Exception {
        Variable_t clause_1 = (Variable_t) n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        Variable_t clause_2 = (Variable_t) n.f2.accept(this, argu);
        String t1 = findType(clause_1, (Method_t) argu).getType();
        String t2 = findType(clause_2, (Method_t) argu).getType();
        if (t1.equals("boolean") && t2.equals("boolean")) {
            return new Variable_t("boolean", null);
        }
        throw new Exception("Bad operand types for operator '&&': " + t1 + " " + t2);
    }

    /**
     * f0 -> Clause()
     * f1 -> "||"
     * f2 -> Clause()
     */
    public Base_t visit(LogicalOrExpression n, Base_t argu) throws Exception {
        Variable_t clause_1 = (Variable_t) n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        Variable_t clause_2 = (Variable_t) n.f2.accept(this, argu);
        String t1 = findType(clause_1, (Method_t) argu).getType();
        String t2 = findType(clause_2, (Method_t) argu).getType();
        if (t1.equals("boolean") && t2.equals("boolean")) {
            return new Variable_t("boolean", null);
        }
        throw new Exception("Bad operand types for operator '||': " + t1 + " " + t2);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> BinOperator()
     * f2 -> PrimaryExpression()
     */
    public Base_t visit(BinaryExpression n, Base_t argu) throws Exception {
        Variable_t clause_1 = (Variable_t) n.f0.accept(this, argu);
        String operator = n.f1.accept(this, argu).getName();
        Variable_t clause_2 = (Variable_t) n.f2.accept(this, argu);
        String t1 = findType(clause_1, (Method_t) argu).getType();
        String t2 = findType(clause_2, (Method_t) argu).getType();
        if ("&".equals(operator) || "|".equals(operator) || "^".equals(operator) || "<<".equals(operator) || ">>".equals(operator)
                || "+".equals(operator) || "-".equals(operator) || "*".equals(operator) || "/".equals(operator) || "%".equals(operator)
        ) {
            if (t1.equals("int") && t2.equals("int")) {
                return new Variable_t("int", null);
            }
        } else if ("==".equals(operator) || "!=".equals(operator) || "<".equals(operator) || "<=".equals(operator) || ">".equals(operator) || ">=".equals(operator)) {
            if (t1.equals("boolean") && t2.equals("boolean")) {
                return new Variable_t("boolean", null);
            } else if (t1.equals("int") && t2.equals("int")) {
                return new Variable_t("boolean", null);
            }
        }
        throw new Exception("Bad operand types for operator '" + operator + "': " + t1 + " " + t2);
    }

    /**
     * f0 -> "&"
     * |	"|"
     * |	"^"
     * |	"<<"
     * |	">>"
     * |	"+"
     * |	"-"
     * |	"*"
     * |	"/"
     * |	"%"
     * |	"=="
     * |	"!="
     * |	"<"
     * |	"<="
     * |	">"
     * |	">="
     */
    public Base_t visit(BinOperator n, Base_t argu) throws Exception {
        String[] _ret = {"&", "|", "^", "<<", ">>", "+", "-", "*", "/", "%", "==", "!=", "<", "<=", ">", ">="};
        String operator = _ret[n.f0.which];
        if ("&".equals(operator) || "|".equals(operator) || "^".equals(operator)
                || "<<".equals(operator) || ">>".equals(operator) || "<<=".equals(operator) || ">>=".equals(operator)
                || "+".equals(operator) || "-".equals(operator) || "*".equals(operator) || "/".equals(operator) || "%".equals(operator)) {
            return new Variable_t("int", operator);
        } else if ("==".equals(operator) || "!=".equals(operator) || "<".equals(operator) || "<=".equals(operator) || ">".equals(operator) || ">=".equals(operator)) {
            return new Variable_t("boolean", operator);
        } else {
            throw new IllegalStateException("BinOperator: Unexpected value: " + operator);
        }
    }

    /**
     * f0 -> "~"
     * f1 -> PrimaryExpression()
     */
    public Base_t visit(BinNotExpression n, Base_t argu) throws Exception {
        Variable_t clause_1 = (Variable_t) n.f1.accept(this, argu);
        String t1 = findType(clause_1, (Method_t) argu).getType();
        if (t1.equals("int")) {
            return new Variable_t("int", null);
        }
        throw new Exception("Bad operand type for operator '~': " + t1);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public Base_t visit(ArrayLookup n, Base_t argu) throws Exception {
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
    public Base_t visit(ArrayLength n, Base_t argu) throws Exception {
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
    public Base_t visit(MessageSend n, Base_t argu) throws Exception {
        Variable_t clazz = (Variable_t) n.f0.accept(this, argu);
        clazz = findType(clazz, (Method_t) argu);                       // now clazz.type_() is the type_ of PrimaryExp
        Variable_t id = (Variable_t) n.f2.accept(this, argu);
        Class_t cl = st_.get(clazz.getType());
        if (!cl.classContainsMeth(id.getName())) {                       // check if class primary expr type_ contains method identifier
            throw new Exception("Method " + id.getName() + " is not declared in class " + clazz.getType());
        }
        Method_t existingmeth = cl.getMethod(id.getName());             // get method identifier (also parameters etc)
        Method_t keepParams = (Method_t) n.f4.accept(this, argu);
        if (n.f4.present()) {   // add parameters to method
            if (existingmeth.method_params.size() != keepParams.method_params.size()) {
                throw new Exception("Number of parameters error." +
                        "\n" + existingmeth.getName() + ": " + existingmeth.method_params.size() +
                        "\n" + keepParams.getName() + ": " + keepParams.method_params.size());
            }
            for (int i = 0; i < existingmeth.method_params.size(); i++) { // for each parameter
                String vartype = keepParams.method_params.get(i).getType();
                if (vartype == null) {
                    Variable_t tmpvar = new Variable_t(null, keepParams.method_params.get(i).getName());
                    tmpvar = findType(tmpvar, (Method_t) argu);
                    vartype = tmpvar.getType();
                }
                if (!vartype.equals(existingmeth.method_params.get(i).getType())) {
                    Class_t parent = st_.get(vartype);
                    if (parent != null) {
                        while (!parent.getName().equals(existingmeth.method_params.get(i).getType())) {
                            if (parent.getParent() == null) {
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
        return new Variable_t(existingmeth.getType_(), null);
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     * f3 -> "?"
     * f4 -> Expression()
     * f5 -> ":"
     * f6 -> Expression()
     */
    public Base_t visit(TernaryExpression n, Base_t argu) throws Exception {
        Variable_t expr = (Variable_t) n.f1.accept(this, argu);
        expr = findType(expr, (Method_t) argu);
        if (expr.getType() == null) {
            expr = findType(expr, (Method_t) argu);
        }
        Variable_t expr_1 = (Variable_t) n.f4.accept(this, argu);
        expr_1 = findType(expr_1, (Method_t) argu);
        if (expr_1.getType() == null) {
            expr_1 = findType(expr_1, (Method_t) argu);
        }
        Variable_t expr_2 = (Variable_t) n.f6.accept(this, argu);
        expr_2 = findType(expr_2, (Method_t) argu);
        if (expr_2.getType() == null) {
            expr_2 = findType(expr_2, (Method_t) argu);
        }
        if (expr.getType().equals("boolean")) {
            if (expr_1.getType().equals(expr_2.getType())) {
                return new Variable_t(expr_1.getType(), null);
            }
            throw new Exception("Ternary types missmatch: " + expr_1.getType() + " " + expr_2.getType());
        }
        throw new Exception("If-condition is not a boolean Expression");
    }

    /**
     * f0 -> <PUBLIC_READ>
     * f1 -> "("
     * f2 -> ")"
     */
    public Base_t visit(PublicReadExpression n, Base_t argu) throws Exception {
        return new Variable_t("int", null);
    }

    /**
     * f0 -> <PRIVATE_READ>
     * f1 -> "("
     * f2 -> ")"
     */
    public Base_t visit(PrivateReadExpression n, Base_t argu) throws Exception {
        return new Variable_t("int", null);
    }

    /**
     * f0 -> <PUBLIC_SEEK>
     * f1 -> "("
     * f2 -> PrimaryExpression()
     * f3 -> ")"
     */
    public Base_t visit(PublicSeekExpression n, Base_t argu) throws Exception {
        String type = findType((Variable_t) n.f2.accept(this, argu), (Method_t) argu).getType();
        if (type.equals("int")) {
            return new Variable_t("int", null);
        }
        throw new Exception("Seek statement index should be integer");
    }

    /**
     * f0 -> <PRIVATE_SEEK>
     * f1 -> "("
     * f2 -> PrimaryExpression()
     * f3 -> ")"
     */
    public Base_t visit(PrivateSeekExpression n, Base_t argu) throws Exception {
        String type = findType((Variable_t) n.f2.accept(this, argu), (Method_t) argu).getType();
        if (type.equals("int")) {
            return new Variable_t("int", null);
        }
        throw new Exception("Seek statement index should be integer");
    }

    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public Base_t visit(ExpressionList n, Base_t argu) throws Exception {
        Variable_t expr = (Variable_t) n.f0.accept(this, argu);
        Method_t meth = (Method_t) n.f1.accept(this, argu);
        meth.method_params.add(expr);
        return meth;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    public Base_t visit(ExpressionTail n, Base_t argu) throws Exception {
        Method_t meth = new Method_t(null, null);
        // create a linked list of variables. (parameters list)
        if (n.f0.present()) {
            for (int i = 0; i < n.f0.size(); i++) {
                meth.method_params.add((Variable_t) n.f0.nodes.get(i).accept(this, argu));
            }
        }
        return meth;
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public Base_t visit(ExpressionTerm n, Base_t argu) throws Exception {
        n.f0.accept(this, argu);
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> NotExpression()
     * | PrimaryExpression()
     */
    public Base_t visit(Clause n, Base_t argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> IntegerLiteral()
     * | TrueLiteral()
     * | FalseLiteral()
     * | Identifier()
     * | ThisExpression()
     * | ArrayAllocationExpression()
     * | AllocationExpression()
     * | BracketExpression()
     */
    public Base_t visit(PrimaryExpression n, Base_t argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public Base_t visit(IntegerLiteral n, Base_t argu) throws Exception {
        return new Variable_t("int", null);
    }

    /**
     * f0 -> "true"
     */
    public Base_t visit(TrueLiteral n, Base_t argu) throws Exception {
        return new Variable_t("boolean", null);
    }

    /**
     * f0 -> "false"
     */
    public Base_t visit(FalseLiteral n, Base_t argu) throws Exception {
        return new Variable_t("boolean", null);
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public Base_t visit(Identifier n, Base_t argu) throws Exception {
        return new Variable_t(null, n.f0.toString());
    }

    /**
     * f0 -> "this"
     */
    public Base_t visit(ThisExpression n, Base_t argu) throws Exception {
        n.f0.accept(this, argu);
        return new Variable_t(((Method_t) argu).getFrom_class_().getName(), null);
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public Base_t visit(ArrayAllocationExpression n, Base_t argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        Variable_t t = (Variable_t) n.f3.accept(this, argu);
        t = findType(t, (Method_t) argu);
        if (!t.getType().equals("int")) {
            throw new Exception("Error: new int[" + t.getType() + "], " + t.getType() + " should be int");
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
    public Base_t visit(AllocationExpression n, Base_t argu) throws Exception {
        n.f0.accept(this, argu);
        Variable_t classname = new Variable_t(n.f1.accept(this, argu).getName(), null);
        n.f2.accept(this, argu);
        n.f3.accept(this, argu);
        // if class does not exist
        if (st_.get(classname.getType()) == null) {
            throw new Exception("Cannot declare " + classname + " type_. This class does not exist");
        }
        return classname;
    }

    /**
     * f0 -> ""
     * f1 -> Clause()
     */
    public Base_t visit(NotExpression n, Base_t argu) throws Exception {
        n.f0.accept(this, argu);
        Variable_t t = (Variable_t) n.f1.accept(this, argu);
        t = findType(t, (Method_t) argu);
        if (t.getType().equals("boolean")) {
            return new Variable_t("boolean", null);
        }
        throw new Exception("Error: Not Clause, " + t + " type_ given. Can apply only to boolean");
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public Base_t visit(BracketExpression n, Base_t argu) throws Exception {
        n.f0.accept(this, argu);
        n.f2.accept(this, argu);
        return n.f1.accept(this, argu);
    }

}
