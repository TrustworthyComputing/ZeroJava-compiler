package zmips_generator;

import syntaxtree.*;
import visitor.GJDepthFirst;
import symbol_table.*;
import base_type.*;
import java.util.Map;
import java.util.HashMap;

public class ZMIPSGenVisitor extends GJDepthFirst<BaseType, BaseType> {
	public String result_;
	private Label L;
	private Map<String, Method_t> st_;
	
	private int glob_temp_cnt_;
	private int base_addr_;
	private boolean immediate_;
	
	private boolean is_inline_meth_;
	private String inline_meth_;

	public ZMIPSGenVisitor(Map<String, Method_t> st) {
		this.L = new Label();
        this.st_ = st;
		this.glob_temp_cnt_ = 0;
		this.base_addr_ = 1000;
		this.immediate_ = false;
		this.result_ = new String();
		this.is_inline_meth_ = false;
		this.inline_meth_ = new String();
	}


	/**
    * f0 -> ( MethodDeclaration() )*
	* f1 -> MainMethodDeclaration()
    * f2 -> <EOF>
	*/
	public BaseType visit(Goal n, BaseType argu) throws Exception {
		n.f0.accept(this, argu);
		n.f1.accept(this, argu);
		n.f2.accept(this, argu);
		this.result_ += "\n";
		return null;
	}

	/**
	 * f0 -> "void"
	 * f1 -> "main"
	 * f2 -> "("
	 * f3 -> "void"
	 * f4 -> ")"
	 * f5 -> "{"
	 * f6 -> ( VarDeclaration() )*
	 * f7 -> ( Statement() )*
	 * f8 -> "}"
	 */
	public BaseType visit(MainMethodDeclaration n, BaseType argu) throws Exception {
		String methName = new String("main");
		Method_t meth = this.st_.get(methName);
		n.f6.accept(this, meth);
		n.f7.accept(this, meth);
		return null;
	}
	
	/**
	 * f0 -> Type()
	 * f1 -> Identifier()
	 * f2 -> "("
	 * f3 -> ( FormalParameterList() )?
	 * f4 -> ")"
	 * f5 -> "{"
	 * f6 -> ( VarDeclaration() )*
	 * f7 -> ( Statement() )*
	 * f8 -> "return"
	 * f9 -> Expression()
	 * f10 -> ";"
	 * f11 -> "}"
	 */
	public BaseType visit(MethodDeclaration n, BaseType argu) throws Exception {
		this.is_inline_meth_ = true;
		String methName = n.f1.accept(this, argu).getName();
		Method_t meth = this.st_.get(methName);
		n.f6.accept(this, meth);
		n.f7.accept(this, meth);
		Variable_t retType = (Variable_t) n.f9.accept(this, meth);
		meth.return_reg = retType.getType();
		meth.body_ = this.inline_meth_;
		this.inline_meth_ = new String();
		this.is_inline_meth_ = false;
		return retType;
	}

	/**
	* f0 -> Type()
	* f1 -> Identifier()
	* f2 -> ";"
	*/
	public BaseType visit(VarDeclaration n, BaseType argu) throws Exception {
		Method_t meth = (Method_t) argu;
		String varName = n.f1.f0.toString();
		String newTemp = new String("$r" + ++glob_temp_cnt_);
		meth = st_.get(meth.getName());
		if (meth != null) {		// if you found method
			meth.addTempToVar(varName, newTemp);
		} else {
			throw new Exception("VarDeclaration Errror 1");
		}
		return null;
	}
	
	/**
	* f0 -> FormalParameter()
	* f1 -> FormalParameterTail()
	*/
	public BaseType visit(FormalParameterList n, BaseType argu) throws Exception {
		n.f0.accept(this, argu);
		n.f1.accept(this, argu);
		return null;
	}

	/**
	* f0 -> Type()
	* f1 -> Identifier()
	*/
	public BaseType visit(FormalParameter n, BaseType argu) throws Exception {
		n.f0.accept(this, argu);
		n.f1.accept(this, argu);
		return null;
	}

	/**
	* f0 -> ( FormalParameterTerm() )*
	*/
	public BaseType visit(FormalParameterTail n, BaseType argu) throws Exception {
		return n.f0.accept(this, argu);
	}

	/**
	* f0 -> ","
	* f1 -> FormalParameter()
	*/
	public BaseType visit(FormalParameterTerm n, BaseType argu) throws Exception {
		n.f0.accept(this, argu);
		n.f1.accept(this, argu);
		return null;
	}

