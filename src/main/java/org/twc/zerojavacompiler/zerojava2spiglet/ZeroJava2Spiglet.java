package org.twc.zerojavacompiler.zerojava2spiglet;

import org.twc.zerojavacompiler.zerojava2spiglet.zerojavasyntaxtree.*;
import org.twc.zerojavacompiler.zerojava2spiglet.zerojavavisitor.GJDepthFirst;
import org.twc.zerojavacompiler.basetype.*;

import java.util.*;

public class ZeroJava2Spiglet extends GJDepthFirst<Base_t, Base_t> {

	private StringBuilder asm_;
	private final Label labels_;
    private final Map<String, Class_t> st_;
    private int globals_;

	public ZeroJava2Spiglet(Map<String, Class_t> st, int globals) {
		this.labels_ = new Label();
        this.st_ = st;
        this.globals_ = globals;
	}

	private void initVtables() {
        for (Map.Entry<String, Class_t> entry : st_.entrySet()) {
			Class_t cl = entry.getValue();
			if (cl.isMain()) {
				continue;
			}
			String label = labels_.newVTableLabel(cl.getName());
			String vtable = newTemp();
			String temp = newTemp();
			this.asm_.append("MOVE ").append(vtable).append(" ").append(label).append("\n");
			int offset = cl.getNumMethods();
			this.asm_.append("MOVE ").append(temp).append(" HALLOCATE ").append(offset * 4).append("\n");
			this.asm_.append("HSTORE ").append(vtable).append(" 0 ").append(temp).append("\n");
            for (Map.Entry<String, Method_t> methods : cl.class_methods_map.entrySet()) {
                String newTemp = newTemp();
                Method_t meth = methods.getValue();
                this.asm_.append("MOVE ").append(newTemp).append(" ").append(meth.getFrom_class_().getName()).append("_").append(meth.getName()).append("\n");
                this.asm_.append("HSTORE ").append(temp).append(" ").append(meth.getMeth_num_() * 4).append(" ").append(newTemp).append("\n");
			}
			this.asm_.append("\n");
    	}
	}

    public String getASM() {
		return asm_.toString();
	}

