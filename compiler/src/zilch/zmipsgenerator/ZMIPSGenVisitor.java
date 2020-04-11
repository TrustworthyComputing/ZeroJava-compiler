package zmipsgenerator;

import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;
import symboltable.*;
import basetype.*;

public class ZMIPSGenVisitor extends GJDepthFirst<BaseType, BaseType> {

	private StringBuilder code_;
	private Label labels_;
	private Map<String, Class_t> st_;
	private List<String> vtables_;
	private int globals_;
	private int heap_;

    public ZMIPSGenVisitor(Map<String, Class_t> st, int globals) {
		this.labels_ = new Label();
        this.st_ = st;
        this.globals_ = globals;
		this.heap_ = 1000;
		vtables_ = new ArrayList<>();
	}

	public String getCode() {
		return code_.toString();
	}

	private void initVtables() {
		for (Map.Entry<String, Class_t> entry : st_.entrySet()) {
			Class_t cl = entry.getValue();
			if (cl.isMain()) {
				continue;
			}
			String vtable_label = labels_.newClassLabel(cl.getName());
			vtables_.add(vtable_label);
			String vtable_reg = new String("$r" + ++globals_);
			String reg = new String("$r" + ++globals_);
			this.code_.append("la " + vtable_reg + ", " + vtable_label + "\n");
			this.code_.append("move " + reg + ", " + reg + ", " + heap_ + "\t; " + cl.getNumMethods() * 4 + "\n");
			heap_ += cl.getNumMethods() * 4;
			this.code_.append("sw " + reg + ", 0(" + vtable_reg + ")\n");
			int i = 0;
			for (Map.Entry<String, Method_t> methods : cl.class_methods_map.entrySet()) {
				String newreg = new String("$r" + ++globals_);
				Method_t meth = methods.getValue();
				String meth_label = new String("__" + meth.getFromClass().getName() + "_" + meth.getName() + "__");
				this.code_.append("la " + newreg + ", " + meth_label  + "\n");
				this.code_.append("sw " + newreg + ", " + i*4  + "(" + reg + ")\n");
				i++;
			}
			this.code_.append("\n");
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
		this.code_.append("\n");
		for (String vtable : vtables_) {
			this.code_.append(vtable + "\n");
		}
		this.code_.append("\n");
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
		this.code_ = new StringBuilder();
		String id = n.f1.accept(this, argu).getName();
        Class_t mainclazz = st_.get(id);
        Method_t meth = mainclazz.getMethod("main");
        initVtables();
		n.f14.accept(this, meth);
		n.f15.accept(this, meth);
		this.code_.append("\n");
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
		if (meth.getFromClass() != null) { 												// is a variable of a function
			String newreg = new String("$r" + ++globals_);
			if ((meth = meth.getFromClass().getMethod(meth.getName())) != null) {		// if you found method
				meth.addRegToVar(varName, newreg);
			} else {
				throw new Exception("VarDeclaration Errror 1");
			}
			this.code_.append(new String("move " + newreg + ", " + newreg + ", 0\n"));
		} else {																	// is a var (field) of a class
			Class_t cl = st_.get(meth.getName());
			if (cl == null) {
				throw new Exception("VarDeclaration Errror 2");
			}
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
        this.code_.append("\n__" + ((Class_t) argu).getName()+ "_" + methName + "__\n");
        n.f7.accept(this, meth);
        n.f8.accept(this, meth);
        Variable_t retType = (Variable_t) n.f10.accept(this, meth);
        this.code_.append("move $v0, $v0, " + retType.getType() + "\n");
        this.code_.append("jr $ra\n");
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
				Class_t cl = st_.get(meth.getFromClass().getName());
				if (cl == null) {
					throw new Exception("something went wrong at AssignmentStatement 2");
				}
				var = cl.classContainsVar(id);
				// class field
				if (var != null) {
					this.code_.append("sw " + expr + ", " + var.getNum()*4 + "($r0)\n");
				}
				return null;
			}
			this.code_.append("move " +  var.getRegister() + ", " + var.getRegister() + ", " + expr +"\n");
		} else { // if a field of a class
			Class_t cl = st_.get(meth.getFromClass().getName());
			if (cl == null) {
				throw new Exception("something went wrong at AssignmentStatement 2");
			}
			var = cl.classContainsVar(id);
			// class field
			if (var != null) {
				this.code_.append("sw " + expr + ", " + var.getNum()*4 + "($r0)\n");
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
		String length = new String("$r" + ++globals_);
		String cond = new String("$r" + ++globals_);
		String error = labels_.new_label();
		String noerror = labels_.new_label();
		String array = ((Variable_t) n.f0.accept(this, argu)).getType();
		this.code_.append("lw " + length + ", 0(" + array + ")\n"); 			// load real size to length
		String pos = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.code_.append("move " + cond + " LT " + pos + " " + length + "\n");	// if pos < arr.length
		this.code_.append("CJUMP " + cond + " " + error + "\n");
		this.code_.append("move " + cond + " LT " + pos + " 0\n");				// if arr.length > 0 g
		String one = new String("$r" + ++globals_);
		this.code_.append("move " + one + " 1\n");
		this.code_.append("move " + cond + " minus " + one + " " + cond + "\n");
		this.code_.append("CJUMP " + cond + " " + error + "\n");
		this.code_.append("j " + noerror + "\n");
		this.code_.append(error + "\n");
		this.code_.append("ERROR\n" + noerror + "\n");
		String reg_array = new String("$r" + ++globals_);
		this.code_.append("move " + reg_array + " " + array + "\n");			// reg_array = &array
		this.code_.append("move " + reg_array + " add " + reg_array + " 4\n");
		String reg = new String("$r" + ++globals_);
		this.code_.append("move " + reg + " mult " + pos + " 4\n");
		this.code_.append("move " + reg_array + " add " + reg_array + " " + reg + "\n");
		String expr = ((Variable_t) n.f5.accept(this, argu)).getType();
		this.code_.append("sw " + expr + ", 0(" + reg_array + ")\n");
		return new Variable_t(reg_array, null);
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
		String elselabel = labels_.new_label();
		String endlabel = labels_.new_label();
		String cond = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.code_.append("CJUMP " + cond + " " + elselabel + "\n"); //if cond not true go to elselabel
		n.f4.accept(this, argu);
		this.code_.append("j " + endlabel + "\n" + elselabel + "\n");
		n.f6.accept(this, argu);
		this.code_.append(endlabel + "\n");
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
		String lstart = labels_.new_label();
		String lend = labels_.new_label();
		String cond = new String("$r" + ++globals_);
		this.code_.append(lstart + "\n");
		String expr = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.code_.append("move " + cond + " " + expr + "\n");
		this.code_.append("CJUMP " + cond + " " + lend + "\n");
		n.f4.accept(this, argu);
		this.code_.append("j " + lstart + "\n" + lend + "\n");
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
		this.code_.append("print " + t + ", " + t + ", " + t + "\n");
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
		String label = labels_.new_label();
		String ret = new String("$r" + ++globals_);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		this.code_.append(new String("move " + ret + " " + t1 + "\n" + "CJUMP " + t1 + " " + label + "\n"));
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.code_.append(new String("move " + ret + " " + t2 + "\n" + label + "\n"));
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "<"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(CompareExpression n, BaseType argu) throws Exception {
		String ret = new String("$r" + ++globals_);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.code_.append(new String("move " + ret + " LT " +  t1 + " " + t2 + "\n"));
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "+"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(PlusExpression n, BaseType argu) throws Exception {
		String ret = new String("$r" + ++globals_);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.code_.append(new String("add " + ret + ", " + t1 + ", " + t2 + "\n"));
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "-"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(MinusExpression n, BaseType argu) throws Exception {
		String ret = new String("$r" + ++globals_);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.code_.append(new String("sub " + ret + ", " +  t1 + ", " + t2 + "\n"));
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "*"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(TimesExpression n, BaseType argu) throws Exception {
		String ret = new String("$r" + ++globals_);
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getType();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.code_.append(new String("mult " + ret + ", " + t1 + ", " + t2 + "\n"));
		return new Variable_t(ret, null);

	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "["
	* f2 -> PrimaryExpression()
	* f3 -> "]"
	*/
	public BaseType visit(ArrayLookup n, BaseType argu) throws Exception {
		String length = new String("$r" + ++globals_);
		String cond = new String("$r" + ++globals_);
		String error = labels_.new_label();
		String noerror = labels_.new_label();
		String array = ((Variable_t) n.f0.accept(this, argu)).getType();
		this.code_.append("lw " + length + ", 0(" + array + ")\n"); 			// load real size to length
		String pos = ((Variable_t) n.f2.accept(this, argu)).getType();
		this.code_.append("move " + cond + " LT " + pos + " " + length + "\n");	// if pos < arr.length
		this.code_.append("CJUMP " + cond + " " + error + "\n");
		this.code_.append("move " + cond + " LT " + pos + " 0\n");				// if arr.length > 0 g
		String one = new String("$r" + ++globals_);
		this.code_.append("move " + one + " 1\n");
		this.code_.append("move " + cond + " minus " + one + " " + cond + "\n");
		this.code_.append("CJUMP " + cond + " " + error + "\n");
		this.code_.append("j " + noerror + "\n");
		this.code_.append(error + "\n");
		this.code_.append("ERROR\n" + noerror + "\n");
		String reg_array = new String("$r" + ++globals_);
		this.code_.append("move " + reg_array + " " + array + "\n");			// reg_array = &array
		this.code_.append("move " + reg_array + " add " + reg_array + " 4\n");
		String reg = new String("$r" + ++globals_);
		this.code_.append("move " + reg + " mult " + pos + " 4\n");
		this.code_.append("move " + reg_array + " add " + reg_array + " " + reg + "\n");
		String ret = new String("$r" + ++globals_);
		this.code_.append("lw " + ret + ", 0(" + reg_array + ")\n");
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "."
	* f2 -> "length"
	*/
	public BaseType visit(ArrayLength n, BaseType argu) throws Exception {
		String len = new String("$r" + ++globals_);
		String t = ((Variable_t) n.f0.accept(this, argu)).getType();
		this.code_.append("lw " + len + ", 0(" + t + ")\n");
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
		Variable_t obj = (Variable_t) n.f0.accept(this, argu);	// obj.reg is type_
        Variable_t func = (Variable_t) n.f2.accept(this, argu);
        String className = obj.getRegister();
        String objreg = obj.getType();
        Class_t cl = st_.get(className);
        Method_t meth = cl.getMethod(func.getName());
        int offset = meth.getMethNum() - 1;
		String vtable_addr = new String("$r" + ++globals_);
		String thisreg = new String("$r" + ++globals_);
		String methreg = new String("$r" + ++globals_);
        this.code_.append("move " + thisreg + ", " + thisreg + ", " + objreg + "\n"); 	// load the address of vtable
        this.code_.append("lw " + vtable_addr + ", 0(" + thisreg + ")\n"); 	// load the address of vtable
        this.code_.append("lw " + methreg + ", " + (offset * 4) + "(" + vtable_addr + ")\n");	// load the right method from vtable
        // add params to method call
        if (n.f4.present()) {														// if meth has params
			Method_t params = (Method_t) n.f4.accept(this, argu);
			this.code_.append("move $a0, $a0, " + thisreg + " ; this object\n");
	        for (int i = 0 ; i < params.method_params.size() ; i++) {				// for every par
				String parreg = new String("$a" + (i+1));
				this.code_.append("move " + parreg + ", " + parreg + ", " + ((Variable_t) params.method_params.get(i)).getType() + "\n");
			}
	    }
		String return_address = labels_.new_label();
        this.code_.append("la $ra, $ra, " + return_address + "\n");
        this.code_.append("jr " + methreg + "\n");
		this.code_.append(return_address + "\n");
		String ret = new String("$r" + ++globals_);
        this.code_.append("move " + ret + ", " + ret + ", $v0\n");
        Variable_t v = new Variable_t(ret, null);
        v.setRegister(meth.getType());
		return v;
	}

  	/**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public BaseType visit(ExpressionList n, BaseType argu) throws Exception {
        Variable_t expr =  (Variable_t) n.f0.accept(this, argu); //na tsekarw th seira
        Method_t meth = (Method_t) n.f1.accept(this, argu);
        meth.method_params.add(expr);
        return meth;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    public BaseType visit(ExpressionTail n, BaseType argu) throws Exception {
        Method_t meth = new Method_t(null, null);
        if (n.f0.present())                 // create a linked list of variables. (parameters list)
            for (int i = 0 ; i < n.f0.size() ; i++)
                meth.method_params.add( (Variable_t)n.f0.nodes.get(i).accept(this, argu) );
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
		String ret = "$r" + ++globals_;
		this.code_.append("move " + ret + ", " + ret + ", " + n.f0.toString() + "\n");
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> "true"
	*/
	public BaseType visit(TrueLiteral n, BaseType argu) throws Exception {
		String ret = new String("$r" + ++globals_);
		this.code_.append(new String("move " + ret + ", " + ret + " 1\n"));
		return new Variable_t(ret, null);
	}

	/**
	* f0 -> "false"
	*/
	public BaseType visit(FalseLiteral n, BaseType argu) throws Exception {
		String ret = new String("$r" + ++globals_);
		this.code_.append(new String("move " + ret + ", " + ret + " 0\n"));
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
				v.setRegister(cl.getName());
				return v;
			} else {										// is a method
				Method_t meth = cl.getMethod(id);
				if (meth == null) { throw new Exception("something went wrong 1"); }
				return new Variable_t(null, id, null);
			}
		} else {											// if argu is a method name
			Method_t meth = (Method_t) argu;
			Variable_t var = meth.methContainsVar(id);
			if (var != null) {								// if a parameter or a local var
				Variable_t v = new Variable_t(var.getRegister(), id);
				v.setRegister(var.getType());
				return v;
			} else {										// a field of class
				cl = st_.get(meth.getFromClass().getName());
				if (cl == null) {
					throw new Exception("something went wrong 2");
				}
				var = cl.classContainsVar(id);
				if (var == null) {
					return new Variable_t(null, id);
				}
				String newreg = "$r" + ++globals_;
				this.code_.append("lw " + newreg + ", " + var.getNum()*4 + "($r0)\n");
				Variable_t v = new Variable_t(newreg, id);
				v.setRegister(var.getType());
				return v;
			}
		}
	}

	/**
	* f0 -> "this"
	*/
	public BaseType visit(ThisExpression n, BaseType argu) throws Exception {
		Variable_t var = new Variable_t("$r0", "this");
		Class_t cl = ((Method_t) argu).getFromClass();
		var.setRegister(cl.getName());
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
		String lstart = labels_.new_label();
		String lend = labels_.new_label();
		String noerror = labels_.new_label();
		String expr = ((Variable_t) n.f3.accept(this, argu)).getType();
		String zero = new String("$r" + ++globals_);
		String cnt = new String("$r" + ++globals_);
		String cond = new String("$r" + ++globals_);
		String size = new String("$r" + ++globals_);
		String alloc_sz = new String("$r" + ++globals_);
		String array = new String("$r" + ++globals_);
		String array_addr = new String("$r" + ++globals_);
		this.code_.append("move " + cond + " LT " + expr + " 0\n"); 						// check if given length > 0
		this.code_.append("CJUMP " + cond + " " + noerror + "\n");
		this.code_.append("ERROR\n" + noerror + "\n");
		this.code_.append("move " + size + " add " + expr + " 1\n"); 						// create room for arraylength
		this.code_.append("move " + alloc_sz + " mult " + size + " 4\n");					// *4 for bytes
		this.code_.append("move " + array + " HALLOCATE " + alloc_sz + "\n"); 				// allocate
		// this.code_.append("move " + array + ", " + array + ", " + heap_ + "\n");
		// heap_ += alloc_sz;
		this.code_.append("sw " + expr + ", 0(" + array + ")\n");							// store array length in first position
		this.code_.append("move " + array_addr + " " + array + "\nmove " + cnt + " 4\n");	// keep array address and init a counter
		this.code_.append(lstart + "\nmove " + cond + " LT " + cnt + " " + alloc_sz + "\n");
		this.code_.append("CJUMP " + cond + " " + lend + "\n"); 							// if !cond goto end
		this.code_.append("move " + array + " add " + array + " 4\n");						// &array++
		this.code_.append("move " + zero + ", " + zero + ", 0\n");
		this.code_.append("sw " + zero + ", 0(" + array + ")\n");
		this.code_.append("move " + cnt + " add " + cnt + " 4\n");							// cnt++
		this.code_.append("j " + lstart + "\n" + lend + "\n");						// loop
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
		String t = new String("$r" + ++globals_);
		String vtable = new String("$r" + ++globals_);
		String vtable_addr = new String("$r" + ++globals_);
		String label = labels_.newClassLabel(cl.getName());
		this.code_.append("move " + t + ", " + t + ", " + heap_ + "\t; " + (cl.getNumVars() + 1) * 4 + "\n");
		heap_ += (cl.getNumVars() + 1) * 4;

        this.code_.append("la " + vtable_addr + " " + label + "\n");
        this.code_.append("lw " + vtable + ", 0(" + vtable_addr + ")\n");
        this.code_.append("sw " + vtable + ", 0(" + t + ")\n");
		String zero = new String("$r" + ++globals_);
        this.code_.append("move " + zero + " 0\n");
        for (int i = 1 ; i <= cl.getNumVars() ; i++)
        	this.code_.append("sw " + zero + ", " + i*4 + "(" + t + ")\n");
        this.code_.append("\n");
        Variable_t var = new Variable_t(t, id);
        var.setRegister(id);
		return var;
	}

	/**
	* f0 -> "!"
	* f1 -> Clause()
	*/
	public BaseType visit(NotExpression n, BaseType argu) throws Exception {
		String ret = new String("$r" + ++globals_);
		String t = ((Variable_t) n.f1.accept(this, argu)).getType();
		String one = new String("$r" + ++globals_);
		this.code_.append("move " + one + ", " + one + ", 1\n");
		this.code_.append("move " + ret + " minus " + one + " " + t + "\n");
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
