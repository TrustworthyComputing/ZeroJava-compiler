import syntaxtree.*;
import visitor.*;
import symbol_table.*;
import tinyram_generator.*;
import java.io.*;
import java.util.Map;
import java.util.HashMap;
import base_type.*;

public class Main {
    public static void main (String [] args){
        if (args.length < 1) {
            System.err.println("Usage: java Main <inputFile1> [<inputFile2>] ...");
            System.exit(1);
        }
        FileInputStream fis = null;
        for (String arg : args) {
            try {
                fis = new FileInputStream(arg);
                TinyJavaParser parser = new TinyJavaParser(fis);
                Goal root = parser.Goal();
                SymbolTableVisitor stvisitor = new SymbolTableVisitor();
                try {
                    root.accept(stvisitor);
                    Map<String, Method_t> symbol_table = stvisitor.getSymbolTable();
                    TinyRAMGenVisitor generator = new TinyRAMGenVisitor(symbol_table);
                    root.accept(generator, null);
                    // stvisitor.printST();
                    
                    /* Write the generated code to the same directory as .asm file */
                    File fp = new File(arg);
                    String path = fp.getPath();
                    path = path.substring(0, path.lastIndexOf('.'));
                    PrintWriter out = new PrintWriter(path + ".asm");
                    out.print(generator.result_);
                    out.close();
                    // Print it to stdout as well
                    System.out.println(path + ".asm\n");
                    System.out.print(generator.result_);
                } catch (Exception ex) {
                    System.out.println("\n\n" + ex.getMessage() + "\n");
                }
            } catch(ParseException ex) {
                System.err.println(ex.getMessage());
            } catch(FileNotFoundException ex) {
                System.err.println(ex.getMessage());
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch(IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}
