package org.twc.zerojavacompiler.zerojava2spiglet;

import org.twc.zerojavacompiler.zerojava2spiglet.zerojavasyntaxtree.*;
import org.twc.zerojavacompiler.zerojava2spiglet.zerojavavisitor.GJDepthFirst;
import org.twc.zerojavacompiler.basetype.*;

import java.util.*;

public class ZeroJava2Spiglet extends GJDepthFirst<Base_t, Base_t> {

    private StringBuilder asm_;
    private final Map<String, Class_t> st_;
    private int label_cnt_;
    private int globals_;
    private int hp_;
    private boolean may_has_error_;

    public String newLabel() {
        return "L" + (++this.label_cnt_);
    }

    public boolean mayHasError() {
        return this.may_has_error_;
    }

    public ZeroJava2Spiglet(Map<String, Class_t> st, int globals, int init_heap_offset) {
        this.label_cnt_ = 0;
        this.st_ = st;
        this.globals_ = globals;
        this.hp_ = init_heap_offset;
        this.may_has_error_ = false;
    }

    private void initVtables() {
        for (Map.Entry<String, Class_t> entry : st_.entrySet()) {
            Class_t cl = entry.getValue();
            if (cl.isMain()) continue;
            String vtable_reg = newTemp();
            this.asm_.append("MOVE ").append(vtable_reg).append(" ").append(hp_).append("\n");
            cl.setVTableAddress(hp_);
            hp_ += cl.getNumMethods();
            for (Map.Entry<String, Method_t> methods : cl.class_methods_map.entrySet()) {
                String new_temp = newTemp();
                Method_t meth = methods.getValue();
                String meth_label = meth.getFrom_class_().getName() + "_" + meth.getName();
                this.asm_.append("MOVE ").append(new_temp).append(" ").append(meth_label).append("\n");
                this.asm_.append("HSTORE ").append(vtable_reg).append(" ").append(meth.getMeth_num_() - 1).append(" ").append(new_temp).append("\n");
            }
            this.asm_.append("\n");
        }
    }

    public String getASM() {
        return asm_.toString();
    }

    public int getHP() {
        return this.hp_;
    }

