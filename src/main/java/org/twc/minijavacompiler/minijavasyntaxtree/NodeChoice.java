//
// Generated by JTB 1.3.2 DIT@UoA patched
//

package org.twc.minijavacompiler.minijavasyntaxtree;

/**
 * Represents a grammar choice, e.g. ( A | B )
 */
public class NodeChoice implements Node {
   public NodeChoice(Node node) {
      this(node, -1);
   }

   public NodeChoice(Node node, int whichChoice) {
      choice = node;
      which = whichChoice;
   }

   public void accept(org.twc.minijavacompiler.minijavavisitor.Visitor v) throws Exception {
      choice.accept(v);
   }
   public <R,A> R accept(org.twc.minijavacompiler.minijavavisitor.GJVisitor<R,A> v, A argu) throws Exception {
      return choice.accept(v,argu);
   }
   public <R> R accept(org.twc.minijavacompiler.minijavavisitor.GJNoArguVisitor<R> v) throws Exception {
      return choice.accept(v);
   }
   public <A> void accept(org.twc.minijavacompiler.minijavavisitor.GJVoidVisitor<A> v, A argu) throws Exception {
      choice.accept(v,argu);
   }

   public Node choice;
   public int which;
}
