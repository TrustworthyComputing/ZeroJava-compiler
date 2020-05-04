package org.twc.zerojavacompiler.zmipsgenerator;

import org.twc.zerojavacompiler.zerojavasyntaxtree.*;
import org.twc.zerojavacompiler.zerojavavisitor.GJDepthFirst;
import org.twc.zerojavacompiler.basetype.*;
import java.util.*;

public class ZMIPSGenVisitor extends GJDepthFirst<Base_t, Base_t> {

	private StringBuilder asm_;
	private Label labels_;
	private Map<String, Class_t> st_;
	private int globals_;
	private int sp_;
	private int hp_;
	private final int INIT_STACK_OFFSET_ = 100;
	private final int INIT_HEAP_OFFSET_ = 0;

    public ZMIPSGenVisitor(Map<String, Class_t> st, int globals) {
		this.labels_ = new Label();
        this.st_ = st;
        this.globals_ = globals;
		this.sp_ = INIT_STACK_OFFSET_;
		this.hp_ = INIT_HEAP_OFFSET_;
	}

	public String getASM() {
		return asm_.toString();
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
			String vtable_reg = newRegister();
			this.asm_.append("move " + vtable_reg + ", " + hp_ + "\t\t\t\t# " + cl.getName() + " vTable ("+ cl.getNumMethods() + " methods)\n");
			cl.setVTableAddress(hp_);
			hp_ += cl.getNumMethods();
			int i = 0;
			for (Map.Entry<String, Method_t> methods : cl.class_methods_map.entrySet()) {
				String newreg = newRegister();
				Method_t meth = methods.getValue();
				String meth_label = new String("__" + meth.getFromClass().getName() + "_" + meth.getName() + "__");
				this.asm_.append("la " + newreg + ", " + meth_label  + "\n");
				this.asm_.append("sw " + newreg + ", " + i  + "(" + vtable_reg + ")\n");
				i++;
			}
			this.asm_.append("\n");
    	}
	}

	/**
	* f0 -> MainClass()
	* f1 -> ( TypeDeclaration() )*
	* f2 -> <EOF>
	*/
	public Base_t visit(Goal n, Base_t argu) throws Exception {
		n.f0.accept(this, argu);
		n.f1.accept(this, argu);
		n.f2.accept(this, argu);
		this.asm_.append(labels_.getErrorCode());
		this.asm_.insert(0, "move $hp, " + hp_ + "\t\t\t\t# initialize heap pointer\n\n");
		this.asm_.insert(0, "move $sp, " + sp_ + "\t\t\t# initialize stack pointer\n");
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
	public Base_t visit(MainClass n, Base_t argu) throws Exception {
		this.asm_ = new StringBuilder();
		String id = n.f1.accept(this, argu).getName();
        Class_t mainclazz = st_.get(id);
        Method_t meth = mainclazz.getMethod("main");
        initVtables();
		n.f14.accept(this, meth);
		n.f15.accept(this, meth);
		// this.asm_.append("\n");
		return null;
	}

	/**
	* f0 -> ClassDeclaration()
	*       | ClassExtendsDeclaration()
	*/
	public Base_t visit(TypeDeclaration n, Base_t argu) throws Exception {
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
	public Base_t visit(ClassDeclaration n, Base_t argu) throws Exception {
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
	public Base_t visit(ClassExtendsDeclaration n, Base_t argu) throws Exception {
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
	public Base_t visit(VarDeclaration n, Base_t argu) throws Exception {
		Method_t meth = (Method_t) argu;
		String varName = n.f1.f0.toString();
		if (meth.getFromClass() != null) { 												// is a variable of a function
			String newreg = newRegister();
			if ((meth = meth.getFromClass().getMethod(meth.getName())) != null) {		// if you found method
				meth.addRegToVar(varName, newreg);
			} else {
				throw new Exception("VarDeclaration Errror 1");
			}
			this.asm_.append("move " + newreg + ", 0\n");
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
	public Base_t visit(MethodDeclaration n, Base_t argu) throws Exception {
		String methName = n.f2.accept(this, argu).getName();
        Method_t meth = ((Class_t) argu).getMethod(methName);
        this.asm_.append("\n__" + ((Class_t) argu).getName()+ "_" + methName + "__\n");
        n.f7.accept(this, meth);
        n.f8.accept(this, meth);
        Variable_t retType = (Variable_t) n.f10.accept(this, meth);
        this.asm_.append("move $v0, " + retType.getRegister() + "\n");

		// load $ra from the stack frame
		this.asm_.append("lw $ra, 0($sp)\t\t\t# load $ra from stack\n");
		this.asm_.append("add $sp, $sp, 1\n");

        this.asm_.append("jr $ra\n");
        return null;
	}

	/**
	* f0 -> FormalParameter()
	* f1 -> FormalParameterTail()
	*/
	public Base_t visit(FormalParameterList n, Base_t argu) throws Exception {
		n.f0.accept(this, argu);
		n.f1.accept(this, argu);
		return null;
	}

	/**
	* f0 -> Type()
	* f1 -> Identifier()
	*/
	public Base_t visit(FormalParameter n, Base_t argu) throws Exception {
		n.f0.accept(this, argu);
		n.f1.accept(this, argu);
		return null;
	}

	/**
	* f0 -> ( FormalParameterTerm() )*
	*/
	public Base_t visit(FormalParameterTail n, Base_t argu) throws Exception {
		return n.f0.accept(this, argu);
	}

	/**
	* f0 -> ","
	* f1 -> FormalParameter()
	*/
	public Base_t visit(FormalParameterTerm n, Base_t argu) throws Exception {
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
	public Base_t visit(Type n, Base_t argu) throws Exception {
		return n.f0.accept(this, argu);
	}

	/**
	* f0 -> "int"
	* f1 -> "["
	* f2 -> "]"
	*/
	public Base_t visit(ArrayType n, Base_t argu) throws Exception {
		n.f0.accept(this, argu);
		n.f1.accept(this, argu);
		n.f2.accept(this, argu);
		return null;
	}

	/**
	* f0 -> "boolean"
	*/
	public Base_t visit(BooleanType n, Base_t argu) throws Exception {
		return n.f0.accept(this, argu);
	}

	/**
	* f0 -> "int"
	*/
	public Base_t visit(IntegerType n, Base_t argu) throws Exception {
		return n.f0.accept(this, argu);
	}

	/**
     * f0 -> Block()
     *       | AssignmentStatement()
     *       | CompoundPlusAssignmentStatement()
     *       | CompoundMinusAssignmentStatement()
     *       | CompoundTimesAssignmentStatement()
     *       | CompoundDivAssignmentStatement()
     *       | CompoundModAssignmentStatement()
     *       | CompoundShiftLeftAssignmentStatement()
     *       | CompounShiftRightAssignmentStatement()
     *       | CompoundBinaryAndAssignmentStatement()
     *       | CompoundBinaryOrAssignmentStatement()
     *       | CompoundBinaryXorAssignmentStatement()
     *       | ArrayAssignmentStatement()
     *       | IfStatement()
     *       | WhileStatement()
     *       | PrintStatement()
     *       | AnswerStatement()
     */
	public Base_t visit(Statement n, Base_t argu) throws Exception {
		return n.f0.accept(this, argu);
	}

	/**
	* f0 -> "{"
	* f1 -> ( Statement() )*
	* f2 -> "}"
	*/
	public Base_t visit(Block n, Base_t argu) throws Exception {
		n.f1.accept(this, argu);
		return null;
	}

	/**
	* f0 -> Identifier()
	* f1 -> "="
	* f2 -> Expression()
	* f3 -> ";"
	*/
	public Base_t visit(AssignmentStatement n, Base_t argu) throws Exception {
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
					this.asm_.append("sw " + expr + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
				}
				return null;
			}
			this.asm_.append("move " +  var.getRegister() + ", " + expr +"\n");
		} else { // if a field of a class
			Class_t cl = st_.get(meth.getFromClass().getName());
			if (cl == null) {
				throw new Exception("something went wrong at AssignmentStatement 2");
			}
			Variable_t var = cl.classContainsVar(id);
			// class field
			if (var != null) {
				this.asm_.append("sw " + expr + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
			}
		}
		return null;
	}

	/**
	* f0 -> Identifier()
	* f1 -> "++"
	* f2 -> ";"
	*/
	public Base_t visit(IncrementAssignmentStatement n, Base_t argu) throws Exception {
		String id = n.f0.accept(this, argu).getName();
		// if a local var
		Method_t meth = (Method_t) argu;
		if (meth != null) {
			Variable_t var = meth.methContainsVar(id);
			if (var == null) { // didnt find the var in method, so its a field of the class
				Class_t cl = st_.get(meth.getFromClass().getName());
				if (cl == null) {
					throw new Exception("something went wrong at IncrementAssignmentStatement 1");
				}
				var = cl.classContainsVar(id);
				// class field
				if (var != null) {
					String temp = newRegister();
					this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
					this.asm_.append("add " + temp + ", " + temp + ", 1\t\t\t# increment field\n");
					this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
				}
				return null;
			}
			this.asm_.append("add " + var.getRegister() + ", " + var.getRegister() + ", 1\n");
		} else { // if a field of a class
			Class_t cl = st_.get(meth.getFromClass().getName());
			if (cl == null) {
				throw new Exception("something went wrong at IncrementAssignmentStatement 2");
			}
			Variable_t var = cl.classContainsVar(id);
			// class field
			if (var != null) {
				String temp = newRegister();
				this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
				this.asm_.append("add " + temp + ", " + temp + ", 1\t\t\t# increment field\n");
				this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
			}
		}
		return null;
	}

	/**
	* f0 -> Identifier()
	* f1 -> "--"
	* f2 -> ";"
	*/
	public Base_t visit(DecrementAssignmentStatement n, Base_t argu) throws Exception {
		String id = n.f0.accept(this, argu).getName();
		// if a local var
		Method_t meth = (Method_t) argu;
		if (meth != null) {
			Variable_t var = meth.methContainsVar(id);
			if (var == null) { // didnt find the var in method, so its a field of the class
				Class_t cl = st_.get(meth.getFromClass().getName());
				if (cl == null) {
					throw new Exception("something went wrong at DecrementAssignmentStatement 1");
				}
				var = cl.classContainsVar(id);
				// class field
				if (var != null) {
					String temp = newRegister();
					this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
					this.asm_.append("sub " + temp + ", " + temp + ", 1\t\t\t# increment field\n");
					this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
				}
				return null;
			}
			this.asm_.append("sub " + var.getRegister() + ", " + var.getRegister() + ", 1\n");
		} else { // if a field of a class
			Class_t cl = st_.get(meth.getFromClass().getName());
			if (cl == null) {
				throw new Exception("something went wrong at DecrementAssignmentStatement 2");
			}
			Variable_t var = cl.classContainsVar(id);
			// class field
			if (var != null) {
				String temp = newRegister();
				this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
				this.asm_.append("sub " + temp + ", " + temp + ", 1\t\t\t# increment field\n");
				this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
			}
		}
		return null;
	}

	/**
	* f0 -> Identifier()
	* f1 -> "+="
	* f2 -> Expression()
	* f3 -> ";"
	*/
	public Base_t visit(CompoundPlusAssignmentStatement n, Base_t argu) throws Exception {
		String id = n.f0.accept(this, argu).getName();
		String expr = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		// if a local var
		Method_t meth = (Method_t) argu;
		if (meth != null) {
			Variable_t var = meth.methContainsVar(id);
			if (var == null) { // didnt find the var in method, so its a field of the class
				Class_t cl = st_.get(meth.getFromClass().getName());
				if (cl == null) {
					throw new Exception("something went wrong at CompoundPlusAssignmentStatement 1");
				}
				var = cl.classContainsVar(id);
				// class field
				if (var != null) {
					String temp = newRegister();
					this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
					this.asm_.append("add " + temp + ", " + temp + ", " + expr +"\t\t\t# increment field\n");
					this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
				}
				return null;
			}
			this.asm_.append("add " + var.getRegister() + ", " + var.getRegister() + ", " + expr +"\n");
		} else { // if a field of a class
			Class_t cl = st_.get(meth.getFromClass().getName());
			if (cl == null) {
				throw new Exception("something went wrong at CompoundPlusAssignmentStatement 2");
			}
			Variable_t var = cl.classContainsVar(id);
			// class field
			if (var != null) {
				String temp = newRegister();
				this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
				this.asm_.append("add " + temp + ", " + temp + ", " + expr +"\t\t\t# increment field\n");
				this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
			}
		}
		return null;
	}

	/**
	* f0 -> Identifier()
	* f1 -> "-="
	* f2 -> Expression()
	* f3 -> ";"
	*/
	public Base_t visit(CompoundMinusAssignmentStatement n, Base_t argu) throws Exception {
		String id = n.f0.accept(this, argu).getName();
		String expr = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		// if a local var
		Method_t meth = (Method_t) argu;
		if (meth != null) {
			Variable_t var = meth.methContainsVar(id);
			if (var == null) { // didnt find the var in method, so its a field of the class
				Class_t cl = st_.get(meth.getFromClass().getName());
				if (cl == null) {
					throw new Exception("something went wrong at CompoundMinusAssignmentStatement 1");
				}
				var = cl.classContainsVar(id);
				// class field
				if (var != null) {
					String temp = newRegister();
					this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
					this.asm_.append("sub " + temp + ", " + temp + ", " + expr +"\t\t\t# increment field\n");
					this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
				}
				return null;
			}
			this.asm_.append("sub " + var.getRegister() + ", " + var.getRegister() + ", " + expr +"\n");
		} else { // if a field of a class
			Class_t cl = st_.get(meth.getFromClass().getName());
			if (cl == null) {
				throw new Exception("something went wrong at CompoundMinusAssignmentStatement 2");
			}
			Variable_t var = cl.classContainsVar(id);
			// class field
			if (var != null) {
				String temp = newRegister();
				this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
				this.asm_.append("sub " + temp + ", " + temp + ", " + expr +"\t\t\t# increment field\n");
				this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
			}
		}
		return null;
	}

	/**
	* f0 -> Identifier()
	* f1 -> "*="
	* f2 -> Expression()
	* f3 -> ";"
	*/
	public Base_t visit(CompoundTimesAssignmentStatement n, Base_t argu) throws Exception {
		String id = n.f0.accept(this, argu).getName();
		String expr = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		// if a local var
		Method_t meth = (Method_t) argu;
		if (meth != null) {
			Variable_t var = meth.methContainsVar(id);
			if (var == null) { // didnt find the var in method, so its a field of the class
				Class_t cl = st_.get(meth.getFromClass().getName());
				if (cl == null) {
					throw new Exception("something went wrong at CompoundTimesAssignmentStatement 1");
				}
				var = cl.classContainsVar(id);
				// class field
				if (var != null) {
					String temp = newRegister();
					this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
					this.asm_.append("mult " + temp + ", " + temp + ", " + expr +"\t\t\t# increment field\n");
					this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
				}
				return null;
			}
			this.asm_.append("mult " + var.getRegister() + ", " + var.getRegister() + ", " + expr +"\n");
		} else { // if a field of a class
			Class_t cl = st_.get(meth.getFromClass().getName());
			if (cl == null) {
				throw new Exception("something went wrong at CompoundTimesAssignmentStatement 2");
			}
			Variable_t var = cl.classContainsVar(id);
			// class field
			if (var != null) {
				String temp = newRegister();
				this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
				this.asm_.append("mult " + temp + ", " + temp + ", " + expr +"\t\t\t# increment field\n");
				this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
			}
		}
		return null;
	}

	/**
	* f0 -> Identifier()
	* f1 -> "/="
	* f2 -> Expression()
	* f3 -> ";"
	*/
	public Base_t visit(CompoundDivAssignmentStatement n, Base_t argu) throws Exception {
		String id = n.f0.accept(this, argu).getName();
		String expr = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		// if a local var
		Method_t meth = (Method_t) argu;
		if (meth != null) {
			Variable_t var = meth.methContainsVar(id);
			if (var == null) { // didnt find the var in method, so its a field of the class
				Class_t cl = st_.get(meth.getFromClass().getName());
				if (cl == null) {
					throw new Exception("something went wrong at CompoundDivAssignmentStatement 1");
				}
				var = cl.classContainsVar(id);
				// class field
				if (var != null) {
					String temp = newRegister();
					this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
					this.asm_.append("div " + temp + ", " + temp + ", " + expr +"\t\t\t# increment field\n");
					this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
				}
				return null;
			}
			this.asm_.append("div " + var.getRegister() + ", " + var.getRegister() + ", " + expr +"\n");
		} else { // if a field of a class
			Class_t cl = st_.get(meth.getFromClass().getName());
			if (cl == null) {
				throw new Exception("something went wrong at CompoundDivAssignmentStatement 2");
			}
			Variable_t var = cl.classContainsVar(id);
			// class field
			if (var != null) {
				String temp = newRegister();
				this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
				this.asm_.append("div " + temp + ", " + temp + ", " + expr +"\t\t\t# increment field\n");
				this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
			}
		}
		return null;
	}

	/**
	* f0 -> Identifier()
	* f1 -> "%="
	* f2 -> Expression()
	* f3 -> ";"
	*/
	public Base_t visit(CompoundModAssignmentStatement n, Base_t argu) throws Exception {
		String id = n.f0.accept(this, argu).getName();
		String expr = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		// if a local var
		Method_t meth = (Method_t) argu;
		if (meth != null) {
			Variable_t var = meth.methContainsVar(id);
			if (var == null) { // didnt find the var in method, so its a field of the class
				Class_t cl = st_.get(meth.getFromClass().getName());
				if (cl == null) {
					throw new Exception("something went wrong at CompoundModAssignmentStatement 1");
				}
				var = cl.classContainsVar(id);
				// class field
				if (var != null) {
					String temp = newRegister();
					this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
					this.asm_.append("mod " + temp + ", " + temp + ", " + expr +"\t\t\t# increment field\n");
					this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
				}
				return null;
			}
			this.asm_.append("mod " + var.getRegister() + ", " + var.getRegister() + ", " + expr +"\n");
		} else { // if a field of a class
			Class_t cl = st_.get(meth.getFromClass().getName());
			if (cl == null) {
				throw new Exception("something went wrong at CompoundModAssignmentStatement 2");
			}
			Variable_t var = cl.classContainsVar(id);
			// class field
			if (var != null) {
				String temp = newRegister();
				this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
				this.asm_.append("mod " + temp + ", " + temp + ", " + expr +"\t\t\t# increment field\n");
				this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
			}
		}
		return null;
	}

	/**
	* f0 -> Identifier()
	* f1 -> "<<="
	* f2 -> Expression()
	* f3 -> ";"
	*/
	public Base_t visit(CompoundShiftLeftAssignmentStatement n, Base_t argu) throws Exception {
		String id = n.f0.accept(this, argu).getName();
		String expr = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		// if a local var
		Method_t meth = (Method_t) argu;
		if (meth != null) {
			Variable_t var = meth.methContainsVar(id);
			if (var == null) { // didnt find the var in method, so its a field of the class
				Class_t cl = st_.get(meth.getFromClass().getName());
				if (cl == null) {
					throw new Exception("something went wrong at CompoundShiftLeftAssignmentStatement 1");
				}
				var = cl.classContainsVar(id);
				// class field
				if (var != null) {
					String temp = newRegister();
					this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
					this.asm_.append("sll " + temp + ", " + temp + ", " + expr +"\t\t\t# increment field\n");
					this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
				}
				return null;
			}
			this.asm_.append("sll " + var.getRegister() + ", " + var.getRegister() + ", " + expr +"\n");
		} else { // if a field of a class
			Class_t cl = st_.get(meth.getFromClass().getName());
			if (cl == null) {
				throw new Exception("something went wrong at CompoundShiftLeftAssignmentStatement 2");
			}
			Variable_t var = cl.classContainsVar(id);
			// class field
			if (var != null) {
				String temp = newRegister();
				this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
				this.asm_.append("sll " + temp + ", " + temp + ", " + expr +"\t\t\t# increment field\n");
				this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
			}
		}
		return null;
	}

	/**
	* f0 -> Identifier()
	* f1 -> ">>="
	* f2 -> Expression()
	* f3 -> ";"
	*/
	public Base_t visit(CompounShiftRightAssignmentStatement n, Base_t argu) throws Exception {
		String id = n.f0.accept(this, argu).getName();
		String expr = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		// if a local var
		Method_t meth = (Method_t) argu;
		if (meth != null) {
			Variable_t var = meth.methContainsVar(id);
			if (var == null) { // didnt find the var in method, so its a field of the class
				Class_t cl = st_.get(meth.getFromClass().getName());
				if (cl == null) {
					throw new Exception("something went wrong at CompounShiftRightAssignmentStatement 1");
				}
				var = cl.classContainsVar(id);
				// class field
				if (var != null) {
					String temp = newRegister();
					this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
					this.asm_.append("srl " + temp + ", " + temp + ", " + expr +"\t\t\t# increment field\n");
					this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
				}
				return null;
			}
			this.asm_.append("srl " + var.getRegister() + ", " + var.getRegister() + ", " + expr +"\n");
		} else { // if a field of a class
			Class_t cl = st_.get(meth.getFromClass().getName());
			if (cl == null) {
				throw new Exception("something went wrong at CompounShiftRightAssignmentStatement 2");
			}
			Variable_t var = cl.classContainsVar(id);
			// class field
			if (var != null) {
				String temp = newRegister();
				this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
				this.asm_.append("srl " + temp + ", " + temp + ", " + expr +"\t\t\t# increment field\n");
				this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
			}
		}
		return null;
	}

	/**
	* f0 -> Identifier()
	* f1 -> &="
	* f2 -> Expression()
	* f3 -> ";"
	*/
	public Base_t visit(CompoundBinaryAndAssignmentStatement n, Base_t argu) throws Exception {
		String id = n.f0.accept(this, argu).getName();
		String expr = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		// if a local var
		Method_t meth = (Method_t) argu;
		if (meth != null) {
			Variable_t var = meth.methContainsVar(id);
			if (var == null) { // didnt find the var in method, so its a field of the class
				Class_t cl = st_.get(meth.getFromClass().getName());
				if (cl == null) {
					throw new Exception("something went wrong at CompoundBinaryAndAssignmentStatement 1");
				}
				var = cl.classContainsVar(id);
				// class field
				if (var != null) {
					String temp = newRegister();
					this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
					this.asm_.append("and " + temp + ", " + temp + ", " + expr +"\t\t\t# increment field\n");
					this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
				}
				return null;
			}
			this.asm_.append("and " + var.getRegister() + ", " + var.getRegister() + ", " + expr +"\n");
		} else { // if a field of a class
			Class_t cl = st_.get(meth.getFromClass().getName());
			if (cl == null) {
				throw new Exception("something went wrong at CompoundBinaryAndAssignmentStatement 2");
			}
			Variable_t var = cl.classContainsVar(id);
			// class field
			if (var != null) {
				String temp = newRegister();
				this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
				this.asm_.append("and " + temp + ", " + temp + ", " + expr +"\t\t\t# increment field\n");
				this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
			}
		}
		return null;
	}

	/**
	* f0 -> Identifier()
	* f1 -> |="
	* f2 -> Expression()
	* f3 -> ";"
	*/
	public Base_t visit(CompoundBinaryOrAssignmentStatement n, Base_t argu) throws Exception {
		String id = n.f0.accept(this, argu).getName();
		String expr = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		// if a local var
		Method_t meth = (Method_t) argu;
		if (meth != null) {
			Variable_t var = meth.methContainsVar(id);
			if (var == null) { // didnt find the var in method, so its a field of the class
				Class_t cl = st_.get(meth.getFromClass().getName());
				if (cl == null) {
					throw new Exception("something went wrong at CompoundBinaryOrAssignmentStatement 1");
				}
				var = cl.classContainsVar(id);
				// class field
				if (var != null) {
					String temp = newRegister();
					this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
					this.asm_.append("or " + temp + ", " + temp + ", " + expr +"\t\t\t# increment field\n");
					this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
				}
				return null;
			}
			this.asm_.append("or " + var.getRegister() + ", " + var.getRegister() + ", " + expr +"\n");
		} else { // if a field of a class
			Class_t cl = st_.get(meth.getFromClass().getName());
			if (cl == null) {
				throw new Exception("something went wrong at CompoundBinaryOrAssignmentStatement 2");
			}
			Variable_t var = cl.classContainsVar(id);
			// class field
			if (var != null) {
				String temp = newRegister();
				this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
				this.asm_.append("or " + temp + ", " + temp + ", " + expr +"\t\t\t# increment field\n");
				this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
			}
		}
		return null;
	}

	/**
	* f0 -> Identifier()
	* f1 -> ^="
	* f2 -> Expression()
	* f3 -> ";"
	*/
	public Base_t visit(CompoundBinaryXorAssignmentStatement n, Base_t argu) throws Exception {
		String id = n.f0.accept(this, argu).getName();
		String expr = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		// if a local var
		Method_t meth = (Method_t) argu;
		if (meth != null) {
			Variable_t var = meth.methContainsVar(id);
			if (var == null) { // didnt find the var in method, so its a field of the class
				Class_t cl = st_.get(meth.getFromClass().getName());
				if (cl == null) {
					throw new Exception("something went wrong at CompoundBinaryXorAssignmentStatement 1");
				}
				var = cl.classContainsVar(id);
				// class field
				if (var != null) {
					String temp = newRegister();
					this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
					this.asm_.append("xor " + temp + ", " + temp + ", " + expr +"\t\t\t# increment field\n");
					this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
				}
				return null;
			}
			this.asm_.append("xor " + var.getRegister() + ", " + var.getRegister() + ", " + expr +"\n");
		} else { // if a field of a class
			Class_t cl = st_.get(meth.getFromClass().getName());
			if (cl == null) {
				throw new Exception("something went wrong at CompoundBinaryXorAssignmentStatement 2");
			}
			Variable_t var = cl.classContainsVar(id);
			// class field
			if (var != null) {
				String temp = newRegister();
				this.asm_.append("lw " + temp + ", " + var.getNum() + "($a0)\t\t\t# load #" + var.getNum() + " field of this\n");
				this.asm_.append("xor " + temp + ", " + temp + ", " + expr +"\t\t\t# increment field\n");
				this.asm_.append("sw " + temp + ", " + var.getNum() + "($a0)\t\t\t# write on #" + var.getNum() + " field of this\n");
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
	public Base_t visit(ArrayAssignmentStatement n, Base_t argu) throws Exception {
		String length = newRegister();
		String error_label = labels_.getErrorLabel();
		String array = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		this.asm_.append("lw " + length + ", 0(" + array + ")\n"); // load length
		String idx = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		// if idx >= arr.length goto error
		this.asm_.append("ble " + length + ", " + idx + ", " + error_label + "\t\t\t# check if in bounds\n");
		// if idx < 0 goto error
		this.asm_.append("blt " + idx + ", $zero, " + error_label + "\t\t\t# check if index is negative\n");
		// skip length
		String temp_array = newRegister();
		this.asm_.append("add " + temp_array + ", " + array + ", 1\n");
		String expr = ((Variable_t) n.f5.accept(this, argu)).getRegister();
		this.asm_.append("sw " + expr + ", " + idx + "(" + temp_array + ")\n");
		return new Variable_t(null, null, temp_array);
	}

	/**
	 * f0 -> IfthenElseStatement()
	 *       | IfthenStatement()
	 */
	public Base_t visit(IfStatement n, Base_t argu) throws Exception {
	   return n.f0.accept(this, argu);
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
	public Base_t visit(IfthenElseStatement n, Base_t argu) throws Exception {
		String end_label = labels_.newLabel();
		String cond = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		String else_label = labels_.newLabel();
		this.asm_.append("beq " + cond + ", $zero, " + else_label + "\n");
		n.f4.accept(this, argu);
		this.asm_.append("j " + end_label + "\n");
		this.asm_.append(else_label + "\n");
		n.f6.accept(this, argu);
		this.asm_.append(end_label + "\n");
		return null;
	}

	/**
     * f0 -> "if"
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> Statement()
     */
	public Base_t visit(IfthenStatement n, Base_t argu) throws Exception {
		String end_label = labels_.newLabel();
		String cond = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("beq " + cond + ", $zero, " + end_label + "\n");
		n.f4.accept(this, argu);
		this.asm_.append(end_label + "\n");
		return null;
	}

	/**
	 * f0 -> "while"
	 * f1 -> "("
	 * f2 -> Expression()
	 * f3 -> ")"
	 * f4 -> Statement()
	 */
	public Base_t visit(WhileStatement n, Base_t argu) throws Exception {
		String start_label = labels_.newLabel();
		String end_label = labels_.newLabel();
		this.asm_.append(start_label + "\n");
		String expr = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("beq " + expr + ", $zero, " + end_label + "\n");
		n.f4.accept(this, argu);
		this.asm_.append("j " + start_label + "\n");
		this.asm_.append(end_label + "\n");
		return null;
	}

	/**
     * f0 -> <PRINT>
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
	public Base_t visit(PrintStatement n, Base_t argu) throws Exception {
		String t = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("print " + t + "\n");
		return null;
	}

	/**
	 * f0 -> <PRINT>
	 * f1 -> "("
	 * f2 -> ")"
	 * f3 -> ";"
	 */
	public Base_t visit(PrintLineStatement n, Base_t argu) throws Exception {
		this.asm_.append("println $r0\n");
		return null;
	}

	/**
     * f0 -> <ANSWER>
     * f1 -> "("
     * f2 -> Expression()
     * f3 -> ")"
     * f4 -> ";"
     */
	public Base_t visit(AnswerStatement n, Base_t argu) throws Exception {
		String t = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("answer " + t + "\n");
		return null;
	}

	/**
     * f0 -> AndExpression()
     *       | OrExpression()
     *       | BinAndExpression()
     *       | BinOrExpression()
     *       | BinXorExpression()
     *       | BinNotExpression()
     *       | ShiftLeftExpression()
     *       | ShiftRightExpression()
     *       | EqualExpression()
     *       | NotEqualExpression()
     *       | LessThanExpression()
     *       | LessThanOrEqualExpression()
     *       | GreaterThanExpression()
     *       | GreaterThanOrEqualExpression()
     *       | PlusExpression()
     *       | MinusExpression()
     *       | TimesExpression()
     *       | DivExpression()
     *       | ModExpression()
     *       | ArrayLookup()
     *       | ArrayLength()
     *       | MessageSend()
     *       | TernaryExpression()
     *       | PublicReadExpression()
     *       | PrivateReadExpression()
     *       | Clause()
     */
	public Base_t visit(Expression n, Base_t argu) throws Exception {
		return n.f0.accept(this, argu);
	}

	/**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
	public Base_t visit(AndExpression n, Base_t argu) throws Exception {
		String exp1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String istrue1 = labels_.newLabel();
		String istrue2 = labels_.newLabel();
		String ret = newRegister();
		this.asm_.append("move " + ret + ", 1\n");
		this.asm_.append("bne " + exp1 + ", $zero, " + istrue1 + "\n");
		this.asm_.append("move " + ret + ", 0\n");
		this.asm_.append("j " + istrue2 + "\n"); 	// early termination
		this.asm_.append(istrue1 + "\n");

		String exp2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("bne " + exp2 + ", $zero, " + istrue2 + "\n");
		this.asm_.append("move " + ret + ", 0\n");
		this.asm_.append(istrue2 + "\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> Clause()
	* f1 -> "||"
	* f2 -> Clause()
	*/
	public Base_t visit(OrExpression n, Base_t argu) throws Exception {
		String exp1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String end_label = labels_.newLabel();
		String ret = newRegister();
		this.asm_.append("move " + ret + ", 1\n");
		this.asm_.append("bne " + exp1 + ", $zero, " + end_label + "\n");
		String exp2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("bne " + exp2 + ", $zero, " + end_label + "\n");
		this.asm_.append("move " + ret + ", 0\n"); // else set ret to 0
		this.asm_.append(end_label + "\n");
		return new Variable_t(null, null, ret);
	}

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "&"
     * f2 -> PrimaryExpression()
     */
    public Base_t visit(BinAndExpression n, Base_t argu) throws Exception {
		String ret = newRegister();
		String exp1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String exp2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("and " + ret + ", " + exp1 + ", " + exp2 + "\n");
		return new Variable_t(null, null, ret);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "|"
     * f2 -> PrimaryExpression()
     */
    public Base_t visit(BinOrExpression n, Base_t argu) throws Exception {
		String ret = newRegister();
		String exp1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String exp2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("or " + ret + ", " + exp1 + ", " + exp2 + "\n");
		return new Variable_t(null, null, ret);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "^"
     * f2 -> PrimaryExpression()
     */
    public Base_t visit(BinXorExpression n, Base_t argu) throws Exception {
		String ret = newRegister();
		String exp1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String exp2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("xor " + ret + ", " + exp1 + ", " + exp2 + "\n");
		return new Variable_t(null, null, ret);
    }

	/**
     * f0 -> "~"
     * f1 -> PrimaryExpression()
     */
    public Base_t visit(BinNotExpression n, Base_t argu) throws Exception {
		String ret = newRegister();
		String exp1 = ((Variable_t) n.f1.accept(this, argu)).getRegister();
		this.asm_.append("not " + ret + ", " + exp1 + "\n");
		return new Variable_t(null, null, ret);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<<"
     * f2 -> PrimaryExpression()
     */
    public Base_t visit(ShiftLeftExpression n, Base_t argu) throws Exception {
		String ret = newRegister();
		String exp1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String exp2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("sll " + ret + ", " + exp1 + ", " + exp2 + "\n");
		return new Variable_t(null, null, ret);
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> ">>"
     * f2 -> PrimaryExpression()
     */
    public Base_t visit(ShiftRightExpression n, Base_t argu) throws Exception {
		String ret = newRegister();
		String exp1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String exp2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("srl " + ret + ", " + exp1 + ", " + exp2 + "\n");
		return new Variable_t(null, null, ret);
    }

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "=="
	* f2 -> PrimaryExpression()
	*/
	public Base_t visit(EqualExpression n, Base_t argu) throws Exception {
		String exp1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String exp2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		String ret = newRegister();
		String end_label = labels_.newLabel();
		this.asm_.append("move " + ret + ", 0\n");
		this.asm_.append("bne " + exp2 + ", " + exp1 + ", " + end_label + "\n");
		this.asm_.append("move " + ret + ", 1\n");
		this.asm_.append(end_label + "\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "!="
	* f2 -> PrimaryExpression()
	*/
	public Base_t visit(NotEqualExpression n, Base_t argu) throws Exception {
		String exp1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String exp2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		String ret = newRegister();
		String end_label = labels_.newLabel();
		this.asm_.append("move " + ret + ", 0\n");
		this.asm_.append("beq " + exp2 + ", " + exp1 + ", " + end_label + "\n");
		this.asm_.append("move " + ret + ", 1\n");
		this.asm_.append(end_label + "\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "<"
	* f2 -> PrimaryExpression()
	*/
	public Base_t visit(LessThanExpression n, Base_t argu) throws Exception {
		String exp1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String exp2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		String ret = newRegister();
		String end_label = labels_.newLabel();
		this.asm_.append("move " + ret + ", 1\n");
		this.asm_.append("blt " + exp1 + ", " + exp2 + ", " + end_label + "\n");
		this.asm_.append("move " + ret + ", 0\n");
		this.asm_.append(end_label + "\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "<="
	* f2 -> PrimaryExpression()
	*/
	public Base_t visit(LessThanOrEqualExpression n, Base_t argu) throws Exception {
		String exp1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String exp2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		String ret = newRegister();
		String end_label = labels_.newLabel();
		this.asm_.append("move " + ret + ", 1\n");
		this.asm_.append("ble " + exp1 + ", " + exp2 + ", " + end_label + "\n");
		this.asm_.append("move " + ret + ", 0\n");
		this.asm_.append(end_label + "\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> ">"
	* f2 -> PrimaryExpression()
	*/
	public Base_t visit(GreaterThanExpression n, Base_t argu) throws Exception {
		String exp1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String exp2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		String ret = newRegister();
		String end_label = labels_.newLabel();
		this.asm_.append("move " + ret + ", 1\n");
		this.asm_.append("blt " + exp2 + ", " + exp1 + ", " + end_label + "\n");
		this.asm_.append("move " + ret + ", 0\n");
		this.asm_.append(end_label + "\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> ">="
	* f2 -> PrimaryExpression()
	*/
	public Base_t visit(GreaterThanOrEqualExpression n, Base_t argu) throws Exception {
		String exp1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String exp2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		String ret = newRegister();
		String end_label = labels_.newLabel();
		this.asm_.append("move " + ret + ", 1\n");
		this.asm_.append("ble " + exp1 + ", " + exp2 + ", " + end_label + "\n");
		this.asm_.append("move " + ret + ", 0\n");
		this.asm_.append(end_label + "\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "+"
	* f2 -> PrimaryExpression()
	*/
	public Base_t visit(PlusExpression n, Base_t argu) throws Exception {
		String ret = newRegister();
		String exp1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String exp2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("add " + ret + ", " + exp1 + ", " + exp2 + "\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "-"
	* f2 -> PrimaryExpression()
	*/
	public Base_t visit(MinusExpression n, Base_t argu) throws Exception {
		String ret = newRegister();
		String exp1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String exp2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("sub " + ret + ", " +  exp1 + ", " + exp2 + "\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "*"
	* f2 -> PrimaryExpression()
	*/
	public Base_t visit(TimesExpression n, Base_t argu) throws Exception {
		String ret = newRegister();
		String exp1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String exp2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("mult " + ret + ", " + exp1 + ", " + exp2 + "\n");
		return new Variable_t(null, null, ret);

	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "/"
	* f2 -> PrimaryExpression()
	*/
	public Base_t visit(DivExpression n, Base_t argu) throws Exception {
		String ret = newRegister();
		String exp1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String exp2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("div " + ret + ", " + exp1 + ", " + exp2 + "\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "%"
	* f2 -> PrimaryExpression()
	*/
	public Base_t visit(ModExpression n, Base_t argu) throws Exception {
		String ret = newRegister();
		String exp1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String exp2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("mod " + ret + ", " + exp1 + ", " + exp2 + "\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "["
	* f2 -> PrimaryExpression()
	* f3 -> "]"
	*/
	public Base_t visit(ArrayLookup n, Base_t argu) throws Exception {
		String length = newRegister();
		String error_label = labels_.getErrorLabel();
		String array = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		// load length
		this.asm_.append("lw " + length + ", 0(" + array + ")\n");
		String idx = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		// if idx >= arr.length goto error
		this.asm_.append("ble " + length + ", " + idx + ", " + error_label + "\t\t\t# check if in bounds\n");
		// if idx < 0 goto error
		this.asm_.append("blt " + idx + ", $zero, " + error_label + "\t\t\t# check if index is negative\n");
		// skip length
		String temp_array = newRegister();
		String ret = newRegister();
		this.asm_.append("add " + temp_array + ", " + array + ", 1\n");
		this.asm_.append("lw " + ret + ", " + idx + "(" + temp_array + ")\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "."
	* f2 -> "length"
	*/
	public Base_t visit(ArrayLength n, Base_t argu) throws Exception {
		String len = newRegister();
		String exp = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		this.asm_.append("lw " + len + ", 0(" + exp + ")\t\t# load array length\n");
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
	public Base_t visit(MessageSend n, Base_t argu) throws Exception {
		Variable_t obj = (Variable_t) n.f0.accept(this, argu);
        Variable_t func = (Variable_t) n.f2.accept(this, argu);
        String objreg = obj.getRegister();
        Class_t cl = st_.get(obj.getType());
		Method_t meth = cl.getMethod(func.getName());
        int offset = meth.getMethNum() - 1;
		String vtable_addr = newRegister();
		String thisreg = newRegister();
		String methreg = newRegister();
        this.asm_.append("move " + thisreg + ", " + objreg + "\n"); 	// load the address of vtable
        this.asm_.append("lw " + vtable_addr + ", 0(" + thisreg + ")\n"); 	// load the address of vtable
        this.asm_.append("lw " + methreg + ", " + offset + "(" + vtable_addr + ")\n");	// load the right method from vtable
        // add params to method call
		this.asm_.append("move $a0, " + thisreg + "\t\t\t# this object\n");
        if (n.f4.present()) {														// if meth has params
			Method_t params = (Method_t) n.f4.accept(this, argu);
	        for (int i = 0 ; i < params.method_params.size() ; i++) {				// for every par
				String parreg = new String("$a" + (i+1));
				this.asm_.append("move " + parreg + ", " + ((Variable_t) params.method_params.get(i)).getRegister() + "\n");
			}
	    }
		String return_address = labels_.newLabel();
        this.asm_.append("la $ra, " + return_address + "\n");

		this.asm_.append("sub $sp, $sp, 1\n");
		this.asm_.append("sw $ra, 0($sp)\t\t\t# store $ra to the stack\n");

        this.asm_.append("jr " + methreg + "\n");
		this.asm_.append(return_address + "\n");
		String ret = newRegister();
        this.asm_.append("move " + ret + ", $v0\n");
		return new Variable_t(meth.getType(), null, ret);
	}

	/**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     * f3 -> "?"
     * f4 -> Expression()
     * f5 -> ":"
     * f6 -> Expression()
     */
    public Base_t visit(TernaryExpression n, Base_t argu) throws Exception {
		String res = newRegister();
		String end_label = labels_.newLabel();
		String else_label = labels_.newLabel();
		String cond = ((Variable_t) n.f1.accept(this, argu)).getRegister();
		this.asm_.append("beq " + cond + ", $zero, " + else_label + "\n");

		String reg_if = ((Variable_t) n.f4.accept(this, argu)).getRegister();
		this.asm_.append("move " + res + ", " + reg_if + "\n");
		this.asm_.append("j " + end_label + "\n");
		this.asm_.append(else_label + "\n");
		String reg_else = ((Variable_t) n.f6.accept(this, argu)).getRegister();
		this.asm_.append("move " + res + ", " + reg_else + "\n");
		this.asm_.append(end_label + "\n");
		return new Variable_t("int", null, res);
    }

    /**
     * f0 -> <PUBLIC_READ>
     * f1 -> "("
     * f2 -> ")"
     */
    public Base_t visit(PublicReadExpression n, Base_t argu) throws Exception {
		String t = newRegister();
		this.asm_.append("pubread " + t + "\n");
		return new Variable_t("int", null, t);
    }

	/**
     * f0 -> <PRIVATE_READ>
     * f1 -> "("
     * f2 -> ")"
     */
    public Base_t visit(PrivateReadExpression n, Base_t argu) throws Exception {
		String t = newRegister();
		this.asm_.append("secread " + t + "\n");
		return new Variable_t("int", null, t);
    }

  	/**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public Base_t visit(ExpressionList n, Base_t argu) throws Exception {
        Variable_t expr =  (Variable_t) n.f0.accept(this, argu); //na tsekarw th seira
        Method_t meth = (Method_t) n.f1.accept(this, argu);
        meth.method_params.add(expr);
        return meth;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    public Base_t visit(ExpressionTail n, Base_t argu) throws Exception {
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
    public Base_t visit(ExpressionTerm n, Base_t argu) throws Exception {
        n.f0.accept(this, argu);
        return (Variable_t) n.f1.accept(this, argu);
    }



	/**
	* f0 -> NotExpression()
	*       | PrimaryExpression()
	*/
	public Base_t visit(Clause n, Base_t argu) throws Exception {
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
	public Base_t visit(PrimaryExpression n, Base_t argu) throws Exception {
		return n.f0.accept(this, argu);
	}

	/**
	* f0 -> <INTEGER_LITERAL>
	*/
	public Base_t visit(IntegerLiteral n, Base_t argu) throws Exception {
		String ret = "$r" + ++globals_;
		this.asm_.append("move " + ret + ", " + n.f0.toString() + "\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> "true"
	*/
	public Base_t visit(TrueLiteral n, Base_t argu) throws Exception {
		String ret = newRegister();
		this.asm_.append("move " + ret + ", 1\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> "false"
	*/
	public Base_t visit(FalseLiteral n, Base_t argu) throws Exception {
		String ret = newRegister();
		this.asm_.append("move " + ret + ", 0\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> <IDENTIFIER>
	*/
	public Base_t visit(Identifier n, Base_t argu) throws Exception {
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
				this.asm_.append("lw " + newreg + ", " + var.getNum() + "($a0)\t\t\t# read #" + var.getNum() + " field from this\n");
				return new Variable_t(var.getType(), id, newreg);
			}
		}
	}

	/**
	 * f0 -> "this"
	 */
	public Base_t visit(ThisExpression n, Base_t argu) throws Exception {
		Class_t cl = ((Method_t) argu).getFromClass();
		return new Variable_t(cl.getName(), "this", "$a0");
	}

	/**
	 * f0 -> "new"
	 * f1 -> "int"
	 * f2 -> "["
	 * f3 -> Expression()
	 * f4 -> "]"
	 */
	public Base_t visit(ArrayAllocationExpression n, Base_t argu) throws Exception {
		String error_label = labels_.getErrorLabel();
		String len = ((Variable_t) n.f3.accept(this, argu)).getRegister();
		String array = newRegister();
		// if len <= 0 goto error
		this.asm_.append("ble " + len + ", $zero, " + error_label + "\t\t\t# Check if length is > 0\n");
		// store array length in first position
		this.asm_.append("move " + array + ", $hp\n");
		this.asm_.append("sw " + len + ", 0(" + array + ")\n");
		// increase heap pointer
		String temp = newRegister();
		this.asm_.append("add " + temp + ", " + len + ", 1\n");
		this.asm_.append("add $hp, $hp, " + temp + "\n");
		return new Variable_t(null, null, array);
	}

	/**
	 * f0 -> "new"
	 * f1 -> Identifier()
	 * f2 -> "("
	 * f3 -> ")"
	 */
	public Base_t visit(AllocationExpression n, Base_t argu) throws Exception {
		String id = n.f1.accept(this, argu).getName();
        Class_t cl = st_.get(id);
		String new_obj = newRegister();
		String vtable = newRegister();
		this.asm_.append("move " + new_obj + ", $hp\t\t\t# " + cl.getName() + " object ("+ (cl.getNumVars() + 1) + " fields)\n");
		this.asm_.append("add $hp, $hp, " + (cl.getNumVars() + 1) + "\n");
        this.asm_.append("la " + vtable + ", " + cl.getVTableAddress() + "\t\t\t\t# load " + cl.getName() + " vTable address\n");
        this.asm_.append("sw " + vtable + ", 0(" + new_obj + ")\n");
        for (int i = 1 ; i <= cl.getNumVars() ; i++) {
			this.asm_.append("sw $zero, " + i + "(" + new_obj + ")\n");
		}
		return new Variable_t(id, id, new_obj);
	}

	/**
	 * f0 -> "!"
	 * f1 -> Clause()
	 */
	public Base_t visit(NotExpression n, Base_t argu) throws Exception {
		String ret = newRegister();
		String t = ((Variable_t) n.f1.accept(this, argu)).getRegister();
		this.asm_.append("move " + ret + ", 1\n");
		this.asm_.append("sub " + ret + ", " + ret + ", " + t + "\n");
		return new Variable_t(null, null, ret);
	}

	/**
	 * f0 -> "("
	 * f1 -> Expression()
	 * f2 -> ")"
	 */
	public Base_t visit(BracketExpression n, Base_t argu) throws Exception {
		return n.f1.accept(this, argu);
	}

}
