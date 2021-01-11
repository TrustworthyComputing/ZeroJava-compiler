#!/bin/bash

sed -i '1s/^/import java.io.File;\
import java.io.FileNotFoundException;\
import java.util.Scanner;\
import java.util.List;\
import java.util.ArrayList;\
/' $1

sed -i '/public static void main/a \
\t\tinitializeScanners();' $1

sed -i '/public static void main/i \
    static List<Integer> pubtape;\
    static List<Integer> auxtape;\
    static int pubtape_idx;\
    static int auxtape_idx;\
    \
    public static void initializeScanners() {\
        try {\
            Scanner pubtape_scanner = new Scanner(new File("pubtape.txt"));\
            Scanner auxtape_scanner = new Scanner(new File("auxtape.txt"));\
    \
            pubtape_idx = 0;\
            auxtape_idx = 0;\
            pubtape = new ArrayList<Integer>();\
            auxtape = new ArrayList<Integer>();\
            while (pubtape_scanner.hasNext()) {\
                pubtape.add( pubtape_scanner.nextInt() );\
            }\
            pubtape_scanner.close();\
            while (auxtape_scanner.hasNext()) {\
                auxtape.add( auxtape_scanner.nextInt() );\
            }\
            auxtape_scanner.close();\
        } catch(FileNotFoundException fnfe) {\
            System.out.println(fnfe.getMessage());\
        }\
    }\
    \
    public static int pubtapeRead() {\
        return pubtape.get(pubtape_idx++);\
    }\
    \
    public static int auxtapeRead() {\
        return auxtape.get(auxtape_idx++);\
    }\
    \
    public static int pubtapeSeek(int idx) {\
        return pubtape.get(idx);\
    }\
    \
    public static int auxtapeSeek(int idx) {\
        return auxtape.get(idx); \
    }
' $1

sed -i 's/Prover.answer/System.out.println/g' $1

sed -i 's/PublicTape.read/pubtapeRead/g' $1
sed -i 's/PrivateTape.read/auxtapeRead/g' $1

sed -i 's/PublicTape.seek/pubtapeSeek/g' $1
sed -i 's/PrivateTape.seek/auxtapeSeek/g' $1
