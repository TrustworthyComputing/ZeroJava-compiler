package org.twc.zerojavacompiler.spiglet2kanga;

import java.util.*;
import org.twc.zerojavacompiler.spigletsyntaxtree.*;
import org.twc.zerojavacompiler.spigletvisitor.*;

public class GetFlowGraphVertex extends GJNoArguDepthFirst<String> {

	HashMap<String, Method> method_map_;
	HashMap<String, Integer> mLabel;
	Method currMethod;
	int vid = 0;

	public GetFlowGraphVertex(HashMap<String, Method> method_map_, HashMap<String, Integer> mLabel) {
		this.method_map_ = method_map_;
		this.mLabel = mLabel;
	}

	// StmtList ::= ( (Label)?Stmt)*
	// get Labels
	public String visit(NodeOptional n) throws Exception {
		if (n.present()) {
			mLabel.put(n.node.accept(this), vid);
		}
		return null;
	}

	/**
	 * f0 -> "MAIN"
	 * f1 -> StmtList()
	 * f2 -> "END"
	 * f3 -> ( Procedure() )*
	 * f4 -> <EOF>
	 */
	public String visit(Goal n) throws Exception {
		currMethod = new Method("MAIN", 0);
		method_map_.put("MAIN", currMethod);
		vid = 0;
		// begin
		currMethod.flowGraph.addVertex(vid);
		vid++;
		n.f1.accept(this);
		// end
		currMethod.flowGraph.addVertex(vid);
		n.f3.accept(this);
		return null;
	}

	/**
	 * f0 -> Label()
	 * f1 -> "["
	 * f2 -> IntegerLiteral()
	 * f3 -> "]"
	 * f4 -> StmtExp()
	 */
	public String visit(Procedure n) throws Exception {
		vid = 0;
		String methodName = n.f0.f0.toString();
		int paramNum = Integer.parseInt(n.f2.accept(this));
		currMethod = new Method(methodName, paramNum);
		method_map_.put(methodName, currMethod);
		n.f4.accept(this);
		return null;
	}

	/**
	 * f0 -> NoOpStmt()
	 * | ErrorStmt()
	 * | CJumpStmt()
	 * | JumpStmt()
	 * | HStoreStmt()
	 * | HLoadStmt()
	 * | MoveStmt()
	 * | PrintStmt()
	 */
	public String visit(Stmt n) throws Exception {
		// Every Statement -> Vertex
		currMethod.flowGraph.addVertex(vid);
		n.f0.accept(this);
		vid++;
		return null;
	}

	/**
	 * f0 -> "CALL"
	 * f1 -> SimpleExp()
	 * f2 -> "("
	 * f3 -> ( Temp() )*
	 * f4 -> ")"
	 */
	public String visit(Call n) throws Exception {
		n.f1.accept(this);
		n.f3.accept(this);
		currMethod.flowGraph.callPos.add(vid);
		// callParamNum uses the MAX
		if (currMethod.callParamNum < n.f3.size())
			currMethod.callParamNum = n.f3.size();
		return null;
	}

	/**
	 * f0 -> "BEGIN"
	 * f1 -> StmtList()
	 * f2 -> "RETURN"
	 * f3 -> SimpleExp()
	 * f4 -> "END"
	 */
	public String visit(StmtExp n) throws Exception {
		// begin
		currMethod.flowGraph.addVertex(vid);
		vid++;
		n.f1.accept(this);
		// return
		currMethod.flowGraph.addVertex(vid);
		vid++;
		n.f3.accept(this);
		// end
		currMethod.flowGraph.addVertex(vid);
		return null;
	}

	/**
	 * f0 -> "TEMP"
	 * f1 -> IntegerLiteral()
	 */
	public String visit(Temp n) throws Exception {
		Integer tempNo = Integer.parseInt(n.f1.accept(this));
		if (!currMethod.mTemp.containsKey(tempNo)) {
			if (tempNo < currMethod.paramNum)
				// parameter
				currMethod.mTemp.put(tempNo, new LiveInterval(tempNo, 0, vid));
			else
				// local Temp (first shows up at vid)
				currMethod.mTemp.put(tempNo, new LiveInterval(tempNo, vid, vid));
		}
		return (tempNo).toString();
	}

	/**
	 * f0 -> <INTEGER_LITERAL>
	 */
	public String visit(IntegerLiteral n) throws Exception {
		return n.f0.toString();
	}

	/**
	 * f0 -> <IDENTIFIER>
	 */
	public String visit(Label n) throws Exception {
		return n.f0.toString();
	}

}