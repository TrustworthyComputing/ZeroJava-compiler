package org.twc.zerojavacompiler.kanga2zmips;

import org.twc.zerojavacompiler.kanga2zmips.kangasyntaxtree.*;
import org.twc.zerojavacompiler.kanga2zmips.kangavisitor.*;

public class Kanga2zMIPS extends GJNoArguDepthFirst<String> {

    private final ZMIPSPrinter zmipsPrinter_;
    private final int sp_;
    private final int hp_;
    private int label_num_;
    private int num_parameters_;

    public Kanga2zMIPS(int hp) {
        this.sp_ = 2000;
        this.label_num_ = 0;
        this.hp_ = hp;
        this.zmipsPrinter_ = new ZMIPSPrinter();
    }

    public String getASM() {
        return zmipsPrinter_.toString();
    }

    private String getNewLabel() {
        return "__RA_" + (label_num_++) + "__";
    }

    // when StmtList ::= ( ( Label() )? Stmt() )*
    // should print Label
    public String visit(NodeOptional n) throws Exception {
        if (n.present()) {
            zmipsPrinter_.printLabel(n.node.accept(this));
        }
        return null;
    }

    /**
     * f0 -> "MAIN"
     * f1 -> "["
     * f2 -> IntegerLiteral()
     * f3 -> "]"
     * f4 -> "["
     * f5 -> IntegerLiteral()
     * f6 -> "]"
     * f7 -> "["
     * f8 -> IntegerLiteral()
     * f9 -> "]"
     * f10 -> StmtList()
     * f11 -> "END"
     * f12 -> ( Procedure() )*
     * f13 -> <EOF>
     */
    public String visit(Goal n) throws Exception {
        num_parameters_ = Integer.parseInt(n.f2.accept(this));
        num_parameters_ = num_parameters_ > 4 ? num_parameters_ - 4 : 0;
        // 4 params using registers
//        int callParamNum = Integer.parseInt(n.f8.accept(this));
//        callParamNum = callParamNum > 4 ? callParamNum - 4 : 0;
//        int stackNum = Integer.parseInt(n.f5.accept(this));
//        stackNum = stackNum - num_parameters_ + callParamNum + 2;
//        String[] beginLines = { "move $sp, " + sp_, "sw $fp, -2($sp)", "sw $ra, -1($sp)", "move $fp, $sp", "sub $sp, $sp, " + stackNum };
//        String[] endLines = { "lw $ra, -1($fp)", "lw $fp, -2($fp)", "add $sp, $sp, " + stackNum, "jr $ra" };
        zmipsPrinter_.begin("main");
        zmipsPrinter_.println("move $hp, " + hp_);
        zmipsPrinter_.println("move $sp, " + sp_);
//        for (String line : beginLines) {
//            zmipsPrinter_.println(line);
//        }
        n.f10.accept(this);
//        for (String line : endLines) {
//            zmipsPrinter_.println(line);
//        }
        zmipsPrinter_.end();
        // other methods
        n.f12.accept(this);
        // final
        zmipsPrinter_.begin("Runtime_Error");
        String[] finalLines = {"move $t0, 0xffffffffffffffff", "answer $t0"};
        for (String line : finalLines) {
            zmipsPrinter_.println(line);
        }
        zmipsPrinter_.end();
        return null;
    }

    /**
     * f0 -> Label()
     * f1 -> "["
     * f2 -> IntegerLiteral()
     * f3 -> "]"
     * f4 -> "["
     * f5 -> IntegerLiteral()
     * f6 -> "]"
     * f7 -> "["
     * f8 -> IntegerLiteral()
     * f9 -> "]"
     * f10 -> StmtList()
     * f11 -> "END"
     */
    public String visit(Procedure n) throws Exception {
        String method = n.f0.accept(this);
        num_parameters_ = Integer.parseInt(n.f2.accept(this));
        num_parameters_ = num_parameters_ > 4 ? num_parameters_ - 4 : 0;
        // 4 params using registers
        int callParamNum = Integer.parseInt(n.f8.accept(this));
        callParamNum = callParamNum > 4 ? callParamNum - 4 : 0;
        int stackNum = Integer.parseInt(n.f5.accept(this));
        stackNum = stackNum - num_parameters_ + callParamNum + 2;
        // parameters of this method is stored above this stack frame
        // additional 2: $ra $fp
        String[] beginLines = {"sw $fp, -2($sp)", "sw $ra, -1($sp)", "move $fp, $sp", "sub $sp, $sp, " + stackNum};
        String[] endLines = {"lw $ra, -1($fp)", "lw $fp, -2($fp)", "add $sp, $sp, " + stackNum, "jr $ra"};

        zmipsPrinter_.begin(method);
        for (String line : beginLines) {
            zmipsPrinter_.println(line);
        }
        n.f10.accept(this);
        for (String line : endLines) {
            zmipsPrinter_.println(line);
        }
        zmipsPrinter_.end();
        return null;
    }

//    /**
//     * f0 -> "NOOP"
//     */
//    public String visit(NoOpStmt n) throws Exception {
//        zmipsPrinter_.println("nop");
//        return null;
//    }

