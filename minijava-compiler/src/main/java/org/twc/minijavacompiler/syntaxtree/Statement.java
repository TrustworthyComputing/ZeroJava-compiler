//
// Generated by JTB 1.3.2 DIT@UoA patched
//

package org.twc.minijavacompiler.syntaxtree;

/**
 * Grammar production:
 * f0 -> Block()
 *       | AssignmentStatement()
 *       | ArrayAssignmentStatement()
 *       | IfStatement()
 *       | WhileStatement()
 *       | PrintStatement()
 *       | AnswerStatement()
 */
public class Statement implements Node {
   public NodeChoice f0;

   public Statement(NodeChoice n0) {
      f0 = n0;
   }

   public void accept(org.twc.minijavacompiler.visitor.Visitor v) throws Exception {
      v.visit(this);
   }
   public <R,A> R accept(org.twc.minijavacompiler.visitor.GJVisitor<R,A> v, A argu) throws Exception {
      return v.visit(this,argu);
   }
   public <R> R accept(org.twc.minijavacompiler.visitor.GJNoArguVisitor<R> v) throws Exception {
      return v.visit(this);
   }
   public <A> void accept(org.twc.minijavacompiler.visitor.GJVoidVisitor<A> v, A argu) throws Exception {
      v.visit(this,argu);
   }
}

