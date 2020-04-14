//
// Generated by JTB 1.3.2 DIT@UoA patched
//

package org.twc.minijavacompiler.minijavasyntaxtree;

/**
 * Grammar production:
 * f0 -> Type()
 * f1 -> Identifier()
 * f2 -> ";"
 */
public class VarDeclaration implements Node {
   public Type f0;
   public Identifier f1;
   public NodeToken f2;

   public VarDeclaration(Type n0, Identifier n1, NodeToken n2) {
      f0 = n0;
      f1 = n1;
      f2 = n2;
   }

   public VarDeclaration(Type n0, Identifier n1) {
      f0 = n0;
      f1 = n1;
      f2 = new NodeToken(";");
   }

   public void accept(org.twc.minijavacompiler.minijavavisitor.Visitor v) throws Exception {
      v.visit(this);
   }
   public <R,A> R accept(org.twc.minijavacompiler.minijavavisitor.GJVisitor<R,A> v, A argu) throws Exception {
      return v.visit(this,argu);
   }
   public <R> R accept(org.twc.minijavacompiler.minijavavisitor.GJNoArguVisitor<R> v) throws Exception {
      return v.visit(this);
   }
   public <A> void accept(org.twc.minijavacompiler.minijavavisitor.GJVoidVisitor<A> v, A argu) throws Exception {
      v.visit(this,argu);
   }
}

