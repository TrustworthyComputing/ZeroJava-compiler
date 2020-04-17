package org.twc.zerojavacompiler;

import org.deri.iris.Configuration;
import org.deri.iris.KnowledgeBase;
import org.deri.iris.api.IKnowledgeBase;
import org.deri.iris.api.basics.IPredicate;
import org.deri.iris.api.basics.IQuery;
import org.deri.iris.api.basics.IRule;
import org.deri.iris.api.terms.IVariable;
import org.deri.iris.compiler.Parser;
import org.deri.iris.optimisations.magicsets.MagicSets;
import org.deri.iris.storage.IRelation;
import org.twc.zerojavacompiler.basetype.Class_t;
import org.twc.zerojavacompiler.factsgen.FactGeneratorVisitor;
import org.twc.zerojavacompiler.zerojavaparser.ZeroJavaParser;
import org.twc.zerojavacompiler.zerojavaparser.ParseException;
import org.twc.zerojavacompiler.optimizer.OptimizerVisitor;
import org.twc.zerojavacompiler.symboltable.SymbolTableVisitor;
import org.twc.zerojavacompiler.symboltable.VisitClasses;
import org.twc.zerojavacompiler.typecheck.TypeCheckVisitor;
import org.twc.zerojavacompiler.zmipsgenerator.ZMIPSGenVisitor;
import org.twc.zerojavacompiler.zmipsparser.ZMIPSParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class Main {

    private static boolean debug_ = false;
    private static boolean enable_opts_ = false;

    public static void main (String[] args){
        if (args.length == 0){
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
                // zerojava typecheck
                System.out.println("===================================================================================");
                System.out.println("Checking file \"" + arg + "\"\n");
                fis = new FileInputStream(arg);
                ZeroJavaParser zerojava_parser = new ZeroJavaParser(fis);
                org.twc.zerojavacompiler.zerojavasyntaxtree.Goal zerojava_root = zerojava_parser.Goal();
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
                System.out.println("[ \u2713 ] All checks passed");
                System.out.println("===================================================================================\n\n");

                // generate zMIPS code
                System.out.println("===================================================================================");
                System.out.println("Generating zMIPS code for \""+ arg + "\"\n");
                ZMIPSGenVisitor generator = new ZMIPSGenVisitor(symbol_table, symtable_visit.getGlobalsNumber());
                zerojava_root.accept(generator, null);
                File fp = new File(arg);
                String path = fp.getPath();
                path = path.substring(0, path.lastIndexOf('.'));
                String zmips_output_path = path + ".zmips";
                String opt_zmips_output_path = path + ".opt.zmips";
                writer = new PrintWriter(zmips_output_path);
                writer.print(generator.getASM());
                if (debug_) {
                    System.out.println(generator.getASM());
                }
                writer.close();
                System.out.println("[ \u2713 ] zMIPS code generated to \"" + zmips_output_path + "\"");
                System.out.println("===================================================================================");

                // optimize zMIPS code
                if (!enable_opts_) continue;

                System.out.println("\n\n===================================================================================");
                System.out.println("Optimizing file \"" + zmips_output_path + "\"\n");

                boolean can_optimize = true;
                Map<String, Map<String, String>> prev_optimizations_map;
                Map<String, Map<String, String>> optimizations_map = null;
                while (can_optimize) {
                    prev_optimizations_map = optimizations_map;
                    String facts_output_path = "target/Facts/" + zmips_output_path.substring(0, zmips_output_path.length() - 6);
                    Path p = Paths.get(facts_output_path);
                    if (! Files.exists(p) && !(new File(facts_output_path)).mkdirs()) {
                        throw new IOException("Error creating folder " + facts_output_path);
                    }
                    fis = new FileInputStream(zmips_output_path);
                    ZMIPSParser zmips_parser = new ZMIPSParser(fis);
                    org.twc.zerojavacompiler.zmipssyntaxtree.Goal zmips_root = zmips_parser.Goal();
                    FactGeneratorVisitor factgen_visitor = new FactGeneratorVisitor();
                    zmips_root.accept(factgen_visitor, null);
                    factgen_visitor.writeFacts(facts_output_path, debug_);
                    System.out.println("[ 1/3 ] zMIPS code relations inference phase completed");

                    Parser iris_parser = new Parser();
                    Map<IPredicate, IRelation> factMap = new HashMap<>();
                    final File factsDirectory = new File(facts_output_path);
                    if (factsDirectory.isDirectory()) {
                        for (final File fileEntry : Objects.requireNonNull(factsDirectory.listFiles())) {
                            if (fileEntry.isDirectory()) {
                                System.out.println("Omitting directory " + fileEntry.getPath());
                            } else {
                                Reader factsReader = new FileReader(fileEntry);
                                iris_parser.parse(factsReader);
                                factMap.putAll(iris_parser.getFacts()); // Retrieve the facts and put all of them in factMap
                            }
                        }
                    } else {
                        System.err.println("Invalid facts directory facts_output_path");
                        System.exit(-1);
                    }
                    File rulesFile = new File("src/main/java/org/twc/zerojavacompiler/staticanalysis/rules.iris");
                    Reader rulesReader = new FileReader(rulesFile);
                    File queriesFile = new File("src/main/java/org/twc/zerojavacompiler/staticanalysis/queries.iris");
                    Reader queriesReader = new FileReader(queriesFile);
                    iris_parser.parse(rulesReader);                                 // Parse rules file.
                    List<IRule> rules = iris_parser.getRules();                     // Retrieve the rules from the parsed file.
                    iris_parser.parse(queriesReader);                               // Parse queries file.
                    List<IQuery> queries = iris_parser.getQueries();                // Retrieve the queries from the parsed file.
                    Configuration configuration = new Configuration();              // Create a default configuration.
                    configuration.programOptmimisers.add(new MagicSets());          // Enable Magic Sets together with rule filtering.
                    IKnowledgeBase knowledgeBase = new KnowledgeBase(factMap, rules, configuration); // Create the knowledge base.
                    optimizations_map = new HashMap<>();
                    for (IQuery query : queries) { // Evaluate all queries over the knowledge base.
                        List<IVariable> variableBindings = new ArrayList<>();
                        IRelation relation = knowledgeBase.execute(query, variableBindings);
                        if (debug_) System.out.println("\n" + query.toString() + "\n" + variableBindings); // Output the variables.
                        String queryType = null;
                        switch ((query.toString())) {
                            case "?- constProp(?m, ?l, ?v, ?val).":
                                queryType = "constProp";
                                break;
                            case "?- copyProp(?m, ?l, ?v1, ?v2).":
                                queryType = "copyProp";
                                break;
                            case "?- deadCode(?m, ?i, ?v).":
                                queryType = "deadCode";
                                break;
                        }
                        if (queryType != null) {
                            Map<String, String> tempOp = new HashMap<>();
                            String str;
                            for (int r = 0; r < relation.size(); r++) {
                                str = (relation.get(r)).toString();
                                if (debug_) System.out.println(relation.get(r));
                                int line = getLine(str);
                                String meth = getMeth(str);
                                if (tempOp.get(meth + line) == null) {
                                    tempOp.put(meth + line, str);
                                } else {
                                    tempOp.put(meth + "-sec-" + line, str);
                                }
                            }
                            optimizations_map.put(queryType, tempOp);
                        } else if (debug_) {
                            for (int r = 0; r < relation.size(); r++) {
                                System.out.println(relation.get(r));
                            }
                        }
                    }
                    if (debug_) { // Print optimizations map
                        printOptMap(optimizations_map);
                    }
                    System.out.println("[ 2/3 ] Static analysis phase completed");

                    OptimizerVisitor optimizer_visitor = new OptimizerVisitor(optimizations_map);
                    zmips_root.accept(optimizer_visitor, null);
                    writer = new PrintWriter(opt_zmips_output_path);
                    if (debug_) {
                        System.out.println("\n" + optimizer_visitor.asm_);
                    }
                    writer.println(optimizer_visitor.asm_);
                    writer.close();
                    System.out.println("[ 3/3 ] Optimization phase completed");

                    can_optimize = prev_optimizations_map == null || !optMapsEquals(prev_optimizations_map, optimizations_map);
                    zmips_output_path = opt_zmips_output_path;
                    System.out.println("\n");
                }

                System.out.println("[ \u2713 ] zMIPS optimized code generated to \"" + opt_zmips_output_path + "\"");
                System.out.println("===================================================================================");
            } catch (ParseException | org.twc.zerojavacompiler.zmipsparser.ParseException | FileNotFoundException ex) {
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

    private static int getLine(String fact) {
        String []parts = fact.split(",");
        return Integer.parseInt(parts[1].substring(1));
    }

    private static String getMeth(String fact) {
        String []parts = fact.split(",");
        return parts[0].substring(2,  parts[0].length()-1);
    }

    private static boolean optMapsEquals(Map<String, Map<String, String>> opts1, Map<String, Map<String, String>> opts2) {
        // printOptMap(opts1);
        // printOptMap(opts2);
        for (String key : opts1.keySet()) {
            if (! compareMaps(opts1.get(key), opts2.get(key))) return false;
        }
        return true;
    }

    private static boolean compareMaps(Map<String, String> map1, Map<String, String> map2) {
        try {
           for (String k : map2.keySet()) {
               if (!map1.get(k).equals(map2.get(k))) {
                   return false;
               }
           }
           for (String y : map1.keySet()) {
               if (!map2.containsKey(y)) {
                   return false;
               }
           }
       } catch (NullPointerException np) {
           return false;
       }
       return true;
    }

    private static void printOptMap(Map<String, Map<String, String>> map) {
        System.out.println("\n------------- Optimizations Map --------------");
        for (Map.Entry<String, Map<String, String>> entry : map.entrySet()) {
            System.out.println(entry.getKey() + ":");
            for (Map.Entry<String, String> e : entry.getValue().entrySet()) {
                System.out.println("\t" + e.getKey() + ":" + e.getValue());
            }
        }
        System.out.println("---------------------------");
    }

}
