package org.twc.minijavacompiler.factsgen;

import org.twc.minijavacompiler.zmipssyntaxtree.*;
import org.twc.minijavacompiler.zmipsvisitor.GJDepthFirst;
import java.util.*;
import java.io.*;

public class FactGeneratorVisitor extends GJDepthFirst<String, String> {

    public LinkedList<Instruction_t> instructions_;
    public LinkedList<AnswerInstruction_t> answers_;
    public LinkedList<Var_t> variables_;
    public LinkedList<VarMove_t> var_moves_;
    public LinkedList<ConstMove_t> const_moves_;
    public LinkedList<BinOpMove_t> bin_op_moves_;
    public LinkedList<VarUse_t> var_uses_;
    public LinkedList<VarDef_t> var_defs_;
    public LinkedList<Jump_t> jumps_;
    public LinkedList<Cjump_t> cjumps_;
    public int inst_num_;
    public int inst_num2_;

    public FactGeneratorVisitor() {
        instructions_ = new LinkedList<>();
        answers_ = new LinkedList<>();
        variables_ = new LinkedList<>();
        var_moves_ = new LinkedList<>();
        const_moves_ = new LinkedList<>();
        bin_op_moves_ = new LinkedList<>();
        var_uses_ = new LinkedList<>();
        var_defs_ = new LinkedList<>();
        jumps_ = new LinkedList<>();
        cjumps_ = new LinkedList<>();
        this.inst_num_ = 0;
        this.inst_num2_ = 0;
    }

    public String visit(NodeSequence n, String argu) throws Exception {
        if (n.size() == 1) {
            return n.elementAt(0).accept(this,argu);
        }
        String _ret = null;
        for ( Enumeration<Node> e = n.elements() ; e.hasMoreElements() ; ) {
            String ret = e.nextElement().accept(this,argu);
            if (ret != null) {
                if (_ret == null)
                    _ret = ret;
            }
        }
        return _ret;
    }

    /**
    * f0 -> ZMIPSProg()
    * f1 -> <EOF>
    */
    public String visit(Goal n, String argu) throws Exception {
        n.f0.accept(this, "Main");
        return null;
    }

    /**
     * f0 -> ( Stmt() )*
     */
    public String visit(ZMIPSProg n, String argu) throws Exception {
        if (n.f0.present()) {
            for (int i = 0 ; i < n.f0.size() ; i++) {
                String str = n.f0.elementAt(i).accept(this, argu);
                this.inst_num_++;
                instructions_.add(new Instruction_t(argu, this.inst_num_, str));
                if (str.toLowerCase().contains("answer".toLowerCase())) {
                    answers_.add(new AnswerInstruction_t(argu, this.inst_num_, str));
                }
            }
        }
        return null;
    }

