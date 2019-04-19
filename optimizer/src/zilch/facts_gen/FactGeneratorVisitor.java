package facts_gen;

import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;
import java.io.*;

public class FactGeneratorVisitor extends GJDepthFirst<String, String> {
    public LinkedList<Instruction_t> instrList;
    public Instruction_t answerInstruction;
    public LinkedList<Var_t> varList;
    public LinkedList<Next_t> nextList;
    public LinkedList<VarMove_t> varMoveList;
    public LinkedList<ConstMove_t> constMoveList;
    public LinkedList<BinOpMove_t> binOpMoveList;
    public LinkedList<VarUse_t> varUseList;
    public LinkedList<VarDef_t> varDefList;
    public LinkedList<Jump_t> jumpList;
    public LinkedList<Cjump_t> cjumpList;
    public int ic1;
    public int ic2;

    public FactGeneratorVisitor() {
        instrList = new LinkedList<Instruction_t>();
        varList = new LinkedList<Var_t>();
        nextList = new LinkedList<Next_t>();
        varMoveList = new LinkedList<VarMove_t>();
        constMoveList = new LinkedList<ConstMove_t>();
        binOpMoveList =  new LinkedList<BinOpMove_t>();
        varUseList = new LinkedList<VarUse_t>();
        varDefList = new LinkedList<VarDef_t>();
        jumpList = new LinkedList<Jump_t>();
        cjumpList = new LinkedList<Cjump_t>();
        this.ic1 = 0;
        this.ic2 = 0;
    }

