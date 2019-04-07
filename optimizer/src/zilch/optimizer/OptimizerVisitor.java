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

    static String getMeth(String fact) {
        String []parts = fact.split(",");
        return parts[0].substring(2,  parts[0].length()-1);
    }

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
        if (num)
            parts[3] = parts[3].substring(1, parts[3].length()-1);
        else
            parts[3] = parts[3].substring(2, parts[3].length()-2);
        return parts[3];
    }

    public String visit(NodeSequence n, String argu) throws Exception {
        if (n.size() == 1)
            return n.elementAt(0).accept(this,argu);
        String _ret = null;
        int _count=0;
        for (Enumeration<Node> e = n.elements() ; e.hasMoreElements() ; ) {
            String ret = e.nextElement().accept(this,argu);
            if (ret != null) {
                if (_ret == null)
                    _ret = ret;
                else
                    _ret += " " + ret;
            }
            _count++;
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
        this.result += "MAIN\n";
        n.f1.accept(this, "MAIN");
        this.result += "END\n";
        n.f3.accept(this, argu);
        return null;
    }

    /**
    * f0 -> ( ( Label() )? Stmt() )*
    */
    public String visit(StmtList n, String argu) throws Exception {
        if (n.f0.present()) {
            for (int i = 0 ; i < n.f0.size() ; i++) {
                String str = n.f0.elementAt(i).accept(this, argu);
                if (str.matches("L(.*)")) {
                    this.result += str + "\n";
                }
                if (str.matches("(.*)ERROR(.*)")) {
                    continue;
                }
                this.ic1++;
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
        this.ic1 = 1;
        String id = n.f0.accept(this, argu);;
        String args = n.f2.accept(this, argu);
        this.result += id + "[" + args + "]\n";
        n.f4.accept(this, id);
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
        String stmt = n.f0.accept(this, argu);
        return stmt;
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
    * f0 -> "CJMP"
    * f1 -> Register()
    * f2 -> Label()
    */
    public String visit(CJumpStmt n, String argu) throws Exception {
        String tmp = n.f1.accept(this, argu);
        String []parts = tmp.split("&");
        tmp = parts[0];
        String label = n.f2.accept(this, argu);
        String op = "CJMP " + tmp + " " + label + "\n";
        String str1 = optimisationMap.get("deadCode").get(argu + ic1);
        if (str1 == null)
            this.result += op;
        else if (!getMeth(str1).equals(argu))
            this.result += op;
        return op;
    }

    /**
    * f0 -> "JMP"
    * f1 -> Label()
    */
    public String visit(JumpStmt n, String argu) throws Exception {
        String label = n.f1.accept(this, argu);
        String op = "JMP " + label + "\n";
        String str1 = optimisationMap.get("deadCode").get(argu + ic1);
        if (str1 == null)
            this.result += op;
        else if (!getMeth(str1).equals(argu))
            this.result += op;
        return op;
    }

    /**
    * f0 -> "STOREW"
    * f1 -> Register()
    * f2 -> IntegerLiteral()
    * f3 -> Register()
    */
    public String visit(StoreWStmt n, String argu) throws Exception {
        String tmp1 = n.f1.accept(this, argu);
        String []parts = tmp1.split("&");
        tmp1 = parts[0];
        String lit = n.f2.accept(this, argu);
        String tmp2 = n.f3.accept(this, argu);
        parts = tmp2.split("&");
        tmp2 = parts[0];
        String op = "STOREW " + tmp1 + " " + lit + " " + tmp2 + "\n";
        String str1 = optimisationMap.get("deadCode").get(argu + ic1);
        if (str1 == null)
            this.result += op;
        else if (!getMeth(str1).equals(argu))
            this.result += op;
        return op;
    }

    /**
    * f0 -> "LOADW"
    * f1 -> Register()
    * f2 -> Register()
    * f3 -> IntegerLiteral()
    */
    public String visit(LoadWStmt n, String argu) throws Exception {
        String tmp1 = n.f1.accept(this, argu);
        String []parts = tmp1.split("&");
        tmp1 = parts[0];
        String tmp2 = n.f2.accept(this, argu);
        parts = tmp2.split("&");
        tmp2 = parts[0];
        String lit = n.f3.accept(this, argu);
        String op = "LOADW " + tmp1 + " " + tmp2 + " " + lit + "\n";
        String str1 = optimisationMap.get("deadCode").get(argu + ic1);
        if (str1 == null)
            this.result += op;
        else if (!getMeth(str1).equals(argu))
            this.result += op;
        return op;
    }

    /**
    * f0 -> "MOV"
    * f1 -> Register()
    * f2 -> Exp()
    */
    public String visit(MovStmt n, String argu) throws Exception {
        n.f0.accept(this, argu);
        String tmp = n.f1.accept(this, argu);
        String []parts = tmp.split("&");
        tmp = parts[0];
        String exp = n.f2.accept(this, argu);
        String op = null;
        if (exp != null) {
            if (exp.matches("r(.*)")) {
                String []parts2 = new String[2];
                parts2 = exp.split("&");
                if (parts2.length == 2)
                    exp = parts2[1];
                else
                    exp = parts2[0];
                op = "MOV " + tmp + " " + exp + "\n";
            } else if (exp.matches("[0-9]+")) {
                op = "MOV " + tmp + " " + Integer.parseInt(exp) + "\n";
            } else {
                op = "MOV " + tmp + " " + exp + "\n";
            }
            String str1 = optimisationMap.get("deadCode").get(argu + ic1);
            if (str1 == null){
                this.result += op;
            } else if (!getMeth(str1).equals(argu))
                this.result += op;
        }
        return op;
    }

    /**
    * f0 -> "PRINT"
    * f1 -> SimpleExp()
    */
    public String visit(PrintStmt n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu);
        String []parts = new String[2];
        parts = exp.split("&");
        if (parts.length == 2)
            exp = parts[1];
        else
            exp = parts[0];
        String op = "PRINT " + exp + "\n";
        String str1 = optimisationMap.get("deadCode").get(argu + ic1);
        if (str1 == null)
            this.result += op;
        else if (!getMeth(str1).equals(argu))
            this.result += op;
        return op;
    }

    /**
    * f0 -> Call()
    *       | HAllocate()
    *       | BinOp()
    *       | SimpleExp()
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
        this.result += "BEGIN\n";
        n.f1.accept(this, argu);
        String exp = n.f3.accept(this, argu);
        String []parts = new String[2];
        parts = exp.split("&");
        if (parts.length == 2)
            exp = parts[1];
        else
            exp = parts[0];
        String op = "RETURN " + exp + "\nEND\n";
        this.result += op;
        return null;
    }

    /**
    * f0 -> "CALL"
    * f1 -> SimpleExp()
    * f2 -> "("
    * f3 -> ( Register() )*
    * f4 -> ")"
    */
    public String visit(Call n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu);
        String tmp = "(";
        if (n.f3.present())
            for (int i = 0 ; i < n.f3.size() ; i++) {
                String t = n.f3.nodes.get(i).accept(this, argu);
                String []parts = t.split("&");
                t = parts[0];
                tmp += t;
                if (i < n.f3.size()-1)
                    tmp += " ";
            }
        tmp += ")";
        String op = "CALL " + exp + " " + tmp;
        return op;
    }

    /**
    * f0 -> "HALLOCATE"
    * f1 -> SimpleExp()
    */
    public String visit(HAllocate n, String argu) throws Exception {
        String exp = n.f1.accept(this, argu);
        String []parts = new String[2];
        parts = exp.split("&");
        if (parts.length == 2)
            exp = parts[1];
        else
            exp = parts[0];
        String op = "HALLOCATE " + exp;
        return op;
    }

    /**
    * f0 -> Operator()
    * f1 -> Register()
    * f2 -> SimpleExp()
    */
    public String visit(BinOp n, String argu) throws Exception {
        String op = n.f0.accept(this, argu);
        String tmp = n.f1.accept(this, argu);
        String exp = n.f2.accept(this, argu);
        String []prts = new String[2];
        prts = tmp.split("&");
        tmp = prts[0];
        String []parts = new String[2];
        parts = exp.split("&");
        if (parts.length == 2)
            exp = parts[1];
        else
            exp = parts[0];
        String ret = op + " " + tmp + " " + exp;
        return ret;
    }

    /**
    * f0 -> "LT"
    *       | "ADD"
    *       | "SUB"
    *       | "MULL"
    */
    public String visit(Operator n, String argu) throws Exception {
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
    * f0 -> "r"
    * f1 -> IntegerLiteral()
    */
    public String visit(Temp n, String argu) throws Exception {
        String t = n.f1.accept(this, argu);
        String ret = "r" + t;
        String str1 = optimisationMap.get("copyProp").get(argu + ic1);
        String str2 = optimisationMap.get("constProp").get(argu + ic1);
        if (str1 != null) { // copy
            if (getMeth(str1).equals(argu) && getTemp(str1).equals(ret))
                return getOpt(str1, false);
        }
        // if (ret.matches("r493(.*)")) {
        //     System.out.println("dsajhdjashkn\n" + str2 + ic1);
        //     if (str2 == null)
        //         System.out.println("aspote: " + optimisationMap.get("constProp").get(255) + optimisationMap.get("constProp").get(256) + optimisationMap.get("constProp").get(254));
        // }
        if (str2 != null) { // constant
            if (getMeth(str2).equals(argu)) { 
                if (str1 != null && getTemp(str1).equals(getTemp(str2))) {
                    if (getTemp(str1).equals(ret) && getMeth(str1).equals(argu))
                        return getOpt(str1, false);
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
