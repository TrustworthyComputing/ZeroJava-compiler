all: compile

compile:
	java -jar jtb132.1.jar -te zilch.jj
	javacc zilch-jtb.jj
	javac base_type/*.java
	javac symbol_table/*.java
	javac zmips_generator/*.java
	javac Main.java

clean:
	rm -f *.class *~ base_type/*.class symbol_table/*.class zmips_generator/*.class syntaxtree/* visitor/*
	rm zilch-jtb.jj ZilchParser.java ZilchParserConstants.java ZilchParserTokenManager.java ParseException.java Token.java TokenMgrError.java JavaCharStream.java
