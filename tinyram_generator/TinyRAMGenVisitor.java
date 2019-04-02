package tinyram_generator;

import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;
import symbol_table.*;
import base_type.*;

public class TinyRAMGenVisitor extends GJDepthFirst<BaseType, BaseType> {
	public String result;
	private Label L;
	private SymbolTable symTable;

	public TinyRAMGenVisitor(SymbolTable symTable) {
		this.L = new Label();
        this.symTable = symTable;   
	}


	/**
	* f0 -> MainClass()
	* f1 -> ( TypeDeclaration() )*
	* f2 -> <EOF>
	*/
	public BaseType visit(Goal n, BaseType argu) throws Exception {
		n.f0.accept(this, argu);
		n.f1.accept(this, argu);
		n.f2.accept(this, argu);
		return null;
	}

	/**
	* f0 -> "class"
	* f1 -> Identifier()
	* f2 -> "{"
	* f3 -> "public"
	* f4 -> "static"
	* f5 -> "void"
	* f6 -> "main"
	* f7 -> "("
	* f8 -> "String"
	* f9 -> "["
	* f10 -> "]"
	* f11 -> Identifier()
	* f12 -> ")"
	* f13 -> "{"
	* f14 -> ( VarDeclaration() )*
	* f15 -> ( Statement() )*
	* f16 -> "}"
	* f17 -> "}"
	*/
	public BaseType visit(MainClass n, BaseType argu) throws Exception {
		this.result = new String();
		String id = n.f1.accept(this, argu).getName();
        Class_t mainclazz = symTable.contains(id);
        Method_t meth = mainclazz.getMethod("main");
		n.f14.accept(this, meth);
		n.f15.accept(this, meth);
		return null;
	}

	/**
	* f0 -> Type()
	* f1 -> Identifier()
	* f2 -> ";"
	*/
	public BaseType visit(VarDeclaration n, BaseType argu) throws Exception {
		Method_t meth = (Method_t) argu;
		String varName = n.f1.f0.toString();
		if (meth.comesFrom != null) { 												// is a variable of a function
			String newTemp = new String("r" + ++symTable.glob_temp_cnt);
			if ((meth = meth.comesFrom.getMethod(meth.getName())) != null) {		// if you found method
				meth.addTempToVar(varName, newTemp);
			} else
				throw new Exception("VarDeclaration Errror 1");
			this.result += new String("MOV " + newTemp + " " + newTemp + " 0\n");
		} else {																	// is a var (field) of a class
			Class_t cl = symTable.contains(meth.getName());
			if (cl == null)															// do nothing for now
				throw new Exception("VarDeclaration Errror 2");
		}
		return null;
	}

	/**
	* f0 -> "public"
	* f1 -> Type()
	* f2 -> Identifier()
	* f3 -> "("
	* f4 -> ( FormalParameterList() )?
	* f5 -> ")"
	* f6 -> "{"
	* f7 -> ( VarDeclaration() )*
	* f8 -> ( Statement() )*
	* f9 -> "return"
	* f10 -> Expression()
	* f11 -> ";"
	* f12 -> "}"
	*/
	public BaseType visit(MethodDeclaration n, BaseType argu) throws Exception {
		String methName = n.f2.accept(this, argu).getName();
        Method_t meth = ((Class_t) argu).getMethod(methName);
        this.result += "\n" + ((Class_t) argu).getName()+ "_" + methName + "[" + (meth.par_cnt+1) + "]\nBEGIN\n";
        n.f7.accept(this, meth);
        n.f8.accept(this, meth);
        Variable_t retType = (Variable_t) n.f10.accept(this, meth);
        this.result += "RETURN " + retType.getType() + "\nEND\n";
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
	*       | PrintStatement2()
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
		String expr = ((Variable_t) n.f2.accept(this, argu)).getType();
		Variable_t var;
		// if a local var
		Method_t meth = (Method_t) argu;
		if (meth != null) { 
			var = meth.methContainsVar(id);
			if (var == null) { // didnt find the var in method, so its a field of the class
				Class_t cl = symTable.contains(meth.comesFrom.getName());
				if (cl == null) {  throw new Exception("something went wrong at AssignmentStatement 2"); }
				var = cl.classContainsVarReverse(id);
				if (var != null)							// class field
				this.result += "STORE " + " r0 " + var.getVarNum()*4 + " " + expr +"\n";
				return null;
			}
			this.result += "MOV " +  var.getTemp() + " " + var.getTemp() + " " + expr +"\n";
		}
		else { // if a field of a class
			Class_t cl = symTable.contains(meth.comesFrom.getName());
			if (cl == null) {  throw new Exception("something went wrong at AssignmentStatement 2"); }
			var = cl.classContainsVarReverse(id);
			if (var != null)							// class field
				this.result += "STORE " + " r0 " + var.getVarNum()*4 + " " + expr +"\n";
		}
		return null;
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
		this.result += "CJMP " + cond + " " + elselabel + "\n"; //if cond not true go to elselabel
		n.f4.accept(this, argu);
		this.result += "JMP " + endlabel + "\n" + elselabel + "\n";
		n.f6.accept(this, argu);
		this.result += endlabel + "\n";
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
		this.result += "\n" + lstart + "\n";
		String expr = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.result += "CNJMP r0 r0 " + lend + "\n";
		n.f4.accept(this, argu);
		this.result += "JMP r0 r0 " + lstart + "\n" + lend + "\n";
		return null;
	}

