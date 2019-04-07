package facts_gen;

import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;
import java.io.*;

public class FactGeneratorVisitor extends GJDepthFirst<String, String> {
    public LinkedList<Instruction_t> instrList;
    public LinkedList<Var_t> varList;
    public LinkedList<Next_t> nextList;
    public LinkedList<VarMove_t> varMoveList;
    public LinkedList<ConstMove_t> constMoveList;
    public LinkedList<BinOpMove_t> binOpMoveList;
    public LinkedList<VarUse_t> varUseList;
    public LinkedList<VarDef_t> varDefList;
    public LinkedList<Cjump_t> cjumpList;
    public LinkedList<Jump_t> jumpList;
    public LinkedList<Args_t> argsList;
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
        cjumpList = new LinkedList<Cjump_t>();
        jumpList = new LinkedList<Jump_t>();
        argsList = new LinkedList<Args_t>();
        this.ic1 = 0;
        this.ic2 = 0;
    }

    public String visit(NodeSequence n, String argu) throws Exception {
        if (n.size() == 1)
            return n.elementAt(0).accept(this,argu);
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
    * f0 -> TinyRAMProg()
    * f1 -> <EOF>
    */
    public String visit(Goal n, String argu) throws Exception {
        n.f0.accept(this, "Main");
        return null;
    }

    /**
    * f0 ->   ( ( Label() )* ( Stmt() )* )*
    */
    public String visit(TinyRAMProg n, String argu) throws Exception {
        if (n.f0.present()) {
            for (int i = 0 ; i < n.f0.size() ; i++) {
                String str = n.f0.elementAt(i).accept(this, argu);
                // if (str.matches("(.*)ERR(.*)")){
                //     ic2--;
                //     continue;
                // }
                this.ic1++;
                instrList.addLast(new Instruction_t("\""+argu+"\"", this.ic1, "\""+str+"\""));
            }
        }
        return null;
    }

    /**
    * f0 -> NoOpStmt()
    *       | ErrorStmt()
    *       | CJumpStmt()
    *       | JumpStmt()
    *       | StoreWStmt()
    *       | LoadWStmt()
    *       | MovStmt()
    *       | PrintStmt()
    */
    public String visit(Stmt n, String argu) throws Exception {
        this.ic2++;
        String stmt = n.f0.accept(this, argu);
        return stmt;
    }   

    /**
    * f0 -> "CJMP"
    * f1 -> Register()
    * f2 -> Label()
    */
    public String visit(CJumpStmt n, String argu) throws Exception {
        // String tmp = n.f1.accept(this, argu);
        // String label = n.f2.accept(this, argu);
        // String op = "CJMP " + tmp + " " + label;
        // cjumpList.addLast(new Cjump_t("\""+argu+"\"", this.ic2, "\""+label+"\""));
        // varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+tmp+"\""));
        // return op;
        return null;
    }

    /**
    * f0 -> "JMP"
    * f1 -> Label()
    */
    public String visit(JumpStmt n, String argu) throws Exception {
        // String label = n.f1.accept(this, argu);
        // String op = "JMP " + label;
        // jumpList.addLast(new Jump_t("\""+argu+"\"", this.ic2, "\""+label+"\""));
        // return op;
        return null;
    }

    /**
    * f0 -> "STOREW"
    * f1 -> Register()
    * f2 -> IntegerLiteral()
    * f3 -> Register()
    */
    public String visit(StoreWStmt n, String argu) throws Exception {
        // String tmp1 = n.f1.accept(this, argu);
        // String lit = n.f2.accept(this, argu);
        // String tmp2 = n.f3.accept(this, argu);
        // String op = "STOREW " + tmp1 + " " + lit + " " + tmp2;
        // varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+tmp1+"\""));
        // varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+tmp2+"\""));
        // return op;
        return null;
    }

    /**
    * f0 -> "LOADW"
    * f1 -> Register()
    * f2 -> Register()
    * f3 -> IntegerLiteral()
    */
    public String visit(LoadWStmt n, String argu) throws Exception {
        // String tmp1 = n.f1.accept(this, argu);
        // String tmp2 = n.f2.accept(this, argu);
        // String lit = n.f3.accept(this, argu);
        // String op = "LOADW " + tmp1 + " " + tmp2 + " " + lit;
        // varDefList.addLast(new VarDef_t("\""+argu+"\"", this.ic2, "\""+tmp1+"\""));
        // varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+tmp2+"\""));
        // return op;
        return null;
    }

    /**
    * f0 -> TwoRegInstrOp()
    * f1 -> Register()
    * f2 -> Register()
    * f3 -> SimpleExp()
    */
    public String visit(TwoRegInstr n, String argu) throws Exception {
        String op = n.f0.accept(this, argu);
        String dst = n.f1.accept(this, argu);
        String sec_reg = n.f1.accept(this, argu);
        String third_reg = n.f3.accept(this, argu);
        if (third_reg == null) { return null; }
        String instr = null;
        if (third_reg.matches("r(.*)")) {
            instr = "MOV " + dst + " " + sec_reg + " " + third_reg;
            varMoveList.addLast(new VarMove_t("\""+argu+"\"", this.ic2, "\""+dst+"\"", "\""+third_reg+"\""));
            varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+third_reg+"\""));
        } else if (third_reg.matches("[0-9]+")) {
            instr = "MOV " + dst + " " + sec_reg + " " + Integer.parseInt(third_reg);
            constMoveList.addLast(new ConstMove_t("\""+argu+"\"", this.ic2, "\""+dst+"\"", Integer.parseInt(third_reg)));
        }
        varDefList.addLast(new VarDef_t("\""+argu+"\"", this.ic2, "\""+dst+"\""));
        return instr;
    }
    
    // /**
    // * f0 -> Operator()
    // * f1 -> Register()
    // * f2 -> SimpleExp()
    // */
    // public String visit(BinOp n, String argu) throws Exception {
    //     String op = n.f0.accept(this, argu);
    //     String tmp = n.f1.accept(this, argu);
    //     String exp = n.f2.accept(this, argu);
    //     varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+tmp+"\""));
    //     if (exp != null && exp.matches("r(.*)")) {
    //         varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+exp+"\""));
    //     }
    //     return op + " " + tmp + " " + exp;
    // }

    /**
    * f0 -> ThreeRegInstrOp()
    * f1 -> Register()
    * f2 -> Register()
    * f3 -> SimpleExp()
    */
    public String visit(ThreeRegInstr n, String argu) throws Exception {
        String op = n.f0.accept(this, argu);
        String dst = n.f1.accept(this, argu);
        String sec_reg = n.f2.accept(this, argu);
        String third_reg = n.f3.accept(this, argu);
        if (third_reg == null) { return null; }
        String instr = null;
        varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+sec_reg+"\""));
        
        if (third_reg.matches("r(.*)")) {
            instr = "ADD " + dst + " " + sec_reg + " " + third_reg;
            // varMoveList.addLast(new VarMove_t("\""+argu+"\"", this.ic2, "\""+dst+"\"", "\""+third_reg+"\""));
            varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+third_reg+"\""));
            
            binOpMoveList.addLast(new BinOpMove_t("\""+argu+"\"", this.ic2, "\""+dst+"\"", "\""+op+" "+sec_reg+" "+third_reg+"\""));
        } else if (third_reg.matches("[0-9]+")) {
            instr = "ADD " + dst + " " + sec_reg + " " + Integer.parseInt(third_reg);
            // constMoveList.addLast(new ConstMove_t("\""+argu+"\"", this.ic2, "\""+dst+"\"", Integer.parseInt(third_reg)));
            
            binOpMoveList.addLast(new BinOpMove_t("\""+argu+"\"", this.ic2, "\""+dst+"\"", "\""+op+" "+sec_reg+" "+Integer.parseInt(third_reg)+"\""));
        }
        varDefList.addLast(new VarDef_t("\""+argu+"\"", this.ic2, "\""+dst+"\""));
        return instr;
    }

    /**
    * f0 -> "PRINT"
    * f1 -> SimpleExp()
    */
    public String visit(PrintStmt n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu);
        String op = "PRINT " + exp;
        if (exp != null && exp.matches("r(.*)"))
            varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+exp+"\""));
        return op;
    }

    // /**
    // * f0 -> "BEGIN"
    // * f1 -> StmtList()
    // * f2 -> "RETURN"
    // * f3 -> SimpleExp()
    // * f4 -> "END"
    // */
    // public String visit(StmtExp n, String argu) throws Exception {
    //     n.f1.accept(this, argu);
    //     this.ic2++;
    //     String exp = n.f3.accept(this, argu);
    //     String ret = "RETURN " + exp;
    //     this.ic1++;
    //     instrList.addLast(new Instruction_t("\""+argu+"\" ", this.ic1, "\""+ret+"\""));
    //     if (exp != null && exp.matches("r(.*)")){
    //         varUseList.addLast(new VarUse_t("\""+argu+"\"", this.ic2, "\""+exp+"\""));
    //     }
    //     return null;
    // }

    /**
    * f0 ->   "MOV"
    *       | "NOT"
    */
    public String visit(TwoRegInstrOp n, String argu) throws Exception {
        return n.f0.choice.toString();
    }

    /**
    * f0 ->   "ADD"
    *       | "SUB"
    *       | "MULL" 
    */
    public String visit(ThreeRegInstrOp n, String argu) throws Exception {
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
        // String v = n.f0.choice.toString();
        String v = n.f0.toString();
        Var_t var = new Var_t("\""+argu+"\"", "\""+v+"\"");
        
        for (Var_t variable : varList) {
            if (variable.temp.equals("\"" + v + "\"")) {
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
