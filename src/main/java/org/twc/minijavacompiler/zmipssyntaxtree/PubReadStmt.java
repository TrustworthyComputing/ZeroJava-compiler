//
// Generated by JTB 1.3.2 DIT@UoA patched
//

package org.twc.minijavacompiler.zmipssyntaxtree;

/**
 * Grammar production:
 * f0 -> "pubread"
 * f1 -> Register()
 */
public class PubReadStmt implements Node {
   public NodeToken f0;
   public Register f1;

   public PubReadStmt(NodeToken n0, Register n1) {
      f0 = n0;
      f1 = n1;
   }

   public PubReadStmt(Register n0) {
      f0 = new NodeToken("pubread");
      f1 = n0;
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

