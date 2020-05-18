package org.twc.zerojavacompiler.spiglet2kanga;

import org.twc.zerojavacompiler.basetype.Method_t;
import org.twc.zerojavacompiler.spiglet2kanga.spigletsyntaxtree.*;
import org.twc.zerojavacompiler.spiglet2kanga.spigletvisitor.*;

import java.util.*;

public class Spiglet2Kanga extends GJNoArguDepthFirst<String> {

    private final HashMap<String, Method_t> method_map_;
    private Method_t currMethod;
    private final StringBuilder asm_;
    private boolean has_procedures_;

    public Spiglet2Kanga(HashMap<String, Method_t> method_map_) {
        this.asm_ = new StringBuilder();
        this.method_map_ = method_map_;
        this.has_procedures_ = false;
    }

    String getNewLabel(String labelName) {
        if ("Runtime_Error".equals(labelName)) {
            return "__" + labelName + "__";
        }
        return "__" + labelName + "_" + currMethod.getName() + "__";
    }

    public String getASM() {
        return asm_.toString();
    }

    public boolean hasProcedures() {
        return this.has_procedures_;
    }

    // tempName->regName
    // if spilled, load tempName in regName
    String temp2Reg(String regName, String tempName) {
        if (currMethod.temp_regs_map.containsKey(tempName)) {
            return currMethod.temp_regs_map.get(tempName);
        } else if (currMethod.save_regs_map.containsKey(tempName)) {
            return currMethod.save_regs_map.get(tempName);
        } else {
			/*
			asm_.append("*****" + tempName);
			for (String i : currMethod.regT.keySet())
				asm_.append("****" + i);
			for (String i : currMethod.regS.keySet())
				asm_.append("***" + i);
			for (String i : currMethod.regSpilled.keySet())
				asm_.append("**" + i);
			*/
            // spilled
            asm_.append("\t\tALOAD ").append(regName).append(" ").append(currMethod.spilled_regs_map.get(tempName)).append("\n");
            return regName;
        }
    }

    // MOVE tempName exp
    // if spilled, store in regSpilled
    void moveToTemp(String tempName, String exp) {
        if (currMethod.spilled_regs_map.containsKey(tempName)) {
            asm_.append("\t\tMOVE v0 ").append(exp).append("\n");
            asm_.append("\t\tASTORE ").append(currMethod.spilled_regs_map.get(tempName)).append(" v0\n");
        } else {
            tempName = temp2Reg("", tempName);
            if (!tempName.equals(exp))
                asm_.append("\t\tMOVE ").append(tempName).append(" ").append(exp).append("\n");
        }
    }

