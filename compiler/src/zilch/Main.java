import syntaxtree.*;
import visitor.*;
import symbol_table.*;
import zmips_generator.*;
import java.io.*;
import java.util.Map;
import java.util.HashMap;
import base_type.*;

public class Main {
       
    public static void main (String [] args){
        if (args.length < 1) {
            System.err.println("Usage: java Main <inputFile1.zl> [<inputFile2.zl>] ...");
            System.exit(1);
        }
        FileInputStream fis = null;
        for (String arg : args) {
            try {
                fis = new FileInputStream(arg);
                ZilchParser parser = new ZilchParser(fis);
                Goal root = parser.Goal();
                SymbolTableVisitor stvisitor = new SymbolTableVisitor();
                try {
                    root.accept(stvisitor);
                    Map<String, Method_t> symbol_table = stvisitor.getSymbolTable();
                    ZMIPSGenVisitor generator = new ZMIPSGenVisitor(symbol_table);
                    root.accept(generator, null);
                    // stvisitor.printST();
                    /* Write the generated code to the same directory as .asm file */
                    File fp = new File(arg);
                    String ext = getFileExtension(fp);
                    String name = getFileNameWithoutExt(fp);
                    String path = getFilePath(fp);
                    if (! ext.equals("zl")) {
                        throw new IOException("Input files should end with a '.zl' extension.");
                    }
                    String outfile = path + "/" + name + ".asm";
                    PrintWriter out = new PrintWriter(outfile);
                    out.print(generator.result_);
                    out.close();
                    // Print it to stdout as well
                    System.out.println(outfile + "\n");
                    System.out.print(generator.result_);
                } catch (Exception ex) {
                    System.out.println("\n" + ex.getMessage() + "\n");
                }
            } catch(ParseException ex) {
                System.err.println(ex.getMessage());
            } catch(FileNotFoundException ex) {
                System.err.println(ex.getMessage());
            } finally {
                try {
                    if (fis != null) { fis.close(); }
                } catch(IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
    
    private static String getFileExtension(File file) {
        String fileName = file.getName();
        if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        }
        return "";
    }
    
    private static String getFilePath(File file) {
        String filepath = file.getPath();
        return filepath.substring(0, filepath.lastIndexOf('/'));
    }
    
    private static String getFileNameWithoutExt(File file) {
        String filename = file.getName();
        return filename.substring(0, filename.lastIndexOf('.'));
    }

}
