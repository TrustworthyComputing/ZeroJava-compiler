package symbol_table;

import syntaxtree.*;
import visitor.GJNoArguDepthFirst;
import java.util.LinkedList;
import base_type.*;

/* Fisrt Visitor Pattern just collects the Classes in a Class_t List */
public class FirstVisitor extends GJNoArguDepthFirst<Class_t> {
    private LinkedList<Class_t> classes;

    public FirstVisitor() { classes = new LinkedList<Class_t>(); }

    public LinkedList<Class_t> getClassList() { return classes; }

    /**
    * f0 -> "class", f1 -> Identifier(), f2 -> "{", f3 -> "public", f4 -> "static", f5 -> "void", f6 -> "main", 
    * f7 -> "(", f8 -> "Class_t", f9 -> "[", f10 -> "]", f11 -> Identifier(), f12 -> ")", f13 -> "{", 
    * f14 -> ( VarDeclaration() )*, f15 -> ( Statement() )*, f16 -> "}", f17 -> "}"
    */
    public Class_t visit(MainClass n) throws Exception {
        classes.addLast(new Class_t(n.f1.accept(this).getName(), null));
        return null;
    }

    /**
    * f0 -> "class", f1 -> Identifier(), f2 -> "{", f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*, f5 -> "}"
    */
    public Class_t visit(ClassDeclaration n) throws Exception {
        String new_id = n.f1.accept(this).getName();
        int i = 0;
        while (i < classes.size())
            if (classes.get(i++).getName().equals(new_id))
                throw new Exception("Class " + new_id + " already exists!");
        classes.addLast(new Class_t(new_id, null));
        return null;
    }

    /**
    * f0 -> "class", f1 -> Identifier(), f2 -> "extends", f3 -> Identifier(), f4 -> "{"
    * f5 -> ( VarDeclaration() )*, f6 -> ( MethodDeclaration() )*, f7 -> "}"
    */
    public Class_t visit(ClassExtendsDeclaration n) throws Exception {
        String new_id = n.f1.accept(this).getName();
        String ext_id = n.f3.accept(this).getName();
        int i = 0;
        while (i < classes.size())
            if (classes.get(i++).getName().equals(new_id))
                throw new Exception("Class " + new_id + " already exists!");
        i = 0;
        boolean found = false;
        while (i < classes.size())
            if (classes.get(i++).getName().equals(ext_id))
                found = true;
        if (!found)
            throw new Exception(ext_id + ": Cannot extend a class before declaring it!");
        classes.addLast(new Class_t(new_id, ext_id));
        return null;
    }

    /**
    * f0 -> <IDENTIFIER>
    */
    public Class_t visit(Identifier n) throws Exception {
        return new Class_t(n.f0.toString(), null);
    }

}
