package org.twc.zerojavacompiler.kanga2zMIPS;

import org.twc.zerojavacompiler.kangasyntaxtree.*;
import org.twc.zerojavacompiler.kangavisitor.*;

public class Kanga2zMIPS extends GJNoArguDepthFirst<String> {

    private final ZMIPSPrinter zmipsPrinter_;

    public Kanga2zMIPS() {
        zmipsPrinter_ = new ZMIPSPrinter();
    }

    public String getASM() {
        return zmipsPrinter_.toString();
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
    int paramNum, stackNum, callParamNum; // about the 3 numbers in method[][][]

    public String visit(Goal n) throws Exception {
        paramNum = Integer.parseInt(n.f2.accept(this));
        paramNum = paramNum > 4 ? paramNum - 4 : 0;
        // 4 params using registers
        callParamNum = Integer.parseInt(n.f8.accept(this));
        callParamNum = callParamNum > 4 ? callParamNum - 4 : 0;
        stackNum = Integer.parseInt(n.f5.accept(this));
        stackNum = stackNum - paramNum + callParamNum + 2;
        // parameters of this method is stored above this stack frame
        // additional 2: $ra $fp
        String[] beginLines = { "sw $fp, -8($sp)", "sw $ra, -4($sp)", "move $fp, $sp",
                "subu $sp, $sp, " + 4 * stackNum };
        String[] endLines = { "lw $ra, -4($fp)", "lw $fp, -8($fp)", "addu $sp, $sp, " + 4 * stackNum, "j $ra" };

        zmipsPrinter_.begin("main");
        for (String line : beginLines)
            zmipsPrinter_.println(line);
        n.f10.accept(this);
        for (String line : endLines)
            zmipsPrinter_.println(line);
        zmipsPrinter_.end();
        // other methods
        n.f12.accept(this);
        // final
        String[] finalLines = { "", ".text", ".globl _halloc", "_halloc:", "li $v0, 9", "syscall", "j $ra", ".text",
                ".globl _print", "_print:", "li $v0, 1", "syscall", "la $a0, newl", "li $v0, 4", "syscall", "j $ra",
                ".data", ".align   0", "newl:", ".asciiz \"\\n\"", ".data", ".align   0", "str_er:",
                ".asciiz \" ERROR: abnormal termination\\n\"" };
        for (String line : finalLines) {
            zmipsPrinter_.println("\t\t" + line);
        }
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
        paramNum = Integer.parseInt(n.f2.accept(this));
        paramNum = paramNum > 4 ? paramNum - 4 : 0;
        // 4 params using registers
        callParamNum = Integer.parseInt(n.f8.accept(this));
        callParamNum = callParamNum > 4 ? callParamNum - 4 : 0;
        stackNum = Integer.parseInt(n.f5.accept(this));
        stackNum = stackNum - paramNum + callParamNum + 2;
        // parameters of this method is stored above this stack frame
        // additional 2: $ra $fp
        String[] beginLines = { "sw $fp, -8($sp)", "sw $ra, -4($sp)", "move $fp, $sp",
                "subu $sp, $sp, " + 4 * stackNum };
        String[] endLines = { "lw $ra, -4($fp)", "lw $fp, -8($fp)", "addu $sp, $sp, " + 4 * stackNum, "j $ra" };

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

    /**
     * f0 -> "NOOP"
     */
    public String visit(NoOpStmt n) throws Exception {
        zmipsPrinter_.println("nop");
        return null;
    }

    /**
     * f0 -> "CJUMP"
     * f1 -> Reg()
     * f2 -> Label()
     */
    public String visit(CJumpStmt n) throws Exception {
        String reg = n.f1.accept(this);
        String label = n.f2.accept(this);
        zmipsPrinter_.println("beqz $" + reg + ", " + label);
        return null;
    }

    /**
     * f0 -> "JUMP"
     * f1 -> Label()
     */
    public String visit(JumpStmt n) throws Exception {
        String label = n.f1.accept(this);
        zmipsPrinter_.println("b " + label);
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
        zmipsPrinter_.println("sw $" + regFrom + ", " + offset + "($" + regTo + ")");
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
        zmipsPrinter_.println("lw $" + regTo + ", " + offset + "($" + regFrom + ")");
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
        zmipsPrinter_.println("move $" + regTo + ", $" + regFrom);
        return null;
    }

    /**
     * f0 -> "PRINT"
     * f1 -> SimpleExp()
     */
    public String visit(PrintStmt n) throws Exception {
        String reg = n.f1.accept(this);
        zmipsPrinter_.println("print $" + reg);
        return null;
    }

    /**
     * f0 -> "ANSWER"
     * f1 -> SimpleExp()
     */
    public String visit(AnswerStmt n) throws Exception {
        String reg = n.f1.accept(this);
        zmipsPrinter_.println("answer $" + reg);
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
        zmipsPrinter_.println("lw $" + regTo + ", " + spilled);
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
        zmipsPrinter_.println("sw $" + regFrom + ", " + spilled);
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
        zmipsPrinter_.println("sw $" + regFrom + ", " + 4 * offset + "($sp)");
        return null;
    }

    /**
     * f0 -> "CALL"
     * f1 -> SimpleExp()
     */
    public String visit(CallStmt n) throws Exception {
        String label = n.f1.accept(this);
        zmipsPrinter_.println("jalr $" + label);
        return null;
    }

    /**
     * f0 -> HAllocate()
     * | BinOp()
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
        String _ret = "v0";
        String reg = n.f1.accept(this);
        zmipsPrinter_.println("move $a0, $" + reg);
        zmipsPrinter_.println("jal _halloc");
        return _ret;
    }

    /**
     * f0 -> Operator()
     * f1 -> Reg()
     * f2 -> SimpleExp()
     */
    public String visit(BinOp n) throws Exception {
        String _ret = "v1";
        String op = n.f0.accept(this);
        String reg1 = n.f1.accept(this);
        String reg2 = n.f2.accept(this);
        zmipsPrinter_.println(op + " $v1, $" + reg1 + ", $" + reg2);
        return _ret;
    }

    /**
     * f0 -> "LT"
     * | "PLUS"
     * | "MINUS"
     * | "TIMES"
     */
    public String visit(Operator n) throws Exception {
        String[] retValue = { "slt", "add", "sub", "mul" };
        return retValue[n.f0.which];
    }

    /**
     * f0 -> "SPILLEDARG"
     * f1 -> IntegerLiteral()
     */
    public String visit(SpilledArg n) throws Exception {
        int idx = Integer.parseInt(n.f1.accept(this));
        // SpilledArg starts from 0

        if (idx >= paramNum) {
            // is not parameter
            // is spilled register/saved register
            idx = paramNum - idx - 3;// below $fp [$ra] [$fp]
        }

        return 4 * idx + "($fp)";
    }

    /**
     * f0 -> Reg()
     * | IntegerLiteral()
     * | Label()
     */
    // returns a simple register
    public String visit(SimpleExp n) throws Exception {
        String _ret = "v1";
        String str = n.f0.accept(this);
        if (n.f0.which == 0) {
            _ret = str;
        } else if (n.f0.which == 1) {
            zmipsPrinter_.println("li $v1, " + str);
        } else {
            zmipsPrinter_.println("la $v1, " + str);
        }
        return _ret;
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
        String[] retValue = { "a0", "a1", "a2", "a3", "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "s0", "s1", "s2",
                "s3", "s4", "s5", "s6", "s7", "t8", "t9", "v0", "v1" };
        return retValue[n.f0.which];
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