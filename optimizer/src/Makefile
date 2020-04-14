JAVAC=javac
JAVACC=javacc
JVM=java
CLASSPATH = lib/iris-0.60.jar:lib/iris-parser-0.60.jar:.

all: compile

compile:
	$(JVM) -jar jtb132di.jar -te zmips.jj
	$(JAVACC) zmips-jtb.jj
	$(JAVAC) syntaxtree/*.java
	$(JAVAC) facts_gen/*.java
	$(JAVAC) -cp $(CLASSPATH) Main.java
	$(JAVAC) optimizer/*.java

clean:
	$(RM) -r *.class syntaxtree/* visitor/* facts_rules/Facts/* facts_gen/*.class optimizer/*.class
	$(RM) zmips-jtb.jj ZMIPSParser.java ZMIPSParserConstants.java ZMIPSParserTokenManager.java ParseException.java Token.java TokenMgrError.java JavaCharStream.java

