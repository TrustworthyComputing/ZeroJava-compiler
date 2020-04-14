//
// Generated by JTB 1.3.2 DIT@UoA patched
//

package org.twc.minijavacompiler.zmipssyntaxtree;

/**
 * Grammar production:
 * f0 -> "sw"
 * f1 -> ","
 * f2 -> SimpleExp()
 * f3 -> "("
 * f4 -> SimpleExp()
 * f5 -> ")"
 */
public class SwStmt implements Node {
   public NodeToken f0;
   public NodeToken f1;
   public SimpleExp f2;
   public NodeToken f3;
   public SimpleExp f4;
   public NodeToken f5;

   public SwStmt(NodeToken n0, NodeToken n1, SimpleExp n2, NodeToken n3, SimpleExp n4, NodeToken n5) {
      f0 = n0;
      f1 = n1;
      f2 = n2;
      f3 = n3;
      f4 = n4;
      f5 = n5;
   }

   public SwStmt(SimpleExp n0, SimpleExp n1) {
      f0 = new NodeToken("sw");
      f1 = new NodeToken(",");
      f2 = n0;
      f3 = new NodeToken("(");
      f4 = n1;
      f5 = new NodeToken(")");
   }

   public void accept(org.twc.minijavacompiler.zmipsvisitor.Visitor v) throws Exception {
      v.visit(this);
   }
   public <R,A> R accept(org.twc.minijavacompiler.zmipsvisitor.GJVisitor<R,A> v, A argu) throws Exception {
      return v.visit(this,argu);
   }
   public <R> R accept(org.twc.minijavacompiler.zmipsvisitor.GJNoArguVisitor<R> v) throws Exception {
      return v.visit(this);
   }
   public <A> void accept(org.twc.minijavacompiler.zmipsvisitor.GJVoidVisitor<A> v, A argu) throws Exception {
      v.visit(this,argu);
   }
}