    public String newTemp() {
		return "TEMP " + ++globals_;
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
		this.asm_.append("MAIN\n\n");
		String id = n.f1.accept(this, argu).getName();
        Method_t meth = st_.get(id).getMethod("main");
        initVtables();
		n.f14.accept(this, meth);
		n.f15.accept(this, meth);
		this.asm_.append("END\n");
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
		if (meth.getFrom_class_() != null) { 												// is a variable of a function
			String newTemp = newTemp();
			if ((meth = meth.getFrom_class_().getMethod(meth.getName())) != null) {		// if you found method
				meth.addRegToVar(varName, newTemp);
			} else {
				throw new Exception("VarDeclaration Error 1");
			}
			this.asm_.append("MOVE ").append(newTemp).append(" 0\n");
		} else {																	// is a var (field) of a class
			Class_t cl = st_.get(meth.getName());
			if (cl == null)															// do nothing for now
			{
				throw new Exception("VarDeclaration Error 2");
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
        this.asm_.append("\n").append(argu.getName()).append("_").append(methName).append("[").append(meth.getNum_parameters_() + 1).append("]\nBEGIN\n");
        n.f7.accept(this, meth);
        n.f8.accept(this, meth);
        Variable_t retType = (Variable_t) n.f10.accept(this, meth);
        this.asm_.append("RETURN ").append(retType.getRegister()).append("\nEND\n");
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
	 *       | IncrementAssignmentStatement()
	 *       | DecrementAssignmentStatement()
	 *       | CompoundAssignmentStatement()
	 *       | ArrayAssignmentStatement()
	 *       | IfStatement()
	 *       | WhileStatement()
	 *       | PrintStatement()
	 *       | PrintLineStatement()
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
		Method_t meth = (Method_t) argu;
		if (meth != null) {
			Variable_t var = meth.methContainsVar(id);
			if (var == null) { // didnt find the var in method, so its a field of the class
				Class_t cl = st_.get(meth.getFrom_class_().getName());
				if (cl == null) {
                    throw new Exception("something went wrong at AssignmentStatement 2");
                }
				var = cl.classContainsVar(id);
				if (var != null) { // class field
                    this.asm_.append("HSTORE " + " TEMP 0 ").append(var.getNum() * 4).append(" ").append(expr).append("\n");
                }
				return null;
			} else { // if a local var
				this.asm_.append("MOVE ").append(var.getRegister()).append(" ").append(expr).append("\n");
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
		Method_t meth = (Method_t) argu;
		if (meth != null) {
			Variable_t var = meth.methContainsVar(id);
			if (var == null) { // didn't find the var in method, so it's a field of the class
				Class_t cl = st_.get(meth.getFrom_class_().getName());
				if (cl == null) {
					throw new Exception("something went wrong at IncrementAssignmentStatement 1");
				}
				var = cl.classContainsVar(id);
				if (var != null) { // class field
					String temp = newTemp();
					this.asm_.append("HLOAD ").append(temp).append(" TEMP 0 ").append(var.getNum() * 4).append("\n");
					this.asm_.append("MOVE ").append(temp).append(" PLUS ").append(temp).append(" 1\n");
					this.asm_.append("HSTORE TEMP 0 ").append(var.getNum() * 4).append(" ").append(temp).append("\n");
				}
				return null;
			} else { // if a local var
				this.asm_.append("MOVE ").append(var.getRegister()).append(" PLUS ").append(var.getRegister()).append(" 1\n");
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
		Method_t meth = (Method_t) argu;
		if (meth != null) { // if a local var
			Variable_t var = meth.methContainsVar(id);
			if (var == null) { // didn't find the var in method, so it's a field of the class
				Class_t cl = st_.get(meth.getFrom_class_().getName());
				if (cl == null) {
					throw new Exception("something went wrong at IncrementAssignmentStatement 1");
				}
				var = cl.classContainsVar(id);
				if (var != null) { // class field
					String temp = newTemp();
					this.asm_.append("HLOAD ").append(temp).append(" TEMP 0 ").append(var.getNum() * 4).append("\n");
					this.asm_.append("MOVE ").append(temp).append(" MINUS ").append(temp).append(" 1\n");
					this.asm_.append("HSTORE TEMP 0 ").append(var.getNum() * 4).append(" ").append(temp).append("\n");
				}
				return null;
			} else {
				this.asm_.append("MOVE ").append(var.getRegister()).append(" MINUS ").append(var.getRegister()).append(" 1\n");
			}
		}
		return null;
	}

	/**
	 * f0 -> Identifier()
	 * f1 -> CompoundOperator()
	 * f2 -> Expression()
	 * f3 -> ";"
	 */
	public Base_t visit(CompoundAssignmentStatement n, Base_t argu) throws Exception {
		String id = n.f0.accept(this, argu).getName();
		String operator = n.f1.accept(this, argu).getName();
		String expr = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		String opcode;
		if ("+=".equals(operator)) {
			opcode = "PLUS";
		} else if ("-=".equals(operator)) {
			opcode = "MINUS";
		} else if ("*=".equals(operator)) {
			opcode = "TIMES";
		} else if ("/=".equals(operator)) {
			opcode = "DIV";
		} else if ("%=".equals(operator)) {
			opcode = "MOD";
		} else if ("<<=".equals(operator)) {
			opcode = "SLL";
		} else if (">>=".equals(operator)) {
			opcode = "SRL";
		} else if ("&=".equals(operator)) {
			opcode = "AND";
		} else if ("|=".equals(operator)) {
			opcode = "OR";
		} else if ("^=".equals(operator)) {
			opcode = "XOR";
		} else {
			throw new IllegalStateException("CompoundAssignmentStatement: Unexpected value: " + operator);
		}
		Method_t meth = (Method_t) argu;
		if (meth != null) {
			Variable_t var = meth.methContainsVar(id);
			if (var == null) { // didn't find the var in method, so it's a field of the class
				Class_t cl = st_.get(meth.getFrom_class_().getName());
				if (cl == null) {
					throw new Exception("something went wrong at IncrementAssignmentStatement 1");
				}
				var = cl.classContainsVar(id);
				if (var != null) { // class field
					String temp = newTemp();
					this.asm_.append("HLOAD ").append(temp).append(" TEMP 0 ").append(var.getNum() * 4).append("\n");
					this.asm_.append("MOVE ").append(temp).append(" ").append(opcode).append(" ").append(temp).append(" 1\n");
					this.asm_.append("HSTORE TEMP 0 ").append(var.getNum() * 4).append(" ").append(temp).append("\n");
				}
				return null;
			} else { // if a local var
				this.asm_.append("MOVE ").append(var.getRegister()).append(" ").append(opcode).append(" ").append(var.getRegister()).append(" ").append(expr).append("\n");
			}
		}
		return null;
	}

	/**
	 * f0 -> "+="
	 * | 	"-="
	 * | 	"*="
	 * | 	"/="
	 * | 	"%="
	 * | 	"<<="
	 * | 	">>="
	 * | 	"&="
	 * | 	"|="
	 * | 	"^="
	 */
	public Base_t visit(CompoundOperator n, Base_t argu) throws Exception {
		String[] _ret = { "+=", "-=", "*=", "/=", "%=", "<<=", ">>=", "&=", "|=", "^="};
		return new Variable_t(_ret[n.f0.which], _ret[n.f0.which]);
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
		String length = newTemp();
		String cond = newTemp();
		String error = labels_.newLabel();
		String noerror = labels_.newLabel();
		String array = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		this.asm_.append("HLOAD ").append(length).append(" ").append(array).append(" 0\n"); 			// load real size to length
		String pos = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("MOVE ").append(cond).append(" LT ").append(pos).append(" ").append(length).append("\n");	// if pos < arr.length
		this.asm_.append("CJUMP ").append(cond).append(" ").append(error).append("\n");
		this.asm_.append("MOVE ").append(cond).append(" LT ").append(pos).append(" 0\n");				// if arr.length > 0 g
		String one = newTemp();
		this.asm_.append("MOVE ").append(one).append(" 1\n");
		this.asm_.append("MOVE ").append(cond).append(" MINUS ").append(one).append(" ").append(cond).append("\n");
		this.asm_.append("CJUMP ").append(cond).append(" ").append(error).append("\n");
		this.asm_.append("JUMP ").append(noerror).append("\n");
		this.asm_.append(error).append(" NOOP\n");
		this.asm_.append("ERROR\n").append(noerror).append(" NOOP\n");
		String temp_array = newTemp();
		this.asm_.append("MOVE ").append(temp_array).append(" ").append(array).append("\n");			// temp_array = &array
		this.asm_.append("MOVE ").append(temp_array).append(" PLUS ").append(temp_array).append(" 4\n");
		String temp = newTemp();
		this.asm_.append("MOVE ").append(temp).append(" TIMES ").append(pos).append(" 4\n");
		this.asm_.append("MOVE ").append(temp_array).append(" PLUS ").append(temp_array).append(" ").append(temp).append("\n");
		String expr = ((Variable_t) n.f5.accept(this, argu)).getRegister();
		this.asm_.append("HSTORE ").append(temp_array).append(" 0 ").append(expr).append("\n");
		return new Variable_t(temp_array, null);
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
	 */
	public Base_t visit(IfthenStatement n, Base_t argu) throws Exception {
		String end_label = labels_.newLabel();
		String cond = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("CJUMP ").append(cond).append(" ").append(end_label).append("\n");
		n.f4.accept(this, argu);
		this.asm_.append(end_label).append(" NOOP\n");
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
	public Base_t visit(IfthenElseStatement n, Base_t argu) throws Exception {
		String else_label = labels_.newLabel();
		String end_label = labels_.newLabel();
		String cond = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("CJUMP ").append(cond).append(" ").append(else_label).append("\n"); //if cond not true go to else_label
		n.f4.accept(this, argu);
		this.asm_.append("JUMP ").append(end_label).append("\n").append(else_label).append(" NOOP\n");
		n.f6.accept(this, argu);
		this.asm_.append(end_label).append(" NOOP\n");
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
		String lstart = labels_.newLabel();
		String lend = labels_.newLabel();
		String cond = newTemp();
		this.asm_.append(lstart).append(" NOOP\n");
		String expr = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("MOVE ").append(cond).append(" ").append(expr).append("\n");
		this.asm_.append("CJUMP ").append(cond).append(" ").append(lend).append("\n");
		n.f4.accept(this, argu);
		this.asm_.append("JUMP ").append(lstart).append("\n").append(lend).append(" NOOP\n");
		return null;
	}

	/**
	* f0 -> "System.out.println"
	* f1 -> "("
	* f2 -> Expression()
	* f3 -> ")"
	* f4 -> ";"
	*/
	public Base_t visit(PrintStatement n, Base_t argu) throws Exception {
		String t = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("PRINT ").append(t).append("\n");
		return null;
	}

	/**
	 * f0 -> "System.out.println"
	 * f1 -> "("
	 * f2 -> ")"
	 * f3 -> ";"
	 */
	public Base_t visit(PrintLineStatement n, Base_t argu) throws Exception {
		this.asm_.append("PRINT TEMP 0\n");
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
        this.asm_.append("ANSWER ").append(t).append("\n");
        return null;
    }

	/**
	 * f0 -> LogicalAndExpression()
	 *       | LogicalOrExpression()
	 *       | BinaryExpression()
	 *       | BinNotExpression()
	 *       | ArrayLookup()
	 *       | ArrayLength()
	 *       | MessageSend()
	 *       | TernaryExpression()
	 *       | PublicReadExpression()
	 *       | PrivateReadExpression()
	 *       | PublicSeekExpression()
	 *       | PrivateSeekExpression()
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
	public Base_t visit(LogicalAndExpression n, Base_t argu) throws Exception {
		String end_label = labels_.newLabel();
		String ret = newTemp();
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		this.asm_.append("MOVE ").append(ret).append(" ").append(t1).append("\n");
		this.asm_.append("CJUMP ").append(t1).append(" ").append(end_label).append("\n");
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("MOVE ").append(ret).append(" ").append(t2).append("\n");
		this.asm_.append(end_label).append(" NOOP\n");
        return new Variable_t(null, null, ret);
	}

	/**
	 * f0 -> Clause()
	 * f1 -> "||"
	 * f2 -> Clause()
	 */
	public Base_t visit(LogicalOrExpression n, Base_t argu) throws Exception {
		String end_label = labels_.newLabel();
		String temp = newTemp();
		String ret = newTemp();
		String one = newTemp();
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		this.asm_.append("MOVE ").append(one).append(" 1\n");
		this.asm_.append("MOVE ").append(temp).append(" MINUS ").append(one).append(" ").append(t1).append("\n");
		this.asm_.append("MOVE ").append(ret).append(" ").append(t1).append("\n");
		this.asm_.append("CJUMP ").append(temp).append(" ").append(end_label).append("\n");
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("MOVE ").append(ret).append(" ").append(t2).append("\n");
		this.asm_.append(end_label).append(" NOOP\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> BinOperator()
	* f2 -> PrimaryExpression()
	*/
	public Base_t visit(BinaryExpression n, Base_t argu) throws Exception {
		String ret = newTemp();
		String t1 = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		String operator = n.f1.accept(this, argu).getName();
		String t2 = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		String opcode;
		if ("&".equals(operator)) {
			opcode = "AND";
		} else if ("|".equals(operator)) {
			opcode = "OR";
		} else if ("^".equals(operator)) {
			opcode = "XOR";
		} else if ("<<".equals(operator)) {
			opcode = "SLL";
		} else if (">>".equals(operator)) {
			opcode = "SRL";
		} else if ("<<=".equals(operator)) {
			opcode = "SLL";
		} else if (">>=".equals(operator)) {
			opcode = "SRL";
		} else if ("+".equals(operator)) {
			opcode = "PLUS";
		} else if ("-".equals(operator)) {
			opcode = "MINUS";
		} else if ("*".equals(operator)) {
			opcode = "TIMES";
		} else if ("/".equals(operator)) {
			opcode = "DIV";
		} else if ("%".equals(operator)) {
			opcode = "MOD";
		} else if ("==".equals(operator)) {
			opcode = "EQ";
		} else if ("!=".equals(operator)) {
			opcode = "NEQ";
		} else if ("<".equals(operator)) {
			opcode = "LT";
		} else if ("<=".equals(operator)) {
			opcode = "LTE";
		} else if (">".equals(operator)) {
			opcode = "GT";
		} else if (">=".equals(operator)) {
			opcode = "GTE";
		} else {
			throw new IllegalStateException("CompoundAssignmentStatement: Unexpected value: " + operator);
		}
		this.asm_.append("MOVE ").append(ret).append(" ").append(opcode).append(" ").append(t1).append(" ").append(t2).append("\n");
        return new Variable_t(null, null, ret);
	}

	/**
	 * f0 -> "&"
	 * |	"|"
	 * |	"^"
	 * |	"<<"
	 * |	">>"
	 * |	"+"
	 * |	"-"
	 * |	"*"
	 * |	"/"
	 * |	"%"
	 * |	"=="
	 * |	"!="
	 * |	"<"
	 * |	"<="
	 * |	">"
	 * |	">="
	 */
	public Base_t visit(BinOperator n, Base_t argu) throws Exception {
		String[] _ret = { "&", "|", "^", "<<", ">>", "+", "-", "*", "/", "%", "==", "!=", "<", "<=", ">", ">=" };
		return new Variable_t(_ret[n.f0.which], _ret[n.f0.which]);
	}

	/**
	 * f0 -> "~"
	 * f1 -> PrimaryExpression()
	 */
	public Base_t visit(BinNotExpression n, Base_t argu) throws Exception {
		String ret = newTemp();
		String t1 = ((Variable_t) n.f1.accept(this, argu)).getRegister();
		this.asm_.append("MOVE ").append(ret).append(" NOT ").append(t1).append("\n");
		return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "["
	* f2 -> PrimaryExpression()
	* f3 -> "]"
	*/
	public Base_t visit(ArrayLookup n, Base_t argu) throws Exception {
		String length = newTemp();
		String cond = newTemp();
		String error = labels_.newLabel();
		String noerror = labels_.newLabel();
		String array = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		this.asm_.append("HLOAD ").append(length).append(" ").append(array).append(" 0\n"); 			// load real size to length
		String pos = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("MOVE ").append(cond).append(" LT ").append(pos).append(" ").append(length).append("\n");	// if pos < arr.length
		this.asm_.append("CJUMP ").append(cond).append(" ").append(error).append("\n");
		this.asm_.append("MOVE ").append(cond).append(" LT ").append(pos).append(" 0\n");				// if arr.length > 0 g
		String one = newTemp();
		this.asm_.append("MOVE ").append(one).append(" 1\n");
		this.asm_.append("MOVE ").append(cond).append(" MINUS ").append(one).append(" ").append(cond).append("\n");
		this.asm_.append("CJUMP ").append(cond).append(" ").append(error).append("\n");
		this.asm_.append("JUMP ").append(noerror).append("\n");
		this.asm_.append(error).append(" NOOP\n");
		this.asm_.append("ERROR\n").append(noerror).append(" NOOP\n");
		String temp_array = newTemp();
		this.asm_.append("MOVE ").append(temp_array).append(" ").append(array).append("\n");			// temp_array = &array
		this.asm_.append("MOVE ").append(temp_array).append(" PLUS ").append(temp_array).append(" 4\n");
		String temp = newTemp();
		this.asm_.append("MOVE ").append(temp).append(" TIMES ").append(pos).append(" 4\n");
		this.asm_.append("MOVE ").append(temp_array).append(" PLUS ").append(temp_array).append(" ").append(temp).append("\n");
		String ret = newTemp();
		this.asm_.append("HLOAD ").append(ret).append(" ").append(temp_array).append(" 0\n");
        return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> PrimaryExpression()
	* f1 -> "."
	* f2 -> "length"
	*/
	public Base_t visit(ArrayLength n, Base_t argu) throws Exception {
		String len = newTemp();
		String t = ((Variable_t) n.f0.accept(this, argu)).getRegister();
		this.asm_.append("HLOAD ").append(len).append(" ").append(t).append(" 0\n");
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
		Variable_t obj = (Variable_t) n.f0.accept(this, argu);	// obj.temp is type
        Variable_t func = (Variable_t) n.f2.accept(this, argu);
        String objreg = obj.getRegister();
        Class_t cl = st_.get(obj.getType());
        Method_t meth = cl.getMethod(func.getName());
        int offset = meth.getMeth_num_() - 1;
		String vtable_addr = newTemp();
		String thisTemp = newTemp();
		String methTemp = newTemp();
        this.asm_.append("MOVE ").append(thisTemp).append(" ").append(objreg).append("\n"); 	// load the address of vtable
        this.asm_.append("HLOAD ").append(vtable_addr).append(" ").append(thisTemp).append(" 0\n"); 	// load the address of vtable
        this.asm_.append("HLOAD ").append(methTemp).append(" ").append(vtable_addr).append(" ").append(offset * 4).append("\n");	// load the right method from vtable
        // add params to method call
		StringBuilder parStr = new StringBuilder(" ");
        if (n.f4.present()) {														// if meth has params
            Method_t params = (Method_t) n.f4.accept(this, argu);
	        for (int i = 0 ; i < params.method_params.size() ; i++) {				// for every par
	        	Variable_t var = params.method_params.get(i);
				String parTemp = newTemp();
	        	this.asm_.append("MOVE ").append(parTemp).append(" ").append(var.getRegister()).append("\n");
	        	parStr.append(parTemp).append(" ");
	        }
	    }
		String ret = newTemp();
        this.asm_.append("MOVE ").append(ret).append(" CALL ").append(methTemp).append("( ").append(thisTemp).append(parStr).append(")\n");
        return new Variable_t(meth.getType_(), null, ret);
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
		String else_label = labels_.newLabel();
		String end_label = labels_.newLabel();
		String res = newTemp();
		String cond = ((Variable_t) n.f1.accept(this, argu)).getRegister();
		this.asm_.append("CJUMP ").append(cond).append(" ").append(else_label).append("\n"); //if cond not true go to else_label
		Variable_t exp1 = (Variable_t) n.f4.accept(this, argu);
		String reg_if = exp1.getRegister();
		String return_type = exp1.getType();
		this.asm_.append("MOVE ").append(res).append(" ").append(reg_if).append("\n");
		this.asm_.append("JUMP ").append(end_label).append("\n");
		this.asm_.append(else_label).append(" NOOP\n");
		String reg_else = ((Variable_t) n.f6.accept(this, argu)).getRegister();
		this.asm_.append("MOVE ").append(res).append(" ").append(reg_else).append("\n");
		this.asm_.append(end_label).append(" NOOP\n");
		return new Variable_t(return_type, null, res);
	}

	/**
	 * f0 -> <PUBLIC_READ>
	 * f1 -> "("
	 * f2 -> ")"
	 */
	public Base_t visit(PublicReadExpression n, Base_t argu) throws Exception {
		String ret = newTemp();
		this.asm_.append("PUBREAD ").append(ret).append("\n");
		return new Variable_t("int", null, ret);
	}

	/**
	 * f0 -> <PRIVATE_READ>
	 * f1 -> "("
	 * f2 -> ")"
	 */
	public Base_t visit(PrivateReadExpression n, Base_t argu) throws Exception {
		String ret = newTemp();
		this.asm_.append("SECREAD ").append(ret).append("\n");
		return new Variable_t("int", null, ret);
	}

	/**
	 * f0 -> <PUBLIC_SEEK>
	 * f1 -> "("
	 * f2 -> PrimaryExpression()
	 * f3 -> ")"
	 */
	public Base_t visit(PublicSeekExpression n, Base_t argu) throws Exception {
		String ret = newTemp();
		String t = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("PUBSEEK ").append(ret).append(" ").append(t).append("\n");
		return new Variable_t("int", null, ret);
	}

	/**
	 * f0 -> <PRIVATE_SEEK>
	 * f1 -> "("
	 * f2 -> PrimaryExpression()
	 * f3 -> ")"
	 */
	public Base_t visit(PrivateSeekExpression n, Base_t argu) throws Exception {
		String ret = newTemp();
		String t = ((Variable_t) n.f2.accept(this, argu)).getRegister();
		this.asm_.append("SECSEEK ").append(ret).append(" ").append(t).append("\n");
		return new Variable_t("int", null, ret);
	}
	
 	/**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public Base_t visit(ExpressionList n, Base_t argu) throws Exception {
        Variable_t expr =  (Variable_t) n.f0.accept(this, argu);
        Method_t meth = (Method_t) n.f1.accept(this, argu);
        meth.method_params.addLast(expr);
        return meth;
    }

    /**
    * f0 -> ( ExpressionTerm() )*
    */
    public Base_t visit(ExpressionTail n, Base_t argu) throws Exception {
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
    public Base_t visit(ExpressionTerm n, Base_t argu) throws Exception {
        n.f0.accept(this, argu);
        return n.f1.accept(this, argu);
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
		String ret = newTemp();
		this.asm_.append("MOVE ").append(ret).append(" ").append(n.f0.toString()).append("\n");
        return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> "true"
	*/
	public Base_t visit(TrueLiteral n, Base_t argu) throws Exception {
		String ret = newTemp();
		this.asm_.append("MOVE ").append(ret).append(" 1\n");
        return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> "false"
	*/
	public Base_t visit(FalseLiteral n, Base_t argu) throws Exception {
		String ret = newTemp();
		this.asm_.append("MOVE ").append(ret).append(" 0\n");
        return new Variable_t(null, null, ret);
	}

	/**
	* f0 -> <IDENTIFIER>
	*/
	public Base_t visit(Identifier n, Base_t argu) throws Exception {
		String id = n.f0.toString();
		if (argu == null)
			return new Variable_t(null, id);
		Class_t cl = st_.get(argu.getName());
		if (cl != null) {									// if argu is a class name
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
				cl = st_.get(meth.getFrom_class_().getName());
				if (cl == null) {
                    throw new Exception("something went wrong 2");
                }
				var = cl.classContainsVar(id);
				if (var == null) {
                    return new Variable_t(null, id);
                }
				String newTemp = newTemp();
				this.asm_.append("HLOAD ").append(newTemp).append(" TEMP 0 ").append(var.getNum() * 4).append("\n");
                return new Variable_t(var.getType(), id, newTemp);
			}
		}
	}

	/**
	* f0 -> "this"
	*/
	public Base_t visit(ThisExpression n, Base_t argu) throws Exception {
        Class_t cl = ((Method_t) argu).getFrom_class_();
        return new Variable_t(cl.getName(), "this", "TEMP 0");
	}

	/**
	* f0 -> "new"
	* f1 -> "int"
	* f2 -> "["
	* f3 -> Expression()
	* f4 -> "]"
	*/
	public Base_t visit(ArrayAllocationExpression n, Base_t argu) throws Exception {
		String lstart = labels_.newLabel();
		String lend = labels_.newLabel();
		String noerror = labels_.newLabel();
		String expr = ((Variable_t) n.f3.accept(this, argu)).getRegister();
		String zero = newTemp();
		String cnt = newTemp();
		String cond = newTemp();
		String size = newTemp();
		String alloc_sz = newTemp();
		String array = newTemp();
		String array_addr = newTemp();
		this.asm_.append("MOVE ").append(cond).append(" LT ").append(expr).append(" 0\n"); 						// check if given length > 0
		this.asm_.append("CJUMP ").append(cond).append(" ").append(noerror).append("\n");
		this.asm_.append("ERROR\n").append(noerror).append(" NOOP\n");
		this.asm_.append("MOVE ").append(size).append(" PLUS ").append(expr).append(" 1\n"); 						// create room for arraylength
		this.asm_.append("MOVE ").append(alloc_sz).append(" TIMES ").append(size).append(" 4\n");					// *4 for bytes
		this.asm_.append("MOVE ").append(array).append(" HALLOCATE ").append(alloc_sz).append("\n"); 				// allocate
		this.asm_.append("HSTORE ").append(array).append(" 0 ").append(expr).append("\n");							// store array length in first position
		this.asm_.append("MOVE ").append(array_addr).append(" ").append(array).append("\nMOVE ").append(cnt).append(" 4\n");	// keep array address and init a counter
		this.asm_.append(lstart).append(" NOOP\nMOVE ").append(cond).append(" LT ").append(cnt).append(" ").append(alloc_sz).append("\n");
		this.asm_.append("CJUMP ").append(cond).append(" ").append(lend).append("\n"); 							// if !cond goto end
		this.asm_.append("MOVE ").append(array).append(" PLUS ").append(array).append(" 4\n");						// &array++
		this.asm_.append("MOVE ").append(zero).append(" 0\n");
		this.asm_.append("HSTORE ").append(array).append(" 0 ").append(zero).append("\n");
		this.asm_.append("MOVE ").append(cnt).append(" PLUS ").append(cnt).append(" 4\n");							// cnt++
		this.asm_.append("JUMP ").append(lstart).append("\n").append(lend).append(" NOOP\n");						// loop
        return new Variable_t(null, null, array_addr);
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
		String t = newTemp();
		String vtable = newTemp();
		String vtable_addr = newTemp();
		String label = labels_.newVTableLabel(cl.getName());
        this.asm_.append("MOVE ").append(t).append(" HALLOCATE ").append((1 + cl.getNumVars()) * 4).append("\n");
        this.asm_.append("MOVE ").append(vtable_addr).append(" ").append(label).append("\n");
        this.asm_.append("HLOAD ").append(vtable).append(" ").append(vtable_addr).append(" 0\n");
        this.asm_.append("HSTORE ").append(t).append(" 0 ").append(vtable).append("\n");
		String zero = newTemp();
        this.asm_.append("MOVE ").append(zero).append(" 0\n");
        for (int i = 1 ; i <= cl.getNumVars() ; i++) {
            this.asm_.append("HSTORE ").append(t).append(" ").append(i * 4).append(" ").append(zero).append("\n");
        }
        this.asm_.append("\n");
        return new Variable_t(id, id, t);
	}

	/**
	* f0 -> "!"
	* f1 -> Clause()
	*/
	public Base_t visit(NotExpression n, Base_t argu) throws Exception {
		String ret = newTemp();
		String t = ((Variable_t) n.f1.accept(this, argu)).getRegister();
		String one = newTemp();
		this.asm_.append("MOVE ").append(one).append(" 1\n");
		this.asm_.append("MOVE ").append(ret).append(" MINUS ").append(one).append(" ").append(t).append("\n");
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