	/**
	* f0 -> ArrayType()
	*       | IntegerType()
	*       | Identifier()
	*/
	public BaseType visit(Type n, BaseType argu) throws Exception {
		return n.f0.accept(this, argu);
	}

	/**
	* f0 -> "int"
	*/
	public BaseType visit(IntegerType n, BaseType argu) throws Exception {
		return n.f0.accept(this, argu);
	}

	/**
     * f0 -> Block()
     *       | AssignmentStatement()
     *       | OpAssignmentStatement()
     *       | ArrayAssignmentStatement()
     *       | PlusPlusExpression()
     *       | MinusMinusExpression()
     *       | IfElseStatement()
     *       | IfStatement()
     *       | WhileStatement()
     *       | PrintStatement()
     *       | ReadPrimaryTape()
     *       | ReadPrivateTape()
     *       | SeekPrimaryTape()
     *       | SeekPrivateTape()
     *       | AnswerStatement()
     */
	public BaseType visit(Statement n, BaseType argu) throws Exception {
		return n.f0.accept(this, argu);
	}

	/**
	* f0 -> "{"
	* f1 -> ( Statement() )*
	* f2 -> "}"
	*/
	public BaseType visit(Block n, BaseType argu) throws Exception {
		n.f1.accept(this, argu);
		return null;
	}

	/**
	* f0 -> Identifier()
	* f1 -> "="
	* f2 -> Expression()
	* f3 -> ";"
	*/
	public BaseType visit(AssignmentStatement n, BaseType argu) throws Exception {
		String id = n.f0.accept(this, argu).getName();
		this.immediate_ = true;
		String expr = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.immediate_ = false;
		Variable_t var;
		// if a local var
		Method_t meth = (Method_t) argu;
		if (meth != null) { 
			var = meth.methContainsVar(id);
			if (this.is_inline_meth_) {
				this.inline_meth_ += "move " +  var.getRegister() + ", " + var.getRegister() + ", " + expr +"\n";
			} else {
				this.result_ += "move " +  var.getRegister() + ", " + var.getRegister() + ", " + expr +"\n";
			}
		}
		return null;
	}
	
	/**
	* f0 -> Identifier()
	* f1 -> OpAssignmentOperator()
	* f2 -> Expression()
	* f3 -> ";"
	*/
	public BaseType visit(OpAssignmentStatement n, BaseType argu) throws Exception {
		String id = n.f0.accept(this, argu).getName();
		String op = n.f1.accept(this, argu).getName();
		this.immediate_ = true;
		String expr = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.immediate_ = false;
		Variable_t var;
		Method_t meth = (Method_t) argu;
		if (meth != null) { 
			var = meth.methContainsVar(id);
			if (this.is_inline_meth_) {
				this.inline_meth_ += op + " " +  var.getRegister() + ", " + var.getRegister() + ", " + expr +"\n";
			} else {
				this.result_ += op + " " +  var.getRegister() + ", " + var.getRegister() + ", " + expr +"\n";
			}
		}
		return null;
	}
	
	/**
     * f0 -> "+="
     *       | "-="
     *       | "*="
     *       | "/="
     *       | "%="
     *       | "<<="
     *       | ">>="
     *       | "&="
     *       | "|="
     *       | "^="
     */
	public BaseType visit(OpAssignmentOperator n, BaseType argu) throws Exception {
		String op = n.f0.choice.toString();
		if (op.equals("+=")) {
			return new Variable_t("add", "add");
		} else if (op.equals("-=")) {
			return new Variable_t("sub", "sub");
		} else if (op.equals("*=")) {
			return new Variable_t("mult", "mult");
		} else if (op.equals("/=")) {
			return new Variable_t("div", "div");
		} else if (op.equals("%=")) {
			return new Variable_t("mod", "mod");
		} else if (op.equals("<<=")) {
			return new Variable_t("sll", "sll");
		} else if (op.equals(">>=")) {
			return new Variable_t("srl", "srl");
		} else if (op.equals("&=")) {
			return new Variable_t("and", "and");
		} else if (op.equals("|=")) {
			return new Variable_t("or", "or");
		} else if (op.equals("^=")) {
			return new Variable_t("xor", "xor");
		}
		return null;
	}
	
