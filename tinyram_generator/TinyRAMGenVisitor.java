package tinyram_generator;

import syntaxtree.*;
import visitor.GJDepthFirst;
import symbol_table.*;
import base_type.*;
import java.util.Map;
import java.util.HashMap;

public class TinyRAMGenVisitor extends GJDepthFirst<BaseType, BaseType> {
	public String result_;
	private Label L;
	private Map<String, Method_t> st_;
	
	private int glob_temp_cnt_;
	private int base_addr_;
	private boolean immediate_;
	
	private boolean is_inline_meth_;
	private String inline_meth_;

	public TinyRAMGenVisitor(Map<String, Method_t> st) {
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
		// this.inline_meth_ += "\n__BEGIN_" + methName + "__\n";
		n.f6.accept(this, meth);
		n.f7.accept(this, meth);
		Variable_t retType = (Variable_t) n.f9.accept(this, meth);
		meth.return_reg = retType.getType();
		// this.inline_meth_ += "RETURN " + retType.getType() + "\n";
		// this.inline_meth_ += "__END_" + methName + "__\n";
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
		String newTemp = new String("r" + ++glob_temp_cnt_);
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
	*       | BooleanType()
	*       | IntegerType()
	*       | Identifier()
	*/
	public BaseType visit(Type n, BaseType argu) throws Exception {
		return n.f0.accept(this, argu);
	}

	/**
	* f0 -> "boolean"
	*/
	public BaseType visit(BooleanType n, BaseType argu) throws Exception {
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
	*       | ArrayAssignmentStatement()
	*       | IfStatement()
	*       | WhileStatement()
	*       | PrintStatement()
	*       | ReadPrimaryTape()
	* 		| ReadPrivateTape()
	*       | SeekPrimaryTape()
	* 		| SeekPrivateTape()
	* 		| AnswerStatement()
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
				this.inline_meth_ += "MOV " +  var.getTemp() + " " + var.getTemp() + " " + expr +"\n";
			} else {
				this.result_ += "MOV " +  var.getTemp() + " " + var.getTemp() + " " + expr +"\n";
			}
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
		String res = new String("r" + ++glob_temp_cnt_);
		
		if (this.is_inline_meth_) {
			this.inline_meth_ += "ADD " + array + " " + array + " " + idx + "\n";
			this.inline_meth_ += "MOV " + res + " " + res + " " + expr + "\n";
			this.inline_meth_ += "STOREW " + res + " " + res + " " + array + "\n";
			this.inline_meth_ += "SUB " + array + " " + array + " " + idx + "\n";
		} else {
			this.result_ += "ADD " + array + " " + array + " " + idx + "\n";
			this.result_ += "MOV " + res + " " + res + " " + expr + "\n";
			this.result_ += "STOREW " + res + " " + res + " " + array + "\n";
			this.result_ += "SUB " + array + " " + array + " " + idx + "\n";
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
	public BaseType visit(IfStatement n, BaseType argu) throws Exception {
		String elselabel = L.new_label();
		String endlabel = L.new_label();
		String cond = ((Variable_t) n.f2.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += "CNJMP " + cond + " " + cond + " " + elselabel + "\n"; //if cond not true go to elselabel
			n.f4.accept(this, argu);
			this.inline_meth_ += "JMP r0 r0 " + endlabel + "\n" + elselabel + "\n";
			n.f6.accept(this, argu);
			this.inline_meth_ += endlabel + "\n";
		} else {
			this.result_ += "CNJMP " + cond + " " + cond + " " + elselabel + "\n"; //if cond not true go to elselabel
			n.f4.accept(this, argu);
			this.result_ += "JMP r0 r0 " + endlabel + "\n" + elselabel + "\n";
			n.f6.accept(this, argu);
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
			this.inline_meth_ += "CNJMP " + expr + " " + expr + " " + lend + "\n";
			n.f4.accept(this, argu);
			this.inline_meth_ += "JMP r0 r0 " + lstart + "\n" + lend + "\n";
		} else {
			this.result_ += lstart + "\n";
			String expr = ((Variable_t) n.f2.accept(this, argu)).getType();
			this.result_ += "CNJMP " + expr + " " + expr + " " + lend + "\n";
			n.f4.accept(this, argu);
			this.result_ += "JMP r0 r0 " + lstart + "\n" + lend + "\n";
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
			this.inline_meth_ += "PRINT " + t + " " + t + " " + t +"\n";
		} else {
			this.result_ += "PRINT " + t + " " + t + " " + t +"\n";
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
			this.inline_meth_ += "READ " + t + " " + t + " " + 0 +"\n";
		} else {
			this.result_ += "READ " + t + " " + t + " " + 0 +"\n";
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
			this.inline_meth_ += "READ " + t + " " + t + " " + 1 +"\n";
		} else {
			this.result_ += "READ " + t + " " + t + " " + 1 +"\n";
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
			this.inline_meth_ += "SEEK " + dst + " " + idx + " " + 0 +"\n";
		} else {
			this.result_ += "SEEK " + dst + " " + idx + " " + 0 +"\n";
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
			this.inline_meth_ += "SEEK " + dst + " " + idx + " " + 1 +"\n";
		} else {
			this.result_ += "SEEK " + dst + " " + idx + " " + 1 +"\n";
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
			this.inline_meth_ += "ANSWER " + t + " " + t + " " + t +"\n";
		} else {
			this.result_ += "ANSWER " + t + " " + t + " " + t +"\n";
		}
		return null;
	}

	/**
	* f0 -> AndExpression()
	* 		| OrExpression()
	* 		| EqExpression()
	*       | LessThanExpression()
	*       | GreaterThanExpression()
	*		| LessEqualThanExpression()
	*		| GreaterEqualThanExpression()
	*       | PlusExpression()
	*       | PlusPlusExpression()
	*       | MinusExpression()
	*       | MinusMinusExpression()
	*       | TimesExpression()
	*       | ArrayLookup()
	*       | MessageSend()
	*       | Clause()
	*/
	public BaseType visit(Expression n, BaseType argu) throws Exception {
		return n.f0.accept(this, argu);
	}

	/**
	* f0 -> Clause()
	* f1 -> "&&"
	* f2 -> Clause()
	*/
	public BaseType visit(AndExpression n, BaseType argu) throws Exception {
		String l1 = L.new_label();
		String ret = new String("r" + ++glob_temp_cnt_);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += new String("CNJMP " + t1 + " " + t1 + " " + l1 + "\n");
			String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
			this.inline_meth_ += new String("CNJMP " + t2 + " " + t2 + " " + l1 + "\n");
			this.inline_meth_ += new String("MOV " + ret + " " + ret + " 1\n");
			this.inline_meth_ += new String(l1 + "\n");
		} else {
			this.result_ += new String("CNJMP " + t1 + " " + t1 + " " + l1 + "\n");
			String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
			this.result_ += new String("CNJMP " + t2 + " " + t2 + " " + l1 + "\n");
			this.result_ += new String("MOV " + ret + " " + ret + " 1\n");
			this.result_ += new String(l1 + "\n");
		}
		return new Variable_t(ret, null);
	}
	
	/**
	* f0 -> Clause()
	* f1 -> "||"
	* f2 -> Clause()
	*/
	public BaseType visit(OrExpression n, BaseType argu) throws Exception {
		String l1 = L.new_label();
		String l2 = L.new_label();
		String ret = new String("r" + ++glob_temp_cnt_);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += new String("CNJMP " + t1 + " " + t1 + " " + l1 + "\n");
			this.inline_meth_ += new String("MOV " + ret + " " + ret + " 1\n");
			this.inline_meth_ += new String("JMP r0 r0 " + l2 + "\n");
			this.inline_meth_ += new String(l1 + "\n");
			String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
			this.inline_meth_ += new String("CNJMP " + t2 + " " + t2 + " " + l2 + "\n");
			this.inline_meth_ += new String("MOV " + ret + " " + ret + " 1\n");
			this.inline_meth_ += new String(l2 + "\n");
		} else {
			this.result_ += new String("CNJMP " + t1 + " " + t1 + " " + l1 + "\n");
			this.result_ += new String("MOV " + ret + " " + ret + " 1\n");
			this.result_ += new String("JMP r0 r0 " + l2 + "\n");
			this.result_ += new String(l1 + "\n");
			String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
			this.result_ += new String("CNJMP " + t2 + " " + t2 + " " + l2 + "\n");
			this.result_ += new String("MOV " + ret + " " + ret + " 1\n");
			this.result_ += new String(l2 + "\n");
		}
		return new Variable_t(ret, null);
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
			this.inline_meth_ += new String("CMPG " + t2 + " " +  t2 + " " + t1 + "\n");
		} else {
			this.result_ += new String("CMPG " + t2 + " " +  t2 + " " + t1 + "\n");
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
			this.inline_meth_ += new String("CMPG " + t1 + " " +  t1 + " " + t2 + "\n");
		} else {
			this.result_ += new String("CMPG " + t1 + " " +  t1 + " " + t2 + "\n");
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
			this.inline_meth_ += new String("CMPGE " + t2 + " " +  t2 + " " + t1 + "\n");
		} else {
			this.result_ += new String("CMPGE " + t2 + " " +  t2 + " " + t1 + "\n");
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
			this.inline_meth_ += new String("CMPGE " + t1 + " " +  t1 + " " + t2 + "\n");
		} else {
			this.result_ += new String("CMPGE " + t1 + " " +  t1 + " " + t2 + "\n");
		}
		return new Variable_t(t1, null);
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
			this.inline_meth_ += new String("CMPE " + t1 + " " +  t1 + " " + t2 + "\n");
		} else {
			this.result_ += new String("CMPE " + t1 + " " +  t1 + " " + t2 + "\n");
		}
		return new Variable_t(t1, null);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "+"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(PlusExpression n, BaseType argu) throws Exception {
		String ret = new String("r" + ++glob_temp_cnt_);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += new String("ADD " + ret + " " + t1 + " " + t2 + "\n");
		} else {
			this.result_ += new String("ADD " + ret + " " + t1 + " " + t2 + "\n");
		}
		return new Variable_t(ret, null);
	}
	
	/**
	* f0 -> PrimaryExpression()
	* f1 -> "++"
	*/
	public BaseType visit(PlusPlusExpression n, BaseType argu) throws Exception {
		String ret = new String("r" + ++glob_temp_cnt_);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += new String("ADD " + ret + " " + t1 + " 1\n");
		} else {
			this.result_ += new String("ADD " + ret + " " + t1 + " 1\n");
		}
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "-"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(MinusExpression n, BaseType argu) throws Exception {
		String ret = new String("r" + ++glob_temp_cnt_);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += new String("SUB " + ret + " " +  t1 + " " + t2 + "\n");
		} else {
			this.result_ += new String("SUB " + ret + " " +  t1 + " " + t2 + "\n");
		}
		return new Variable_t(ret, null);
	}
	
	/**
	* f0 -> PrimaryExpression()
	* f1 -> "--"
	*/
	public BaseType visit(MinusMinusExpression n, BaseType argu) throws Exception {
		String ret = new String("r" + ++glob_temp_cnt_);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += new String("SUB " + ret + " " +  t1 + " 1\n");
		} else {
			this.result_ += new String("SUB " + ret + " " +  t1 + " 1\n");
		}
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "*"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(TimesExpression n, BaseType argu) throws Exception {
		String ret = new String("r" + ++glob_temp_cnt_);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		if (this.is_inline_meth_) {
			this.inline_meth_ += new String("MULL " + ret + " " + t1 + " " + t2 + "\n");
		} else {
			this.result_ += new String("MULL " + ret + " " + t1 + " " + t2 + "\n");
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
		String res = new String("r" + ++glob_temp_cnt_);
		if (this.is_inline_meth_) {
			this.inline_meth_ += "ADD " + array + " " + array + " " + idx + "\n";
			this.inline_meth_ += "LOADW " + res + " " + res + " " + array + "\n";
			this.inline_meth_ += "SUB " + array + " " + array + " " + idx + "\n";
		} else {
			this.result_ += "ADD " + array + " " + array + " " + idx + "\n";
			this.result_ += "LOADW " + res + " " + res + " " + array + "\n";
			this.result_ += "SUB " + array + " " + array + " " + idx + "\n";
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
				String param = "r" + var.getVarNum();
				this.result_ += "MOV " + param + " " + param + " " + var.getType() + "\n";
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
	* f0 -> NotExpression()
	*       | PrimaryExpression()
	*/
	public BaseType visit(Clause n, BaseType argu) throws Exception {
		return n.f0.accept(this, argu);
	}

	/**
	* f0 -> IntegerLiteral()
	*       | TrueLiteral()
	*       | FalseLiteral()
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
			String ret = "r" + ++glob_temp_cnt_;
			if (this.is_inline_meth_) {
				this.inline_meth_ += "MOV " + ret + " " + ret + " " + n.f0.toString() + "\n";
			} else {			
				this.result_ += "MOV " + ret + " " + ret + " " + n.f0.toString() + "\n";
			}
			return new Variable_t(ret, null);
		}
		return new Variable_t(n.f0.toString(), null);
	}

	/**
	* f0 -> "true"
	*/
	public BaseType visit(TrueLiteral n, BaseType argu) throws Exception {
		String ret = new String("r" + ++glob_temp_cnt_);
		if (this.is_inline_meth_) {
			this.inline_meth_ += new String("MOV " + ret + " " + ret + " 1\n");
		} else {			
			this.result_ += new String("MOV " + ret + " " + ret + " 1\n");
		}
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> "false"
	*/
	public BaseType visit(FalseLiteral n, BaseType argu) throws Exception {
		String ret = new String("r" + ++glob_temp_cnt_);
		if (this.is_inline_meth_) {
			this.inline_meth_ += new String("MOV " + ret + " " + ret + " 0\n");
		} else {			
			this.result_ += new String("MOV " + ret + " " + ret + " 0\n");
		}
		return new Variable_t(ret, null);
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
			v = new Variable_t(var.getTemp(), id);
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
	* f1 -> Clause()
	*/
	public BaseType visit(NotExpression n, BaseType argu) throws Exception {
		String ret = new String("r" + ++glob_temp_cnt_);
		String t = ((Variable_t) n.f1.accept(this, argu)).getType();
		String one = new String("r" + ++glob_temp_cnt_);
		if (this.is_inline_meth_) {
			this.inline_meth_ += "MOV " + one + " " + one + " 1\n";
			this.inline_meth_ += "SUB " + ret + " " + one + " " + t + "\n";
		} else {			
			this.result_ += "MOV " + one + " " + one + " 1\n";
			this.result_ += "SUB " + ret + " " + one + " " + t + "\n";
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
