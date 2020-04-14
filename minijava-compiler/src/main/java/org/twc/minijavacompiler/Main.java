package org.twc.minijavacompiler;

import java.io.*;
import java.util.Map;
import java.util.ArrayList;
import org.twc.minijavacompiler.minijavaparser.*;
import org.twc.minijavacompiler.basetype.*;
import org.twc.minijavacompiler.minijavasyntaxtree.*;
import org.twc.minijavacompiler.symboltable.*;
import org.twc.minijavacompiler.typecheck.*;
import org.twc.minijavacompiler.zmipsgenerator.*;

public class Main {

    private static boolean DEBUG = false;

    public static void main (String[] args){
        if (args.length == 0){
            System.err.println("fatal error: no input files.");
            System.exit(-1);
        }
        ArrayList<String> input_files = new ArrayList<>();
        for (String arg : args) {
            if (arg.toUpperCase().equals("-DEBUG") || arg.toUpperCase().equals("--DEBUG")) {
                DEBUG = true;
            } else {
                input_files.add(arg);
            }
        }
        for (String arg : input_files) {
            System.out.println("============================================================================");
            System.out.println("Checking file '" + arg + "'");
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(arg);
                MiniJavaParser parser = new MiniJavaParser(fis);
                Goal root = parser.Goal();
                VisitClasses firstvisit = new VisitClasses();
                SymbolTableVisitor symtable_visit = null;
                Map<String, Class_t> symbol_table = null;
                try {
                    root.accept(firstvisit);
                    symtable_visit = new SymbolTableVisitor(firstvisit.getClassList());
                    root.accept(symtable_visit);
                    symbol_table = symtable_visit.getSymbolTable();
                    if (DEBUG) {
                        System.out.println();
                        symtable_visit.printSymbolTable();
                    }
                    TypeCheckVisitor type_checker = new TypeCheckVisitor(symbol_table);
                    root.accept(type_checker, null);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.exit(-1);
                }
                System.out.println("[ \u2713 ] Type checking successful.");
                System.out.println("============================================================================\n");

                PrintWriter writer = null;
                try {
                    ZMIPSGenVisitor generator = new ZMIPSGenVisitor(symbol_table, symtable_visit.getGlobalsNumber());
                    root.accept(generator, null);
                    File fp = new File(arg);
                    String path = fp.getPath();
                    path = path.substring(0, path.lastIndexOf('.'));
                    writer = new PrintWriter(path + ".zmips");
                    writer.print(generator.getASM());
                    if (DEBUG) {
                        System.out.println(generator.getASM());
                    }
                    writer.close();

                    System.out.println("============================================================================");
                    System.out.println("[ \u2713 ] zMIPS code generated to " + path + ".zmips");
                    System.out.println("============================================================================");
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    if (writer != null) writer.close();
                }
            } catch (ParseException ex) {
                System.out.println(ex.getMessage());
            } catch (FileNotFoundException ex) {
                System.err.println(ex.getMessage());
            } finally {
                try {
                    if (fis != null) fis.close();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}
