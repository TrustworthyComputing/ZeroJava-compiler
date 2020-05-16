package org.twc.zerojavacompiler.spigletoptimizer;

import org.twc.zerojavacompiler.spiglet2kanga.spigletsyntaxtree.*;
import org.twc.zerojavacompiler.spiglet2kanga.spigletvisitor.GJDepthFirst;

import java.util.Enumeration;
import java.util.Map;

public class OptimizerVisitor extends GJDepthFirst<String, String> {
    private StringBuilder asm_;
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

    static String getMeth(String fact) {
        String[] parts = fact.split(",");
        return parts[0].substring(2, parts[0].length() - 1);
    }

    static String getTemp(String fact) {
        String[] parts = fact.split(",");
        parts[2] = parts[2].substring(2, parts[2].length() - 1);
        return parts[2];
    }

    static String getOpt(String fact, boolean num) {
        String[] parts = fact.split(",");
        if (num) {
            parts[3] = parts[3].substring(1, parts[3].length() - 1);
        } else {
            parts[3] = parts[3].substring(2, parts[3].length() - 2);
        }
        return parts[3];
    }

    public String visit(NodeSequence n, String argu) throws Exception {
        if (n.size() == 1) {
            return n.elementAt(0).accept(this, argu);
        }
        StringBuilder _ret = null;
        for (Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
            String ret = e.nextElement().accept(this, argu);
            if (ret != null) {
                if (_ret == null) {
                    _ret = new StringBuilder(ret);
                } else {
                    _ret.append(" ").append(ret);
                }
            }
        }
        assert _ret != null;
        return _ret.toString();
    }

    /**
     * f0 -> "MAIN"
     * f1 -> StmtList()
     * f2 -> "END"
     * f3 -> ( Procedure() )*
     * f4 -> <EOF>
     */
    public String visit(Goal n, String argu) throws Exception {
        this.asm_.append("MAIN\n");
        n.f1.accept(this, "MAIN");
        this.asm_.append("END\n");
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
    public String visit(Procedure n, String argu) throws Exception {
        this.instr_cnt_ = 1;
        String id = n.f0.accept(this, argu);
        String args = n.f2.accept(this, argu);
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
    public String visit(Stmt n, String argu) throws Exception {
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
        String[] parts = tmp.split("&");
        tmp = parts[0];
        String label = n.f2.accept(this, argu);
        String instr = "CJUMP " + tmp + " " + label + "\n";
        String str1 = optimizations_.get("deadCode").get(argu + instr_cnt_);
        if (str1 == null) {
            this.asm_.append(instr);
        } else if (!getMeth(str1).equals(argu)) {
            this.asm_.append(instr);
        }
        return instr;
    }

    /**
     * f0 -> "JUMP"
     * f1 -> Label()
     */
    public String visit(JumpStmt n, String argu) throws Exception {
        String label = n.f1.accept(this, argu);
        String instr = "JUMP " + label + "\n";
        String str1 = optimizations_.get("deadCode").get(argu + instr_cnt_);
        if (str1 == null) {
            this.asm_.append(instr);
        } else if (!getMeth(str1).equals(argu)) {
            this.asm_.append(instr);
        }
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
        String[] parts = tmp1.split("&");
        tmp1 = parts[0];
        String lit = n.f2.accept(this, argu);
        String tmp2 = n.f3.accept(this, argu);
        parts = tmp2.split("&");
        tmp2 = parts[0];
        String instr = "HSTORE " + tmp1 + " " + lit + " " + tmp2 + "\n";
        String str1 = optimizations_.get("deadCode").get(argu + instr_cnt_);
        if (str1 == null) {
            this.asm_.append(instr);
        } else if (!getMeth(str1).equals(argu)) {
            this.asm_.append(instr);
        }
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
        String[] parts = tmp1.split("&");
        tmp1 = parts[0];
        String tmp2 = n.f2.accept(this, argu);
        parts = tmp2.split("&");
        tmp2 = parts[0];
        String lit = n.f3.accept(this, argu);
        String instr = "HLOAD " + tmp1 + " " + tmp2 + " " + lit + "\n";
        String str1 = optimizations_.get("deadCode").get(argu + instr_cnt_);
        if (str1 == null) {
            this.asm_.append(instr);
        } else if (!getMeth(str1).equals(argu)) {
            this.asm_.append(instr);
        }
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
        String[] parts = tmp.split("&");
        tmp = parts[0];
        String exp = n.f2.accept(this, argu);
        String instr = null;
        if (exp != null) {
            if (exp.matches("TEMP(.*)")) {
                String[] parts2 = exp.split("&");
                if (parts2.length == 2) {
                    exp = parts2[1];
                } else {
                    exp = parts2[0];
                }
                instr = "MOVE " + tmp + " " + exp + "\n";
            } else if (exp.matches("[0-9]+")) {
                instr = "MOVE " + tmp + " " + Integer.parseInt(exp) + "\n";
            } else {
                instr = "MOVE " + tmp + " " + exp + "\n";
            }
            String str1 = optimizations_.get("deadCode").get(argu + instr_cnt_);
            if (str1 == null) {
                this.asm_.append(instr);
            } else if (!getMeth(str1).equals(argu)) {
                this.asm_.append(instr);
            }
        }
        return instr;
    }

    /**
     * f0 -> "PRINT"
     * f1 -> SimpleExp()
     */
    public String visit(PrintStmt n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu);
        String[] parts = exp.split("&");
        if (parts.length == 2) {
            exp = parts[1];
        } else {
            exp = parts[0];
        }
        String instr = "PRINT " + exp + "\n";
        String str1 = optimizations_.get("deadCode").get(argu + instr_cnt_);
        if (str1 == null) {
            this.asm_.append(instr);
        } else if (!getMeth(str1).equals(argu)) {
            this.asm_.append(instr);
        }
        return instr;
    }

    /**
     * f0 -> "PRINTLN"
     * f1 -> SimpleExp()
     */
    public String visit(PrintlnStmt n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu);
        String[] parts = exp.split("&");
        if (parts.length == 2) {
            exp = parts[1];
        } else {
            exp = parts[0];
        }
        String instr = "PRINTLN " + exp + "\n";
        String str1 = optimizations_.get("deadCode").get(argu + instr_cnt_);
        if (str1 == null) {
            this.asm_.append(instr);
        } else if (!getMeth(str1).equals(argu)) {
            this.asm_.append(instr);
        }
        return instr;
    }