    public String visit(NodeSequence n, String argu) throws Exception {
        if (n.size() == 1) {
            return n.elementAt(0).accept(this,argu);
        }
        String _ret = null;
        int _count=0;
        for ( Enumeration<Node> e = n.elements() ; e.hasMoreElements() ; ) {
            String ret = e.nextElement().accept(this,argu);
            if (ret != null) {
                if (_ret == null)
                    _ret = ret;
            }
            _count++;
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
                this.ic1++;
                instrList.addLast(new Instruction_t("\""+argu+"\"", this.ic1, "\""+str+"\""));
                if (str.toLowerCase().contains("answer".toLowerCase())) {
                    answerInstruction = new Instruction_t("\""+argu+"\"", this.ic1, "\""+str+"\"");
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
     *       | swStmt()
     *       | lwStmt()
     *       | PrintStmt()
     *       | AnswerStmt()
     *       | ReadStmt()
     *       | SeekStmt()
     */
    public String visit(Stmt n, String argu) throws Exception {
        this.ic2++;
        return n.f0.accept(this, argu);
    }   

    /**
     * f0 -> JmpOps()
     * f1 -> Register()
     * f2 -> ","
     * f3 -> Register()
     * f4 -> ","
     * f5 -> Label()
     */
    public String visit(JmpStmts n, String argu) throws Exception {
        String op = n.f0.accept(this, argu);
        String reg1 = n.f1.accept(this, argu);
        String reg2 = n.f3.accept(this, argu);
        String label = n.f5.accept(this, argu);
        String instr = op + ", " + reg1 + ", " + reg2 + ", " + label;
        
        if (op.equals("j")) {
            jumpList.addLast(new Jump_t("\""+argu+"\"", this.ic2, "\""+label+"\""));
        } else {
            cjumpList.addLast(new Cjump_t("\""+argu+"\"", this.ic2, "\""+label+"\""));
        }
        
        return instr;
    }

    /**
     * f0 -> ComparisonOps()
     * f1 -> Register()
     * f2 -> ","
     * f3 -> Register()
     * f4 -> ","
     * f5 -> SimpleExp()
     */
     public String visit(ComparisonStmts n, String argu) throws Exception {
         String op = n.f0.accept(this, argu);
         String reg1 = n.f1.accept(this, argu);
         String reg2 = n.f3.accept(this, argu);
         String reg3 = n.f5.accept(this, argu);
         String instr = op + ", " + reg1 + ", " + reg2 + ", " + reg3;
         varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+reg2+"\""));
         varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+reg3+"\""));
         return instr;
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
    public String visit(swStmt n, String argu) throws Exception {
        String src = n.f1.accept(this, argu);
        String idx = n.f3.accept(this, argu);
        String addr = n.f5.accept(this, argu);
        String op = "sw " + src + ", " + idx + "(" + addr + ")";
        varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+src+"\""));
        if (addr.startsWith("$r")) {
            varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+addr+"\""));
        }
        if (addr.startsWith("$r")) {
            varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+idx+"\""));
        }
        return op;
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
    public String visit(lwStmt n, String argu) throws Exception {
        String dst = n.f1.accept(this, argu);
        String idx = n.f3.accept(this, argu);
        String addr = n.f5.accept(this, argu);
        String op = "lw " + dst + ", " + idx + "(" + addr + ")";
        varDefList.addLast(new VarDef_t("\""+argu+"\"", this.ic2, "\""+dst+"\""));
        if (addr.startsWith("$r")) {
            varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+addr+"\""));
        }
        if (addr.startsWith("$r")) {
            varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+idx+"\""));
        }
        return op;
    }

    /**
     * f0 -> TwoRegInstrOp()
     * f1 -> Register()
     * f2 -> ","
     * f3 -> Register()
     * f4 -> ","
     * f5 -> SimpleExp()
     */
    public String visit(TwoRegInstr n, String argu) throws Exception {
        String op = n.f0.accept(this, argu);
        String dst = n.f1.accept(this, argu);
        String sec_reg = n.f3.accept(this, argu);
        String src = n.f5.accept(this, argu);
        if (src == null) { return null; }
        String instr = op + " " + dst + ", " + sec_reg + ", " + src;
        if (src.startsWith("$r")) {
            varMoveList.addLast(new VarMove_t("\""+argu+"\"", this.ic2, "\""+dst+"\"", "\""+src+"\""));
            varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+src+"\""));
        } else if (src.matches("[0-9]+")) {
            constMoveList.addLast(new ConstMove_t("\""+argu+"\"", this.ic2, "\""+dst+"\"", Integer.parseInt(src) ));
        }
        varDefList.addLast(new VarDef_t("\""+argu+"\"", this.ic2, "\""+dst+"\""));
        return instr;
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
        varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+src1+"\""));
        if (src2.startsWith("$r")) { // if third argument is not immediate
            varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+src2+"\""));
        }

        binOpMoveList.addLast(new BinOpMove_t("\""+argu+"\"", this.ic2, "\""+dst+"\"", "\""+src1+"\"", "\""+src2+"\""));
        
        varDefList.addLast(new VarDef_t("\""+argu+"\"", this.ic2, "\""+dst+"\""));
        return instr;
    }

    /**
     * f0 -> "print"
     * f1 -> Register()
     * f2 -> ","
     * f3 -> Register()
     * f4 -> ","
     * f5 -> Register()
     */
    public String visit(PrintStmt n, String argu) throws Exception {
        String reg = n.f5.accept(this, argu);
        String op = "print " + reg + ", " + reg + ", " + reg;
        if (reg != null && reg.startsWith("$r")) {
            varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+reg+"\""));
        }
        return op;
    }
    
    /**
     * f0 -> "answer"
     * f1 -> Register()
     * f2 -> ","
     * f3 -> Register()
     * f4 -> ","
     * f5 -> Register()
     */
    public String visit(AnswerStmt n, String argu) throws Exception {
        String reg = n.f3.accept(this, argu);
        String op = "answer " + reg + ", " + reg + ", " + reg;
        if (reg != null && reg.startsWith("$r")) {
            varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+reg+"\""));
        }
        return op;
    }

    /**
     * f0 -> "read"
     * f1 -> Register()
     * f2 -> ","
     * f3 -> Register()
     * f4 -> ","
     * f5 -> SimpleExp()
     */
    public String visit(ReadStmt n, String argu) throws Exception {
        String dst = n.f1.accept(this, argu);
        String tape = n.f5.accept(this, argu);
        String op = "read " + dst + ", " + dst + ", " + tape;
        if (tape != null && tape.startsWith("$r")) {
            varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+tape+"\""));
        }
        varDefList.addLast(new VarDef_t("\""+argu+"\"", this.ic2, "\""+dst+"\""));
        return op;
    }
    
    /**
     * f0 -> "seek"
     * f1 -> Register()
     * f2 -> ","
     * f3 -> SimpleExp()
     * f4 -> ","
     * f5 -> SimpleExp()
     */
    public String visit(SeekStmt n, String argu) throws Exception {
        String dst = n.f1.accept(this, argu);
        String sec_reg = n.f3.accept(this, argu);
        String tape = n.f5.accept(this, argu);
        String op = "seek " + dst + ", " + sec_reg + ", " + tape;
        if (sec_reg != null && sec_reg.startsWith("$r")) {
            varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+sec_reg+"\""));
        }
        if (tape != null && tape.startsWith("$r")) {
            varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+tape+"\""));
        }
        varDefList.addLast(new VarDef_t("\""+argu+"\"", this.ic2, "\""+dst+"\""));
        return op;
    }

    /**
     * f0 -> "move"
     *       | "not"
     */
    public String visit(TwoRegInstrOp n, String argu) throws Exception {
        return n.f0.choice.toString();
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
     * f0 -> "j"
     *       | "cjmp"
     *       | "cnjmp"
     */
    public String visit(JmpOps n, String argu) throws Exception {
        return n.f0.choice.toString();
    }
    
    /**
     * f0 -> "j"
     *       | "cjmp"
     *       | "cnjmp"
     */
    public String visit(ComparisonOps n, String argu) throws Exception {
        return n.f0.choice.toString();
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
        Var_t var = new Var_t("\""+argu+"\"", "\""+v+"\"");
        for (Var_t variable : varList) {
            if (variable.var.equals("\"" + v + "\"")) {
                return v;
            }
        }
        varList.addLast(var);
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

}