    /**
     * f0 -> "CJUMP"
     * f1 -> Reg()
     * f2 -> Label()
     */
    public String visit(CJumpStmt n) throws Exception {
        String reg = n.f1.accept(this);
        String label = n.f2.accept(this);
        zmipsPrinter_.println("beq " + reg + ", $zero, " + label);
        return null;
    }

    /**
     * f0 -> "JUMP"
     * f1 -> Label()
     */
    public String visit(JumpStmt n) throws Exception {
        String label = n.f1.accept(this);
        zmipsPrinter_.println("j " + label);
        return null;
    }

    /**
     * f0 -> "HSTORE"
     * f1 -> Reg()
     * f2 -> IntegerLiteral()
     * f3 -> Reg()
     */
    public String visit(HStoreStmt n) throws Exception {
        String regTo = n.f1.accept(this);
        String offset = n.f2.accept(this);
        String regFrom = n.f3.accept(this);
        zmipsPrinter_.println("sw " + regFrom + ", " + offset + "(" + regTo + ")");
        return null;
    }

    /**
     * f0 -> "HLOAD"
     * f1 -> Reg()
     * f2 -> Reg()
     * f3 -> IntegerLiteral()
     */
    public String visit(HLoadStmt n) throws Exception {
        String regTo = n.f1.accept(this);
        String regFrom = n.f2.accept(this);
        String offset = n.f3.accept(this);
        zmipsPrinter_.println("lw " + regTo + ", " + offset + "(" + regFrom + ")");
        return null;
    }

    /**
     * f0 -> "MOVE"
     * f1 -> Reg()
     * f2 -> Exp()
     */
    public String visit(MoveStmt n) throws Exception {
        String regTo = n.f1.accept(this);
        String regFrom = n.f2.accept(this);
        if (regFrom.startsWith("__") && regFrom.endsWith("__")) {
            zmipsPrinter_.println("la " + regTo + ", " + regFrom);
        } else {
            zmipsPrinter_.println("move " + regTo + ", " + regFrom);
        }
        return null;
    }

    /**
     * f0 -> "PRINT"
     * f1 -> SimpleExp()
     */
    public String visit(PrintStmt n) throws Exception {
        String reg = n.f1.accept(this);
        zmipsPrinter_.println("print " + reg);
        return null;
    }

    /**
     * f0 -> "PRINTLN"
     * f1 -> SimpleExp()
     */
    public String visit(PrintlnStmt n) throws Exception {
        String reg = n.f1.accept(this);
        zmipsPrinter_.println("println " + reg);
        return null;
    }

    /**
     * f0 -> "ANSWER"
     * f1 -> SimpleExp()
     */
    public String visit(AnswerStmt n) throws Exception {
        String reg = n.f1.accept(this);
        zmipsPrinter_.println("answer " + reg);
        return null;
    }

    /**
     * f0 -> "PUBREAD"
     * f1 -> Reg()
     */
    public String visit(PublicReadStmt n) throws Exception {
        String reg = n.f1.accept(this);
        zmipsPrinter_.println("pubread " + reg);
        return null;
    }

    /**
     * f0 -> "SECREAD"
     * f1 -> Reg()
     */
    public String visit(PrivateReadStmt n) throws Exception {
        String reg = n.f1.accept(this);
        zmipsPrinter_.println("secread " + reg);
        return null;
    }

    /**
     * f0 -> "PUBSEEK"
     * f1 -> Reg()
     * f2 -> SimpleExp()
     */
    public String visit(PublicSeekStmt n) throws Exception {
        String reg = n.f1.accept(this);
        String idx = n.f2.accept(this);
        zmipsPrinter_.println("pubread $" + reg + ", " + idx);
        return null;
    }

    /**
     * f0 -> "SECSEEK"
     * f1 -> Reg()
     * f2 -> SimpleExp()
     */
    public String visit(PrivateSeekStmt n) throws Exception {
        String reg = n.f1.accept(this);
        String idx = n.f2.accept(this);
        zmipsPrinter_.println("secread " + reg + ", " + idx);
        return null;
    }

    /**
     * f0 -> "ALOAD"
     * f1 -> Reg()
     * f2 -> SpilledArg()
     */
    public String visit(ALoadStmt n) throws Exception {
        String regTo = n.f1.accept(this);
        String spilled = n.f2.accept(this);
        zmipsPrinter_.println("lw " + regTo + ", " + spilled);
        return null;
    }

