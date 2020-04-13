import basetype.*;
import syntaxtree.*;
import visitor.*;
import symboltable.*;
import typecheck.*;
import zmipsgenerator.*;
import java.io.*;
import java.util.Map;

public class Main {
    public static void main (String [] args){
        if (args.length < 1){
            System.err.println("Usage: java Main <inputFile1> [<inputFile2>] ...");
            System.exit(1);
        }
        FileInputStream fis = null;
        int i = -1;
        while (++i < args.length) {
            try {
                fis = new FileInputStream(args[i]);
                MiniJavaParser parser = new MiniJavaParser(fis);
                Goal root = parser.Goal();
                VisitClasses firstvisit = new VisitClasses();
                try {
                    root.accept(firstvisit);
                    SymbolTableVisitor secondvisit = new SymbolTableVisitor(firstvisit.getClassList());
                    root.accept(secondvisit);
                    Map<String, Class_t> symbol_table = secondvisit.getSymbolTable();
                    // secondvisit.printSymbolTable();
                    TypeCheckVisitor type_checker = new TypeCheckVisitor(symbol_table);
                    root.accept(type_checker, null);
                    // System.out.println("\nType Check Completed Successfully!\n");

                    ZMIPSGenVisitor generator = new ZMIPSGenVisitor(symbol_table, secondvisit.getGlobalsNumber());
                    root.accept(generator, null);
                    File fp = new File(args[i]);
                    String path = fp.getPath();
                    path = path.substring(0, path.lastIndexOf('.'));
                    PrintWriter out = new PrintWriter(path + ".zmips");
                    out.print(generator.getASM());
                    System.out.print(generator.getASM());
                    out.close();
                } catch (Exception ex) {
                    System.out.println("\n\nAn error occured: " + ex.getMessage() + "\n\n");
                }
            } catch(Exception ex) {
                System.err.println(ex.getMessage());
            } finally {
                try {
                    if (fis != null)
                        fis.close();
                } catch(IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}
