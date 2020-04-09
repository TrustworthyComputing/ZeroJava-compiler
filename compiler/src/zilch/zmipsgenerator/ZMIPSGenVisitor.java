package zmipsgenerator;

import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;
import symboltable.*;
import basetype.*;

public class ZMIPSGenVisitor extends GJDepthFirst<BaseType, BaseType> {

	private StringBuilder code;
	private Label L;
	private Map<String, Class_t> st_;
	private int glob_temp_cnt;

    public ZMIPSGenVisitor(Map<String, Class_t> st_, int glob_temp_cnt) {
		this.L = new Label();
        this.st_ = st_;
        this.glob_temp_cnt = glob_temp_cnt;
	}

	public String getCode() {
		return code.toString();
	}

	private void initVtables() {
		for (Map.Entry<String, Class_t> entry : st_.entrySet()) {
			Class_t cl = entry.getValue();
			if (cl.isMain) {
				continue;
			}
			String label = L.newClassLabel(cl.getName());
			String vtable = new String("TEMP " + ++glob_temp_cnt);
			String temp = new String("TEMP " + ++glob_temp_cnt);
			this.code.append("MOVE " + vtable + " " + label + "\n");
			int offset = cl.meth_cnt;
			this.code.append("MOVE " + temp + " HALLOCATE " + offset*4 + "\n");
			this.code.append("HSTORE " + vtable + " 0 " + temp + "\n");
			int i = 0;
			for (Map.Entry<String, Method_t> methods : cl.class_methods_map.entrySet()) {
				String newTemp = new String("TEMP " + ++glob_temp_cnt);
				Method_t meth = methods.getValue();
				this.code.append("MOVE " + newTemp + " " + meth.comesFrom.getName() + "_" + meth.getName() + "\n");
				this.code.append("HSTORE " + temp + " " + i*4 + " " + newTemp + "\n");
				i++;
			}
			this.code.append("\n");
    	}
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
		this.code = new StringBuilder();
		this.code.append("MAIN\n\n");
		String id = n.f1.accept(this, argu).getName();
        Class_t mainclazz = st_.get(id);
        Method_t meth = mainclazz.getMethod("main");
        initVtables();
		n.f14.accept(this, meth);
		n.f15.accept(this, meth);
		this.code.append("END\n");
		return null;
	}

	/**
	* f0 -> ClassDeclaration()
	*       | ClassExtendsDeclaration()
	*/
	public BaseType visit(TypeDeclaration n, BaseType argu) throws Exception {
		return n.f0.accept(this, argu);
	}

	/**
	* f0 -> "class"
	* f1 -> Identifier()
	* f2 -> "{"
	* f3 -> ( VarDeclaration() )*
	* f4 -> ( MethodDeclaration() )*
	* f5 -> "}"
	*/
	public BaseType visit(ClassDeclaration n, BaseType argu) throws Exception {
		n.f0.accept(this, argu);
        String s = n.f1.accept(this, argu).getName();
        Class_t class_id = st_.get(s);
        n.f3.accept(this, new Method_t(null, class_id.getName()));
        n.f4.accept(this, class_id);
        return null;
	}

