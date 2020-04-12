all: compile

compile:
	java -jar jtb132di.jar -te miniJava.jj
	javacc miniJava-jtb.jj
	javac Main.java
	javac basetype/*.java
	javac symboltable/*.java
	javac typecheck/*.java
	javac zmipsgenerator/*.java

execute:
	java Main

clean:
	$(RM) -f *.class *~ basetype/*.class symboltable/*.class syntaxtree/*.class visitor/*.class typecheck/*.class zmipsgenerator/*.class