	/**
	* f0 -> Identifier()
	* f1 -> "["
	* f2 -> Expression()
	* f3 -> "]"
	* f4 -> "="
	* f5 -> Expression()
	* f6 -> ";"
	*/
	public BaseType visit(ArrayAssignmentStatement n, BaseType argu) throws Exception {
		String array = ((Variable_t) n.f0.accept(this, argu)).getType();
		String idx = ((Variable_t) n.f2.accept(this, argu)).getType();		
		this.immediate_ = true;
		String expr = ((Variable_t) n.f5.accept(this, argu)).getType();
		this.immediate_ = false;
		String res = new String("$r" + ++glob_temp_cnt_);
		
		if (this.is_inline_meth_) {
			// this.inline_meth_ += "add " + array + ", " + array + ", " + idx + "\n";
			this.inline_meth_ += "move " + res + ", " + res + ", " + expr + "\n";
			this.inline_meth_ += "sw " + res + ", " + idx + "(" + array + ")\n";
			// this.inline_meth_ += "sub " + array + ", " + array + ", " + idx + "\n";
		} else {
			// this.result_ += "add " + array + ", " + array + ", " + idx + "\n";
			this.result_ += "move " + res + ", " + res + ", " + expr + "\n";
			this.result_ += "sw " + res + ", " + idx + "(" + array + ")\n";
			// this.result_ += "sub " + array + ", " + array + ", " + idx + "\n";
		}
		return new Variable_t(res, null);
	}
	
	/**
	* f0 -> "if"
	* f1 -> "("
	* f2 -> Expression()
	* f3 -> ")"
	* f4 -> Statement()
	* f5 -> "else"
	* f6 -> Statement()
	*/
	public BaseType visit(IfElseStatement n, BaseType argu) throws Exception {
		String elselabel = L.new_label();
		String endlabel = L.new_label();
		String cond = ((Variable_t) n.f2.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += "cnjmp " + cond + ", " + cond + ", " + elselabel + "\n"; //if cond not true go to elselabel
			n.f4.accept(this, argu);
			this.inline_meth_ += "j $r0, $r0, " + endlabel + "\n";
			this.inline_meth_ +=  elselabel + "\n";
			n.f6.accept(this, argu);
			this.inline_meth_ += endlabel + "\n";
		} else {
			this.result_ += "cnjmp " + cond + ", " + cond + ", " + elselabel + "\n"; //if cond not true go to elselabel
			n.f4.accept(this, argu);
			this.result_ += "j $r0, $r0, " + endlabel + "\n";
			this.result_ +=  elselabel + "\n";
			n.f6.accept(this, argu);
			this.result_ += endlabel + "\n";
		}
		return null;
	}
	
	/**
	* f0 -> "if"
	* f1 -> "("
	* f2 -> Expression()
	* f3 -> ")"
	* f4 -> Statement()
	*/
	public BaseType visit(IfStatement n, BaseType argu) throws Exception {
		String endlabel = L.new_label();
		String cond = ((Variable_t) n.f2.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += "cnjmp " + cond + ", " + cond + ", " + endlabel + "\n";
			n.f4.accept(this, argu);
			this.inline_meth_ += endlabel + "\n";
		} else {
			this.result_ += "cnjmp " + cond + ", " + cond + ", " + endlabel + "\n";
			n.f4.accept(this, argu);
			this.result_ += endlabel + "\n";
		}
		return null;
	}

	/**
	* f0 -> "while"
	* f1 -> "("
	* f2 -> Expression()
	* f3 -> ")"
	* f4 -> Statement()
	*/
	public BaseType visit(WhileStatement n, BaseType argu) throws Exception {
		String lstart = L.new_label();
		String lend = L.new_label();
		if (this.is_inline_meth_) {
			this.inline_meth_ += lstart + "\n";
			String expr = ((Variable_t) n.f2.accept(this, argu)).getType();
			this.inline_meth_ += "cnjmp " + expr + ", " + expr + ", " + lend + "\n";
			n.f4.accept(this, argu);
			this.inline_meth_ += "j $r0, $r0, " + lstart + "\n" + lend + "\n";
		} else {
			this.result_ += lstart + "\n";
			String expr = ((Variable_t) n.f2.accept(this, argu)).getType();
			this.result_ += "cnjmp " + expr + ", " + expr + ", " + lend + "\n";
			n.f4.accept(this, argu);
			this.result_ += "j $r0, $r0, " + lstart + "\n" + lend + "\n";
		}
		return null;
	}

