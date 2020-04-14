//
// Generated by JTB 1.3.2 DIT@UoA patched
//

package org.twc.minijavacompiler.syntaxtree;

/**
 * Grammar production:
 * f0 -> "~"
 * f1 -> PrimaryExpression()
 */
public class BinNotExpression implements Node {
   public NodeToken f0;
   public PrimaryExpression f1;

   public BinNotExpression(NodeToken n0, PrimaryExpression n1) {
      f0 = n0;
      f1 = n1;
   }

   public BinNotExpression(PrimaryExpression n0) {
      f0 = new NodeToken("~");
      f1 = n0;
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