    public String newTemp() {
        return "TEMP " + ++globals_;
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
    public Base_t visit(MainClass n, Base_t argu) throws Exception {
        this.asm_ = new StringBuilder();
        this.asm_.append("MAIN\n");
        String id = n.f1.accept(this, argu).getName();
        Method_t meth = st_.get(id).getMethod("main");
        initVtables();
        n.f14.accept(this, meth);
        n.f15.accept(this, meth);
        this.asm_.append("Runtime_Error NOOP\n");
        this.asm_.append("END\n");
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
        n.f3.accept(this, new Method_t(null, class_id.getName()));
        n.f4.accept(this, class_id);
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
        n.f1.accept(this, argu);
        n.f3.accept(this, argu);
        n.f5.accept(this, new Method_t(null, class_id.getName()));
        n.f6.accept(this, class_id);
        return null;
    }

    /**
     * f0 -> Type()
     * f1 -> Variable()
     * f2 -> ( VarDeclarationRest() )*
     * f3 -> ";"
     */
    public Base_t visit(VarDeclaration n, Base_t argu) throws Exception {
        Method_t meth = (Method_t) argu;
        String varType = ((Variable_t) n.f0.accept(this, argu)).getType();
        Variable_t first_var = (Variable_t) n.f1.accept(this, argu);

        List<Variable_t> vars = new ArrayList<>();
        vars.add(first_var);
        if (n.f2.present()) {
            for (int i = 0; i < n.f2.size(); i++) {
                Variable_t var = (Variable_t) n.f2.nodes.get(i).accept(this, argu);
                vars.add(new Variable_t(varType, var.getName(), var.getRegister()));
            }
        }
        for (Variable_t var : vars) {
            String varName = var.getName();
            if (meth.getFrom_class_() != null) { // is a variable of a function
                String tmp = (var.getRegister() == null) ? newTemp() : var.getRegister();
                if ((meth = meth.getFrom_class_().getMethod(meth.getName())) != null) {
                    meth.addRegToVar(varName, tmp);
                } else {
                    throw new Exception("VarDeclaration Error 1");
                }
                if (var.getRegister() == null) {
                    if (varType == null) {
                        this.asm_.append("MOVE ").append(tmp).append(" 0\n");
                    }
                }
            } else { // is a field of a class
                Class_t cl = st_.get(meth.getName());
                if (cl == null) {
                    throw new Exception("VarDeclaration Error 2");
                }
            }
        }
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> ( VarInit() )?
     */
    public Base_t visit(Variable n, Base_t argu) throws Exception {
        String varname = n.f0.accept(this, argu).getName();
        String reg = null;
        if (n.f1.present()) {
            reg = ((Variable_t) n.f1.accept(this, argu)).getRegister();
        }
        return new Variable_t("", varname, reg);
    }

    /**
     * f0 -> "="
     * f1 -> Expression()
     */
    public Base_t visit(VarInit n, Base_t argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> Variable()
     */
    public Base_t visit(VarDeclarationRest n, Base_t argu) throws Exception {
        return n.f1.accept(this, argu);
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
        this.asm_.append("\n").append(argu.getName()).append("_").append(methName).append("[").append(meth.getNum_parameters_() + 1).append("]\nBEGIN\n");
        n.f7.accept(this, meth);
        n.f8.accept(this, meth);
        Variable_t retType = (Variable_t) n.f10.accept(this, meth);
        this.asm_.append("RETURN ").append(retType.getRegister()).append("\nEND\n");
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
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return null;
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
        return new Variable_t("int[]");
    }

    /**
     * f0 -> "boolean"
     */
    public Base_t visit(BooleanType n, Base_t argu) throws Exception {
        return new Variable_t("boolean");
    }

    /**
     * f0 -> "int"
     */
    public Base_t visit(IntegerType n, Base_t argu) throws Exception {
        return new Variable_t("int");
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
     * | ExitStatement()
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
        String id = n.f0.accept(this, argu).getName();
        String expr = ((Variable_t) n.f2.accept(this, argu)).getRegister();
        Method_t meth = (Method_t) argu;
        if (meth != null) {
            Variable_t var = meth.methContainsVar(id);
            if (var == null) { // didnt find the var in method, so its a field of the class
                Class_t cl = st_.get(meth.getFrom_class_().getName());
                if (cl == null) {
                    throw new Exception("something went wrong at AssignmentStatement 2");
                }
                var = cl.classContainsVar(id);
                if (var != null) { // class field
                    this.asm_.append("HSTORE " + " TEMP 0 ").append(var.getNum()).append(" ").append(expr).append("\n");
                }
                return null;
            } else { // if a local var
                this.asm_.append("MOVE ").append(var.getRegister()).append(" ").append(expr).append("\n");
            }
        }
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "++"
     * f2 -> ";"
     */
    public Base_t visit(IncrementAssignmentStatement n, Base_t argu) throws Exception {
        String id = n.f0.accept(this, argu).getName();
        Method_t meth = (Method_t) argu;
        if (meth != null) {
            Variable_t var = meth.methContainsVar(id);
            if (var == null) { // didn't find the var in method, so it's a field of the class
                Class_t cl = st_.get(meth.getFrom_class_().getName());
                if (cl == null) {
                    throw new Exception("something went wrong at IncrementAssignmentStatement 1");
                }
                var = cl.classContainsVar(id);
                if (var != null) { // class field
                    String temp = newTemp();
                    this.asm_.append("HLOAD ").append(temp).append(" TEMP 0 ").append(var.getNum()).append("\n");
                    this.asm_.append("MOVE ").append(temp).append(" PLUS ").append(temp).append(" 1\n");
                    this.asm_.append("HSTORE TEMP 0 ").append(var.getNum()).append(" ").append(temp).append("\n");
                }
                return null;
            } else { // if a local var
                this.asm_.append("MOVE ").append(var.getRegister()).append(" PLUS ").append(var.getRegister()).append(" 1\n");
            }
        }
        return null;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "--"
     * f2 -> ";"
     */
    public Base_t visit(DecrementAssignmentStatement n, Base_t argu) throws Exception {
        String id = n.f0.accept(this, argu).getName();
        Method_t meth = (Method_t) argu;
        if (meth != null) { // if a local var
            Variable_t var = meth.methContainsVar(id);
            if (var == null) { // didn't find the var in method, so it's a field of the class
                Class_t cl = st_.get(meth.getFrom_class_().getName());
                if (cl == null) {
                    throw new Exception("something went wrong at IncrementAssignmentStatement 1");
                }
                var = cl.classContainsVar(id);
                if (var != null) { // class field
                    String temp = newTemp();
                    this.asm_.append("HLOAD ").append(temp).append(" TEMP 0 ").append(var.getNum()).append("\n");
                    this.asm_.append("MOVE ").append(temp).append(" MINUS ").append(temp).append(" 1\n");
                    this.asm_.append("HSTORE TEMP 0 ").append(var.getNum()).append(" ").append(temp).append("\n");
                }
                return null;
            } else {
                this.asm_.append("MOVE ").append(var.getRegister()).append(" MINUS ").append(var.getRegister()).append(" 1\n");
            }
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
        String id = n.f0.accept(this, argu).getName();
        String operator = n.f1.accept(this, argu).getName();
        String expr = ((Variable_t) n.f2.accept(this, argu)).getRegister();
        String opcode;
        if ("+=".equals(operator)) {
            opcode = "PLUS";
        } else if ("-=".equals(operator)) {
            opcode = "MINUS";
        } else if ("*=".equals(operator)) {
            opcode = "TIMES";
        } else if ("/=".equals(operator)) {
            opcode = "DIV";
        } else if ("%=".equals(operator)) {
            opcode = "MOD";
        } else if ("<<=".equals(operator)) {
            opcode = "SLL";
        } else if (">>=".equals(operator)) {
            opcode = "SRL";
        } else if ("&=".equals(operator)) {
            opcode = "AND";
        } else if ("|=".equals(operator)) {
            opcode = "OR";
        } else if ("^=".equals(operator)) {
            opcode = "XOR";
        } else {
            throw new IllegalStateException("CompoundAssignmentStatement: unexpected value " + operator);
        }
        Method_t meth = (Method_t) argu;
        if (meth != null) {
            Variable_t var = meth.methContainsVar(id);
            if (var == null) { // didn't find the var in method, so it's a field of the class
                Class_t cl = st_.get(meth.getFrom_class_().getName());
                if (cl == null) {
                    throw new Exception("something went wrong at IncrementAssignmentStatement 1");
                }
                var = cl.classContainsVar(id);
                if (var != null) { // class field
                    String temp = newTemp();
                    this.asm_.append("HLOAD ").append(temp).append(" TEMP 0 ").append(var.getNum()).append("\n");
                    this.asm_.append("MOVE ").append(temp).append(" ").append(opcode).append(" ").append(temp).append(" 1\n");
                    this.asm_.append("HSTORE TEMP 0 ").append(var.getNum()).append(" ").append(temp).append("\n");
                }
                return null;
            } else { // if a local var
                this.asm_.append("MOVE ").append(var.getRegister()).append(" ").append(opcode).append(" ").append(var.getRegister()).append(" ").append(expr).append("\n");
            }
        }
        return null;
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
        return new Variable_t(_ret[n.f0.which], _ret[n.f0.which]);
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
        String length = newTemp();
        String cond = newTemp();
        this.may_has_error_ = true;
        String error_label = "Runtime_Error";
        String array = ((Variable_t) n.f0.accept(this, argu)).getRegister();
        this.asm_.append("HLOAD ").append(length).append(" ").append(array).append(" 0\n"); // load length
        String idx = ((Variable_t) n.f2.accept(this, argu)).getRegister();
        String one = newTemp();
        // if idx < arr.length
        this.asm_.append("MOVE ").append(cond).append(" LT ").append(idx).append(" ").append(length).append("\n");
        this.asm_.append("CJUMP ").append(cond).append(" ").append(error_label).append("\n");
        // if idx >= 0
        this.asm_.append("MOVE ").append(cond).append(" LT ").append(idx).append(" 0\n");
        this.asm_.append("MOVE ").append(one).append(" 1\n");
        this.asm_.append("MOVE ").append(cond).append(" MINUS ").append(one).append(" ").append(cond).append("\n");
        this.asm_.append("CJUMP ").append(cond).append(" ").append(error_label).append("\n");
        String temp_array = newTemp();
        // temp_array = &array
        this.asm_.append("MOVE ").append(temp_array).append(" PLUS ").append(array).append(" 1\n");
        this.asm_.append("MOVE ").append(temp_array).append(" PLUS ").append(temp_array).append(" ").append(idx).append("\n");
        String expr = ((Variable_t) n.f5.accept(this, argu)).getRegister();
        this.asm_.append("HSTORE ").append(temp_array).append(" 0 ").append(expr).append("\n");
        return new Variable_t(temp_array);
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
        String end_label = this.newLabel();
        String cond = ((Variable_t) n.f2.accept(this, argu)).getRegister();
        this.asm_.append("CJUMP ").append(cond).append(" ").append(end_label).append("\n");
        n.f4.accept(this, argu);
        this.asm_.append(end_label).append(" NOOP\n");
        return null;
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
        String else_label = this.newLabel();
        String end_label = this.newLabel();
        String cond = ((Variable_t) n.f2.accept(this, argu)).getRegister();
        this.asm_.append("CJUMP ").append(cond).append(" ").append(else_label).append("\n"); //if cond not true go to else_label
        n.f4.accept(this, argu);
        this.asm_.append("JUMP ").append(end_label).append("\n").append(else_label).append(" NOOP\n");
        n.f6.accept(this, argu);
        this.asm_.append(end_label).append(" NOOP\n");
        return null;
    }

    /**
     * f0 -> "while"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
    public Base_t visit(WhileStatement n, Base_t argu) throws Exception {
        String lstart = this.newLabel();
        String lend = this.newLabel();
        String cond = newTemp();
        this.asm_.append(lstart).append(" NOOP\n");
        String expr = ((Variable_t) n.f2.accept(this, argu)).getRegister();
        this.asm_.append("MOVE ").append(cond).append(" ").append(expr).append("\n");
        this.asm_.append("CJUMP ").append(cond).append(" ").append(lend).append("\n");
        n.f4.accept(this, argu);
        this.asm_.append("JUMP ").append(lstart).append("\n").append(lend).append(" NOOP\n");
        return null;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public Base_t visit(PrintStatement n, Base_t argu) throws Exception {
        String t = ((Variable_t) n.f2.accept(this, argu)).getRegister();
        this.asm_.append("PRINT ").append(t).append("\n");
        return null;
    }

    /**
     * f0 -> "System.out.println"
     * f1 -> "("
     * f2 -> ")"
     * f3 -> ";"
     */
    public Base_t visit(PrintLineStatement n, Base_t argu) throws Exception {
        this.asm_.append("PRINT TEMP 0\n");
        return null;
    }

    /**
     * f0 -> "System.exit"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public Base_t visit(ExitStatement n, Base_t argu) throws Exception {
        String t = ((Variable_t) n.f2.accept(this, argu)).getRegister();
        this.asm_.append("EXIT ").append(t).append("\n");
        return null;
    }

    /**
     * f0 -> <ANSWER>
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
    public Base_t visit(AnswerStatement n, Base_t argu) throws Exception {
        String t = ((Variable_t) n.f2.accept(this, argu)).getRegister();
        this.asm_.append("ANSWER ").append(t).append("\n");
        return null;
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
        String end_label = this.newLabel();
        String ret = newTemp();
        String t1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
        this.asm_.append("MOVE ").append(ret).append(" ").append(t1).append("\n");
        this.asm_.append("CJUMP ").append(t1).append(" ").append(end_label).append("\n");
        String t2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
        this.asm_.append("MOVE ").append(ret).append(" ").append(t2).append("\n");
        this.asm_.append(end_label).append(" NOOP\n");
        return new Variable_t(null, null, ret);
    }

    /**
     * f0 -> Clause()
     * f1 -> "||"
     * f2 -> Clause()
     */
    public Base_t visit(LogicalOrExpression n, Base_t argu) throws Exception {
        String end_label = this.newLabel();
        String temp = newTemp();
        String ret = newTemp();
        String one = newTemp();
        String t1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
        this.asm_.append("MOVE ").append(one).append(" 1\n");
        this.asm_.append("MOVE ").append(temp).append(" MINUS ").append(one).append(" ").append(t1).append("\n");
        this.asm_.append("MOVE ").append(ret).append(" ").append(t1).append("\n");
        this.asm_.append("CJUMP ").append(temp).append(" ").append(end_label).append("\n");
        String t2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
        this.asm_.append("MOVE ").append(ret).append(" ").append(t2).append("\n");
        this.asm_.append(end_label).append(" NOOP\n");
        return new Variable_t(null, null, ret);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> BinOperator()
     * f2 -> PrimaryExpression()
     */
    public Base_t visit(BinaryExpression n, Base_t argu) throws Exception {
        String ret = newTemp();
        String t1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
        String operator = n.f1.accept(this, argu).getName();
        String t2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
        String opcode;
        if ("&".equals(operator)) {
            opcode = "AND";
        } else if ("|".equals(operator)) {
            opcode = "OR";
        } else if ("^".equals(operator)) {
            opcode = "XOR";
        } else if ("<<".equals(operator)) {
            opcode = "SLL";
        } else if (">>".equals(operator)) {
            opcode = "SRL";
        } else if ("<<=".equals(operator)) {
            opcode = "SLL";
        } else if (">>=".equals(operator)) {
            opcode = "SRL";
        } else if ("+".equals(operator)) {
            opcode = "PLUS";
        } else if ("-".equals(operator)) {
            opcode = "MINUS";
        } else if ("*".equals(operator)) {
            opcode = "TIMES";
        } else if ("/".equals(operator)) {
            opcode = "DIV";
        } else if ("%".equals(operator)) {
            opcode = "MOD";
        } else if ("==".equals(operator)) {
            opcode = "EQ";
        } else if ("!=".equals(operator)) {
            opcode = "NEQ";
        } else if ("<".equals(operator)) {
            opcode = "LT";
        } else if ("<=".equals(operator)) {
            opcode = "LTE";
        } else if (">".equals(operator)) {
            opcode = "GT";
        } else if (">=".equals(operator)) {
            opcode = "GTE";
        } else {
            throw new IllegalStateException("CompoundAssignmentStatement: Unexpected value: " + operator);
        }
        if ("GT".equals(opcode)) {
            String new_temp = newTemp();
            this.asm_.append("MOVE ").append(new_temp).append(" ").append(t2).append("\n");
            this.asm_.append("MOVE ").append(ret).append(" LT ").append(new_temp).append(" ").append(t1).append("\n");
        } else if ("GTE".equals(opcode)) {
            String new_temp = newTemp();
            this.asm_.append("MOVE ").append(new_temp).append(" ").append(t2).append("\n");
            this.asm_.append("MOVE ").append(ret).append(" LTE ").append(new_temp).append(" ").append(t1).append("\n");
        } else {
            this.asm_.append("MOVE ").append(ret).append(" ").append(opcode).append(" ").append(t1).append(" ").append(t2).append("\n");
        }
        return new Variable_t(null, null, ret);
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
        return new Variable_t(_ret[n.f0.which], _ret[n.f0.which]);
    }

    /**
     * f0 -> "~"
     * f1 -> PrimaryExpression()
     */
    public Base_t visit(BinNotExpression n, Base_t argu) throws Exception {
        String ret = newTemp();
        String t1 = ((Variable_t) n.f1.accept(this, argu)).getRegister();
        this.asm_.append("MOVE ").append(ret).append(" NOT ").append(t1).append("\n");
        return new Variable_t(null, null, ret);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public Base_t visit(ArrayLookup n, Base_t argu) throws Exception {
        String length = newTemp();
        String cond = newTemp();
        this.may_has_error_ = true;
        String error_label = "Runtime_Error";
        String array = ((Variable_t) n.f0.accept(this, argu)).getRegister();
        // load length
        this.asm_.append("HLOAD ").append(length).append(" ").append(array).append(" 0\n");
        String idx = ((Variable_t) n.f2.accept(this, argu)).getRegister();
        // if idx < arr.length
        this.asm_.append("MOVE ").append(cond).append(" LT ").append(idx).append(" ").append(length).append("\n");
        this.asm_.append("CJUMP ").append(cond).append(" ").append(error_label).append("\n");
        // if idx >= 0
        this.asm_.append("MOVE ").append(cond).append(" LT ").append(idx).append(" 0\n");
        String one = newTemp();
        this.asm_.append("MOVE ").append(one).append(" 1\n");
        this.asm_.append("MOVE ").append(cond).append(" MINUS ").append(one).append(" ").append(cond).append("\n");
        this.asm_.append("CJUMP ").append(cond).append(" ").append(error_label).append("\n");
        String temp_array = newTemp();
        // skip length
        this.asm_.append("MOVE ").append(temp_array).append(" PLUS ").append(array).append(" 1\n");
        this.asm_.append("MOVE ").append(temp_array).append(" PLUS ").append(temp_array).append(" ").append(idx).append("\n");
        String ret = newTemp();
        this.asm_.append("HLOAD ").append(ret).append(" ").append(temp_array).append(" 0\n");
        return new Variable_t(null, null, ret);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public Base_t visit(ArrayLength n, Base_t argu) throws Exception {
        String len = newTemp();
        String t = ((Variable_t) n.f0.accept(this, argu)).getRegister();
        this.asm_.append("HLOAD ").append(len).append(" ").append(t).append(" 0\n");
        return new Variable_t(null, null, len);
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
        Variable_t obj = (Variable_t) n.f0.accept(this, argu);    // obj.temp is type
        Variable_t func = (Variable_t) n.f2.accept(this, argu);
        String objreg = obj.getRegister();
        Class_t cl = st_.get(obj.getType());
        Method_t meth = cl.getMethod(func.getName());
        int offset = meth.getMeth_num_() - 1;
        String vtable_addr = newTemp();
        String this_temp = newTemp();
        String meth_temp = newTemp();
        // load the correct method from the vtable
        this.asm_.append("MOVE ").append(this_temp).append(" ").append(objreg).append("\n");
        this.asm_.append("HLOAD ").append(vtable_addr).append(" ").append(this_temp).append(" 0\n");
        this.asm_.append("HLOAD ").append(meth_temp).append(" ").append(vtable_addr).append(" ").append(offset).append("\n");
        // add parameters to method
        // this.asm_.append("MOVE TEMP 0").append(this_temp).append("\n");
        StringBuilder parStr = new StringBuilder(" ");
        if (n.f4.present()) {
            Method_t params = (Method_t) n.f4.accept(this, argu);
            for (int i = 0; i < params.method_params.size(); i++) {                // for every par
                String par_temp = newTemp();
                this.asm_.append("MOVE ").append(par_temp).append(" ").append((params.method_params.get(i)).getRegister()).append("\n");
                parStr.append(par_temp).append(" ");
            }
        }
        String ret = newTemp();
        this.asm_.append("MOVE ").append(ret).append(" CALL ").append(meth_temp).append(" ( ").append(this_temp).append(parStr).append(")\n");
        return new Variable_t(meth.getType_(), null, ret);
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
        String else_label = this.newLabel();
        String end_label = this.newLabel();
        String res = newTemp();
        String cond = ((Variable_t) n.f1.accept(this, argu)).getRegister();
        this.asm_.append("CJUMP ").append(cond).append(" ").append(else_label).append("\n"); //if cond not true go to else_label
        Variable_t exp1 = (Variable_t) n.f4.accept(this, argu);
        String reg_if = exp1.getRegister();
        String return_type = exp1.getType();
        this.asm_.append("MOVE ").append(res).append(" ").append(reg_if).append("\n");
        this.asm_.append("JUMP ").append(end_label).append("\n");
        this.asm_.append(else_label).append(" NOOP\n");
        String reg_else = ((Variable_t) n.f6.accept(this, argu)).getRegister();
        this.asm_.append("MOVE ").append(res).append(" ").append(reg_else).append("\n");
        this.asm_.append(end_label).append(" NOOP\n");
        return new Variable_t(return_type, null, res);
    }

    /**
     * f0 -> <PUBLIC_READ>
     * f1 -> "("
     * f2 -> ")"
     */
    public Base_t visit(PublicReadExpression n, Base_t argu) throws Exception {
        String ret = newTemp();
        this.asm_.append("PUBREAD ").append(ret).append("\n");
        return new Variable_t("int", null, ret);
    }

    /**
     * f0 -> <PRIVATE_READ>
     * f1 -> "("
     * f2 -> ")"
     */
    public Base_t visit(PrivateReadExpression n, Base_t argu) throws Exception {
        String ret = newTemp();
        this.asm_.append("SECREAD ").append(ret).append("\n");
        return new Variable_t("int", null, ret);
    }

    /**
     * f0 -> <PUBLIC_SEEK>
     * f1 -> "("
     * f2 -> PrimaryExpression()
     * f3 -> ")"
     */
    public Base_t visit(PublicSeekExpression n, Base_t argu) throws Exception {
        String ret = newTemp();
        String t = ((Variable_t) n.f2.accept(this, argu)).getRegister();
        this.asm_.append("PUBSEEK ").append(ret).append(" ").append(t).append("\n");
        return new Variable_t("int", null, ret);
    }

    /**
     * f0 -> <PRIVATE_SEEK>
     * f1 -> "("
     * f2 -> PrimaryExpression()
     * f3 -> ")"
     */
    public Base_t visit(PrivateSeekExpression n, Base_t argu) throws Exception {
        String ret = newTemp();
        String t = ((Variable_t) n.f2.accept(this, argu)).getRegister();
        this.asm_.append("SECSEEK ").append(ret).append(" ").append(t).append("\n");
        return new Variable_t("int", null, ret);
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
     * | NegIntegerLiteral()
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
        String ret = newTemp();
        this.asm_.append("MOVE ").append(ret).append(" ").append(n.f0.toString()).append("\n");
        return new Variable_t(null, null, ret);
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public Base_t visit(NegIntegerLiteral n, Base_t argu) throws Exception {
        String ret = newTemp();
        this.asm_.append("MOVE ").append(ret).append(" ").append(n.f0.toString()).append("\n");
        return new Variable_t(null, null, ret);
    }

    /**
     * f0 -> "true"
     */
    public Base_t visit(TrueLiteral n, Base_t argu) throws Exception {
        String ret = newTemp();
        this.asm_.append("MOVE ").append(ret).append(" 1\n");
        return new Variable_t(null, null, ret);
    }

    /**
     * f0 -> "false"
     */
    public Base_t visit(FalseLiteral n, Base_t argu) throws Exception {
        String ret = newTemp();
        this.asm_.append("MOVE ").append(ret).append(" 0\n");
        return new Variable_t(null, null, ret);
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public Base_t visit(Identifier n, Base_t argu) throws Exception {
        String id = n.f0.toString();
        if (argu == null) {
            return new Variable_t(null, id);
        }
        Class_t cl = st_.get(argu.getName());
        if (cl != null) {                                    // if argu is a class name
            Variable_t var = cl.classContainsVar(id);
            if (var != null) {                                // and id is a field of that class
                return new Variable_t(cl.getName(), id, var.getRegister());
            } else {                                        // is a method
                Method_t meth = cl.getMethod(id);
                if (meth == null) {
                    throw new Exception("something went wrong 1");
                }
                return new Variable_t(null, id, null);
            }
        } else {                                            // if argu is a method name
            Method_t meth = (Method_t) argu;
            Variable_t var = meth.methContainsVar(id);
            if (var != null) {                                // if a parameter or a local var
                return new Variable_t(var.getType(), id, var.getRegister());
            } else {                                        // a field of class
                cl = st_.get(meth.getFrom_class_().getName());
                if (cl == null) {
                    throw new Exception("something went wrong 2");
                }
                var = cl.classContainsVar(id);
                if (var == null) {
                    return new Variable_t(null, id);
                }
                String new_temp = newTemp();
                this.asm_.append("HLOAD ").append(new_temp).append(" TEMP 0 ").append(var.getNum()).append("\n");
                return new Variable_t(var.getType(), id, new_temp);
            }
        }
    }

    /**
     * f0 -> "this"
     */
    public Base_t visit(ThisExpression n, Base_t argu) throws Exception {
        Class_t cl = ((Method_t) argu).getFrom_class_();
        return new Variable_t(cl.getName(), "this", "TEMP 0");
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public Base_t visit(ArrayAllocationExpression n, Base_t argu) throws Exception {
        String len = ((Variable_t) n.f3.accept(this, argu)).getRegister();
        String cond = newTemp();
        String one = newTemp();
        String len_to_alloc = newTemp();
        String array = newTemp();
        // check if length > 0
        this.may_has_error_ = true;
        this.asm_.append("MOVE ").append(cond).append(" LT ").append(len).append(" 0\n");
        this.asm_.append("MOVE ").append(one).append(" 1\n");
        this.asm_.append("MOVE ").append(cond).append(" MINUS ").append(one).append(" ").append(cond).append("\n");
        this.asm_.append("CJUMP ").append(cond).append(" Runtime_Error\n");
        this.asm_.append("MOVE ").append(len_to_alloc).append(" PLUS ").append(len).append(" 1\n"); // create room for array length
        this.asm_.append("MOVE ").append(array).append(" HALLOCATE ").append(len_to_alloc).append("\n"); // allocate
        this.asm_.append("HSTORE ").append(array).append(" 0 ").append(len).append("\n"); // store array length in first position
        return new Variable_t(null, null, array);
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public Base_t visit(AllocationExpression n, Base_t argu) throws Exception {
        String id = n.f1.accept(this, argu).getName();
        Class_t cl = st_.get(id);
        String new_obj = newTemp();
        // String vtable_ptr = newTemp();
        String vtable = newTemp();
        this.asm_.append("MOVE ").append(new_obj).append(" HALLOCATE ").append(1 + cl.getNumVars()).append("\n");
        this.asm_.append("MOVE ").append(vtable).append(" ").append(cl.getVTableAddress()).append("\n");
        this.asm_.append("HSTORE ").append(new_obj).append(" 0 ").append(vtable).append("\n");
        String zero = newTemp();
        this.asm_.append("MOVE ").append(zero).append(" 0\n");
        for (int i = 1; i <= cl.getNumVars(); i++) {
            this.asm_.append("HSTORE ").append(new_obj).append(" ").append(i).append(" ").append(zero).append("\n");
        }
        this.asm_.append("\n");
        return new Variable_t(id, id, new_obj);
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public Base_t visit(NotExpression n, Base_t argu) throws Exception {
        String ret = newTemp();
        String t = ((Variable_t) n.f1.accept(this, argu)).getRegister();
        String one = newTemp();
        this.asm_.append("MOVE ").append(one).append(" 1\n");
        this.asm_.append("MOVE ").append(ret).append(" MINUS ").append(one).append(" ").append(t).append("\n");
        return new Variable_t(null, null, ret);
    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public Base_t visit(BracketExpression n, Base_t argu) throws Exception {
        return n.f1.accept(this, argu);
    }

}