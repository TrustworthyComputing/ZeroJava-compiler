package org.twc.zerojavacompiler.spigletoptimizer;

import org.twc.zerojavacompiler.spiglet2kanga.spigletsyntaxtree.*;
import org.twc.zerojavacompiler.spiglet2kanga.spigletvisitor.GJDepthFirst;

import java.util.Enumeration;
import java.util.Map;

class Optimizations {

    public String instr_or_temp_;
    public String const_prop_;

    public Optimizations(String instr_or_temp) {
        this.instr_or_temp_ = instr_or_temp;
        this.const_prop_ = null;
    }

    public Optimizations(String instr_or_temp, String const_prop_) {
        this.instr_or_temp_ = instr_or_temp;
        this.const_prop_ = const_prop_;
    }

    public String getOptimizedTemp() {
        if (this.const_prop_ != null) {
            return this.const_prop_;
        } else {
            return this.instr_or_temp_;
        }
    }

}

public class OptimizerVisitor extends GJDepthFirst<Optimizations, String> {

    private final StringBuilder asm_;
    public int instr_cnt_;
    private final Map<String, Map<String, String>> optimizations_;

    public OptimizerVisitor(Map<String, Map<String, String>> optimizations_) {
        this.asm_ = new StringBuilder();
        this.instr_cnt_ = 1;
        this.optimizations_ = optimizations_;
    }

    public String getAsm() {
        return this.asm_.toString();
    }

    private String getMethFromFact(String fact) {
        String[] parts = fact.split(",");
        return parts[0].substring(2, parts[0].length() - 1);
    }

    private String getImmediateFromFact(String fact) {
        String[] parts = fact.split(",");
        return parts[3].substring(1, parts[3].length() - 1);
    }

    private String getFirstTempFromFact(String fact) {
        String[] parts = fact.split(",");
        return parts[2].substring(2, parts[2].length() - 1);
    }

    private String getSecondTempFromFact(String fact) {
        String[] parts = fact.split(",");
        return parts[3].substring(2, parts[3].length() - 2);
    }

    private void checkIfDeadCode(String meth, String instr) {
        String instr_dead_code = optimizations_.get("deadCode").get(meth + instr_cnt_);
        if (instr_dead_code == null) {
            this.asm_.append(instr);
        } else if (!getMethFromFact(instr_dead_code).equals(meth)) {
            this.asm_.append(instr);
        }
    }

    // get Labels
    public Optimizations visit(NodeOptional n, String argu) throws Exception {
        if (n.present()) {
            asm_.append(n.node.accept(this, argu).instr_or_temp_);
        }
        return null;
    }

    /**
     * f0 -> "MAIN"
     * f1 -> StmtList()
     * f2 -> "END"
     * f3 -> ( Procedure() )*
     * f4 -> <EOF>
     */
    public Optimizations visit(Goal n, String argu) throws Exception {
        this.asm_.append("MAIN\n");
        n.f1.accept(this, "MAIN");
        this.asm_.append("END\n");
        n.f3.accept(this, argu);
        return null;
    }

