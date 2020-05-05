package org.twc.zerojavacompiler.optimizer;

import org.twc.zerojavacompiler.zmipssyntaxtree.*;
import org.twc.zerojavacompiler.zmipsvisitor.GJDepthFirst;

import java.util.Enumeration;
import java.util.Map;

public class OptimizerVisitor extends GJDepthFirst<String, String> {

    public String asm_;
    private int instr_cnt;
    private Map<String, Map<String, String>> optimisationMap;
    private boolean label_from_stmt;
    private boolean is_dst;

    public OptimizerVisitor(Map<String, Map<String, String>> optimisationMap) {
        asm_ = "";
        this.instr_cnt = 1;
        this.optimisationMap = optimisationMap;
        this.label_from_stmt = true;
        this.is_dst = false;
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
        StringBuilder _ret = null;
        for (Enumeration<Node> e = n.elements() ; e.hasMoreElements() ; ) {
            String ret = e.nextElement().accept(this,argu);
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
                n.f0.elementAt(i).accept(this, argu);
                this.instr_cnt++;
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
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> JmpOps()
     * f1 -> Label()
     */
     public String visit(JmpStmts n, String argu) throws Exception {
         String op = n.f0.accept(this, argu);
         this.label_from_stmt = false;
         String label = n.f1.accept(this, argu);
         this.label_from_stmt = true;
         String instr = op + " " + label + "\n";
         String opt_found = optimisationMap.get("deadCode").get(argu + instr_cnt);
         if (opt_found == null) {
             this.asm_ += instr;
         }
         return instr;
     }

     /**
      * f0 ->    "j"
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
      * f3 -> Register()
      * f4 -> ","
      * f5 -> SimpleExp()
      */
      public String visit(ComparisonStmts n, String argu) throws Exception {
          String op = n.f0.accept(this, argu);
          String reg1 = n.f1.accept(this, argu);
          String reg2 = n.f3.accept(this, argu);
          this.label_from_stmt = false;
          String label = n.f5.accept(this, argu);
          this.label_from_stmt = true;
          if (reg2 == null) { return null; }
          if (reg2.startsWith("$")) {
              reg2 = reg2.split("&")[0];
          }
          if (reg1.startsWith("$")) {
              reg1 = reg1.split("&")[0];
          }
          String instr = op + " " + reg1 + ", " + reg2 + ", " + label + "\n";
          String opt_found = optimisationMap.get("deadCode").get(argu + instr_cnt);
          if (opt_found == null){
              this.asm_ += instr;
          }
          return instr;
      }

      /**
       * f0 -> "beq"
       *       | "bne"
       *       | "blt"
       *       | "ble"
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
        String dst = n.f1.accept(this, argu).split("&")[0];
        String idx = n.f3.accept(this, argu);
        this.label_from_stmt = false;
        String addr = n.f5.accept(this, argu).split("&")[0];
        this.label_from_stmt = true;
        if (idx.startsWith("$")) {
            String []parts;
            parts = idx.split("&");
            if (parts.length == 2) {
                idx = parts[1];
            } else {
                idx = parts[0];
            }
        }
        String instr = "lw " + dst + ", " + idx + "(" + addr + ")\n";
        String opt_found = optimisationMap.get("deadCode").get(argu + instr_cnt);
        if (opt_found == null) {
            this.asm_ += instr;
        }
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
    public String visit(SwStmt n, String argu) throws Exception {
        String src = n.f1.accept(this, argu).split("&")[0];
        String idx = n.f3.accept(this, argu);
        this.label_from_stmt = false;
        String addr = n.f5.accept(this, argu).split("&")[0];
        this.label_from_stmt = true;
        if (idx.startsWith("$")) {
            String []parts;
            parts = idx.split("&");
            if (parts.length == 2) {
                idx = parts[1];
            } else {
                idx = parts[0];
            }
        }
        String intsr = "sw " + src + ", " + idx + "(" + addr + ")\n";
        String opt_found = optimisationMap.get("deadCode").get(argu + instr_cnt);
        if (opt_found == null) {
            this.asm_ += intsr;
        }
        return intsr;
    }

    /**
     * f0 -> TwoRegInstrOp()
     * f1 -> Register()
     * f2 -> ","
     * f3 -> SimpleExp()
     */
    public String visit(TwoRegInstr n, String argu) throws Exception {
        String op = n.f0.accept(this, argu);
        String dst = n.f1.accept(this, argu).split("&")[0];
        this.label_from_stmt = false;
        String src = n.f3.accept(this, argu);
        this.label_from_stmt = true;
        if (src == null) { return null; }
        String instr;
        if (src.startsWith("$")) {
            String []parts;
            parts = src.split("&");
            if (parts.length == 2) {
                src = parts[1];
            } else {
                src = parts[0];
            }
        }
        instr = op + " " + dst + ", " + src + "\n";
        String opt_found = optimisationMap.get("deadCode").get(argu + instr_cnt);
        if (opt_found == null){
            this.asm_ += instr;
        }
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
        this.is_dst = true;
        String dst = n.f1.accept(this, argu).split("&")[0];
        this.is_dst = false;
        String reg2 = n.f3.accept(this, argu).split("&")[0];
        this.label_from_stmt = false;
        String reg3 = n.f5.accept(this, argu);
        this.label_from_stmt = true;
        if (reg3 == null) { return null; }
        String instr;
        if (reg3.startsWith("$")) {
            String []parts;
            parts = reg3.split("&");
            if (parts.length == 2) {
                reg3 = parts[1];
            } else {
                reg3 = parts[0];
            }
        }
        instr = op + " " + dst + ", " + reg2 + ", " + reg3 + "\n";
        String opt_found = optimisationMap.get("deadCode").get(argu + instr_cnt);
        if (opt_found == null){
            this.asm_ += instr;
        }
        return instr;
    }

    /**
     * f0 -> "and"
     *       | "or"
     *       | "xor"
     *       | "add"
     *       | "sub"
     *       | "mult"
     *       | "div"
     *       | "mod"
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
        String reg = n.f1.accept(this, argu);
        String []parts;
        parts = reg.split("&");
        if (parts.length == 2) {
            reg = parts[1];
        } else {
            reg = parts[0];
        }
        String instr = "print " + reg + "\n";
        // String opt_found = optimisationMap.get("deadCode").get(argu + instr_cnt);
        // if (opt_found == null) {
        this.asm_ += instr;
        // }
        return instr;
    }

    /**
     * f0 -> "println"
     * f1 -> SimpleExp()
     */
    public String visit(PrintLineStmt n, String argu) throws Exception {
        String reg = n.f1.accept(this, argu);
        String []parts;
        parts = reg.split("&");
        if (parts.length == 2) {
            reg = parts[1];
        } else {
            reg = parts[0];
        }
        String instr = "println " + reg + "\n";
        this.asm_ += instr;
        return instr;
    }

    /**
     * f0 -> "answer"
     * f1 -> SimpleExp()
     */
    public String visit(AnswerStmt n, String argu) throws Exception {
        String reg = n.f1.accept(this, argu);
        String []parts;
        parts = reg.split("&");
        if (parts.length == 2) {
            reg = parts[1];
        } else {
            reg = parts[0];
        }
        String instr = "answer " + reg + "\n";
        String opt_found = optimisationMap.get("deadCode").get(argu + instr_cnt);
        if (opt_found == null) {
            this.asm_ += instr;
        }
        return instr;
    }

    /**
     * f0 -> "pubread"
     * f1 -> Register()
     */
    public String visit(PubReadStmt n, String argu) throws Exception {
        String dst = n.f1.accept(this, argu).split("&")[0];
        String instr = "pubread " + dst + "\n";
        String opt_found = optimisationMap.get("deadCode").get(argu + instr_cnt);
        if (opt_found == null){
            this.asm_ += instr;
        }
        return instr;
    }

    /**
     * f0 -> "secread"
     * f1 -> Register()
     */
    public String visit(SecReadStmt n, String argu) throws Exception {
        String dst = n.f1.accept(this, argu).split("&")[0];
        String instr = "secread " + dst + "\n";
        String opt_found = optimisationMap.get("deadCode").get(argu + instr_cnt);
        if (opt_found == null){
            this.asm_ += instr;
        }
        return instr;
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
        String reg = n.f0.toString();
        // If it is a destination register return
        if (this.is_dst) { return reg; }

        // Check for copy propagation optimizations for reg2
        String copy_opt_1 = optimisationMap.get("copyProp").get(argu + instr_cnt);
        if (copy_opt_1 != null) {
            if (getTemp(copy_opt_1).equals(reg)) {
                return getOpt(copy_opt_1, false);
            }
        }
        // Check for copy propagation optimizations for reg3
        String copy_opt_2 = optimisationMap.get("copyProp").get(argu + "-sec-" + instr_cnt);
        if (copy_opt_2 != null) {
            if (getTemp(copy_opt_2).equals(reg)) {
                return getOpt(copy_opt_2, false);
            }
        }

        // Check for constant propagation optimizations for reg2
        String const_opt_1 = optimisationMap.get("constProp").get(argu + instr_cnt);
        if (const_opt_1 != null) {
            if (getTemp(const_opt_1).equals(reg)) {
                return reg + "&" + getOpt(const_opt_1, true);
            }
        }
        // Check for constant propagation optimizations for reg3
        String const_opt_2 = optimisationMap.get("constProp").get(argu + "-sec-" + instr_cnt);
        if (const_opt_2 != null) {
            if (getTemp(const_opt_2).equals(reg)) {
                return reg + "&" + getOpt(const_opt_2, true);
            }
        }

        // If no optimizations found, return register
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
        String ret = n.f0.toString();
        if (this.label_from_stmt) {
            this.asm_ += ret + "\n";
        }
        return ret;
    }

}
