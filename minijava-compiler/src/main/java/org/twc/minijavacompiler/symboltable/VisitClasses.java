package org.twc.minijavacompiler.symboltable;

import org.twc.minijavacompiler.minijavasyntaxtree.*;
import org.twc.minijavacompiler.minijavavisitor.GJNoArguDepthFirst;
import org.twc.minijavacompiler.basetype.*;
import java.util.Map;
import java.util.HashMap;

/* Fisrt Visitor Pattern just collects the Classes in a Map */
public class VisitClasses extends GJNoArguDepthFirst<Class_t> {

    private Map<String, Class_t> st_;

    public VisitClasses() {
        st_ = new HashMap<>();
    }

    public Map<String, Class_t> getClassList() {
        return st_;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    public Class_t visit(MainClass n) throws Exception {
        String class_name = n.f1.accept(this).getName();
        st_.put(class_name, new Class_t(class_name, null));
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    public Class_t visit(ClassDeclaration n) throws Exception {
        String class_name = n.f1.accept(this).getName();
        if (st_.containsKey(class_name)) {
            throw new Exception("Class " + class_name + " already exists.");
        }
        st_.put(class_name, new Class_t(class_name, null));
        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    public Class_t visit(ClassExtendsDeclaration n) throws Exception {
        String class_name = n.f1.accept(this).getName();
        String class_extends_name = n.f3.accept(this).getName();
        if (st_.containsKey(class_name)) {
            throw new Exception("Class " + class_name + " already exists.");
        }
        if (! st_.containsKey(class_extends_name)) {
            throw new Exception("Class " + class_extends_name + " is not defined. Cannot extend a class before declaring it.");
        }
        st_.put(class_name, new Class_t(class_name, class_extends_name));
        return null;
    }

    /**
    * f0 -> <IDENTIFIER>
    */
    public Class_t visit(Identifier n) throws Exception {
        return new Class_t(n.f0.toString(), null);
    }

}