    /**
     * f0 -> "ASTORE"
     * f1 -> SpilledArg()
     * f2 -> Reg()
     */
    public String visit(AStoreStmt n) throws Exception {
        String spilled = n.f1.accept(this);
        String regFrom = n.f2.accept(this);
        zmipsPrinter_.println("sw " + regFrom + ", " + spilled);
        return null;
    }

    /**
     * f0 -> "PASSARG"
     * f1 -> IntegerLiteral()
     * f2 -> Reg()
     */
    public String visit(PassArgStmt n) throws Exception {
        // PASSARG starts from 1
        int offset = Integer.parseInt(n.f1.accept(this)) - 1;
        String regFrom = n.f2.accept(this);
        zmipsPrinter_.println("sw " + regFrom + ", " + offset + "($sp)");
        return null;
    }

    /**
     * f0 -> "CALL"
     * f1 -> SimpleExp()
     */
    public String visit(CallStmt n) throws Exception {
        String label = n.f1.accept(this);
        String return_addr = getNewLabel();
        zmipsPrinter_.println("la $ra, " + return_addr);
        zmipsPrinter_.println("jr " + label);
        zmipsPrinter_.printLabel(return_addr);
        return null;
    }

    /**
     * f0 -> HAllocate()
     * | BinOp()
     * | NotExp()
     * | SimpleExp()
     */
    public String visit(Exp n) throws Exception {
        return n.f0.accept(this);
    }

    /**
     * f0 -> "HALLOCATE"
     * f1 -> SimpleExp()
     */
    public String visit(HAllocate n) throws Exception {
        String _ret = "$v0";
        String reg = n.f1.accept(this);
        zmipsPrinter_.println("move " + _ret + ", $hp");
        zmipsPrinter_.println("add $hp, $hp, " + reg);
        return _ret;
    }

    /**
     * f0 -> Operator()
     * f1 -> Reg()
     * f2 -> SimpleExp()
     */
    public String visit(BinOp n) throws Exception {
        String _ret = "$v1";
        String op = n.f0.accept(this);
        String reg1 = n.f1.accept(this);
        String reg2 = n.f2.accept(this);
        zmipsPrinter_.println(op + " " + _ret + ", " + reg1 + ", " + reg2);
        return _ret;
    }

    /**
     * f0 -> "LT"
     * | "LTE"
     * | "EQ"
     * | "NEQ"
     * | "PLUS"
     * | "MINUS"
     * | "TIMES"
     * | "DIV"
     * | "MOD"
     * | "AND"
     * | "OR"
     * | "XOR"
     * | "SLL"
     * | "SRL"
     */
    public String visit(Operator n) throws Exception {
        String[] retValue = {
                "slt", "sle", "seq", "sne",         // comparison
                "add", "sub", "mult", "div", "mod", // arithmetic
                "and", "or", "xor", "sll", "slr"    // bitwise
        };
        return retValue[n.f0.which];
    }

    /**
     * f0 -> "NOT"
     * f1 -> SimpleExp()
     */
    public String visit(NotExp n) throws Exception {
        String _ret = "$v1";
        String reg = n.f1.accept(this);
        zmipsPrinter_.println("not " + _ret + ", " + reg);
        return _ret;
    }

    /**
     * f0 -> "SPILLEDARG"
     * f1 -> IntegerLiteral()
     */
    public String visit(SpilledArg n) throws Exception {
        int idx = Integer.parseInt(n.f1.accept(this));
        // SpilledArg starts from 0
        if (idx >= num_parameters_) {
            // is not parameter
            // is spilled register/saved register
            idx = num_parameters_ - idx - 3;// below $fp [$ra] [$fp]
        }
        return idx + "($fp)";
    }

    /**
     * f0 -> Reg()
     * | IntegerLiteral()
     * | Label()
     */
    // returns a simple register
    public String visit(SimpleExp n) throws Exception {
        String str = n.f0.accept(this);
        if (n.f0.which == 2) { // if label
            return "__" + str + "__";
        } else {
            return str;
        }
    }

    /**
     * f0 -> "a0"
     * | "a1"
     * | "a2"
     * | "a3"
     * | "t0"
     * | "t1"
     * | "t2"
     * | "t3"
     * | "t4"
     * | "t5"
     * | "t6"
     * | "t7"
     * | "s0"
     * | "s1"
     * | "s2"
     * | "s3"
     * | "s4"
     * | "s5"
     * | "s6"
     * | "s7"
     * | "t8"
     * | "t9"
     * | "v0"
     * | "v1"
     */
    public String visit(Reg n) throws Exception {
        String[] retValue = {"a0", "a1", "a2", "a3", "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "s0", "s1", "s2",
                "s3", "s4", "s5", "s6", "s7", "t8", "t9", "v0", "v1"};
        return "$" + retValue[n.f0.which];
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public String visit(IntegerLiteral n) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Label n) throws Exception {
        return n.f0.toString();
    }

}