package optimizer;

import syntaxtree.*;
import visitor.GJDepthFirst;
import facts_gen.*;
import java.util.*;
import java.io.*;

public class OptimizerVisitor extends GJDepthFirst<String, String> {
    public String result;
    public int ic1;
    private Map<String, Map<String, String>> optimisationMap;
    
    public OptimizerVisitor(Map<String, Map<String, String>> optimisationMap) {
        result = new String();
        this.ic1 = 1;
        this.optimisationMap = optimisationMap;
    }

    // static String getMeth(String fact) {
    //     String []parts = fact.split(",");
    //     return parts[0].substring(2,  parts[0].length()-1);
    // }

    static int getLine(String fact) {
        String []parts = fact.split(",");
        return Integer.parseInt(parts[1].substring(1));
    }

    static String getTemp(String fact) {
        String []parts = fact.split(",");
        parts[2] = parts[2].substring(2, parts[2].length()-1);
        return parts[2];
    }

    static String getOpt(String fact, boolean num) {
        String []parts = fact.split(",");
        if (num) {
            parts[3] = parts[3].substring(1, parts[3].length()-1);
        } else {
            parts[3] = parts[3].substring(2, parts[3].length()-2);
        }
        return parts[3];
    }

    public String visit(NodeSequence n, String argu) throws Exception {
        if (n.size() == 1) {
            return n.elementAt(0).accept(this,argu);
        }
        String _ret = null;
        int _count=0;
        for (Enumeration<Node> e = n.elements() ; e.hasMoreElements() ; ) {
            String ret = e.nextElement().accept(this,argu);
            if (ret != null) {
                if (_ret == null) {
                    _ret = ret;
                } else {
                    _ret += " " + ret;
                }
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
     * f0 -> ( Stmt() )*
     */
    public String visit(TinyRAMProg n, String argu) throws Exception {
        if (n.f0.present()) {
            for (int i = 0 ; i < n.f0.size() ; i++) {
                String str = n.f0.elementAt(i).accept(this, argu);
                this.ic1++;
            }
        }
        return null;
    }

    /**
     * f0 -> Label()
     *       | TwoRegInstr()
     *       | ThreeRegInstr()
     *       | JmpStmts()
     *       | StoreWStmt()
     *       | LoadWStmt()
     *       | PrintStmt()
     *       | AnswerStmt()
     *       | ReadStmt()
     *       | SeekStmt()
     */
    public String visit(Stmt n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }   

    // /**
    // * f0 -> "CJMP"
    // * f1 -> Register()
    // * f2 -> Label()
    // */
    // public String visit(CJumpStmt n, String argu) throws Exception {
    //     String tmp = n.f1.accept(this, argu);
    //     String []parts = tmp.split("&");
    //     tmp = parts[0];
    //     String label = n.f2.accept(this, argu);
    //     String op = "CJMP " + tmp + " " + label + "\n";
    //     String opt_found = optimisationMap.get("deadCode").get(argu + ic1);
    //     if (opt_found == null)
    //         this.result += op;
    //     return op;
    // }

    /**
     * f0 -> JmpOps()
     * f1 -> Register()
     * f2 -> Register()
     * f3 -> Label()
     */
     public String visit(JmpStmts n, String argu) throws Exception {
         String op = n.f0.accept(this, argu);
         String reg1 = n.f1.accept(this, argu);
         String reg2 = n.f2.accept(this, argu);
         String label = n.f3.accept(this, argu);
         String instr = op + " " + reg1 + " " + reg2 + " " + label;
         
         String opt_found = optimisationMap.get("deadCode").get(argu + ic1);
         if (opt_found == null) {
             this.result += instr;
         }
         return instr;
     }

    /**
    * f0 -> "STOREW"
    * f1 -> Register()
    * f2 -> IntegerLiteral()
    * f3 -> Register()
    */
    public String visit(StoreWStmt n, String argu) throws Exception {
        String src = n.f1.accept(this, argu).split("&")[0];
        String reg2 = n.f2.accept(this, argu);
        String addr = n.f3.accept(this, argu).split("&")[0];
        String intsr = "STOREW " + src + " " + reg2 + " " + addr + "\n";
        String opt_found = optimisationMap.get("deadCode").get(argu + ic1);
        if (opt_found == null) {
            this.result += intsr;
        }
        return intsr;
    }

    /**
    * f0 -> "LOADW"
    * f1 -> Register()
    * f2 -> Register()
    * f3 -> IntegerLiteral()
    */
    public String visit(LoadWStmt n, String argu) throws Exception {
        String dst = n.f1.accept(this, argu).split("&")[0];
        String reg = n.f2.accept(this, argu).split("&")[0];
        String addr = n.f3.accept(this, argu);
        String instr = "LOADW " + dst + " " + reg + " " + addr + "\n";
        String opt_found = optimisationMap.get("deadCode").get(argu + ic1);
        if (opt_found == null) {
            this.result += instr;
        }
        return instr;
    }

    /**
     * f0 -> TwoRegInstrOp()
     * f1 -> Register()
     * f2 -> Register()
     * f3 -> SimpleExp()
     */
    public String visit(TwoRegInstr n, String argu) throws Exception {
        String op = n.f0.accept(this, argu);
        String dst = n.f1.accept(this, argu).split("&")[0];
        String reg2 = n.f2.accept(this, argu);
        String src = n.f3.accept(this, argu);
        if (src == null) { return null; }
        String instr = null;
        if (src.matches("r(.*)")) {
            String []parts = new String[2];
            parts = src.split("&");
            if (parts.length == 2) {
                src = parts[1];
            } else {
                src = parts[0];
            }
            instr = op + " " + dst + " " + reg2 + " " + src + "\n";
        } else if (src.matches("[0-9]+")) {
            instr = op + " " + dst + " " + reg2 + " " + Integer.parseInt(src) + "\n";
        }
        String opt_found = optimisationMap.get("deadCode").get(argu + ic1);
        if (opt_found == null){
            this.result += instr;
        }
        return instr;
    }
    
    // /**
    // * f0 -> Operator()
    // * f1 -> Register()
    // * f2 -> SimpleExp()
    // */
    // public String visit(BinOp n, String argu) throws Exception {
    //     String op = n.f0.accept(this, argu);
    //     String dst = n.f1.accept(this, argu);
    //     String exp = n.f2.accept(this, argu);
    
    //     String []prts = new String[2];
    //     prts = dst.split("&");
    //     dst = prts[0];
    //     String []parts = new String[2];
    //     parts = exp.split("&");
    //     if (parts.length == 2) exp = parts[1];
    //     else exp = parts[0];
    //     String ret = op + " " + dst + " " + exp;
    //     return ret;
    // }
    
    /**
     * f0 -> ThreeRegInstrOp()
     * f1 -> Register()
     * f2 -> Register()
     * f3 -> SimpleExp()
     */
    public String visit(ThreeRegInstr n, String argu) throws Exception {
        String op = n.f0.accept(this, argu);
        String dst = n.f1.accept(this, argu).split("&")[0];
        String reg2 = n.f2.accept(this, argu).split("&")[0];
        String reg3 = n.f3.accept(this, argu);
        
        System.out.println("\nparts:"+ reg3);
        
        if (reg3 == null) { return null; }
        String instr = null;
        if (reg3.matches("r(.*)")) {
            String []parts = new String[2];
            parts = reg3.split("&");
            if (parts.length == 2) {
                reg3 = parts[1];
            } else {
                reg3 = parts[0];
            }
            instr = op + " " + dst + " " + reg2 + " " + reg3 + "\n";
        } else if (reg3.matches("[0-9]+")) {
            instr = op + " " + dst + " " + reg2 + " " + Integer.parseInt(reg3) + "\n";
        }
        String opt_found = optimisationMap.get("deadCode").get(argu + ic1);
        if (opt_found == null){
            this.result += instr;
        }
        return instr;
    }
    

// TODO
    /**
     * f0 -> "PRINT"
     * f1 -> Register()
     * f2 -> Register()
     * f3 -> Register()
     */
    public String visit(PrintStmt n, String argu) throws Exception {
        String reg = n.f3.accept(this, argu);
        String []parts = new String[2];
        parts = reg.split("&");
        if (parts.length == 2) {
            reg = parts[1];
        } else {
            reg = parts[0];
        }
        String instr = "PRINT " + reg + " " + reg + " " + reg + "\n";
        String opt_found = optimisationMap.get("deadCode").get(argu + ic1);
        if (opt_found == null) {
            this.result += instr;
        }
        return instr;
    }
    
    /**
     * f0 -> "ANSWER"
     * f1 -> Register()
     * f2 -> Register()
     * f3 -> Register()
     */
    public String visit(AnswerStmt n, String argu) throws Exception {
        String reg = n.f3.accept(this, argu);
        String []parts = new String[2];
        parts = reg.split("&");
        if (parts.length == 2) {
            reg = parts[1];
        } else {
            reg = parts[0];
        }
        String instr = "ANSWER " + reg + " " + reg + " " + reg + "\n";
        String opt_found = optimisationMap.get("deadCode").get(argu + ic1);
        if (opt_found == null) {
            this.result += instr;
        }
        return instr;
    }

    /**
     * f0 -> "MOV"
     *       | "NOT"
     */
    public String visit(TwoRegInstrOp n, String argu) throws Exception {
        return n.f0.choice.toString();
    }

    /**
     * f0 -> "AND"
     *       | "OR"
     *       | "XOR"
     *       | "ADD"
     *       | "SUB"
     *       | "MULL"
     *       | "SHL"
     *       | "SHR"
     */
    public String visit(ThreeRegInstrOp n, String argu) throws Exception {
        return n.f0.choice.toString();
    }
    
    /**
     * f0 -> "JMP"
     *       | "CJMP"
     *       | "CNJMP"
     */
    public String visit(JmpOps n, String argu) throws Exception {
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
    // public String visit(Register n, String argu) throws Exception {
    //     String reg = n.f0.toString();
    //     String copy_opt_found = optimisationMap.get("copyProp").get(argu + ic1);
    //     String const_opt_found = optimisationMap.get("constProp").get(argu + ic1);
    //     if (copy_opt_found != null && getTemp(copy_opt_found).equals(reg)) {
    //         return getOpt(copy_opt_found, false);
    //     }
    //     if (const_opt_found != null && getTemp(const_opt_found).equals(reg)) {
    //         if (copy_opt_found != null && getTemp(copy_opt_found).equals(getTemp(const_opt_found))) {
    //             return getOpt(copy_opt_found, false);
    //         }
    //         return reg + "&" + getOpt(const_opt_found, true);
    //     }
    //     return reg;
    // }
    
    public String visit(Register n, String argu) throws Exception {
        String reg = n.f0.toString();
        String str1 = optimisationMap.get("copyProp").get(argu + ic1);
        String str2 = optimisationMap.get("constProp").get(argu + ic1);
        String str2_2 = optimisationMap.get("constProp").get(argu + "-sec-" +  ic1);
        if (str2_2 != null) { // if two constant propagations in the same line
            str2 = str2_2;
            System.out.println("PROP : " + str2);
        }
        if (str1 != null) { // copy
            System.out.println("str1:" + str1);
            if (getTemp(str1).equals(reg))
                return getOpt(str1, false);
        }
        if (str2 != null) { // constant
            
            System.out.println("str2:" + str2);
            if (getTemp(str2).equals(reg)) {
                return reg + "&" + getOpt(str2, true);
            } 
            if (str1 != null && getTemp(str1).equals(getTemp(str2))) {
                if (getTemp(str1).equals(reg))
                    return getOpt(str1, false);
            }
        }
        return reg;
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
