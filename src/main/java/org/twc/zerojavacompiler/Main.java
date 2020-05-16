package org.twc.zerojavacompiler;

import org.twc.zerojavacompiler.basetype.Class_t;
import org.twc.zerojavacompiler.basetype.Method_t;
import org.twc.zerojavacompiler.spigletoptimizer.SpigletOptimizer;
import org.twc.zerojavacompiler.zerojava2spiglet.SymbolTableVisitor;
import org.twc.zerojavacompiler.zerojava2spiglet.TypeCheckVisitor;
import org.twc.zerojavacompiler.zerojava2spiglet.VisitClasses;
import org.twc.zerojavacompiler.zerojava2spiglet.ZeroJava2Spiglet;
import org.twc.zerojavacompiler.zerojava2spiglet.zerojavaparser.ParseException;
import org.twc.zerojavacompiler.zerojava2spiglet.zerojavaparser.ZeroJavaParser;
import org.twc.zerojavacompiler.spiglet2kanga.GetFlowGraph;
import org.twc.zerojavacompiler.spiglet2kanga.GetFlowGraphVertex;
import org.twc.zerojavacompiler.spiglet2kanga.Spiglet2Kanga;
import org.twc.zerojavacompiler.spiglet2kanga.Temp2Reg;
import org.twc.zerojavacompiler.spiglet2kanga.spigletparser.SpigletParser;
import org.twc.zerojavacompiler.kanga2zmips.Kanga2zMIPS;
import org.twc.zerojavacompiler.kanga2zmips.kangaparser.KangaParser;

import java.io.*;
import java.util.*;

public class Main {

    private static boolean debug_ = false;
    private static boolean enable_opts_ = false;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("fatal error: no input files.");
            System.exit(-1);
        }
        ArrayList<String> input_files = new ArrayList<>();
        for (String arg : args) {
            if (arg.toUpperCase().equals("-DEBUG") || arg.toUpperCase().equals("--DEBUG")) {
                debug_ = true;
            } else if (arg.toUpperCase().equals("-OPTS") || arg.toUpperCase().equals("--OPTS")) {
                enable_opts_ = true;
            } else {
                input_files.add(arg);
            }
        }
        for (String arg : input_files) {
            FileInputStream fis = null;
            PrintWriter writer = null;
            try {
                // zerojava2spiglet
                System.out.println("===================================================================================");
                System.out.println("Compiling file \"" + arg + "\"");
                fis = new FileInputStream(arg);
                ZeroJavaParser zerojava_parser = new ZeroJavaParser(fis);
                org.twc.zerojavacompiler.zerojava2spiglet.zerojavasyntaxtree.Goal zerojava_root = zerojava_parser.Goal();
                VisitClasses firstvisit = new VisitClasses();
                zerojava_root.accept(firstvisit);
                System.out.println("[ 1/3 ] Class name collection phase completed");
                SymbolTableVisitor symtable_visit = new SymbolTableVisitor(firstvisit.getClassList());
                zerojava_root.accept(symtable_visit);
                Map<String, Class_t> symbol_table = symtable_visit.getSymbolTable();
                if (debug_) {
                    System.out.println();
                    symtable_visit.printSymbolTable();
                }
                System.out.println("[ 2/3 ] Class members and methods info collection phase completed");
                TypeCheckVisitor type_checker = new TypeCheckVisitor(symbol_table);
                zerojava_root.accept(type_checker, null);
                System.out.println("[ 3/3 ] Type checking phase completed");
                System.out.println("[ \033[0;32m \u2713 \033[0m ] All checks passed");

                // generate Spiglet code
                ZeroJava2Spiglet zerojava2spiglet = new ZeroJava2Spiglet(symbol_table, symtable_visit.getGlobalsNumber());
                zerojava_root.accept(zerojava2spiglet, null);
                File fp = new File(arg);
                String path = fp.getPath();
                path = path.substring(0, path.lastIndexOf('.'));
                String spiglet_output_path = path + ".spg";
                writer = new PrintWriter(spiglet_output_path);
                writer.print(zerojava2spiglet.getASM());
                if (debug_) {
                    System.out.println(zerojava2spiglet.getASM());
                }
                int hp_ = zerojava2spiglet.getHP();
                writer.close();
                System.out.println("[ \033[0;32m \u2713 \033[0m ] Spiglet code generated to \"" + spiglet_output_path + "\"");

                // optimize zMIPS code
                if (enable_opts_) {
                    SpigletOptimizer spq_optimizer = new SpigletOptimizer(debug_);
                    spq_optimizer.performOptimizations(fp);
                }

                // generate Kanga code
                fis = new FileInputStream(spiglet_output_path);
                SpigletParser spiglet_parser = new SpigletParser(fis);
                org.twc.zerojavacompiler.spiglet2kanga.spigletsyntaxtree.Node spiglet_ast = spiglet_parser.Goal();
                HashMap<String, Method_t> method_map_ = new HashMap<>();
                HashMap<String, Integer> mLabel = new HashMap<>();
                spiglet_ast.accept(new GetFlowGraphVertex(method_map_, mLabel));
                spiglet_ast.accept(new GetFlowGraph(method_map_, mLabel));
                System.out.println("[ 1/3 ] Flow graph creation phase completed");
                new Temp2Reg(method_map_).LinearScan();
                System.out.println("[ 2/3 ] Linear scan on flow graph phase completed");
                Spiglet2Kanga spiglet2kanga = new Spiglet2Kanga(method_map_);
                spiglet_ast.accept(spiglet2kanga);
                System.out.println("[ 3/3 ] Register allocation phase completed");
                path = fp.getPath();
                path = path.substring(0, path.lastIndexOf('.'));
                String kanga_output_path = path + ".kg";
                writer = new PrintWriter(kanga_output_path);
                writer.print(spiglet2kanga.getASM());
                if (debug_) {
                    System.out.println(spiglet2kanga.getASM());
                }
                writer.close();
                System.out.println("[ \033[0;32m \u2713 \033[0m ] Kanga code generated to \"" + kanga_output_path + "\"");


                // generate zMIPS code from Kanga
                fis = new FileInputStream(kanga_output_path);
                KangaParser kanga_parser = new KangaParser(fis);
                org.twc.zerojavacompiler.kanga2zmips.kangasyntaxtree.Node kanga_ast = kanga_parser.Goal();
                Kanga2zMIPS kanga2zmips = new Kanga2zMIPS(hp_);
                kanga_ast.accept(kanga2zmips);
                path = fp.getPath();
                path = path.substring(0, path.lastIndexOf('.'));
                String zmips_output_path = path + ".zmips";
                writer = new PrintWriter(zmips_output_path);
                writer.print(kanga2zmips.getASM());
                if (debug_) {
                    System.out.println(kanga2zmips.getASM());
                }
                writer.close();
                System.out.println("[ \033[0;32m \u2713 \033[0m ] zMIPS code generated to \"" + zmips_output_path + "\"");
                System.out.println("===================================================================================");
            } catch (ParseException
                    | org.twc.zerojavacompiler.spiglet2kanga.spigletparser.ParseException
                    | org.twc.zerojavacompiler.kanga2zmips.kangaparser.ParseException
                    | FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                try {
                    if (fis != null) fis.close();
                    if (writer != null) writer.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.exit(-1);
                }
            }
        }
    }

}
