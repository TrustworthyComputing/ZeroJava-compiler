package org.twc.minijavacompiler;

import java.nio.file.*;
import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.twc.minijavacompiler.minijavaparser.*;
import org.twc.minijavacompiler.basetype.*;
import org.twc.minijavacompiler.minijavasyntaxtree.*;
import org.twc.minijavacompiler.symboltable.*;
import org.twc.minijavacompiler.typecheck.*;
import org.twc.minijavacompiler.zmipsgenerator.*;

import org.twc.minijavacompiler.zmipsparser.*;
import org.twc.minijavacompiler.zmipssyntaxtree.*;
import org.twc.minijavacompiler.zmipsvisitor.*;
import org.twc.minijavacompiler.factsgen.*;
import org.twc.minijavacompiler.optimizer.*;

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
                // minijava typecheck
                System.out.println("===================================================================================");
                System.out.println("Checking file \"" + arg + "\"\n");
                fis = new FileInputStream(arg);
                MiniJavaParser minijava_parser = new MiniJavaParser(fis);
                org.twc.minijavacompiler.minijavasyntaxtree.Goal minijava_root = minijava_parser.Goal();
                VisitClasses firstvisit = new VisitClasses();
                minijava_root.accept(firstvisit);
                System.out.println("[ 1/3 ] Class name collection phase completed");
                SymbolTableVisitor symtable_visit = new SymbolTableVisitor(firstvisit.getClassList());
                minijava_root.accept(symtable_visit);
                Map<String, Class_t> symbol_table = symtable_visit.getSymbolTable();
                if (debug_) {
                    System.out.println();
                    symtable_visit.printSymbolTable();
                }
                System.out.println("[ 2/3 ] Class members and methods info collection phase completed");
                TypeCheckVisitor type_checker = new TypeCheckVisitor(symbol_table);
                minijava_root.accept(type_checker, null);
                System.out.println("[ 3/3 ] Type checking phase completed");
                System.out.println("[ \u2713 ] All checks passed");
                System.out.println("===================================================================================\n\n");

                // generate zMIPS code
                System.out.println("===================================================================================");
                System.out.println("Generating zMIPS code for \""+ arg + "\"\n");
                ZMIPSGenVisitor generator = new ZMIPSGenVisitor(symbol_table, symtable_visit.getGlobalsNumber());
                minijava_root.accept(generator, null);
                File fp = new File(arg);
                String path = fp.getPath();
                path = path.substring(0, path.lastIndexOf('.'));
                String zmips_output_path = path + ".zmips";
                writer = new PrintWriter(zmips_output_path);
                writer.print(generator.getASM());
                if (debug_) {
                    System.out.println(generator.getASM());
                }
                writer.close();
                System.out.println("[ \u2713 ] zMIPS code generated to \"" + zmips_output_path + "\"");
                System.out.println("===================================================================================\n\n");

                // optimize zMIPS code
                System.out.println("===================================================================================");
                System.out.println("Optimizing file \"" + zmips_output_path + "\"\n");
                String facts_output_path = "target/Facts/" + zmips_output_path.substring(0, zmips_output_path.length() - 6);
                Path p = Paths.get(facts_output_path);
                if (! Files.exists(p) && !(new File(facts_output_path)).mkdirs()) {
                    throw new IOException("Error creating folder " + facts_output_path);
                }
                fis = new FileInputStream(zmips_output_path);
                ZMIPSParser zmips_parser = new ZMIPSParser(fis);
                org.twc.minijavacompiler.zmipssyntaxtree.Goal zmips_root = zmips_parser.Goal();
                FactGeneratorVisitor factgen_visitor = new FactGeneratorVisitor();
                zmips_root.accept(factgen_visitor, null);
                factgen_visitor.writeFacts(facts_output_path, debug_);
                System.out.println("[ 1/3 ] zMIPS code relations inference phase completed");

                Parser iris_parser = new Parser();
                Map<IPredicate, IRelation> factMap = new HashMap<>();
                final File factsDirectory = new File(facts_output_path);
                if (factsDirectory.isDirectory()) {
                    for (final File fileEntry : factsDirectory.listFiles()) {
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
                File rulesFile = new File("src/main/java/org/twc/minijavacompiler/staticanalysis/rules.iris");
                Reader rulesReader = new FileReader(rulesFile);
                File queriesFile = new File("src/main/java/org/twc/minijavacompiler/staticanalysis/queries.iris");
                Reader queriesReader = new FileReader(queriesFile);
                iris_parser.parse(rulesReader);                                 // Parse rules file.
                List<IRule> rules = iris_parser.getRules();                     // Retrieve the rules from the parsed file.
                iris_parser.parse(queriesReader);                               // Parse queries file.
                List<IQuery> queries = iris_parser.getQueries();                // Retrieve the queries from the parsed file.
                Configuration configuration = new Configuration();              // Create a default configuration.
                configuration.programOptmimisers.add(new MagicSets());          // Enable Magic Sets together with rule filtering.
                IKnowledgeBase knowledgeBase = new KnowledgeBase(factMap, rules, configuration); // Create the knowledge base.
                Map<String, Map<String, String>> optimisations_map = new HashMap<>();
                for (IQuery query : queries) { // Evaluate all queries over the knowledge base.
                    List<IVariable> variableBindings = new ArrayList<>();
                    IRelation relation = knowledgeBase.execute(query, variableBindings);
                    if (debug_) System.out.println("\n" + query.toString() + "\n" + variableBindings); // Output the variables.
                    String queryType = null;
                    if ((query.toString()).equals("?- constProp(?m, ?l, ?v, ?val).")) {
                        queryType = "constProp";
                    } else if ((query.toString()).equals("?- copyProp(?m, ?l, ?v1, ?v2).")) {
                        queryType = "copyProp";
                    } else if ((query.toString()).equals("?- deadCode(?m, ?i, ?v).")) {
                        queryType = "deadCode";
                    }
                    if (queryType != null) {
                        Map<String, String> tempOp = new HashMap<>();
                        String str = null;
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
                        optimisations_map.put(queryType, tempOp);
                    } else if (debug_) {
                        for (int r = 0; r < relation.size(); r++) {
                            System.out.println(relation.get(r));
                        }
                    }
                }
                /* Print optimizations map */
                if (debug_) {
                    System.out.println("\n------------- Optimizations Map --------------");
                    for (Map.Entry<String, Map<String, String>> entry : optimisations_map.entrySet()) {
                        System.out.println(entry.getKey() + ":");
                        for (Map.Entry<String, String> e : entry.getValue().entrySet()) {
                            System.out.println("\t" + e.getKey() + ":" + e.getValue().toString());
                        }
                    }
                    System.out.println("---------------------------");
                }
                System.out.println("[ 2/3 ] Static analysis phase completed");

                OptimizerVisitor optimizer_visitor = new OptimizerVisitor(optimisations_map);
                zmips_root.accept(optimizer_visitor, null);
                String opt_zmips_output_path = zmips_output_path.substring(0, zmips_output_path.length() - 6);
                if (opt_zmips_output_path.endsWith(".opt")) {
                    opt_zmips_output_path = opt_zmips_output_path.substring(0, opt_zmips_output_path.length() - 4);
                }
                opt_zmips_output_path = opt_zmips_output_path + ".opt.zmips";
                writer = new PrintWriter(opt_zmips_output_path);
                if (debug_) {
                    System.out.println("\n" + optimizer_visitor.result);
                }
                writer.println(optimizer_visitor.result);
                writer.close();
                System.out.println("[ 3/3 ] Optimization phase completed");

                System.out.println("[ \u2713 ] zMIPS optimized code generated to \"" + opt_zmips_output_path + "\"");
                System.out.println("===================================================================================");
            } catch (org.twc.minijavacompiler.minijavaparser.ParseException ex) {
                ex.printStackTrace();
            } catch (org.twc.minijavacompiler.zmipsparser.ParseException ex) {
                ex.printStackTrace();
            } catch (FileNotFoundException ex) {
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

    static int getLine(String fact) {
        String []parts = fact.split(",");
        return Integer.parseInt(parts[1].substring(1));
    }

    static String getMeth(String fact) {
        String []parts = fact.split(",");
        return parts[0].substring(2,  parts[0].length()-1);
    }

}