	/**
	* f0 -> "Out.print"
	* f1 -> "("
	* f2 -> Expression()
	* f3 -> ")"
	* f4 -> ";"
	*/
	public BaseType visit(PrintStatement n, BaseType argu) throws Exception {
		String t = ((Variable_t) n.f2.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += "print " + t + ", " + t + ", " + t +"\n";
		} else {
			this.result_ += "print " + t + ", " + t + ", " + t +"\n";
		}
		return null;
	}
	
	/**
	* f0 -> "PrimaryTape.read"
	* f1 -> "("
	* f2 -> Expression()
	* f3 -> ")"
	* f4 -> ";"
	*/
	public BaseType visit(ReadPrimaryTape n, BaseType argu) throws Exception {
		String t = ((Variable_t) n.f2.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += "read " + t + ", " + t + ", " + 0 +"\n";
		} else {
			this.result_ += "read " + t + ", " + t + ", " + 0 +"\n";
		}
		return null;
	}
	
	/**
	* f0 -> "PrivateTape.read"
	* f1 -> "("
	* f2 -> Expression()
	* f3 -> ")"
	* f4 -> ";"
	*/
	public BaseType visit(ReadPrivateTape n, BaseType argu) throws Exception {
		String t = ((Variable_t) n.f2.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += "read " + t + ", " + t + ", " + 1 +"\n";
		} else {
			this.result_ += "read " + t + ", " + t + ", " + 1 +"\n";
		}
		return null;
	}
	
	/**
	* f0 -> "PrimaryTape.seek"
	* f1 -> "("
	* f2 -> Expression()
	* f3 -> ","
	* f4 -> Expression()
	* f5 -> ")"
	* f6 -> ";"
	*/
	public BaseType visit(SeekPrimaryTape n, BaseType argu) throws Exception {
		String dst = ((Variable_t) n.f2.accept(this, argu)).getType();
		String idx = ((Variable_t) n.f4.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += "seek " + dst + ", " + idx + ", " + 0 +"\n";
		} else {
			this.result_ += "seek " + dst + ", " + idx + ", " + 0 +"\n";
		}
		return null;
	}
	
	/**
	* f0 -> "PrivateTape.seek"
	* f1 -> "("
	* f2 -> Expression()
	* f3 -> ","
	* f4 -> Expression()
	* f5 -> ")"
	* f6 -> ";"
	*/
	public BaseType visit(SeekPrivateTape n, BaseType argu) throws Exception {
		String dst = ((Variable_t) n.f2.accept(this, argu)).getType();
		String idx = ((Variable_t) n.f4.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += "seek " + dst + ", " + idx + ", " + 1 +"\n";
		} else {
			this.result_ += "seek " + dst + ", " + idx + ", " + 1 +"\n";
		}
		return null;
	}
	
	/**
	* f0 -> "Answer"
	* f1 -> "("
	* f2 -> Expression()
	* f3 -> ")"
	* f4 -> ";"
	*/
	public BaseType visit(AnswerStatement n, BaseType argu) throws Exception {
		String t = ((Variable_t) n.f2.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += "answer " + t + ", " + t + ", " + t +"\n";
		} else {
			this.result_ += "answer " + t + ", " + t + ", " + t +"\n";
		}
		return null;
	}

	/**
     * f0 -> AndExpression()
     *       | OrExpression()
     *       | EqExpression()
     *       | NeqExpression()
     *       | LessThanExpression()
     *       | GreaterThanExpression()
     *       | LessEqualThanExpression()
     *       | GreaterEqualThanExpression()
     *       | BinaryExpression()
     *       | ArrayLookup()
     *       | MethodCall()
     *       | PrimaryExpression()
     */
	public BaseType visit(Expression n, BaseType argu) throws Exception {
		return n.f0.accept(this, argu);
	}