	/**
	* f0 -> "class"
	* f1 -> Identifier()
	* f2 -> "extends"
	* f3 -> Identifier()
	* f4 -> "{"
	* f5 -> ( VarDeclaration() )*
	* f6 -> ( MethodDeclaration() )*
	* f7 -> "}"
	*/
	public BaseType visit(ClassExtendsDeclaration n, BaseType argu) throws Exception {
        n.f0.accept(this, argu);
        String s = n.f1.accept(this, argu).getName();
        Class_t class_id = st_.get(s);
        n.f1.accept(this, argu);
        n.f3.accept(this, argu);
        n.f5.accept(this, new Method_t(null, class_id.getName()));
        n.f6.accept(this, class_id);
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
			String newTemp = new String("TEMP " + ++glob_temp_cnt);
			if ((meth = meth.comesFrom.getMethod(meth.getName())) != null) {		// if you found method
				meth.addTempToVar(varName, newTemp);
			} else
				throw new Exception("VarDeclaration Errror 1");
			this.code.append(new String("MOVE " + newTemp + " 0\n"));
		} else {																	// is a var (field) of a class
			Class_t cl = st_.get(meth.getName());
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
        this.code.append("\n" + ((Class_t) argu).getName()+ "_" + methName + "[" + (meth.par_cnt+1) + "]\nBEGIN\n");
        n.f7.accept(this, meth);
        n.f8.accept(this, meth);
        Variable_t retType = (Variable_t) n.f10.accept(this, meth);
        this.code.append("RETURN " + retType.getType() + "\nEND\n");
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
	* f0 -> "int"
	* f1 -> "["
	* f2 -> "]"
	*/
	public BaseType visit(ArrayType n, BaseType argu) throws Exception {
		n.f0.accept(this, argu);
		n.f1.accept(this, argu);
		n.f2.accept(this, argu);
		return null;
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
				Class_t cl = st_.get(meth.comesFrom.getName());
				if (cl == null) {  throw new Exception("something went wrong at AssignmentStatement 2"); }
				var = cl.classContainsVar(id);
				if (var != null)							// class field
				this.code.append("HSTORE " + " TEMP 0 " + var.getVarNum()*4 + " " + expr +"\n");
				return null;
			}
			this.code.append("MOVE " +  var.getRegister() + " " + expr +"\n");
		}
		else { // if a field of a class
			Class_t cl = st_.get(meth.comesFrom.getName());
			if (cl == null) {  throw new Exception("something went wrong at AssignmentStatement 2"); }
			var = cl.classContainsVar(id);
			if (var != null)							// class field
				this.code.append("HSTORE " + " TEMP 0 " + var.getVarNum()*4 + " " + expr +"\n");
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
		String length = new String("TEMP " + ++glob_temp_cnt);
		String cond = new String("TEMP " + ++glob_temp_cnt);
		String error = L.new_label();
		String noerror = L.new_label();
		String array = ((Variable_t) n.f0.accept(this, argu)).getType();
		this.code.append("HLOAD " + length + " " + array + " 0\n"); 			// load real size to length
		String pos = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.code.append("MOVE " + cond + " LT " + pos + " " + length + "\n");	// if pos < arr.length
		this.code.append("CJUMP " + cond + " " + error + "\n");
		this.code.append("MOVE " + cond + " LT " + pos + " 0\n");				// if arr.length > 0 g
		String one = new String("TEMP " + ++glob_temp_cnt);
		this.code.append("MOVE " + one + " 1\n");
		this.code.append("MOVE " + cond + " MINUS " + one + " " + cond + "\n");
		this.code.append("CJUMP " + cond + " " + error + "\n");
		this.code.append("JUMP " + noerror + "\n");
		this.code.append(error + " NOOP\n");
		this.code.append("ERROR\n" + noerror + " NOOP\n");
		String temp_array = new String("TEMP " + ++glob_temp_cnt);
		this.code.append("MOVE " + temp_array + " " + array + "\n");			// temp_array = &array
		this.code.append("MOVE " + temp_array + " PLUS " + temp_array + " 4\n");
		String temp = new String("TEMP " + ++glob_temp_cnt);
		this.code.append("MOVE " + temp + " TIMES " + pos + " 4\n");
		this.code.append("MOVE " + temp_array + " PLUS " + temp_array + " " + temp + "\n");
		String expr = ((Variable_t) n.f5.accept(this, argu)).getType();
		this.code.append("HSTORE " + temp_array + " 0 " + expr + "\n");
		return new Variable_t(temp_array, null);
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
		this.code.append("CJUMP " + cond + " " + elselabel + "\n"); //if cond not true go to elselabel
		n.f4.accept(this, argu);
		this.code.append("JUMP " + endlabel + "\n" + elselabel + " NOOP\n");
		n.f6.accept(this, argu);
		this.code.append(endlabel + " NOOP\n");
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
		String cond = new String("TEMP " + ++glob_temp_cnt);
		this.code.append(lstart + " NOOP\n");
		String expr = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.code.append("MOVE " + cond + " " + expr + "\n");
		this.code.append("CJUMP " + cond + " " + lend + "\n");
		n.f4.accept(this, argu);
		this.code.append("JUMP " + lstart + "\n" + lend + " NOOP\n");
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
		this.code.append("PRINT " + t + "\n");
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
		String ret = new String("TEMP " + ++glob_temp_cnt);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		this.code.append(new String("MOVE " + ret + " " + t1 + "\n" + "CJUMP " + t1 + " " + label + "\n"));
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.code.append(new String("MOVE " + ret + " " + t2 + "\n" + label + " NOOP\n"));
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "<"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(CompareExpression n, BaseType argu) throws Exception {
		String ret = new String("TEMP " + ++glob_temp_cnt);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.code.append(new String("MOVE " + ret + " LT " +  t1 + " " + t2 + "\n"));
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "+"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(PlusExpression n, BaseType argu) throws Exception {
		String ret = new String("TEMP " + ++glob_temp_cnt);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.code.append(new String("MOVE " + ret + " PLUS " + t1 + " " + t2 + "\n"));
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "-"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(MinusExpression n, BaseType argu) throws Exception {
		String ret = new String("TEMP " + ++glob_temp_cnt);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.code.append(new String("MOVE " + ret + " MINUS " +  t1 + " " + t2 + "\n"));
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "*"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(TimesExpression n, BaseType argu) throws Exception {
		String ret = new String("TEMP " + ++glob_temp_cnt);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.code.append(new String("MOVE " + ret + " TIMES " + t1 + " " + t2 + "\n"));
		return new Variable_t(ret, null);

	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "["
	* f2 -> PrimaryExpression()
	* f3 -> "]"
	*/
	public BaseType visit(ArrayLookup n, BaseType argu) throws Exception {
		String length = new String("TEMP " + ++glob_temp_cnt);
		String cond = new String("TEMP " + ++glob_temp_cnt);
		String error = L.new_label();
		String noerror = L.new_label();
		String array = ((Variable_t) n.f0.accept(this, argu)).getType();
		this.code.append("HLOAD " + length + " " + array + " 0\n"); 			// load real size to length
		String pos = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.code.append("MOVE " + cond + " LT " + pos + " " + length + "\n");	// if pos < arr.length
		this.code.append("CJUMP " + cond + " " + error + "\n");
		this.code.append("MOVE " + cond + " LT " + pos + " 0\n");				// if arr.length > 0 g
		String one = new String("TEMP " + ++glob_temp_cnt);
		this.code.append("MOVE " + one + " 1\n");
		this.code.append("MOVE " + cond + " MINUS " + one + " " + cond + "\n");
		this.code.append("CJUMP " + cond + " " + error + "\n");
		this.code.append("JUMP " + noerror + "\n");
		this.code.append(error + " NOOP\n");
		this.code.append("ERROR\n" + noerror + " NOOP\n");
		String temp_array = new String("TEMP " + ++glob_temp_cnt);
		this.code.append("MOVE " + temp_array + " " + array + "\n");			// temp_array = &array
		this.code.append("MOVE " + temp_array + " PLUS " + temp_array + " 4\n");
		String temp = new String("TEMP " + ++glob_temp_cnt);
		this.code.append("MOVE " + temp + " TIMES " + pos + " 4\n");
		this.code.append("MOVE " + temp_array + " PLUS " + temp_array + " " + temp + "\n");
		String ret = new String("TEMP " + ++glob_temp_cnt);
		this.code.append("HLOAD " + ret + " " + temp_array + " 0\n");
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "."
	* f2 -> "length"
	*/
	public BaseType visit(ArrayLength n, BaseType argu) throws Exception {
		String len = new String("TEMP " + ++glob_temp_cnt);
		String t = ((Variable_t) n.f0.accept(this, argu)).getType();
		this.code.append("HLOAD " + len + " " + t + " 0\n");
		return new Variable_t(len, null);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "."
	* f2 -> Identifier()
	* f3 -> "("
	* f4 -> ( ExpressionList() )?
	* f5 -> ")"
	*/
	public BaseType visit(MessageSend n, BaseType argu) throws Exception {
		Variable_t obj = (Variable_t) n.f0.accept(this, argu);	// obj.temp is type
        Variable_t func = (Variable_t) n.f2.accept(this, argu);
        String className = obj.getRegister();
        String objTemp = obj.getType();
        Class_t cl = st_.get(className);
        Method_t meth = cl.getMethod(func.getName());
        int offset = meth.meth_num - 1;
		String vtable_addr = new String("TEMP " + ++glob_temp_cnt);
		String thisTemp = new String("TEMP " + ++glob_temp_cnt);
		String methTemp = new String("TEMP " + ++glob_temp_cnt);
        this.code.append("MOVE " + thisTemp + " " + objTemp + "\n"); 	// load the address of vtable
        this.code.append("HLOAD " + vtable_addr + " " + thisTemp + " 0\n"); 	// load the address of vtable
        this.code.append("HLOAD " + methTemp + " " + vtable_addr + " " + (offset*4) + "\n");	// load the right method from vtable
        // add params to method call
		Method_t params = (Method_t) n.f4.accept(this, argu);
		String parStr = new String(" ");
        if (n.f4.present()) {														// if meth has params
	        for (int i = 0 ; i < params.method_params.size() ; i++) {				// for every par
	        	Variable_t var = ((Variable_t) params.method_params.get(i));
				String parTemp = new String("TEMP " + ++glob_temp_cnt);
	        	this.code.append("MOVE " + parTemp + " " + var.getType() + "\n");
	        	parStr += parTemp + " ";
	        }
	    }
		String ret = new String("TEMP " + ++glob_temp_cnt);
        this.code.append("MOVE " + ret + " CALL " + methTemp + "( " + thisTemp + parStr + ")\n");
        Variable_t v = new Variable_t(ret, null);
        v.var_temp = meth.getType();
		return v;
	}

 /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public BaseType visit(ExpressionList n, BaseType argu) throws Exception {
        Variable_t expr =  (Variable_t) n.f0.accept(this, argu); //na tsekarw th seira
        Method_t meth = (Method_t) n.f1.accept(this, argu);
        meth.method_params.addLast(expr);
        return meth;
    }

    /**
    * f0 -> ( ExpressionTerm() )*
    */
    public BaseType visit(ExpressionTail n, BaseType argu) throws Exception {
        Method_t meth = new Method_t(null, null);
        if (n.f0.present())                 // create a linked list of variables. (parameters list)
            for (int i = 0 ; i < n.f0.size() ; i++)
                meth.method_params.addLast( (Variable_t)n.f0.nodes.get(i).accept(this, argu) );
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
		String ret = "TEMP " + ++glob_temp_cnt;
		this.code.append("MOVE " + ret + " " + n.f0.toString() + "\n");
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> "true"
	*/
	public BaseType visit(TrueLiteral n, BaseType argu) throws Exception {
		String ret = new String("TEMP " + ++glob_temp_cnt);
		this.code.append(new String("MOVE " + ret + " 1\n"));
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> "false"
	*/
	public BaseType visit(FalseLiteral n, BaseType argu) throws Exception {
		String ret = new String("TEMP " + ++glob_temp_cnt);
		this.code.append(new String("MOVE " + ret + " 0\n"));
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
		Class_t cl = st_.get(argu.getName());
		// if argu is a class name
		if (cl != null) {
			Variable_t var = cl.classContainsVar(id);
			if (var != null) {								// and id is a field of that class
				Variable_t v = new Variable_t(var.getRegister(), id);
				v.var_temp = cl.getName();
				return v;
			} else {										// is a method
				Method_t meth = cl.getMethod(id);
				if (meth == null) { throw new Exception("something went wrong 1"); }
				return new Variable_t(null, id);
			}
		} else {											// if argu is a method name
			Method_t meth = (Method_t) argu;
			Variable_t var = meth.methContainsVar(id);
			if (var != null) {								// if a parameter or a local var
				Variable_t v = new Variable_t(var.getRegister(), id);
				v.var_temp = var.getType();
				return v;
			} else {										// a field of class
				cl = st_.get(meth.comesFrom.getName());
				if (cl == null) {
					throw new Exception("something went wrong 2");
				}
				var = cl.classContainsVar(id);
				if (var == null) {
					return new Variable_t(null, id);
				}
				String newTemp = "TEMP " + ++glob_temp_cnt;
				this.code.append("HLOAD " + newTemp + " TEMP 0 " + var.getVarNum()*4 + "\n");
				Variable_t v = new Variable_t(newTemp, id);
				v.var_temp = var.getType();
				return v;
			}
		}
	}

	/**
	* f0 -> "this"
	*/
	public BaseType visit(ThisExpression n, BaseType argu) throws Exception {
		Variable_t var = new Variable_t("TEMP 0", "this");
		Class_t cl = ((Method_t) argu).comesFrom;
		var.var_temp = cl.getName();
		return var;
	}

	/**
	* f0 -> "new"
	* f1 -> "int"
	* f2 -> "["
	* f3 -> Expression()
	* f4 -> "]"
	*/
	public BaseType visit(ArrayAllocationExpression n, BaseType argu) throws Exception {
		String lstart = L.new_label();
		String lend = L.new_label();
		String noerror = L.new_label();
		String expr = ((Variable_t) n.f3.accept(this, argu)).getType();
		String zero = new String("TEMP " + ++glob_temp_cnt);
		String cnt = new String("TEMP " + ++glob_temp_cnt);
		String cond = new String("TEMP " + ++glob_temp_cnt);
		String size = new String("TEMP " + ++glob_temp_cnt);
		String alloc_sz = new String("TEMP " + ++glob_temp_cnt);
		String array = new String("TEMP " + ++glob_temp_cnt);
		String array_addr = new String("TEMP " + ++glob_temp_cnt);
		this.code.append("MOVE " + cond + " LT " + expr + " 0\n"); 						// check if given length > 0
		this.code.append("CJUMP " + cond + " " + noerror + "\n");
		this.code.append("ERROR\n" + noerror + " NOOP\n");
		this.code.append("MOVE " + size + " PLUS " + expr + " 1\n"); 						// create room for arraylength
		this.code.append("MOVE " + alloc_sz + " TIMES " + size + " 4\n");					// *4 for bytes
		this.code.append("MOVE " + array + " HALLOCATE " + alloc_sz + "\n"); 				// allocate
		this.code.append("HSTORE " + array + " 0 " + expr + "\n");							// store array length in first position
		this.code.append("MOVE " + array_addr + " " + array + "\nMOVE " + cnt + " 4\n");	// keep array address and init a counter
		this.code.append(lstart + " NOOP\nMOVE " + cond + " LT " + cnt + " " + alloc_sz + "\n");
		this.code.append("CJUMP " + cond + " " + lend + "\n"); 							// if !cond goto end
		this.code.append("MOVE " + array + " PLUS " + array + " 4\n");						// &array++
		this.code.append("MOVE " + zero + " 0\n");
		this.code.append("HSTORE " + array + " 0 " + zero + "\n");
		this.code.append("MOVE " + cnt + " PLUS " + cnt + " 4\n");							// cnt++
		this.code.append("JUMP " + lstart + "\n" + lend + " NOOP\n");						// loop
		return new Variable_t(array_addr, null);
	}

	/**
	* f0 -> "new"
	* f1 -> Identifier()
	* f2 -> "("
	* f3 -> ")"
	*/
	public BaseType visit(AllocationExpression n, BaseType argu) throws Exception {
		String id = n.f1.accept(this, argu).getName();
        Class_t cl = st_.get(id);
		String t = new String("TEMP " + ++glob_temp_cnt);
		String vtable = new String("TEMP " + ++glob_temp_cnt);
		String vtable_addr = new String("TEMP " + ++glob_temp_cnt);
		String label = L.newClassLabel(cl.getName());
        this.code.append("MOVE " + t + " HALLOCATE " + (1+cl.var_cnt)*4 + "\n");
        this.code.append("MOVE " + vtable_addr + " " + label + "\n");
        this.code.append("HLOAD " + vtable + " " + vtable_addr + " 0\n");
        this.code.append("HSTORE " + t + " 0 " + vtable + "\n");
		String zero = new String("TEMP " + ++glob_temp_cnt);
        this.code.append("MOVE " + zero + " 0\n");
        for (int i = 1 ; i <= cl.var_cnt ; i++)
        	this.code.append("HSTORE " + t + " " + i*4 + " " + zero + "\n");
        this.code.append("\n");
        Variable_t var = new Variable_t(t, id);
        var.var_temp = id;
		return var;
	}

	/**
	* f0 -> "!"
	* f1 -> Clause()
	*/
	public BaseType visit(NotExpression n, BaseType argu) throws Exception {
		String ret = new String("TEMP " + ++glob_temp_cnt);
		String t = ((Variable_t) n.f1.accept(this, argu)).getType();
		String one = new String("TEMP " + ++glob_temp_cnt);
		this.code.append("MOVE " + one + " 1\n");
		this.code.append("MOVE " + ret + " MINUS " + one + " " + t + "\n");
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