    /**
     * f0 -> ( ( Label() )? Stmt() )*
     */
    public Optimizations visit(StmtList n, String argu) throws Exception {
        if (n.f0.present()) {
            for (int i = 0; i < n.f0.size(); i++) {
                Optimizations opt = n.f0.elementAt(i).accept(this, argu);
                if (opt == null) {
                    this.instr_cnt_++;
                    continue;
                }
                System.out.println(opt.instr_or_temp_);
                String str = opt.instr_or_temp_;
                if (str.matches("L(.*)")) {
                    this.asm_.append(str).append("\n");
                }
                if (str.matches("(.*)ERROR(.*)")) {
                    continue;
                }
                this.instr_cnt_++;
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
    public Optimizations visit(Procedure n, String argu) throws Exception {
        this.instr_cnt_ = 1;
        String id = n.f0.accept(this, argu).instr_or_temp_;
        String args = n.f2.accept(this, argu).instr_or_temp_;
        this.asm_.append(id).append("[").append(args).append("]\n");
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
    public Optimizations visit(Stmt n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "NOOP"
     */
    public Optimizations visit(NoOpStmt n, String argu) throws Exception {
        asm_.append("\t\tNOOP\n");
        return new Optimizations("NOOP");
    }

    /**
     * f0 -> "ERROR"
     */
    public Optimizations visit(ErrorStmt n, String argu) throws Exception {
        return new Optimizations("ERROR");
    }

    /**
     * f0 -> "CJUMP"
     * f1 -> Temp()
     * f2 -> Label()
     */
    public Optimizations visit(CJumpStmt n, String argu) throws Exception {
        String tmp = n.f1.accept(this, argu).instr_or_temp_;
        String label = n.f2.accept(this, argu).instr_or_temp_;
        String instr = "CJUMP " + tmp + " " + label + "\n";
        checkIfDeadCode(argu, instr);
        return new Optimizations(instr);
    }

    /**
     * f0 -> "JUMP"
     * f1 -> Label()
     */
    public Optimizations visit(JumpStmt n, String argu) throws Exception {
        String label = n.f1.accept(this, argu).instr_or_temp_;
        String instr = "JUMP " + label + "\n";
        checkIfDeadCode(argu, instr);
        return new Optimizations(instr);
    }

    /**
     * f0 -> "HSTORE"
     * f1 -> Temp()
     * f2 -> IntegerLiteral()
     * f3 -> Temp()
     */
    public Optimizations visit(HStoreStmt n, String argu) throws Exception {
        String tmp1 = n.f1.accept(this, argu).instr_or_temp_;
        String lit = n.f2.accept(this, argu).instr_or_temp_;
        String tmp2 = n.f3.accept(this, argu).instr_or_temp_;
        String instr = "HSTORE " + tmp1 + " " + lit + " " + tmp2 + "\n";
        checkIfDeadCode(argu, instr);
        return new Optimizations(instr);
    }

    /**
     * f0 -> "HLOAD"
     * f1 -> Temp()
     * f2 -> Temp()
     * f3 -> IntegerLiteral()
     */
    public Optimizations visit(HLoadStmt n, String argu) throws Exception {
        String tmp1 = n.f1.accept(this, argu).instr_or_temp_;
        String tmp2 = n.f2.accept(this, argu).instr_or_temp_;
        String lit = n.f3.accept(this, argu).instr_or_temp_;
        String instr = "HLOAD " + tmp1 + " " + tmp2 + " " + lit + "\n";
        checkIfDeadCode(argu, instr);
        return new Optimizations(instr);
    }

    /**
     * f0 -> "MOVE"
     * f1 -> Temp()
     * f2 -> Exp()
     */
    public Optimizations visit(MoveStmt n, String argu) throws Exception {
        n.f0.accept(this, argu);
        String tmp = n.f1.accept(this, argu).instr_or_temp_;
        String exp = n.f2.accept(this, argu).getOptimizedTemp();
        String instr = "MOVE " + tmp + " " + exp + "\n";
        checkIfDeadCode(argu, instr);
        return new Optimizations(instr);
    }

    /**
     * f0 -> "PRINT"
     * f1 -> SimpleExp()
     */
    public Optimizations visit(PrintStmt n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu).getOptimizedTemp();
        String instr = "PRINT " + exp + "\n";
        checkIfDeadCode(argu, instr);
        return new Optimizations(instr);
    }

    /**
     * f0 -> "PRINTLN"
     * f1 -> SimpleExp()
     */
    public Optimizations visit(PrintlnStmt n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu).getOptimizedTemp();
        String instr = "PRINTLN " + exp + "\n";
        checkIfDeadCode(argu, instr);
        return new Optimizations(instr);
    }

    /**
     * f0 -> "ANSWER"
     * f1 -> SimpleExp()
     */
    public Optimizations visit(AnswerStmt n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu).getOptimizedTemp();
        String instr = "ANSWER " + exp + "\n";
        checkIfDeadCode(argu, instr);
        return new Optimizations(instr);
    }

    /**
     * f0 -> "PUBREAD"
     * f1 -> Temp()
     */
    public Optimizations visit(PublicReadStmt n, String argu) throws Exception {
        String expr = n.f1.accept(this, argu).getOptimizedTemp();
        String instr = "PUBREAD " + expr + "\n";
        checkIfDeadCode(argu, instr);
        return new Optimizations(instr);
    }

    /**
     * f0 -> "SECREAD"
     * f1 -> Temp()
     */
    public Optimizations visit(PrivateReadStmt n, String argu) throws Exception {
        String expr = n.f1.accept(this, argu).getOptimizedTemp();
        String instr = "SECREAD " + expr + "\n";
        checkIfDeadCode(argu, instr);
        return new Optimizations(instr);
    }

    /**
     * f0 -> "PUBSEEK"
     * f1 -> Temp()
     * f2 -> SimpleExp()
     */
    public Optimizations visit(PublicSeekStmt n, String argu) throws Exception {
        String tmp = n.f1.accept(this, argu).instr_or_temp_;
        String exp = n.f2.accept(this, argu).getOptimizedTemp();
        String instr = "PUBSEEK " + tmp + " " + exp + "\n";
        checkIfDeadCode(argu, instr);
        return new Optimizations(instr);
    }

    /**
     * f0 -> "SECSEEK"
     * f1 -> Temp()
     * f2 -> SimpleExp()
     */
    public Optimizations visit(PrivateSeekStmt n, String argu) throws Exception {
        String tmp = n.f1.accept(this, argu).instr_or_temp_;
        String exp = n.f2.accept(this, argu).getOptimizedTemp();
        String instr = "SECSEEK " + tmp + " " + exp + "\n";
        checkIfDeadCode(argu, instr);
        return new Optimizations(instr);
    }

    /**
     * f0 -> Call()
     * | HAllocate()
     * | BinOp()
     * | NotExp()
     * | SimpleExp()
     */
    public Optimizations visit(Exp n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "BEGIN"
     * f1 -> StmtList()
     * f2 -> "RETURN"
     * f3 -> SimpleExp()
     * f4 -> "END"
     */
    public Optimizations visit(StmtExp n, String argu) throws Exception {
        this.asm_.append("BEGIN\n");
        n.f1.accept(this, argu);
        String exp = n.f3.accept(this, argu).getOptimizedTemp();
        String instr = "RETURN " + exp + "\nEND\n";
        this.asm_.append(instr);
        return null;
    }

    /**
     * f0 -> "CALL"
     * f1 -> SimpleExp()
     * f2 -> "("
     * f3 -> ( Temp() )*
     * f4 -> ")"
     */
    public Optimizations visit(Call n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu).instr_or_temp_;
        StringBuilder args = new StringBuilder("(");
        if (n.f3.present()) {
            for (int i = 0; i < n.f3.size(); i++) {
                String temp = n.f3.nodes.get(i).accept(this, argu).instr_or_temp_;
                args.append(temp);
                if (i < n.f3.size() - 1) {
                    args.append(" ");
                }
            }
        }
        args.append(")");
        return new Optimizations("CALL " + exp + " " + args);
    }

    /**
     * f0 -> "HALLOCATE"
     * f1 -> SimpleExp()
     */
    public Optimizations visit(HAllocate n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu).getOptimizedTemp();
        return new Optimizations("HALLOCATE " + exp);
    }

    /**
     * f0 -> Operator()
     * f1 -> Temp()
     * f2 -> SimpleExp()
     */
    public Optimizations visit(BinOp n, String argu) throws Exception {
        String op = n.f0.accept(this, argu).instr_or_temp_;
        String tmp = n.f1.accept(this, argu).instr_or_temp_;
        String exp = n.f2.accept(this, argu).getOptimizedTemp();
        return new Optimizations(op + " " + tmp + " " + exp);
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
    public Optimizations visit(Operator n, String argu) throws Exception {
        return new Optimizations(n.f0.choice.toString());
    }

    /**
     * f0 -> "NOT"
     * f1 -> SimpleExp()
     */
    public Optimizations visit(NotExp n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu).getOptimizedTemp();
        String instr = "NOT " + exp + "\n";
        checkIfDeadCode(argu, instr);
        return new Optimizations(instr);
    }

    /**
     * f0 -> Temp()
     * | IntegerLiteral()
     * | Label()
     */
    public Optimizations visit(SimpleExp n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "TEMP"
     * f1 -> IntegerLiteral()
     */
    public Optimizations visit(Temp n, String argu) throws Exception {
        String t = n.f1.accept(this, argu).instr_or_temp_;
        String ret = "TEMP " + t;
        String copy_prop_fact = optimizations_.get("copyProp").get(argu + instr_cnt_);
        if (copy_prop_fact != null) {
            if (getMethFromFact(copy_prop_fact).equals(argu) && getFirstTempFromFact(copy_prop_fact).equals(ret)) {
                return new Optimizations(getSecondTempFromFact(copy_prop_fact));
            }
        }
        String const_prop_fact = optimizations_.get("constProp").get(argu + instr_cnt_);
        if (const_prop_fact != null) {
            if (getMethFromFact(const_prop_fact).equals(argu)) {
                if (copy_prop_fact != null && getFirstTempFromFact(copy_prop_fact).equals(getFirstTempFromFact(const_prop_fact))) {
                    if (getFirstTempFromFact(copy_prop_fact).equals(ret) && getMethFromFact(copy_prop_fact).equals(argu)) {
                        return new Optimizations(getImmediateFromFact(copy_prop_fact));
                    }
                }
                if (getFirstTempFromFact(const_prop_fact).equals(ret)) {
                    return new Optimizations(ret, getImmediateFromFact(const_prop_fact));
                }
            }
        }
        return new Optimizations(ret);
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public Optimizations visit(IntegerLiteral n, String argu) throws Exception {
        return new Optimizations(n.f0.toString());
    }

    /**
     * f0 -> <IDENTIFIER>
     */
    public Optimizations visit(Label n, String argu) throws Exception {
        return new Optimizations(n.f0.toString());
    }

}