	/**
	 * f0 ->  "+"
	 * 		| "-"
	 * 		| "*"
	 * 		| "/"
	 * 		| "%"
	 * 		| "|"
	 * 		| "&"
	 * 		| "^"
	 * 		| "<<"
	 * 		| ">>"
	 */
	public BaseType visit(BinaryOperator n, BaseType argu) throws Exception {
		String op = n.f0.choice.toString();
		if (op.equals("+")) {
			return new Variable_t("add", "add");
		} else if (op.equals("-")) {
			return new Variable_t("sub", "sub");
		} else if (op.equals("*")) {
			return new Variable_t("mult", "mult");
		} else if (op.equals("/")) {
			return new Variable_t("div", "div");
		} else if (op.equals("%")) {
			return new Variable_t("mod", "mod");
		} else if (op.equals("|")) {
			return new Variable_t("or", "or");
		} else if (op.equals("&")) {
			return new Variable_t("and", "and");
		} else if (op.equals("^")) {
			return new Variable_t("xor", "xor");
		}  else if (op.equals("<<")) {
			return new Variable_t("sll", "sll");
		} else if (op.equals(">>")) {
			return new Variable_t("srl", "srl");
		} else {
			throw new Exception("Not supported operator");
		}
	}
	
	/**
	* f0 -> PrimaryExpression()
	* f1 -> "&&"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(AndExpression n, BaseType argu) throws Exception {
		String l1 = L.new_label();
		String ret = new String("$r" + ++glob_temp_cnt_);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += new String("cnjmp " + t1 + ", " + t1 + ", " + l1 + "\n");
			String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
			this.inline_meth_ += new String("cnjmp " + t2 + ", " + t2 + ", " + l1 + "\n");
			this.inline_meth_ += new String("move " + ret + ", " + ret + ", 1\n");
			this.inline_meth_ += new String(l1 + "\n");
		} else {
			this.result_ += new String("cnjmp " + t1 + ", " + t1 + ", " + l1 + "\n");
			String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
			this.result_ += new String("cnjmp " + t2 + ", " + t2 + ", " + l1 + "\n");
			this.result_ += new String("move " + ret + ", " + ret + ", 1\n");
			this.result_ += new String(l1 + "\n");
		}
		return new Variable_t(ret, null);
	}
	
	/**
	* f0 -> PrimaryExpression()
	* f1 -> "||"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(OrExpression n, BaseType argu) throws Exception {
		String l1 = L.new_label();
		String l2 = L.new_label();
		String ret = new String("$r" + ++glob_temp_cnt_);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += new String("cnjmp " + t1 + ", " + t1 + ", " + l1 + "\n");
			this.inline_meth_ += new String("move " + ret + ", " + ret + ", 1\n");
			this.inline_meth_ += new String("j $r0, $r0, " + l2 + "\n");
			this.inline_meth_ += new String(l1 + "\n");
			String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
			this.inline_meth_ += new String("cnjmp " + t2 + ", " + t2 + ", " + l2 + "\n");
			this.inline_meth_ += new String("move " + ret + ", " + ret + ", 1\n");
			this.inline_meth_ += new String(l2 + "\n");
		} else {
			this.result_ += new String("cnjmp " + t1 + ", " + t1 + ", " + l1 + "\n");
			this.result_ += new String("move " + ret + ", " + ret + ", 1\n");
			this.result_ += new String("j $r0, $r0, " + l2 + "\n");
			this.result_ += new String(l1 + "\n");
			String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
			this.result_ += new String("cnjmp " + t2 + ", " + t2 + ", " + l2 + "\n");
			this.result_ += new String("move " + ret + ", " + ret + ", 1\n");
			this.result_ += new String(l2 + "\n");
		}
		return new Variable_t(ret, null);
	}
	
	/**
	* f0 -> PrimaryExpression()
	* f1 -> "=="
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(EqExpression n, BaseType argu) throws Exception {
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += new String("cmpe " + t1 + ", " +  t1 + ", " + t2 + "\n");
		} else {
			this.result_ += new String("cmpe " + t1 + ", " +  t1 + ", " + t2 + "\n");
		}
		return new Variable_t(t1, null);
	}
	
	/**
	* f0 -> PrimaryExpression()
	* f1 -> "!="
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(NeqExpression n, BaseType argu) throws Exception {
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += new String("cmpne " + t1 + ", " +  t1 + ", " + t2 + "\n");
		} else {
			this.result_ += new String("cmpne " + t1 + ", " +  t1 + ", " + t2 + "\n");
		}
		return new Variable_t(t1, null);
	}
	
	/**
	* f0 -> PrimaryExpression()
	* f1 -> "<"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(LessThanExpression n, BaseType argu) throws Exception {
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += new String("cmpg " + t2 + ", " +  t2 + ", " + t1 + "\n");
		} else {
			this.result_ += new String("cmpg " + t2 + ", " +  t2 + ", " + t1 + "\n");
		}
		return new Variable_t(t2, null);
	}
	
	/**
	* f0 -> PrimaryExpression()
	* f1 -> ">"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(GreaterThanExpression n, BaseType argu) throws Exception {
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += new String("cmpg " + t1 + ", " +  t1 + ", " + t2 + "\n");
		} else {
			this.result_ += new String("cmpg " + t1 + ", " +  t1 + ", " + t2 + "\n");
		}
		return new Variable_t(t1, null);
	}
	
	/**
	* f0 -> PrimaryExpression()
	* f1 -> "<="
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(LessEqualThanExpression n, BaseType argu) throws Exception {
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += new String("cmpge " + t2 + ", " +  t2 + ", " + t1 + "\n");
		} else {
			this.result_ += new String("cmpge " + t2 + ", " +  t2 + ", " + t1 + "\n");
		}
		return new Variable_t(t2, null);
	}
	
	/**
	* f0 -> PrimaryExpression()
	* f1 -> ">="
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(GreaterEqualThanExpression n, BaseType argu) throws Exception {
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += new String("cmpge " + t1 + ", " +  t1 + ", " + t2 + "\n");
		} else {
			this.result_ += new String("cmpge " + t1 + ", " +  t1 + ", " + t2 + "\n");
		}
		return new Variable_t(t1, null);
	}

	
	/**
	* f0 -> Identifier()
	* f1 -> "++"
	*/
	public BaseType visit(PlusPlusExpression n, BaseType argu) throws Exception {
		String reg = ((Variable_t) n.f0.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += new String("add " + reg + ", " + reg + ", 1\n");
		} else {
			this.result_ += new String("add " + reg + ", " + reg + ", 1\n");
		}
		return new Variable_t(reg, null);
	}
	