    /**
     * f0 -> Label()
     *       | TwoRegInstr()
     *       | ThreeRegInstr()
     *       | JmpStmts()
     *       | SwStmt()
     *       | LwStmt()
     *       | PrintStmt()
     *       | AnswerStmt()
     *       | ReadStmt()
     *       | SeekStmt()
     */
    public String visit(Stmt n, String argu) throws Exception {
        this.inst_num2_++;
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> JmpOps()
     * f1 -> Label()
     */
    public String visit(JmpStmts n, String argu) throws Exception {
        String op = n.f0.accept(this, argu);
        String label = n.f1.accept(this, argu);
        String instr = op + label;
        if (op.equals("j")) {
            jumps_.add(new Jump_t(argu, this.inst_num2_, label));
        } else if (op.equals("jr")) {
            jumps_.add(new Jump_t(argu, this.inst_num2_, label));
            var_uses_.add(new VarUse_t(argu, this.inst_num2_, label));
            if (label.equals("$ra")) {
                var_uses_.add(new VarUse_t(argu, this.inst_num2_, "$sp"));
                var_uses_.add(new VarUse_t(argu, this.inst_num2_, "$v0"));
            }
        } else {
            cjumps_.add(new Cjump_t(argu, this.inst_num2_, label));
        }
        return instr;
    }

    /**
     * f0 ->    "j"
     *      | "jr"
     *      | "cjmp"
     *      | "cnjmp"
     */
    public String visit(JmpOps n, String argu) throws Exception {
        return n.f0.choice.toString();
    }

    /**
     * f0 -> ComparisonOps()
     * f1 -> Register()
     * f2 -> ","
     * f3 -> SimpleExp()
     */
    public String visit(ComparisonStmts n, String argu) throws Exception {
        String op = n.f0.accept(this, argu);
        String src1 = n.f1.accept(this, argu);
        String src2 = n.f3.accept(this, argu);
        String instr = op + " " + src1 + ", " + src2;
        var_uses_.add(new VarUse_t(argu, this.inst_num2_, src1));
        if (src2.startsWith("$")) {
            var_uses_.add(new VarUse_t(argu, this.inst_num2_, src2));
        }
        return instr;
    }

    /**
     * f0 -> "cmpe"
     *       | "cmpg"
     *       | "cmpge"
     */
    public String visit(ComparisonOps n, String argu) throws Exception {
        return n.f0.choice.toString();
    }

    /**
    * f0 -> "lw"
    * f1 -> Register()
    * f2 -> ","
    * f3 -> SimpleExp()
    * f4 -> "("
    * f5 -> SimpleExp()
    * f6 -> ")"
    */
    public String visit(LwStmt n, String argu) throws Exception {
        String dst = n.f1.accept(this, argu);
        String idx = n.f3.accept(this, argu);
        String addr = n.f5.accept(this, argu);
        String op = "lw " + dst + ", " + idx + "(" + addr + ")";
        var_defs_.add(new VarDef_t(argu, this.inst_num2_, dst));
        if (addr.startsWith("$")) {
            var_uses_.add(new VarUse_t(argu, this.inst_num2_, addr));
        }
        if (addr.startsWith("$")) {
            var_uses_.add(new VarUse_t(argu, this.inst_num2_, idx));
        }
        return op;
    }

     /**
     * f0 -> "sw"
     * f1 -> Register()
     * f2 -> ","
     * f3 -> SimpleExp()
     * f4 -> "("
     * f5 -> SimpleExp()
     * f6 -> ")"
     */
    public String visit(SwStmt n, String argu) throws Exception {
        String src = n.f1.accept(this, argu);
        String idx = n.f3.accept(this, argu);
        String addr = n.f5.accept(this, argu);
        String op = "sw " + src + ", " + idx + "(" + addr + ")";
        var_uses_.add(new VarUse_t(argu, this.inst_num2_, src));
        if (addr.startsWith("$")) {
            var_uses_.add(new VarUse_t(argu, this.inst_num2_, addr));
        }
        if (addr.startsWith("$")) {
            var_uses_.add(new VarUse_t(argu, this.inst_num2_, idx));
        }
        return op;
    }

    /**
     * f0 -> TwoRegInstrOp()
     * f1 -> Register()
     * f2 -> ","
     * f3 -> SimpleExp()
     */
    public String visit(TwoRegInstr n, String argu) throws Exception {
        String op = n.f0.accept(this, argu);
        String dst = n.f1.accept(this, argu);
        String src = n.f3.accept(this, argu);
        if (src == null) { return null; }
        String instr = op + " " + dst + ", " + src;
        if (src.startsWith("$")) {
            var_moves_.add(new VarMove_t(argu, this.inst_num2_, dst, src));
            var_uses_.add(new VarUse_t(argu, this.inst_num2_, src));
        } else if (src.matches("[0-9]+")) {
            const_moves_.add(new ConstMove_t(argu, this.inst_num2_, dst, Integer.parseInt(src) ));
        }
        if (op.equals("la")) {
            if (dst.equals("$ra")) {
                var_uses_.add(new VarUse_t(argu, this.inst_num2_, "$a0"));
                var_uses_.add(new VarUse_t(argu, this.inst_num2_, "$a1"));
                var_uses_.add(new VarUse_t(argu, this.inst_num2_, "$a2"));
                var_uses_.add(new VarUse_t(argu, this.inst_num2_, "$a3"));
                var_uses_.add(new VarUse_t(argu, this.inst_num2_, "$a4"));
            }
        }
        var_defs_.add(new VarDef_t(argu, this.inst_num2_, dst));
        return instr;
    }

    /**
     * f0 -> "move"
     *       | "la"
     *       | "not"
     */
    public String visit(TwoRegInstrOp n, String argu) throws Exception {
        return n.f0.choice.toString();
    }

    /**
     * f0 -> ThreeRegInstrOp()
     * f1 -> Register()
     * f2 -> ","
     * f3 -> Register()
     * f4 -> ","
     * f5 -> SimpleExp()
     */
    public String visit(ThreeRegInstr n, String argu) throws Exception {
        String op = n.f0.accept(this, argu);
        String dst = n.f1.accept(this, argu);
        String src1 = n.f3.accept(this, argu);
        String src2 = n.f5.accept(this, argu);
        if (src2 == null) { return null; }
        String instr = op + " " + dst + ", " + src1 + ", " + src2;
        var_uses_.add(new VarUse_t(argu, this.inst_num2_, src1));
        if (src2.startsWith("$")) { // if third argument is not immediate
            var_uses_.add(new VarUse_t(argu, this.inst_num2_, src2));
        }

        bin_op_moves_.add(new BinOpMove_t(argu, this.inst_num2_, dst, src1, src2));

        var_defs_.add(new VarDef_t(argu, this.inst_num2_, dst));
        return instr;
    }

    /**
     * f0 -> "and"
     *       | "or"
     *       | "xor"
     *       | "add"
     *       | "sub"
     *       | "mult"
     *       | "sll"
     *       | "srl"
     */
    public String visit(ThreeRegInstrOp n, String argu) throws Exception {
        return n.f0.choice.toString();
    }

    /**
     * f0 -> "print"
     * f1 -> SimpleExp()
     */
    public String visit(PrintStmt n, String argu) throws Exception {
        String src = n.f1.accept(this, argu);
        String op = "print " + src;
        if (src.startsWith("$")) {
            var_uses_.add(new VarUse_t(argu, this.inst_num2_, src));
        }
        return op;
    }

    /**
     * f0 -> "answer"
     * f1 -> SimpleExp()
     */
    public String visit(AnswerStmt n, String argu) throws Exception {
        String src = n.f1.accept(this, argu);
        String op = "answer " + src;
        if (src.startsWith("$")) {
            var_uses_.add(new VarUse_t(argu, this.inst_num2_, src));
        }
        return op;
    }

    /**
     * f0 -> "pubread"
     * f1 -> Register()
     */
    public String visit(PubReadStmt n, String argu) throws Exception {
        String dst = n.f1.accept(this, argu);
        String op = "pubread " + dst;
        var_defs_.add(new VarDef_t(argu, this.inst_num2_, dst));
        return op;
    }

    /**
     * f0 -> "secread"
     * f1 -> Register()
     */
    public String visit(SecReadStmt n, String argu) throws Exception {
        String dst = n.f1.accept(this, argu);
        String op = "secread " + dst;
        var_defs_.add(new VarDef_t(argu, this.inst_num2_, dst));
        return op;
    }

    /**
     * f0 -> Register()
     *       | IntegerLiteral()
     *       | Label()
     */
    public String visit(SimpleExp n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> <REGISTER>
     */
    public String visit(Register n, String argu) throws Exception {
        String v = n.f0.toString();
        Var_t var = new Var_t(argu, v);
        for (Var_t variable : variables_) {
            if (variable.var.equals("\"" + v + "\"")) {
                return v;
            }
        }
        variables_.add(var);
        return v;
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public String visit(IntegerLiteral n, String argu) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Label n, String argu) throws Exception {
        return n.f0.toString();
    }

    public void writeFacts(String path, boolean print_facts) {
        try {
            PrintWriter file = new PrintWriter(path + "/Vars.iris");
            if (print_facts) System.out.println("\nVars:");
            for (Var_t var_t : variables_) {
                var_t.writeRecord(file, print_facts);
            }
            file.close();

            file = new PrintWriter(path + "/varMoves.iris");
            if (print_facts) System.out.println("\nvarMoves:");
            for (VarMove_t varMove_t : var_moves_) {
                varMove_t.writeRecord(file, print_facts);
            }
            file.close();

            file = new PrintWriter(path + "/ConstMoves.iris");
            if (print_facts) System.out.println("\nConstMoves:");
            for (ConstMove_t constMove_t : const_moves_) {
                constMove_t.writeRecord(file, print_facts);
            }
            file.close();

            file = new PrintWriter(path + "/BinOpMoves.iris");
            if (print_facts) System.out.println("\nBinOpMoves:");
            for (BinOpMove_t binOpMove_t : bin_op_moves_) {
                binOpMove_t.writeRecord(file, print_facts);
            }
            file.close();

            file = new PrintWriter(path + "/Instructions.iris");
            if (print_facts) System.out.println("\nInstructions:");
            for (Instruction_t instruction_t : instructions_) {
                instruction_t.writeRecord(file, print_facts);
            }
            file.close();

            file = new PrintWriter(path + "/AnswerInstructions.iris");
            if (print_facts) System.out.println("\nAnswerInstructions:");
            for (AnswerInstruction_t answerInstruction_t : answers_) {
                answerInstruction_t.writeRecord(file, print_facts);
            }
            file.close();

            file = new PrintWriter(path + "/VarUses.iris");
            if (print_facts) System.out.println("\nVarUses:");
            for (VarUse_t varUse_t : var_uses_) {
                varUse_t.writeRecord(file, print_facts);
            }
            file.close();

            file = new PrintWriter(path + "/VarDefs.iris");
            if (print_facts) System.out.println("\nVarDefs:");
            for (VarDef_t varDef_t : var_defs_) {
                varDef_t.writeRecord(file, print_facts);
            }
            file.close();

            file = new PrintWriter(path + "/Jumps.iris");
            if (print_facts) System.out.println("\nJumps:");
            for (Jump_t jump_t : jumps_) {
                jump_t.writeRecord(file, print_facts);
            }
            file.close();

            file = new PrintWriter(path + "/Cjumps.iris");
            if (print_facts) System.out.println("\nCjumps:");
            for (Cjump_t cjump_t : cjumps_) {
                cjump_t.writeRecord(file, print_facts);
            }
            file.close();
        } catch(FileNotFoundException ex) {
            System.err.println(ex.getMessage());
        }
    }

}