    // StmtList ::= ( (Label)?Stmt)*
    // get Labels
    public String visit(NodeOptional n) throws Exception {
        if (n.present()) {
            asm_.append(getNewLabel(n.node.accept(this)));
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
    public String visit(Goal n) throws Exception {
        currMethod = method_map_.get("MAIN");
        asm_.append("MAIN [").append(currMethod.getNum_parameters_()).append("][").append(currMethod.getStack_num_()).append("][").append(currMethod.getCall_param_num_()).append("]\n");
        n.f1.accept(this);
        asm_.append("END");
        n.f3.accept(this);
        return null;
    }

    /**
     * f0 -> Label()
     * f1 -> "["
     * f2 -> IntegerLiteral()
     * f3 -> "]"
     * f4 -> StmtExp()
     */
    public String visit(Procedure n) throws Exception {
        this.has_procedures_ = true;
        String methodName = n.f0.accept(this);
        currMethod = method_map_.get(methodName);
        asm_.append("\n").append(methodName).append(" [").append(currMethod.getNum_parameters_()).append("][").append(currMethod.getStack_num_()).append("][").append(currMethod.getCall_param_num_()).append("]\n");
        n.f4.accept(this);
        return null;
    }

    /**
     * f0 -> "NOOP"
     */
    public String visit(NoOpStmt n) throws Exception {
        asm_.append("\t\tNOOP\n");
        return null;
    }

    /**
     * f0 -> "ERROR"
     */
    public String visit(ErrorStmt n) throws Exception {
        asm_.append("\t\tERROR");
        return null;
    }

    /**
     * f0 -> "CJUMP"
     * f1 -> Temp()
     * f2 -> Label()
     */
    public String visit(CJumpStmt n) throws Exception {
        asm_.append("\t\tCJUMP ").append(temp2Reg("v0", n.f1.accept(this))).append(" ").append(getNewLabel(n.f2.accept(this))).append("\n");
        return null;
    }

    /**
     * f0 -> "JUMP"
     * f1 -> Label()
     */
    public String visit(JumpStmt n) throws Exception {
        asm_.append("\t\tJUMP ").append(getNewLabel(n.f1.accept(this))).append("\n");
        return null;
    }

    /**
     * f0 -> "HSTORE"
     * f1 -> Temp()
     * f2 -> IntegerLiteral()
     * f3 -> Temp()
     */
    public String visit(HStoreStmt n) throws Exception {
        asm_.append("\t\tHSTORE ").append(temp2Reg("v0", n.f1.accept(this))).append(" ").append(n.f2.accept(this)).append(" ").append(temp2Reg("v1", n.f3.accept(this))).append("\n");
        return null;
    }

    /**
     * f0 -> "HLOAD"
     * f1 -> Temp()
     * f2 -> Temp()
     * f3 -> IntegerLiteral()
     */
    public String visit(HLoadStmt n) throws Exception {
        String tempTo = n.f1.accept(this);
        String regFrom = temp2Reg("v1", n.f2.accept(this));
        String offset = n.f3.accept(this);
        if (currMethod.spilled_regs_map.containsKey(tempTo)) {
            asm_.append("\t\tHLOAD v1 ").append(regFrom).append(" ").append(offset).append("\n");
            moveToTemp(tempTo, "v1");
        } else {
            asm_.append("\t\tHLOAD ").append(temp2Reg("", tempTo)).append(" ").append(regFrom).append(" ").append(offset).append("\n");
        }
        return null;
    }

    /**
     * f0 -> "MOVE"
     * f1 -> Temp()
     * f2 -> Exp()
     */
    public String visit(MoveStmt n) throws Exception {
        moveToTemp(n.f1.accept(this), n.f2.accept(this));
        return null;
    }

    /**
     * f0 -> "PRINT"
     * f1 -> SimpleExp()
     */
    public String visit(PrintStmt n) throws Exception {
        asm_.append("\t\tPRINT ").append(n.f1.accept(this)).append("\n");
        return null;
    }

    /**
     * f0 -> "PRINTLN"
     * f1 -> SimpleExp()
     */
    public String visit(PrintlnStmt n) throws Exception {
        asm_.append("\t\tPRINTLN ").append(n.f1.accept(this)).append("\n");
        return null;
    }

    /**
     * f0 -> "ANSWER"
     * f1 -> SimpleExp()
     */
    public String visit(AnswerStmt n) throws Exception {
        asm_.append("\t\tANSWER ").append(n.f1.accept(this)).append("\n");
        return null;
    }

    /**
     * f0 -> "PUBREAD"
     * f1 -> Temp()
     */
    public String visit(PublicReadStmt n) throws Exception {
        asm_.append("\t\tPUBREAD ").append(n.f1.accept(this)).append("\n");
        return null;
    }

    /**
     * f0 -> "SECREAD"
     * f1 -> Temp()
     */
    public String visit(PrivateReadStmt n) throws Exception {
        asm_.append("\t\tSECREAD ").append(n.f1.accept(this)).append("\n");
        return null;
    }

    /**
     * f0 -> "PUBSEEK"
     * f1 -> Temp()
     * f2 -> SimpleExp()
     */
    public String visit(PublicSeekStmt n) throws Exception {
        asm_.append("\t\tPUBSEEK ").append(n.f1.accept(this)).append(" ").append(n.f2.accept(this)).append("\n");
        return null;
    }

    /**
     * f0 -> "SECSEEK"
     * f1 -> Temp()
     * f2 -> SimpleExp()
     */
    public String visit(PrivateSeekStmt n) throws Exception {
        asm_.append("\t\tSECSEEK ").append(n.f1.accept(this)).append(" ").append(n.f2.accept(this)).append("\n");
        return null;
    }

    /**
     * f0 -> Call()
     * | HAllocate()
     * | BinOp()
     * | NotExp()
     * | SimpleExp()
     */
    public String visit(Exp n) throws Exception {
        return n.f0.accept(this);
    }

    /**
     * f0 -> "BEGIN"
     * f1 -> StmtList()
     * f2 -> "RETURN"
     * f3 -> SimpleExp()
     * f4 -> "END"
     */
    public String visit(StmtExp n) throws Exception {
        int stackIdx = currMethod.getNum_parameters_() > 4 ? currMethod.getNum_parameters_() - 4 : 0;
        // store callee-saved S
        if (currMethod.save_regs_map.size() != 0) {
            for (int idx = stackIdx; idx < stackIdx + currMethod.save_regs_map.size(); idx++) {
                if (idx - stackIdx > 7) break;
                asm_.append("\t\tASTORE SPILLEDARG ").append(idx).append(" s").append(idx - stackIdx).append("\n");
            }
        }
        // move params regA to TEMP
        for (stackIdx = 0; stackIdx < currMethod.getNum_parameters_() && stackIdx < 4; stackIdx++) {
            if (currMethod.temp_reg_intervals.containsKey(stackIdx)) {
                moveToTemp("TEMP " + stackIdx, "a" + stackIdx);
            }
        }
        // load params(>4)
        for (; stackIdx < currMethod.getNum_parameters_(); stackIdx++) {
            String tempName = "TEMP " + stackIdx;
            if (currMethod.temp_reg_intervals.containsKey(stackIdx)) {
                if (currMethod.spilled_regs_map.containsKey(tempName)) {
                    asm_.append("\t\tALOAD v0 SPILLEDARG ").append(stackIdx - 4).append("\n");
                    moveToTemp(tempName, "v0");
                } else {
                    asm_.append("\t\tALOAD ").append(temp2Reg("", tempName)).append(" SPILLEDARG ").append(stackIdx - 4).append("\n");
                }
            }
        }

        n.f1.accept(this);
        // v0 stores returnValue
        String ret_exp = n.f3.accept(this);
        asm_.append("\t\tMOVE v0 ").append(ret_exp).append("\n");
        // restore callee-saved S
        stackIdx = currMethod.getNum_parameters_() > 4 ? currMethod.getNum_parameters_() - 4 : 0;
        if (currMethod.save_regs_map.size() != 0) {
            for (int j = stackIdx; j < stackIdx + currMethod.save_regs_map.size(); j++) {
                if (j - stackIdx > 7) break;
                asm_.append("\t\tALOAD s").append(j - stackIdx).append(" SPILLEDARG ").append(j).append("\n");
            }
        }
        asm_.append("\nEND");
        return null;
    }

    /**
     * f0 -> "CALL"
     * f1 -> SimpleExp()
     * f2 -> "("
     * f3 -> ( Temp() )*
     * f4 -> ")"
     */
    public String visit(Call n) throws Exception {
        Vector<Node> vTemp = n.f3.nodes;
        int nParam = vTemp.size();
        int paramIdx;
        // pass params
        for (paramIdx = 0; paramIdx < nParam && paramIdx < 4; paramIdx++)
            asm_.append("\t\tMOVE a").append(paramIdx).append(" ").append(temp2Reg("v0", vTemp.get(paramIdx).accept(this))).append("\n");
        for (; paramIdx < nParam; paramIdx++)
            asm_.append("\t\tPASSARG ").append(paramIdx - 3).append(" ").append(temp2Reg("v0", vTemp.get(paramIdx).accept(this))).append("\n");
        // call
        asm_.append("\t\tCALL ").append(n.f1.accept(this)).append("\n");
        return "v0";
    }

    /**
     * f0 -> "HALLOCATE"
     * f1 -> SimpleExp()
     */
    public String visit(HAllocate n) throws Exception {
        return "HALLOCATE " + n.f1.accept(this);
    }

    /**
     * f0 -> Operator()
     * f1 -> Temp()
     * f2 -> SimpleExp()
     */
    public String visit(BinOp n) throws Exception {
        return n.f0.accept(this) + temp2Reg("v0", n.f1.accept(this)) + " " + n.f2.accept(this);
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
    public String visit(Operator n) throws Exception {
        String[] _ret = {
                "LT ", "LTE ", "GT ", "GTE ", "EQ ", "NEQ ",
                "PLUS ", "MINUS ", "TIMES ", "DIV ", "MOD ",
                "AND ", "OR ", "XOR ", "SLL ", "SRL "};
        return _ret[n.f0.which];
    }

    /**
     * f0 -> "NOT"
     * f1 -> SimpleExp()
     */
    public String visit(NotExp n) throws Exception {
        return "NOT " + n.f1.accept(this);
    }

    /**
     * f0 -> Temp() | IntegerLiteral() | Label()
     */
    public String visit(SimpleExp n) throws Exception {
        String _ret = n.f0.accept(this);
        if (n.f0.which == 0) {
            _ret = temp2Reg("v1", _ret);
        }
        return _ret;
    }

    /**
     * f0 -> "TEMP"
     * f1 -> IntegerLiteral()
     */
    public String visit(Temp n) throws Exception {
        return "TEMP " + n.f1.accept(this);
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