	/**
	* f0 -> "System.out.println"
	* f1 -> "("
	* f2 -> Expression()
	* f3 -> ")"
	* f4 -> ";"
	*/
	public BaseType visit(PrintStatement n, BaseType argu) throws Exception {
		String t = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.result += "PRINT " + t + " " + t + " " + t +"\n";
		return null;
	}
	
	/**
	* f0 -> "PrimaryTape.read"
	* f1 -> "("
	* f2 -> Identifier()
	* f3 -> ")"
	* f4 -> ";"
	*/
	public BaseType visit(ReadPrimaryTape n, BaseType argu) throws Exception {
		String t = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.result += "READ " + t + " " + t + " " + 0 +"\n";
		return null;
	}
	
	/**
	* f0 -> "PrivateTape.read"
	* f1 -> "("
	* f2 -> Identifier()
	* f3 -> ")"
	* f4 -> ";"
	*/
	public BaseType visit(ReadPrivateTape n, BaseType argu) throws Exception {
		String t = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.result += "READ " + t + " " + t + " " + 1 +"\n";
		return null;
	}
	
	/**
	* f0 -> "Answer"
	* f1 -> "("
	* f2 -> Identifier()
	* f3 -> ")"
	* f4 -> ";"
	*/
	public BaseType visit(AnswerStatement n, BaseType argu) throws Exception {
		String t = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.result += "ANSWER " + t + " " + t + " " + t +"\n";
		return null;
	}

