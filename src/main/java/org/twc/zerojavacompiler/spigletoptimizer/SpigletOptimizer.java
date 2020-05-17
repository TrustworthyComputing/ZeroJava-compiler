package org.twc.zerojavacompiler.spigletoptimizer;

import org.twc.zerojavacompiler.spiglet2kanga.spigletparser.SpigletParser;
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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SpigletOptimizer {

    private final boolean debug_;

    public SpigletOptimizer(boolean debug) {
        this.debug_ = debug;
    }

    public void performOptimizations(File fp) throws Exception {
        String filepath_no_extension = fp.getPath();
        filepath_no_extension = filepath_no_extension.substring(0, filepath_no_extension.lastIndexOf('.'));
        String spg_filepath = filepath_no_extension + ".spg";
        System.out.println("[  \u2022  ] Optimizing Spiglet code until a fixed-point");

        boolean can_optimize = true;
        Map<String, Map<String, String>> prev_optimizations_map;
        Map<String, Map<String, String>> optimizations_map = null;
        while (can_optimize) {
            prev_optimizations_map = optimizations_map;
            String facts_output_path = "target/Facts/" + filepath_no_extension;
            Path p = Paths.get(facts_output_path);
            if (!Files.exists(p) && !(new File(facts_output_path)).mkdirs()) {
                throw new IOException("Error creating folder " + facts_output_path);
            }
            FileInputStream fis = new FileInputStream(spg_filepath);
            SpigletParser spiglet_parser = new SpigletParser(fis);
            org.twc.zerojavacompiler.spiglet2kanga.spigletsyntaxtree.Goal spiglet_root = spiglet_parser.Goal();
            FactGeneratorVisitor factgen_visitor = new FactGeneratorVisitor();
            spiglet_root.accept(factgen_visitor, null);
            factgen_visitor.writeFacts(facts_output_path, debug_);
            System.out.println("\t[ 1/3 ] Spiglet code relations inference phase completed");

            org.deri.iris.compiler.Parser iris_parser = new Parser();
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
            File rulesFile = new File("src/main/java/org/twc/zerojavacompiler/spigletoptimizer/staticanalysis/rules.iris");
            Reader rulesReader = new FileReader(rulesFile);
            File queriesFile = new File("src/main/java/org/twc/zerojavacompiler/spigletoptimizer/staticanalysis/queries.iris");
            Reader queriesReader = new FileReader(queriesFile);
            iris_parser.parse(rulesReader);
            List<IRule> rules = iris_parser.getRules();
            iris_parser.parse(queriesReader);
            List<IQuery> queries = iris_parser.getQueries();
            Configuration configuration = new Configuration();
            configuration.programOptmimisers.add(new MagicSets());
            IKnowledgeBase knowledgeBase = new KnowledgeBase(factMap, rules, configuration);
            optimizations_map = new HashMap<>();
            // Evaluate all queries over the knowledge base.
            for (IQuery query : queries) {
                List<IVariable> variableBindings = new ArrayList<>();
                IRelation relation = knowledgeBase.execute(query, variableBindings);
                if (debug_) {
                    System.out.println("\n" + query.toString() + "\n" + variableBindings);
                }
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
            System.out.println("\t[ 2/3 ] Static analysis phase completed");

            OptimizerVisitor optimizer_visitor = new OptimizerVisitor(optimizations_map);
            spiglet_root.accept(optimizer_visitor, null);
            PrintWriter writer = new PrintWriter(spg_filepath);
            if (debug_) {
                System.out.println("\n" + optimizer_visitor.getAsm());
            }
            writer.println(optimizer_visitor.getAsm());
            writer.close();
            System.out.println("\t[ 3/3 ] Performed static analysis optimizations");
            can_optimize = prev_optimizations_map == null || !optMapsEquals(prev_optimizations_map, optimizations_map);
            if (can_optimize) {
                System.out.println("\t[ \033[0;32m \u2713 \033[0m ] Optimization phase completed, proceeding to next iteration");
            } else {
                System.out.println("\t[ \033[0;32m \u2713 \033[0m ] Optimization phase completed, reached a fixed-point");
            }
        }
        System.out.println("[ \033[0;32m \u2713 \033[0m ] Spiglet optimized code generated to \"" + spg_filepath + "\"");
    }

    private static int getLine(String fact) {
        String[] parts = fact.split(",");
        return Integer.parseInt(parts[1].substring(1));
    }

    private String getMeth(String fact) {
        String[] parts = fact.split(",");
        return parts[0].substring(2, parts[0].length() - 1);
    }

    private boolean optMapsEquals(Map<String, Map<String, String>> opts1, Map<String, Map<String, String>> opts2) {
        // printOptMap(opts1);
        // printOptMap(opts2);
        for (String key : opts1.keySet()) {
            if (!compareMaps(opts1.get(key), opts2.get(key))) return false;
        }
        return true;
    }

    private boolean compareMaps(Map<String, String> map1, Map<String, String> map2) {
        try {
            for (String k : map2.keySet()) {
                if (!map1.get(k).equals(map2.get(k))) return false;
            }
            for (String y : map1.keySet()) {
                if (!map2.containsKey(y)) return false;
            }
        } catch (NullPointerException np) {
            return false;
        }
        return true;
    }

    private void printOptMap(Map<String, Map<String, String>> map) {
        System.out.println("\n================== Optimizations Map ==================");
        for (Map.Entry<String, Map<String, String>> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " : ");
            if (entry.getValue() == null) {
                System.out.println("None");
                continue;
            }
            for (Map.Entry<String, String> e : entry.getValue().entrySet()) {
                System.out.println("\t" + e.getKey() + ":" + e.getValue());
            }
        }
        System.out.println("=========================================================");
    }

}