    /**
     * f0 -> "ANSWER"
     * f1 -> SimpleExp()
     */
    public String visit(AnswerStmt n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu);
        String[] parts = exp.split("&");
        if (parts.length == 2) {
            exp = parts[1];
        } else {
            exp = parts[0];
        }
        String instr = "ANSWER " + exp + "\n";
        String str1 = optimizations_.get("deadCode").get(argu + instr_cnt_);
        if (str1 == null) {
            this.asm_.append(instr);
        } else if (!getMeth(str1).equals(argu)) {
            this.asm_.append(instr);
        }
        return instr;
    }

    /**
     * f0 -> "PUBREAD"
     * f1 -> Temp()
     */
    public String visit(PublicReadStmt n, String argu) throws Exception {
        String expr = n.f1.accept(this, argu);
        String[] parts = expr.split("&");
        if (parts.length == 2) {
            expr = parts[1];
        } else {
            expr = parts[0];
        }
        String instr = "PUBREAD " + expr + "\n";
        String str1 = optimizations_.get("deadCode").get(argu + instr_cnt_);
        if (str1 == null) {
            this.asm_.append(instr);
        } else if (!getMeth(str1).equals(argu)) {
            this.asm_.append(instr);
        }
        return instr;
    }

    /**
     * f0 -> "SECREAD"
     * f1 -> Temp()
     */
    public String visit(PrivateReadStmt n, String argu) throws Exception {
        String expr = n.f1.accept(this, argu);
        String[] parts = expr.split("&");
        if (parts.length == 2) {
            expr = parts[1];
        } else {
            expr = parts[0];
        }
        String instr = "SECREAD " + expr + "\n";
        String str1 = optimizations_.get("deadCode").get(argu + instr_cnt_);
        if (str1 == null) {
            this.asm_.append(instr);
        } else if (!getMeth(str1).equals(argu)) {
            this.asm_.append(instr);
        }
        return instr;
    }

    /**
     * f0 -> "PUBSEEK"
     * f1 -> Temp()
     * f2 -> SimpleExp()
     */
    public String visit(PublicSeekStmt n, String argu) throws Exception {
        String tmp = n.f1.accept(this, argu);
        String[] parts = tmp.split("&");
        tmp = parts[0];
        String exp = n.f2.accept(this, argu);
        String instr = null;
        if (exp != null) {
            if (exp.matches("TEMP(.*)")) {
                String[] parts2 = exp.split("&");
                if (parts2.length == 2) {
                    exp = parts2[1];
                } else {
                    exp = parts2[0];
                }
                instr = "PUBSEEK " + tmp + " " + exp + "\n";
            } else if (exp.matches("[0-9]+")) {
                instr = "PUBSEEK " + tmp + " " + Integer.parseInt(exp) + "\n";
            } else {
                instr = "PUBSEEK " + tmp + " " + exp + "\n";
            }
            String str1 = optimizations_.get("deadCode").get(argu + instr_cnt_);
            if (str1 == null) {
                this.asm_.append(instr);
            } else if (!getMeth(str1).equals(argu)) {
                this.asm_.append(instr);
            }
        }
        return instr;
    }

    /**
     * f0 -> "SECSEEK"
     * f1 -> Temp()
     * f2 -> SimpleExp()
     */
    public String visit(PrivateSeekStmt n, String argu) throws Exception {
        String tmp = n.f1.accept(this, argu);
        String[] parts = tmp.split("&");
        tmp = parts[0];
        String exp = n.f2.accept(this, argu);
        String instr = null;
        if (exp != null) {
            if (exp.matches("TEMP(.*)")) {
                String[] parts2 = exp.split("&");
                if (parts2.length == 2) {
                    exp = parts2[1];
                } else {
                    exp = parts2[0];
                }
                instr = "SECSEEK " + tmp + " " + exp + "\n";
            } else if (exp.matches("[0-9]+")) {
                instr = "SECSEEK " + tmp + " " + Integer.parseInt(exp) + "\n";
            } else {
                instr = "SECSEEK " + tmp + " " + exp + "\n";
            }
            String str1 = optimizations_.get("deadCode").get(argu + instr_cnt_);
            if (str1 == null) {
                this.asm_.append(instr);
            } else if (!getMeth(str1).equals(argu)) {
                this.asm_.append(instr);
            }
        }
        return instr;
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
        this.asm_.append("BEGIN\n");
        n.f1.accept(this, argu);
        String exp = n.f3.accept(this, argu);
        String[] parts = exp.split("&");
        if (parts.length == 2) {
            exp = parts[1];
        } else {
            exp = parts[0];
        }
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
    public String visit(Call n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu);
        StringBuilder tmp = new StringBuilder("(");
        if (n.f3.present()) {
            for (int i = 0; i < n.f3.size(); i++) {
                String t = n.f3.nodes.get(i).accept(this, argu);
                String[] parts = t.split("&");
                t = parts[0];
                tmp.append(t);
                if (i < n.f3.size() - 1) {
                    tmp.append(" ");
                }
            }
        }
        tmp.append(")");
        return "CALL " + exp + " " + tmp;
    }

    /**
     * f0 -> "HALLOCATE"
     * f1 -> SimpleExp()
     */
    public String visit(HAllocate n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu);
        String[] parts = exp.split("&");
        if (parts.length == 2) {
            exp = parts[1];
        } else {
            exp = parts[0];
        }
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
        String[] parts = tmp.split("&");
        tmp = parts[0];
        parts = exp.split("&");
        if (parts.length == 2) {
            exp = parts[1];
        } else {
            exp = parts[0];
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
        String[] parts = exp.split("&");
        if (parts.length == 2) {
            exp = parts[1];
        } else {
            exp = parts[0];
        }
        String instr = "NOT " + exp + "\n";
        String str1 = optimizations_.get("deadCode").get(argu + instr_cnt_);
        if (str1 == null) {
            this.asm_.append(instr);
        } else if (!getMeth(str1).equals(argu)) {
            this.asm_.append(instr);
        }
        return instr;
    }

    /**
     * f0 -> "TEMP"
     * f1 -> IntegerLiteral()
     */
    public String visit(Temp n, String argu) throws Exception {
        String t = n.f1.accept(this, argu);
        String ret = "TEMP " + t;
        assert optimizations_ != null;
        String str1 = optimizations_.get("copyProp").get(argu + instr_cnt_);
        String str2 = optimizations_.get("constProp").get(argu + instr_cnt_);
        if (str1 != null) { // copy
            if (getMeth(str1).equals(argu) && getTemp(str1).equals(ret)) {
                return getOpt(str1, false);
            }
        }
        if (str2 != null) { // constant
            if (getMeth(str2).equals(argu)) {
                if (str1 != null && getTemp(str1).equals(getTemp(str2))) {
                    if (getTemp(str1).equals(ret) && getMeth(str1).equals(argu)) {
                        return getOpt(str1, false);
                    }
                }
                if (getTemp(str2).equals(ret)) {
                    return ret + "&" + getOpt(str2, true);
                }
            }
        }
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

}