	/**
	* f0 -> PrimaryExpression()
	* f1 -> "--"
	*/
	public BaseType visit(MinusMinusExpression n, BaseType argu) throws Exception {
		String reg = ((Variable_t) n.f0.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += new String("sub " + reg + ", " +  reg + ", 1\n");
		} else {
			this.result_ += new String("sub " + reg + ", " +  reg + ", 1\n");
		}
		return new Variable_t(reg, null);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> BinaryOperator()
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(BinaryExpression n, BaseType argu) throws Exception {
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		if (! t1.startsWith("$r")) {
			String imm = t1;
			t1 = new String("$r" + ++glob_temp_cnt_);
			if (this.is_inline_meth_) {
				this.inline_meth_ += new String("move " + t1 + ", " +  t1 + ", " + imm + "\n");
			} else {
				this.result_ += new String("move " + t1 + ", " +  t1 + ", " + imm + "\n");
			}
		}
		String ret = new String("$r" + ++glob_temp_cnt_);
		String op = ((Variable_t) n.f1.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += new String(op + " " + ret + ", " +  t1 + ", " + t2 + "\n");
		} else {
			this.result_ += new String(op + " " + ret + ", " +  t1 + ", " + t2 + "\n");
		}
		return new Variable_t(ret, null);
	}
	
	/**
	* f0 -> PrimaryExpression()
	* f1 -> "["
	* f2 -> PrimaryExpression()
	* f3 -> "]"
	*/
	public BaseType visit(ArrayLookup n, BaseType argu) throws Exception {
		String array = ((Variable_t) n.f0.accept(this, argu)).getType();
		String idx = ((Variable_t) n.f2.accept(this, argu)).getType();		
		String res = new String("$r" + ++glob_temp_cnt_);
		if (this.is_inline_meth_) {
			this.inline_meth_ += "lw " + res + ", " + idx + "(" + array + ")\n";
		} else {
			this.result_ += "lw " + res + ", " + idx + "(" + array + ")\n";
		}
		return new Variable_t(res, null);
	}
	
	/**
	 * f0 -> Identifier()
	 * f1 -> "("
	 * f2 -> ( ExpressionList() )?
	 * f3 -> ")"
	 */
	public BaseType visit(MethodCall n, BaseType argu) throws Exception {
		Variable_t v = (Variable_t) n.f0.accept(this, argu);
		Method_t meth = this.st_.get(v.getName());
		Method_t params = (Method_t) n.f2.accept(this, argu);
		// if meth has params
		if (n.f2.present()) {	
			for (Map.Entry<String, Variable_t> entry : params.methodParamsMap_.entrySet()) {
			    Variable_t var = entry.getValue();
				String param = "$r" + var.getVarNum();
				this.result_ += "move " + param + ", " + param + ", " + var.getType() + "\n";
			}
		}
		// Inline body of the method 
		if (this.is_inline_meth_) {
			this.inline_meth_ += meth.body_;
		} else {			
			this.result_ += meth.body_;
		}
		return new Variable_t(meth.return_reg, null);
	}

 	/**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public BaseType visit(ExpressionList n, BaseType argu) throws Exception {
        Variable_t expr =  (Variable_t) n.f0.accept(this, argu); //na tsekarw th seira 
        Method_t meth = (Method_t) n.f1.accept(this, argu);
		meth.methodParamsMap_.put(expr.getName(), expr);
        return meth;
    }

    /**
    * f0 -> ( ExpressionTerm() )*
    */
    public BaseType visit(ExpressionTail n, BaseType argu) throws Exception {
        Method_t meth = new Method_t(null, null);
        if (n.f0.present()) {                // create a linked list of variables. (parameters list)
            for (int i = 0 ; i < n.f0.size() ; i++) {
				Variable_t var = (Variable_t)n.f0.nodes.get(i).accept(this, argu);
				meth.methodParamsMap_.put(var.getName(), var);
			}
		}
        return meth;
    }

    /**
    * f0 -> ","
    * f1 -> Expression()
    */
    public BaseType visit(ExpressionTerm n, BaseType argu) throws Exception {
        n.f0.accept(this, argu);
        return (Variable_t) n.f1.accept(this, argu);
    }

	/**
	* f0 -> IntegerLiteral()
	*       | Identifier()
	*       | ArrayAllocationExpression()
	*       | AllocationExpression()
	*       | BracketExpression()
	*/
	public BaseType visit(PrimaryExpression n, BaseType argu) throws Exception {
		return n.f0.accept(this, argu);
	}

	/**
	* f0 -> <INTEGER_LITERAL>
	*/
	public BaseType visit(IntegerLiteral n, BaseType argu) throws Exception {
		if (!immediate_) {
			String ret = "$r" + ++glob_temp_cnt_;
			if (this.is_inline_meth_) {
				this.inline_meth_ += "move " + ret + ", " + ret + ", " + n.f0.toString() + "\n";
			} else {			
				this.result_ += "move " + ret + ", " + ret + ", " + n.f0.toString() + "\n";
			}
			return new Variable_t(ret, null);
		}
		return new Variable_t(n.f0.toString(), null);
	}

	/**
	* f0 -> <IDENTIFIER>
	*/
	public BaseType visit(Identifier n, BaseType argu) throws Exception {
		String id = n.f0.toString();
		if (argu == null) {
			return new Variable_t(null, id);
		}
		// if argu is a method name -- we are inside method argu.getName()
		Method_t meth = (Method_t) argu;
		Variable_t var = meth.methContainsVar(id);				
		Variable_t v;
		if (var != null) { // if a parameter or a local var
			v = new Variable_t(var.getRegister(), id);
			v.var_temp = var.getType();
		} else { // if a function name
			Method_t m = this.st_.get(id);
			v = new Variable_t(m.getType(), id);
		}
		return v;
	}

	/**
	* f0 -> "new"
	* f1 -> "int"
	* f2 -> "["
	* f3 -> Expression()
	* f4 -> "]"
	*/
	public BaseType visit(ArrayAllocationExpression n, BaseType argu) throws Exception {
		this.immediate_ = true;
		String array_size = ((Variable_t) n.f3.accept(this, argu)).getType();
		this.immediate_ = false;
		String base = Integer.toString(this.base_addr_);
		this.base_addr_ += Integer.parseInt(array_size);
		return new Variable_t(base, null);
	}
	
	/**
	* f0 -> "!"
	* f1 -> PrimaryExpression()
	*/
	public BaseType visit(NotExpression n, BaseType argu) throws Exception {
		String ret = new String("$r" + ++glob_temp_cnt_);
		String t = ((Variable_t) n.f1.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += "not " + ret + ", " + ret + ", " + t + "\n";
		} else {			
			this.result_ += "not " + ret + ", " + ret + ", " + t + "\n";
		}
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> "("
	* f1 -> Expression()
	* f2 -> ")"
	*/
	public BaseType visit(BracketExpression n, BaseType argu) throws Exception {
		return n.f1.accept(this, argu);
	}

}
