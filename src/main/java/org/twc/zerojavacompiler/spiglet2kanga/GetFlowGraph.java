package org.twc.zerojavacompiler.spiglet2kanga;

import org.twc.zerojavacompiler.spigletsyntaxtree.*;
import org.twc.zerojavacompiler.spigletvisitor.*;
import java.util.*;

public class GetFlowGraph extends GJNoArguDepthFirst<String> {

	HashMap<String, Method> method_map_;
	HashMap<String, Integer> mLabel;
	Method currMethod;
	FlowGraphVertex currVertex;
	int vid = 0;
	boolean duringCall = false;

	public GetFlowGraph(HashMap<String, Method> method_map_, HashMap<String, Integer> mLabel) {
		this.method_map_ = method_map_;
		this.mLabel = mLabel;
	}

	/**
	 * f0 -> "MAIN"
	 * f1 -> StmtList()
	 * f2 -> "END"
	 * f3 -> ( Procedure() )*
	 * f4 -> <EOF>
	 */
	public String visit(Goal n) throws Exception {
		currMethod = method_map_.get("MAIN");
		// begin
		vid = 0;
		currMethod.flowGraph.addEdge(0, 1);
		vid = 1;
		n.f1.accept(this);
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
		currMethod = method_map_.get(methodName);
		n.f2.accept(this);
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
		currVertex = currMethod.flowGraph.getVertex(vid);
		n.f0.accept(this);
		vid++;
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
		currMethod.flowGraph.addEdge(vid, vid + 1);
		vid++;
		n.f1.accept(this);
		n.f3.accept(this);
		// end
		currMethod.flowGraph.addEdge(vid, vid + 1);
		return null;
	}

	/**
	 * f0 -> "NOOP"
	 */
	public String visit(NoOpStmt n) throws Exception {
		currMethod.flowGraph.addEdge(vid, vid + 1);
		return null;
	}

	/**
	 * f0 -> "ERROR"
	 */
	public String visit(ErrorStmt n) throws Exception {
		currMethod.flowGraph.addEdge(vid, vid + 1);
		return null;
	}

	/**
	 * f0 -> "CJUMP"
	 * f1 -> Temp()
	 * f2 -> Label()
	 */
	public String visit(CJumpStmt n) throws Exception {
		// Temp Use
		int tempNo = Integer.parseInt(n.f1.accept(this));
		currVertex.Use.add(tempNo);
		int jumpVid = mLabel.get(n.f2.accept(this));
		currMethod.flowGraph.addEdge(vid, vid + 1);
		currMethod.flowGraph.addEdge(vid, jumpVid);
		return null;
	}

	/**
	 * f0 -> "JUMP"
	 * f1 -> Label()
	 */
	public String visit(JumpStmt n) throws Exception {
		int jumpVid = mLabel.get(n.f1.accept(this));
		currMethod.flowGraph.addEdge(vid, jumpVid);
		return null;
	}

	/**
	 * f0 -> "HSTORE"
	 * f1 -> Temp()
	 * f2 -> IntegerLiteral()
	 * f3 -> Temp()
	 */
	public String visit(HStoreStmt n) throws Exception {
		// Temp Use
		currVertex.Use.add(Integer.parseInt(n.f1.accept(this)));
		currVertex.Use.add(Integer.parseInt(n.f3.accept(this)));
		currMethod.flowGraph.addEdge(vid, vid + 1);
		return null;
	}

	/**
	 * f0 -> "HLOAD"
	 * f1 -> Temp()
	 * f2 -> Temp()
	 * f3 -> IntegerLiteral()
	 */
	public String visit(HLoadStmt n) throws Exception {
		// Temp Def
		currVertex.Def.add(Integer.parseInt(n.f1.accept(this)));
		// Temp Use
		currVertex.Use.add(Integer.parseInt(n.f2.accept(this)));
		currMethod.flowGraph.addEdge(vid, vid + 1);
		return null;
	}

	/**
	 * f0 -> "MOVE"
	 * f1 -> Temp()
	 * f2 -> Exp()
	 */
	public String visit(MoveStmt n) throws Exception {
		// Temp Def
		currVertex.Def.add(Integer.parseInt(n.f1.accept(this)));
		currMethod.flowGraph.addEdge(vid, vid + 1);
		n.f2.accept(this);
		return null;
	}

	/**
	 * f0 -> "PRINT"
	 * f1 -> SimpleExp()
	 */
	public String visit(PrintStmt n) throws Exception {
		n.f1.accept(this);
		currMethod.flowGraph.addEdge(vid, vid + 1);
		return null;
	}

	/**
	 * f0 -> Call()
	 * | HAllocate()
	 * | BinOp()
	 * | SimpleExp()
	 */
	public String visit(Exp n) throws Exception {
		return n.f0.accept(this);
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
		duringCall = true;
		n.f3.accept(this);
		duringCall = false;
		return null;
	}

	/**
	 * f0 -> "HALLOCATE"
	 * f1 -> SimpleExp()
	 */
	public String visit(HAllocate n) throws Exception {
		return n.f1.accept(this);
	}

	/**
	 * f0 -> Operator()
	 * f1 -> Temp()
	 * f2 -> SimpleExp()
	 */
	public String visit(BinOp n) throws Exception {
		n.f0.accept(this);
		// Temp Use
		currVertex.Use.add(Integer.parseInt(n.f1.accept(this)));
		n.f2.accept(this);
		return null;
	}

	/**
	 * f0 -> Temp()
	 * | IntegerLiteral()
	 * | Label()
	 */
	public String visit(SimpleExp n) throws Exception {
		if (n.f0.which == 0) // Temp Use
			currVertex.Use.add(Integer.parseInt(n.f0.accept(this)));
		return null;
	}

	/**
	 * f0 -> "TEMP"
	 * f1 -> IntegerLiteral()
	 */
	public String visit(Temp n) throws Exception {
		Integer tempNo = Integer.parseInt(n.f1.accept(this));
		if (duringCall) // Temp Use
			currVertex.Use.add(tempNo);
		return tempNo.toString();
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