//
// Generated by JTB 1.3.2 DIT@UoA patched
//

package org.twc.minijavacompiler.zmipssyntaxtree;

/**
 * The interface which NodeList, NodeListOptional, and NodeSequence
 * implement.
 */
public interface NodeListInterface extends Node {
   public void addNode(Node n);
   public Node elementAt(int i);
   public java.util.Enumeration<Node> elements();
   public int size();

   public void accept(org.twc.minijavacompiler.zmipsvisitor.Visitor v) throws Exception;
   public <R,A> R accept(org.twc.minijavacompiler.zmipsvisitor.GJVisitor<R,A> v, A argu) throws Exception;
   public <R> R accept(org.twc.minijavacompiler.zmipsvisitor.GJNoArguVisitor<R> v) throws Exception;
   public <A> void accept(org.twc.minijavacompiler.zmipsvisitor.GJVoidVisitor<A> v, A argu) throws Exception;
}