	/**
	* f0 -> AndExpression()
	*       | CompareExpression()
	*       | PlusExpression()
	*       | MinusExpression()
	*       | TimesExpression()
	*       | ArrayLookup()
	*       | ArrayLength()
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
		String label = L.new_label();
		String ret = new String("r" + ++symTable.glob_temp_cnt);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		this.result += new String("MOV " + ret + " r0 " + t1 + "\n" + "CJMP " + t1 + " " + label + "\n");
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.result += new String("MOV " + ret + " r0 " + t2 + "\n" + label + "\n");
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "<"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(CompareExpression n, BaseType argu) throws Exception {
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.result += new String("CMPG " + t2 + " " +  t2 + " " + t1 + "\n");
		return new Variable_t("r0", null);
	}
	
	/**
	* f0 -> PrimaryExpression()
	* f1 -> "=="
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(EqExpression n, BaseType argu) throws Exception {
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.result += new String("CMPE " + t1 + " " +  t1 + " " + t2 + "\n");
		return new Variable_t("r0", null);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "+"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(PlusExpression n, BaseType argu) throws Exception {
		String ret = new String("r" + ++symTable.glob_temp_cnt);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.result += new String("ADD " + ret + " " + t1 + " " + t2 + "\n");
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "-"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(MinusExpression n, BaseType argu) throws Exception {
		String ret = new String("r" + ++symTable.glob_temp_cnt);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.result += new String("SUB " + ret + " " +  t1 + " " + t2 + "\n");
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "*"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(TimesExpression n, BaseType argu) throws Exception {
		String ret = new String("r" + ++symTable.glob_temp_cnt);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.result += new String("MUL " + ret + " " + t1 + " " + t2 + "\n");
		return new Variable_t(ret, null);
		
	}

 	/**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public BaseType visit(ExpressionList n, BaseType argu) throws Exception {
        Variable_t expr =  (Variable_t) n.f0.accept(this, argu); //na tsekarw th seira 
        Method_t meth = (Method_t) n.f1.accept(this, argu);
        meth.methodParams.addLast(expr);
        return meth;
    }

    /**
    * f0 -> ( ExpressionTerm() )*
    */
    public BaseType visit(ExpressionTail n, BaseType argu) throws Exception {
        Method_t meth = new Method_t(null, null);
        if (n.f0.present())                 // create a linked list of variables. (parameters list)
            for (int i = 0 ; i < n.f0.size() ; i++)
                meth.methodParams.addLast( (Variable_t)n.f0.nodes.get(i).accept(this, argu) );
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
	*       | ThisExpression()
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
		String ret = "r" + ++symTable.glob_temp_cnt;
		this.result += "MOV " + ret + " r0 " + n.f0.toString() + "\n";
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> "true"
	*/
	public BaseType visit(TrueLiteral n, BaseType argu) throws Exception {
		String ret = new String("r" + ++symTable.glob_temp_cnt);
		this.result += new String("MOV " + ret + " r0 1\n");
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> "false"
	*/
	public BaseType visit(FalseLiteral n, BaseType argu) throws Exception {
		String ret = new String("r" + ++symTable.glob_temp_cnt);
		this.result += new String("MOV " + ret + " r0 0\n");
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> <IDENTIFIER>
	*/
	public BaseType visit(Identifier n, BaseType argu) throws Exception {
		String id = n.f0.toString();
		if (argu == null) 
			return new Variable_t(null, id);
		Class_t cl = symTable.contains(argu.getName());
		Variable_t var;
		if (cl != null) {									// if argu is a class name
			var = cl.classContainsVarReverse(id);
			if (var != null) {								// and id is a field of that class
				Variable_t v = new Variable_t(var.getTemp(), id);
				v.var_temp = cl.getName();
				return v;
			} else {										// is a method
				Method_t meth = cl.getMethod(id);
				if (meth == null) { throw new Exception("something went wrong 1"); }
				return new Variable_t(null, id);
			}
		} else {											// if argu is a method name 
			Method_t meth = (Method_t) argu;
			var = meth.methContainsVar(id);				
			if (var != null) {								// if a parameter or a local var
				Variable_t v = new Variable_t(var.getTemp(), id);
				v.var_temp = var.getType();
				return v;
			} else {										// a field of class
				cl = symTable.contains(meth.comesFrom.getName());
				if (cl == null) {  throw new Exception("something went wrong 2"); }
				var = cl.classContainsVarReverse(id);
				if (var == null) {  return new Variable_t(null, id);  }
				String newTemp = "r" + ++symTable.glob_temp_cnt;
				this.result += "LOAD " + newTemp + " r0 " + var.getVarNum()*4 + "\n";
				Variable_t v = new Variable_t(newTemp, id);
				v.var_temp = var.getType();
				return v;
			}
		}
	}

	/**
	* f0 -> "!"
	* f1 -> Clause()
	*/
	public BaseType visit(NotExpression n, BaseType argu) throws Exception {
		String ret = new String("r" + ++symTable.glob_temp_cnt);
		String t = ((Variable_t) n.f1.accept(this, argu)).getType();
		String one = new String("r" + ++symTable.glob_temp_cnt);
		this.result += "MOV " + one + " r0 1\n";
		this.result += "MOV " + ret + " MINUS " + one + " " + t + "\n";
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
