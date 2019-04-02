import syntaxtree.*;
import visitor.*;
import symbol_table.*;
import tinyram_generator.*;
import java.io.*;

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
                TinyJavaParser parser = new TinyJavaParser(fis);
                Goal root = parser.Goal();
                FirstVisitor firstvisit = new FirstVisitor();
                try {
                    root.accept(firstvisit);
                    SecondVisitor secondvisit = new SecondVisitor(firstvisit.getClassMap());
                    root.accept(secondvisit);
                    SymbolTable ST = secondvisit.getSymbolTable();
                    TinyRAMGenVisitor generator = new TinyRAMGenVisitor(ST);
                    root.accept(generator, null);
                    ST.printST();       // printing the symbol table
                    File fp = new File(args[i]);
                    String path = fp.getPath();
                    path = path.substring(0, path.lastIndexOf('.'));
                    PrintWriter out = new PrintWriter(path + ".asm");
                    out.print(generator.result);
                    out.close();
                    // Print it to stdout as well
                    System.out.println(path + ".asm\n");
                    System.out.print(generator.result);
                } catch (Exception ex) {
                    System.out.println("\n\nAn Error Occured: " + ex.getMessage() + "\n\n");
                }
            } catch(ParseException ex) {
                System.err.println(ex.getMessage());
            } catch(FileNotFoundException ex) {
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
