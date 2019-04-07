JAVAC=javac
JAVACC=javacc
JVM=java
CLASSPATH = lib/iris-0.60.jar:lib/iris-parser-0.60.jar:.

all: compile

compile: 
	$(JVM) -jar jtb132.1.jar -te tinyram.jj
	$(JAVACC) tinyram-jtb.jj
	$(JAVAC) syntaxtree/*.java
	$(JAVAC) facts_gen/*.java
	# $(JAVAC) optimizer/*.java
	$(JAVAC) -cp $(CLASSPATH) Main.java 

clean:
	rm -r *.class syntaxtree/* visitor/* facts_rules/Facts/* facts_gen/*.class optimizer/*.class
	rm tinyram-jtb.jj TinyRAMParser.java TinyRAMParserConstants.java TinyRAMParserTokenManager.java ParseException.java Token.java TokenMgrError.java JavaCharStream.java
	
