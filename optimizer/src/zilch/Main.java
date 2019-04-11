import syntaxtree.*;
import visitor.*;
import facts_gen.*;
import optimizer.*;
import java.io.*;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.*;
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
    
    public static void main (String [] args){
        if (args.length < 1){
            System.err.println("Usage: java Main <inputFile1> [<inputFile2>] ...");
            System.exit(1);
        }
        FileInputStream fis = null;
        for (String arg : args) {
            try {
                fis = new FileInputStream(arg);
                TinyRAMParser spigParser = new TinyRAMParser(fis);
                Goal root = spigParser.Goal();
                File fp = new File(arg);
                String ext = getFileExtension(fp);
                String name = getFileNameWithoutExt(fp);
                String abs_path = getFilePath(fp);
                String path = "facts_rules/Facts/" + abs_path;
                abs_path = abs_path + "/" + name;
                try {
                    if (! ext.equals("asm")) {
                        throw new IOException("Input files should end with a '.asm' extension.");
                    }
                    Path p = Paths.get(path);
                    if (! Files.exists(p) && !(new File(path)).mkdirs()) {
                        throw new IOException("Error creating folder " + path);
                    } 
                    FactGeneratorVisitor dlVisitor = new FactGeneratorVisitor();
                    root.accept(dlVisitor, null);                  
                    writeFacts(dlVisitor, path);

                    Parser parser = new Parser();
                    Map<IPredicate, IRelation> factMap = new HashMap<>();
                    final File factsDirectory = new File(path);
                    if (factsDirectory.isDirectory()) {
                        for (final File fileEntry : factsDirectory.listFiles()) {
                            if (fileEntry.isDirectory()) {
                                System.out.println("Omitting directory " + fileEntry.getPath());
                            } else {
                                Reader factsReader = new FileReader(fileEntry);
                                parser.parse(factsReader);
                                factMap.putAll(parser.getFacts()); // Retrieve the facts and put all of them in factMap
                            }
                        }
                    } else {
                        System.err.println("Invalid facts directory path");
                        System.exit(-1);
                    }
                    File rulesFile = new File("facts_rules/Rules/rules.iris");
                    Reader rulesReader = new FileReader(rulesFile);
                    File queriesFile = new File("facts_rules/Rules/queries.iris");
                    Reader queriesReader = new FileReader(queriesFile);
                    parser.parse(rulesReader);              // Parse rules file.
                    List<IRule> rules = parser.getRules();  // Retrieve the rules from the parsed file.
                    parser.parse(queriesReader);            // Parse queries file.
                    List<IQuery> queries = parser.getQueries(); // Retrieve the queries from the parsed file.
                    Configuration configuration = new Configuration(); // Create a default configuration.
                    configuration.programOptmimisers.add(new MagicSets()); // Enable Magic Sets together with rule filtering.
                    IKnowledgeBase knowledgeBase = new KnowledgeBase(factMap, rules, configuration); // Create the knowledge base.
                    
                    Map<String, Map<String, String>> optimisationMap = new HashMap<>();
                    for (IQuery query : queries) { // Evaluate all queries over the knowledge base.
                        List<IVariable> variableBindings = new ArrayList<>();
                        IRelation relation = knowledgeBase.execute(query, variableBindings);
                        String queryType = null;
                        if ((query.toString()).equals("?- constProp(?meth, ?l, ?v, ?val).")) {
                            queryType = "constProp";
                        } else if ((query.toString()).equals("?- copyProp(?meth, ?l, ?v1, ?v2).")) {
                            queryType = "copyProp";
                        } else if ((query.toString()).equals("?- deadCode(?meth, ?i, ?v).")) {
                            queryType = "deadCode";
                        }
                        if (queryType != null) {
                            Map<String, String> tempOp = new HashMap<>();
                            String str = null;
                            for (int r = 0; r < relation.size(); r++) {
                                str = (relation.get(r)).toString();
                                int line = getLine(str);
                                String meth = getMeth(str);
                                if (tempOp.get(meth + line) == null) {
                                    tempOp.put(meth + line, str);
                                } 
                                else {
                                    tempOp.put(meth + "-sec-" + line, str);
                                }
                            }
                            optimisationMap.put(queryType, tempOp);
                        } else {
                            // for (int r = 0; r < relation.size(); r++) {
                            //     System.out.println(relation.get(r));
                            // }
                        }
                    }
                    /* Print optimizations map */
                    // System.out.println();
                    // for (Map.Entry<String, Map<String, String>> entry : optimisationMap.entrySet()) {
                    //     System.out.println(entry.getKey() + ":");
                    //     for (Map.Entry<String, String> e : entry.getValue().entrySet()) {
                    //         System.out.println("\t" + e.getKey() + ":" + e.getValue().toString());
                    //     }
                    // }

                    OptimizerVisitor opt = new OptimizerVisitor(optimisationMap);
                    root.accept(opt, null);
                    if (abs_path.endsWith(".opt")) {
                        abs_path = abs_path.substring(0, abs_path.length() - 4);
                    }
                    System.out.println(abs_path + ".opt.asm");
                    PrintWriter writer = new PrintWriter(abs_path + ".opt.asm");
                    
                    // System.out.println("\n" + opt.result);
    
                    writer.println(opt.result);
                    writer.close();
                } catch (Exception ex) {
                    System.out.println("\n"+ ex.getMessage() + "\n");
                }
            } catch(ParseException ex) { System.err.println(ex.getMessage());
            } catch(FileNotFoundException ex) { System.err.println(ex.getMessage());
            } finally {
                try { if (fis != null) fis.close();
                } catch(IOException ex) { System.err.println(ex.getMessage()); }
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
    
    static void writeFacts(FactGeneratorVisitor dlVisitor, String path) {
        try {
            PrintWriter file = new PrintWriter(path + "/Vars.iris");
            // System.out.println("\nVars:");
            for (int k = 0 ; k < dlVisitor.varList.size() ; k++)
                dlVisitor.varList.get(k).writerec(file);
            file.close();
            
            file = new PrintWriter(path + "/varMoves.iris");
            // System.out.println("\nvarMoves:");
            for (int k = 0 ; k < dlVisitor.varMoveList.size() ; k++)
                dlVisitor.varMoveList.get(k).writerec(file);
            file.close();
            
            file = new PrintWriter(path + "/ConstMoves.iris");
            // System.out.println("\nConstMoves:");
            for (int k = 0 ; k < dlVisitor.constMoveList.size() ; k++)
                dlVisitor.constMoveList.get(k).writerec(file);
            file.close();
            
            file = new PrintWriter(path + "/BinOpMoves.iris");
            // System.out.println("\nBinOpMoves:");
            for (int k = 0 ; k < dlVisitor.binOpMoveList.size() ; k++)
                dlVisitor.binOpMoveList.get(k).writerec(file);
            file.close();
            
            file = new PrintWriter(path + "/Instructions.iris");
            // System.out.println("\nInstructions:");
            for (int k = 0 ; k < dlVisitor.instrList.size() ; k++)
                dlVisitor.instrList.get(k).writerec(file);
            file.close();
            
            file = new PrintWriter(path + "/VarUses.iris");
            // System.out.println("\nVarUses:");
            for (int k = 0 ; k < dlVisitor.varUseList.size() ; k++)
                dlVisitor.varUseList.get(k).writerec(file);
            file.close();
            
            file = new PrintWriter(path + "/VarDefs.iris");
            // System.out.println("\nVarDefs:");
            for (int k = 0 ; k < dlVisitor.varDefList.size() ; k++)
                dlVisitor.varDefList.get(k).writerec(file);
            file.close();
            
            file = new PrintWriter(path + "/Jumps.iris");
            // System.out.println("\nJumps:");
            for (int k = 0 ; k < dlVisitor.jumpList.size() ; k++)
                dlVisitor.jumpList.get(k).writerec(file);
            file.close();
            
            file = new PrintWriter(path + "/Args.iris");
            // System.out.println("\nArgs:");
            for (int k = 0 ; k < dlVisitor.argsList.size() ; k++)
                dlVisitor.argsList.get(k).writerec(file);
            file.close();
        } catch(FileNotFoundException ex) {
            System.err.println(ex.getMessage());
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
