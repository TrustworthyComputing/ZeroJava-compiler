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
	private int obj_heap_;
	private boolean use_arrays_heap_;
	private final int arrays_heap_ = 2000;

    public ZMIPSGenVisitor(Map<String, Class_t> st, int globals) {
		this.labels_ = new Label();
        this.st_ = st;
        this.globals_ = globals;
		this.obj_heap_ = 1000;
		this.use_arrays_heap_ = false;
		vtables_ = new ArrayList<>();
	}

	public String getCode() {
		return code_.toString();
	}

	public String newRegister() {
		return new String("$r" + ++globals_);
	}

	private void initVtables() {
		for (Map.Entry<String, Class_t> entry : st_.entrySet()) {
			Class_t cl = entry.getValue();
			if (cl.isMain()) {
				continue;
			}
			String vtable_label = labels_.newClassLabel(cl.getName());
			vtables_.add(vtable_label);
			String vtable_reg = newRegister();
			String reg = newRegister();
			this.code_.append("la " + vtable_reg + ", " + vtable_label + "\n");
			this.code_.append("move " + reg + ", " + obj_heap_ + "\t; " + cl.getNumMethods() + "\n");
			obj_heap_ += cl.getNumMethods();
			this.code_.append("sw " + reg + ", 0(" + vtable_reg + ")\n");
			int i = 0;
			for (Map.Entry<String, Method_t> methods : cl.class_methods_map.entrySet()) {
				String newreg = newRegister();
				Method_t meth = methods.getValue();
				String meth_label = new String("__" + meth.getFromClass().getName() + "_" + meth.getName() + "__");
				this.code_.append("la " + newreg + ", " + meth_label  + "\n");
				this.code_.append("sw " + newreg + ", " + i  + "(" + reg + ")\n");
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
		if (vtables_.size() > 0) {
			this.code_.append("\n");
		}
		for (String vtable : vtables_) {
			this.code_.append(vtable + "\n");
		}
		this.code_.append(labels_.getErrorCode());
		if (vtables_.size() > 0) {
			this.code_.append("\n");
		}
		if (this.use_arrays_heap_) {
			this.code_.insert(0, "move $hp, " + arrays_heap_ + "\n\n");
		}
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
			String newreg = newRegister();
			if ((meth = meth.getFromClass().getMethod(meth.getName())) != null) {		// if you found method
				meth.addRegToVar(varName, newreg);
			} else {
				throw new Exception("VarDeclaration Errror 1");
			}
			this.code_.append("move " + newreg + ", 0\n");
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
        this.code_.append("move $v0, " + retType.getRegister() + "\n");
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
		String expr = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		// if a local var
		Method_t meth = (Method_t) argu;
		if (meth != null) {
			Variable_t var = meth.methContainsVar(id);
			if (var == null) { // didnt find the var in method, so its a field of the class
				Class_t cl = st_.get(meth.getFromClass().getName());
				if (cl == null) {
					throw new Exception("something went wrong at AssignmentStatement 2");
				}
				var = cl.classContainsVar(id);
				// class field
				if (var != null) {
					this.code_.append("sw " + expr + ", " + var.getNum() + "($r0)\n");
				}
				return null;
			}
			this.code_.append("move " +  var.getRegister() + ", " + expr +"\n");
		} else { // if a field of a class
			Class_t cl = st_.get(meth.getFromClass().getName());
			if (cl == null) {
				throw new Exception("something went wrong at AssignmentStatement 2");
			}
			Variable_t var = cl.classContainsVar(id);
			// class field
			if (var != null) {
				this.code_.append("sw " + expr + ", " + var.getNum() + "($r0)\n");
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
		this.use_arrays_heap_ = true;
		String length = newRegister();
		String error_label = labels_.getErrorLabel();
		String array = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		this.code_.append("lw " + length + ", 0(" + array + ")\n"); // load length
		String idx = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		// if idx >= arr.length goto error
		this.code_.append("cmpg " + length + ", " + idx + "\n"); // length > idx
		this.code_.append("cnjmp " + error_label + "\n");
		// if idx <= 0 goto error
		this.code_.append("cmpge $zero, " + idx + "\n"); // 0 >= idx
		this.code_.append("cjmp " + error_label + "\n");
		// skip length
		String temp_array = newRegister();
		this.code_.append("add " + temp_array + ", " + array + ", 1\n");
		String expr = ((Variable_t) n.f5.accept(this, argu)).getRegister();
		this.code_.append("sw " + expr + ", " + idx + "(" + temp_array + ")\n");
		return new Variable_t(null, null, temp_array);
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
		String elselabel = labels_.newLabel();
		String endlabel = labels_.newLabel();
		String cond = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.code_.append("cmpg " + cond + ", 0\n");
		this.code_.append("cnjmp " + elselabel + "\n");
		n.f4.accept(this, argu);
		this.code_.append("j " + endlabel + "\n");
		this.code_.append(elselabel + "\n");
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
		String start_label = labels_.newLabel();
		String end_label = labels_.newLabel();
		this.code_.append(start_label + "\n");
		String expr = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.code_.append("cmpg " + expr + ", 0\n");
		this.code_.append("cnjmp " + end_label + "\n");
		n.f4.accept(this, argu);
		this.code_.append("j " + start_label + "\n");
		this.code_.append(end_label + "\n");
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
		String t = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.code_.append("print " + t + "\n");
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
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		String istrue1 = labels_.newLabel();
		String istrue2 = labels_.newLabel();
		String ret = newRegister();
		this.code_.append("move " + ret + ", 1\n");
		this.code_.append("cmpg " + t1 + ", 0\n");
		this.code_.append("cjmp " + istrue1 + "\n");
		this.code_.append("move " + ret + ", 0\n");
		this.code_.append("jmp " + istrue2 + "\n"); 	// early termination
		this.code_.append(istrue1 + "\n");
		this.code_.append("cmpg " + t2 + ", 0\n");
		this.code_.append("cjmp " + istrue2 + "\n");
		this.code_.append("move " + ret + ", 0\n");
		this.code_.append(istrue2 + "\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "<"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(CompareExpression n, BaseType argu) throws Exception {
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		String ret = newRegister();
		String lt = labels_.newLabel();
		this.code_.append("cmpg " + t2 + ", " + t1 + "\n");
		this.code_.append("cnjmp " + lt + "\n");
		this.code_.append("move " + ret + ", 1\n");
		this.code_.append(lt + "\n");
		return new Variable_t(null, null, ret);
	}

	// t1 < t2 --> LT t1 t2 --> cmpg t2 t1

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "+"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(PlusExpression n, BaseType argu) throws Exception {
		String ret = newRegister();
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.code_.append("add " + ret + ", " + t1 + ", " + t2 + "\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "-"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(MinusExpression n, BaseType argu) throws Exception {
		String ret = newRegister();
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.code_.append("sub " + ret + ", " +  t1 + ", " + t2 + "\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "*"
	* f2 -> PrimaryExpression()
	*/
	public BaseType visit(TimesExpression n, BaseType argu) throws Exception {
		String ret = newRegister();
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.code_.append("mult " + ret + ", " + t1 + ", " + t2 + "\n");
		return new Variable_t(null, null, ret);

	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "["
	* f2 -> PrimaryExpression()
	* f3 -> "]"
	*/
	public BaseType visit(ArrayLookup n, BaseType argu) throws Exception {
		this.use_arrays_heap_ = true;
		String length = newRegister();
		String error_label = labels_.getErrorLabel();
		String array = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		// load length
		this.code_.append("lw " + length + ", 0(" + array + ")\n");
		String idx = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		// if idx >= arr.length goto error
		this.code_.append("cmpg " + length + ", " + idx + "\n"); // length > idx
		this.code_.append("cnjmp " + error_label + "\n");
		// if idx <= 0 goto error
		this.code_.append("cmpge $zero, " + idx + "\n"); // 0 >= idx
		this.code_.append("cjmp " + error_label + "\n");
		// skip length
		String temp_array = newRegister();
		String ret = newRegister();
		this.code_.append("add " + temp_array + ", " + array + ", 1\n");
		this.code_.append("lw " + ret + "," + idx + "(" + temp_array + ")\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "."
	* f2 -> "length"
	*/
	public BaseType visit(ArrayLength n, BaseType argu) throws Exception {
		this.use_arrays_heap_ = true;
		String len = newRegister();
		String t = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		this.code_.append("lw " + len + ", 0(" + t + ")\t ; load array length\n");
		return new Variable_t(null, null, len);
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
		Variable_t obj = (Variable_t) n.f0.accept(this, argu);
        Variable_t func = (Variable_t) n.f2.accept(this, argu);
        String objreg = obj.getRegister();
        Class_t cl = st_.get(obj.getType());
		Method_t meth = cl.getMethod(func.getName());
        int offset = meth.getMethNum() - 1;
		String vtable_addr = newRegister();
		String thisreg = newRegister();
		String methreg = newRegister();
        this.code_.append("move " + thisreg + ", " + objreg + "\n"); 	// load the address of vtable
        this.code_.append("lw " + vtable_addr + ", 0(" + thisreg + ")\n"); 	// load the address of vtable
        this.code_.append("lw " + methreg + ", " + offset + "(" + vtable_addr + ")\n");	// load the right method from vtable
        // add params to method call
        if (n.f4.present()) {														// if meth has params
			Method_t params = (Method_t) n.f4.accept(this, argu);
			this.code_.append("move $a0, " + thisreg + " ; this object\n");
	        for (int i = 0 ; i < params.method_params.size() ; i++) {				// for every par
				String parreg = new String("$a" + (i+1));
				this.code_.append("move " + parreg + ", " + ((Variable_t) params.method_params.get(i)).getRegister() + "\n");
			}
	    }
		String return_address = labels_.newLabel();
        this.code_.append("la $ra, " + return_address + "\n");
        this.code_.append("jr " + methreg + "\n");
		this.code_.append(return_address + "\n");
		String ret = newRegister();
        this.code_.append("move " + ret + ", $v0\n");
		return new Variable_t(meth.getType(), null, ret);
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
		// create a linked list of variables. (parameters list)
        if (n.f0.present()) {
			for (int i = 0 ; i < n.f0.size() ; i++) {
				meth.method_params.add( (Variable_t)n.f0.nodes.get(i).accept(this, argu) );
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
		this.code_.append("move " + ret + ", " + n.f0.toString() + "\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> "true"
	*/
	public BaseType visit(TrueLiteral n, BaseType argu) throws Exception {
		String ret = newRegister();
		this.code_.append("move " + ret + ", 1\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> "false"
	*/
	public BaseType visit(FalseLiteral n, BaseType argu) throws Exception {
		String ret = newRegister();
		this.code_.append("move " + ret + ", 0\n");
		return new Variable_t(null, null, ret);
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
				return new Variable_t(cl.getName(), id, var.getRegister());
			} else {										// is a method
				Method_t meth = cl.getMethod(id);
				if (meth == null) { throw new Exception("something went wrong 1"); }
				return new Variable_t(null, id, null);
			}
		} else {											// if argu is a method name
			Method_t meth = (Method_t) argu;
			Variable_t var = meth.methContainsVar(id);
			if (var != null) {								// if a parameter or a local var
				return new Variable_t(var.getType(), id, var.getRegister());
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
				this.code_.append("lw " + newreg + ", " + var.getNum() + "($r0)\n");
				return new Variable_t(var.getType(), id, newreg);
			}
		}
	}

	/**
	 * f0 -> "this"
	 */
	public BaseType visit(ThisExpression n, BaseType argu) throws Exception {
		Class_t cl = ((Method_t) argu).getFromClass();
		return new Variable_t(cl.getName(), "this", "$r0");
	}

	/**
	 * f0 -> "new"
	 * f1 -> "int"
	 * f2 -> "["
	 * f3 -> Expression()
	 * f4 -> "]"
	 */
	public BaseType visit(ArrayAllocationExpression n, BaseType argu) throws Exception {
		this.use_arrays_heap_ = true;
		String error_label = labels_.getErrorLabel();
		String len = ((Variable_t) n.f3.accept(this, argu)).getRegister();
		String array = newRegister();
		// check if given length > 0
		this.code_.append("cmpg " + len + ", 0\t\t\t\t; Check if length is > 0\n");
		this.code_.append("cnjmp " + error_label + "\n");
		// store array length in first position
		this.code_.append("move " + array + ", $hp\n");
		this.code_.append("sw " + len + ", 0(" + array + ")\n");
		// increase heap pointer
		this.code_.append("add " + len + ", " + len + ", 1\n");
		this.code_.append("add $hp, $hp, " + len + "\n");
		return new Variable_t(null, null, array);
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
		String t = newRegister();
		String vtable = newRegister();
		String vtable_addr = newRegister();
		String label = labels_.newClassLabel(cl.getName());
		this.code_.append("move " + t + ", " + obj_heap_ + "\t; " + (cl.getNumVars() + 1) + "\n");
		obj_heap_ += (cl.getNumVars() + 1);
        this.code_.append("la " + vtable_addr + ", " + label + "\n");
        this.code_.append("lw " + vtable + ", 0(" + vtable_addr + ")\n");
        this.code_.append("sw " + vtable + ", 0(" + t + ")\n");
        for (int i = 1 ; i <= cl.getNumVars() ; i++) {
			this.code_.append("sw $zero, " + i + "(" + t + ")\n");
		}
        this.code_.append("\n");
		return new Variable_t(id, id, t);
	}

	/**
	 * f0 -> "!"
	 * f1 -> Clause()
	 */
	public BaseType visit(NotExpression n, BaseType argu) throws Exception {
		String ret = newRegister();
		String t = ((Variable_t) n.f1.accept(this, argu)).getRegister();
		this.code_.append("move " + ret + ", 1\n");
		this.code_.append("sub " + ret + ", " + ret + ", " + t + "\n");
		return new Variable_t(null, null, ret);
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
