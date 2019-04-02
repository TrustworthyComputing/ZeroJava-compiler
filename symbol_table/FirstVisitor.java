package symbol_table;

import syntaxtree.*;
import visitor.GJNoArguDepthFirst;
import java.util.LinkedList;
import base_type.*;
import java.util.Map;
import java.util.HashMap;


/* Fisrt Visitor Pattern just collects the Classes in a Class_t List */
public class FirstVisitor extends GJNoArguDepthFirst<Class_t> {
    private Map<String, Class_t> classMap_;

    public FirstVisitor() { 
        classMap_ = new HashMap<>();
    }

    public Map<String, Class_t> getClassMap() {
        return classMap_;
    }

    /**
    * f0 -> "class", f1 -> Identifier(), f2 -> "{", f3 -> "public", f4 -> "static", f5 -> "void", f6 -> "main", 
    * f7 -> "(", f8 -> "Class_t", f9 -> "[", f10 -> "]", f11 -> Identifier(), f12 -> ")", f13 -> "{", 
    * f14 -> ( VarDeclaration() )*, f15 -> ( Statement() )*, f16 -> "}", f17 -> "}"
    */
    public Class_t visit(MainClass n) throws Exception {
        String cname = n.f1.accept(this).getName();
        classMap_.put(cname, new Class_t(cname, null));
        return null;
    }

    /**
    * f0 -> "class", f1 -> Identifier(), f2 -> "{", f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*, f5 -> "}"
    */
    public Class_t visit(ClassDeclaration n) throws Exception {
        String cname = n.f1.accept(this).getName();
        if (classMap_.containsKey(cname)) {
            throw new Exception("Class " + cname + " already exists!");
        }
        classMap_.put(cname, new Class_t(cname, null));
        return null;
    }

    /**
    * f0 -> <IDENTIFIER>
    */
    public Class_t visit(Identifier n) throws Exception {
        return new Class_t(n.f0.toString(), null);
    }

}
