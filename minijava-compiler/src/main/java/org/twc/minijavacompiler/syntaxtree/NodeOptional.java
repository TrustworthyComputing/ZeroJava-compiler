//
// Generated by JTB 1.3.2 DIT@UoA patched
//

package org.twc.minijavacompiler.syntaxtree;

/**
 * Represents an grammar optional node, e.g. ( A )? or [ A ]
 */
public class NodeOptional implements Node {
   public NodeOptional() {
      node = null;
   }

   public NodeOptional(Node n) {
      addNode(n);
   }

   public void addNode(Node n)  {
      if ( node != null)                // Oh oh!
         throw new Error("Attempt to set optional node twice");

      node = n;
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
   public boolean present()   { return node != null; }

   public Node node;
}

