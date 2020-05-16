package org.twc.zerojavacompiler.spigletoptimizer;

import org.twc.zerojavacompiler.spiglet2kanga.spigletsyntaxtree.*;
import org.twc.zerojavacompiler.spiglet2kanga.spigletvisitor.*;
import org.twc.zerojavacompiler.spigletoptimizer.factsgen.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class FactGeneratorVisitor extends GJDepthFirst<String, String> {
    public LinkedList<InstructionType> instructions_;
    public LinkedList<AnswerInstructionType> answers_;
    public LinkedList<VariableType> variables_;
    public LinkedList<VarMoveType> var_moves_;
    public LinkedList<ConstMoveType> const_moves_;
    public LinkedList<BinOpMoveType> bin_op_moves_;
    public LinkedList<VarUseType> var_uses_;
    public LinkedList<VarDefType> var_defs_;
    public LinkedList<JumpType> jumps_;
    public LinkedList<ConditionalJumpType> conditional_jumps_;
    public LinkedList<ArgumentType> arguments_;
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
        conditional_jumps_ = new LinkedList<>();
        arguments_ = new LinkedList<>();
        this.inst_num_ = 0;
        this.inst_num2_ = 0;
    }

    public String visit(NodeSequence n, String argu) throws Exception {
        if (n.size() == 1) {
            return n.elementAt(0).accept(this, argu);
        }
        String _ret = null;
        for (Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
            String ret = e.nextElement().accept(this, argu);
            if (ret != null) {
                if (_ret == null) {
                    _ret = ret;
                }
            }
        }
        return _ret;
    }

    /**
     * f0 -> "MAIN"
     * f1 -> StmtList()
     * f2 -> "END"
     * f3 -> ( Procedure() )*
     * f4 -> <EOF>
     */
    public String visit(Goal n, String argu) throws Exception {
        n.f1.accept(this, "MAIN");
        n.f3.accept(this, argu);
        return null;
    }

    /**
     * f0 -> ( ( Label() )? Stmt() )*
     */
    public String visit(StmtList n, String argu) throws Exception {
        if (n.f0.present()) {
            for (int i = 0; i < n.f0.size(); i++) {
                String str = n.f0.elementAt(i).accept(this, argu);
                if (str.matches("(.*)ERR(.*)")) {
                    inst_num2_--;
                    continue;
                }
                this.inst_num_++;
                instructions_.add(new InstructionType(argu, this.inst_num_, str));
                if (str.toLowerCase().contains("answer".toLowerCase())) {
                    answers_.add(new AnswerInstructionType(argu, this.inst_num_, str));
                }
            }
        }
        return null;
    }

    /**
     * f0 -> Label()
     * f1 -> "["
     * f2 -> IntegerLiteral()
     * f3 -> "]"
     * f4 -> StmtExp()
     */
    public String visit(Procedure n, String argu) throws Exception {
        this.inst_num_ = 0;
        this.inst_num2_ = 0;
        String id = n.f0.accept(this, argu);
        int args = Integer.parseInt(n.f2.accept(this, argu));
        for (int i = 0; i < args; i++) {
            arguments_.add(new ArgumentType(id, "TEMP " + i));
        }
        n.f4.accept(this, id);
        return null;
    }

    /**
     * f0 -> NoOpStmt()
     * | ErrorStmt()
     * | CJumpStmt()
     * | JumpStmt()
     * | HStoreStmt()
     * | HLoadStmt()
     * | MoveStmt()
     * | PrintStmt()
     * | PrintlnStmt()
     * | AnswerStmt()
     */
    public String visit(Stmt n, String argu) throws Exception {
        this.inst_num2_++;
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "NOOP"
     */
    public String visit(NoOpStmt n, String argu) throws Exception {
        return "NOOP";
    }

    /**
     * f0 -> "ERROR"
     */
    public String visit(ErrorStmt n, String argu) throws Exception {
        return "ERROR";
    }

    /**
     * f0 -> "CJUMP"
     * f1 -> Temp()
     * f2 -> Label()
     */
    public String visit(CJumpStmt n, String argu) throws Exception {
        String tmp = n.f1.accept(this, argu);
        String label = n.f2.accept(this, argu);
        String instr = "CJUMP " + tmp + " " + label;
        conditional_jumps_.add(new ConditionalJumpType(argu, this.inst_num2_, label));
        var_uses_.add(new VarUseType(argu, this.inst_num2_, tmp));
        return instr;
    }

    /**
     * f0 -> "JUMP"
     * f1 -> Label()
     */
    public String visit(JumpStmt n, String argu) throws Exception {
        String label = n.f1.accept(this, argu);
        String instr = "JUMP " + label;
        jumps_.add(new JumpType(argu, this.inst_num2_, label));
        return instr;
    }

    /**
     * f0 -> "HSTORE"
     * f1 -> Temp()
     * f2 -> IntegerLiteral()
     * f3 -> Temp()
     */
    public String visit(HStoreStmt n, String argu) throws Exception {
        String tmp1 = n.f1.accept(this, argu);
        String lit = n.f2.accept(this, argu);
        String tmp2 = n.f3.accept(this, argu);
        String instr = "HSTORE " + tmp1 + " " + lit + " " + tmp2;
        var_uses_.add(new VarUseType(argu, this.inst_num2_, tmp1));
        var_uses_.add(new VarUseType(argu, this.inst_num2_, tmp2));
        return instr;
    }

    /**
     * f0 -> "HLOAD"
     * f1 -> Temp()
     * f2 -> Temp()
     * f3 -> IntegerLiteral()
     */
    public String visit(HLoadStmt n, String argu) throws Exception {
        String tmp1 = n.f1.accept(this, argu);
        String tmp2 = n.f2.accept(this, argu);
        String lit = n.f3.accept(this, argu);
        String instr = "HLOAD " + tmp1 + " " + tmp2 + " " + lit;
        var_defs_.add(new VarDefType(argu, this.inst_num2_, tmp1));
        var_uses_.add(new VarUseType(argu, this.inst_num2_, tmp2));
        return instr;
    }

    /**
     * f0 -> "MOVE"
     * f1 -> Temp()
     * f2 -> Exp()
     */
    public String visit(MoveStmt n, String argu) throws Exception {
        n.f0.accept(this, argu);
        String tmp = n.f1.accept(this, argu);
        String exp = n.f2.accept(this, argu);
        String instr = null;
        if (exp != null) {
            if (exp.matches("TEMP(.*)")) {
                instr = "MOVE " + tmp + " " + exp;
                var_moves_.add(new VarMoveType(argu, this.inst_num2_, tmp, exp));
                var_uses_.add(new VarUseType(argu, this.inst_num2_, exp));
            } else if (exp.matches("[0-9]+")) {
                instr = "MOVE " + tmp + " " + Integer.parseInt(exp);
                const_moves_.add(new ConstMoveType(argu, this.inst_num2_, tmp, Integer.parseInt(exp)));
            } else {
                instr = "MOVE " + tmp + " " + exp;
                bin_op_moves_.add(new BinOpMoveType(argu, this.inst_num2_, tmp, exp));
            }
        }
        var_defs_.add(new VarDefType(argu, this.inst_num2_, tmp));
        return instr;
    }

    /**
     * f0 -> "PRINT"
     * f1 -> SimpleExp()
     */
    public String visit(PrintStmt n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu);
        String instr = "PRINT " + exp;
        if (exp != null && exp.matches("TEMP(.*)")) {
            var_uses_.add(new VarUseType(argu, this.inst_num2_, exp));
        }
        return instr;
    }

    /**
     * f0 -> "PRINTLN"
     * f1 -> SimpleExp()
     */
    public String visit(PrintlnStmt n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu);
        String instr = "PRINTLN " + exp;
        if (exp != null && exp.matches("TEMP(.*)")) {
            var_uses_.add(new VarUseType(argu, this.inst_num2_, exp));
        }
        return instr;
    }

    /**
     * f0 -> "ANSWER"
     * f1 -> SimpleExp()
     */
    public String visit(AnswerStmt n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu);
        String instr = "ANSWER " + exp;
        if (exp != null && exp.matches("TEMP(.*)")) {
            var_uses_.add(new VarUseType(argu, this.inst_num2_, exp));
        }
        return instr;
    }

    /**
     * f0 -> "PUBREAD"
     * f1 -> Temp()
     */
    public String visit(PublicReadStmt n, String argu) throws Exception {
        String dst = n.f1.accept(this, argu);
        String op = "PUBREAD " + dst;
        var_defs_.add(new VarDefType(argu, this.inst_num2_, dst));
        return op;
    }

    /**
     * f0 -> "SECREAD"
     * f1 -> Temp()
     */
    public String visit(PrivateReadStmt n, String argu) throws Exception {
        String dst = n.f1.accept(this, argu);
        String op = "SECREAD " + dst;
        var_defs_.add(new VarDefType(argu, this.inst_num2_, dst));
        return op;
    }

    /**
     * f0 -> "PUBSEEK"
     * f1 -> Temp()
     * f2 -> SimpleExp()
     */
    public String visit(PublicSeekStmt n, String argu) throws Exception {
        String dst = n.f1.accept(this, argu);
        String idx = n.f2.accept(this, argu);
        String op = "PUBSEEK " + dst + ", " + idx;
        var_defs_.add(new VarDefType(argu, this.inst_num2_, dst));
        if (idx.startsWith("TEMP")) {
            var_uses_.add(new VarUseType(argu, this.inst_num2_, idx));
        }
        return op;
    }

    /**
     * f0 -> "SECSEEK"
     * f1 -> Temp()
     * f2 -> SimpleExp()
     */
    public String visit(PrivateSeekStmt n, String argu) throws Exception {
        String dst = n.f1.accept(this, argu);
        String idx = n.f2.accept(this, argu);
        String op = "SECSEEK " + dst + ", " + idx;
        var_defs_.add(new VarDefType(argu, this.inst_num2_, dst));
        if (idx.startsWith("TEMP")) {
            var_uses_.add(new VarUseType(argu, this.inst_num2_, idx));
        }
        return op;
    }

    /**
     * f0 -> Call()
     * | HAllocate()
     * | BinOp()
     * | NotExp()
     * | SimpleExp()
     */
    public String visit(Exp n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "BEGIN"
     * f1 -> StmtList()
     * f2 -> "RETURN"
     * f3 -> SimpleExp()
     * f4 -> "END"
     */
    public String visit(StmtExp n, String argu) throws Exception {
        n.f1.accept(this, argu);
        this.inst_num2_++;
        String exp = n.f3.accept(this, argu);
        String ret = "RETURN " + exp;
        this.inst_num_++;
        instructions_.add(new InstructionType(argu + " ", this.inst_num_, ret));
        if (exp != null && exp.matches("TEMP(.*)")) {
            var_uses_.add(new VarUseType(argu, this.inst_num2_, exp));
        }
        return null;
    }

    /**
     * f0 -> "CALL"
     * f1 -> SimpleExp()
     * f2 -> "("
     * f3 -> ( Temp() )*
     * f4 -> ")"
     */
    public String visit(Call n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu);
        var_uses_.add(new VarUseType(argu, this.inst_num2_, exp));
        StringBuilder args = new StringBuilder("(");
        if (n.f3.present()) {
            for (int i = 0; i < n.f3.size(); i++) {
                String t = n.f3.nodes.get(i).accept(this, argu);
                args.append(t);
                if (i < n.f3.size() - 1) {
                    args.append(", ");
                }
                var_uses_.add(new VarUseType(argu, this.inst_num2_, t));
            }
        }
        args.append(")");
        return "CALL " + exp + " " + args.toString();
    }

    /**
     * f0 -> "HALLOCATE"
     * f1 -> SimpleExp()
     */
    public String visit(HAllocate n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu);
        return "HALLOCATE " + exp;
    }

    /**
     * f0 -> Operator()
     * f1 -> Temp()
     * f2 -> SimpleExp()
     */
    public String visit(BinOp n, String argu) throws Exception {
        String op = n.f0.accept(this, argu);
        String tmp = n.f1.accept(this, argu);
        String exp = n.f2.accept(this, argu);
        var_uses_.add(new VarUseType(argu, this.inst_num2_, tmp));
        if (exp != null && exp.matches("TEMP(.*)")) {
            var_uses_.add(new VarUseType(argu, this.inst_num2_, exp));
        }
        return op + " " + tmp + " " + exp;
    }

    /**
     * f0 -> "LT"
     * | "LTE"
     * | "GT"
     * | "GTE"
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
    public String visit(Operator n, String argu) throws Exception {
        return n.f0.choice.toString();
    }

    /**
     * f0 -> Temp()
     * | IntegerLiteral()
     * | Label()
     */
    public String visit(SimpleExp n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "NOT"
     * f1 -> SimpleExp()
     */
    public String visit(NotExp n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu);
        return "NOT " + exp;
    }

    /**
     * f0 -> "TEMP"
     * f1 -> IntegerLiteral()
     */
    public String visit(Temp n, String argu) throws Exception {
        String t = n.f1.accept(this, argu);
        String ret = "TEMP " + t;
        for (VariableType var : variables_) {
            if (var.meth_name.equals("\"" + argu + "\"") && var.var.equals("\"" + ret + "\"")) {
                return ret;
            }
        }
        variables_.add(new VariableType(argu, ret));
        return ret;
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
            PrintWriter file = new PrintWriter(path + "/Instructions.iris");
            if (print_facts) System.out.println("\nInstructions:");
            for (InstructionType instruction : instructions_) {
                instruction.writeRecord(file, print_facts);
            }
            file.close();

            file = new PrintWriter(path + "/AnswerInstructions.iris");
            if (print_facts) System.out.println("\nAnswerInstructions:");
            for (AnswerInstructionType answer_instruction : answers_) {
                answer_instruction.writeRecord(file, print_facts);
            }
            file.close();

            file = new PrintWriter(path + "/Vars.iris");
            if (print_facts) System.out.println("\nVars:");
            for (VariableType variable : variables_) {
                variable.writeRecord(file, print_facts);
            }
            file.close();

            file = new PrintWriter(path + "/VarMoves.iris");
            if (print_facts) System.out.println("\nvarMoves:");
            for (VarMoveType var_move : var_moves_) {
                var_move.writeRecord(file, print_facts);
            }
            file.close();

            file = new PrintWriter(path + "/ConstMoves.iris");
            if (print_facts) System.out.println("\nConstMoves:");
            for (ConstMoveType const_move : const_moves_) {
                const_move.writeRecord(file, print_facts);
            }
            file.close();

            file = new PrintWriter(path + "/BinOpMoves.iris");
            if (print_facts) System.out.println("\nBinOpMoves:");
            for (BinOpMoveType bin_op_move : bin_op_moves_) {
                bin_op_move.writeRecord(file, print_facts);
            }
            file.close();

            file = new PrintWriter(path + "/VarUses.iris");
            if (print_facts) System.out.println("\nVarUses:");
            for (VarUseType var_use : var_uses_) {
                var_use.writeRecord(file, print_facts);
            }
            file.close();

            file = new PrintWriter(path + "/VarDefs.iris");
            if (print_facts) System.out.println("\nVarDefs:");
            for (VarDefType var_def : var_defs_) {
                var_def.writeRecord(file, print_facts);
            }
            file.close();

            file = new PrintWriter(path + "/Jumps.iris");
            if (print_facts) System.out.println("\nJumps:");
            for (JumpType jump : jumps_) {
                jump.writeRecord(file, print_facts);
            }
            file.close();

            file = new PrintWriter(path + "/Cjumps.iris");
            if (print_facts) System.out.println("\nCjumps:");
            for (ConditionalJumpType cjump : conditional_jumps_) {
                cjump.writeRecord(file, print_facts);
            }
            file.close();

            file = new PrintWriter(path + "/Args.iris");
            if (print_facts) System.out.println("\nArgs:");
            for (ArgumentType arg : arguments_) {
                arg.writeRecord(file, print_facts);
            }
            file.close();
        } catch (FileNotFoundException ex) {
            System.err.println(ex.getMessage());
        }
    }

}
