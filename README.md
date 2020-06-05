<h1 align="center">ZeroJava to zMIPS compiler <a href="https://github.com/TrustworthyComputing/ZeroJava-compiler/blob/master/LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue.svg"></a> </h1>
<h3 align="center">A compiler to translate ZeroJava code to zMIPS assembly and create zero-knowledge proofs</h3>
<p align="center">
	<img src="./logos/compiler_logo.png" height="20%" width="20%">
</p>

## Overview
A compiler to translate ZeroJava, a subset of Java designed for zero-knowledge arguments, to zMIPS.
zMIPS assembly can in turn be consumed by the [Zilch framework](https://github.com/TrustworthyComputing/Zilch) to create zero-knowledge proofs.


### ZeroJava Language
ZeroJava is a custom subset of Java tailored to zero-knowledge proofs. It is possible to compile ZeroJava programs to byte code with a Java compiler if the ZK-specific instructions are omitted.
ZeroJava abstains from features of Java that complicate the run-time system, such as exceptions and multi-threading.


ZeroJava is object-oriented, like Java.
The basic types of ZeroJava are `int` for integer, `boolean` for logical values, and `int[]` for arrays of integers.
Classes contain attributes and methods with arguments and return type of basic or class types.
ZeroJava supports single inheritance but not interfaces and function overloading (i.e., each method name must be unique).
In addition, all methods are inherently polymorphic; meaning that a method can be defined in a subclass if it has the same return type and arguments as in the parent.
Fields in the base and derived class are allowed to have the same names, and are essentially different fields.
All ZeroJava methods are `public` and all fields `protected`, which means that a class method cannot access fields of another class, with the exception of its parent.
A class's own methods can be called via `this`.
Local variables are defined only at the beginning of a method and local variables shadow fields of the surrounding class with the same name.

In ZeroJava, the `new` operator calls a default void constructor.
In addition, there are no inner classes and there are no static methods or fields.
A ZeroJava program begins with a special main class which does not have fields and methods and contains the `main` method (i.e., `public static void main(String[] args)`).
After main class, other classes may be defined that can have fields and methods.

For `int` arrays, ZeroJava supports the assignment and lookup (`[]`) operators, as well as the `array.length` expression, which returns the size of the array.
ZeroJava supports `while`, `if` code blocks as well as ternary operators.
The assignment `A a = new B();` when `B extends A` is correct, and the same applies when a method expects a parameter of type `A` and a `B` instance is given instead.
Finally, ZeroJava supports comments like Java, where the delimiter `//` is used for a single line comment and delimiters `/*` and `*/` are used for a block of lines.


### ZeroJava Operators

#### Arithmetic
| Operator | Description        											|
|----------|----------------------------------------------------------------|
| `+`      | Adds two operands			 									|
| `-`      | Subtracts second operand from the first				 		|
| `*`      | Multiplies both operands			 							|
| `/`      | Divides numerator by de-numerator			 					|
| `%`      | Modulus Operator and remainder of after an integer division	|
| `++`     | Increment operator increases the integer value by one			|
| `--`     | Decrement operator decreases the integer value by one			|

#### Logical
| Operator | Description        	|
|----------|------------------------|
| `!`      | Logical `not`			|
| `&&`     | Logical `and`			|
| `\|\|`   | Logical `or`			|

#### Relational
| Operator | Description        	|
|----------|------------------------|
| `==`     | Equal 					|
| `!=`     | Not Equal				|
| `<`      | Less than				|
| `>`      | Greater than			|
| `<=`     | Less or Equal than		|
| `>=`     | Greater or Equal than	|

#### Bitwise
| Operator | Description        			|
|----------|--------------------------------|
| `&`      | Binary and 					|
| `\|`     | Binary or						|
| `^`      | Binary xor						|
| `<<`     | Binary Left shift operator 	|
| `>>`     | Binary Right shift operator 	|

#### Assignment
| Operator | Description        					|
|----------|----------------------------------------|
| `=`      | Simple assignment operator 			|
| `+=`     | Add and assignment operator  			|
| `-=`     | Subtract and assignment operator 		|
| `*=`     | Multiply and assignment operator 		|
| `/=`     | Divide and assignment operator 		|
| `%=`     | Modulo and assignment operator 		|
| `<<=`    | Left shift and assignment operator		|
| `>>=`    | Right shift and assignment operator 	|
| `&=`     | Bitwise and assignment operator 		|
| `^=`     | Bitwise xor and assignment operator	|
| `\|=`    | Bitwise or and assignment operator 	|

### Built-in Functions
| Built in ZeroJava Function Name   | zMIPS instruction | Description 		|
|-----------------------------------|-------------------|---------------------------------------------------------------|
| `Prover.answer(int);` 			| `answer`			| returns the result 											|
| `System.out.println(int);`		| `print`			| prints contents of integer variable 							|
| `int PublicTape.read();`			| `pubread dst`		| return next word from public tape								|
| `int PrivateTape.read();`			| `secread dst`		| return next word from private tape 							|
| `int PublicTape.seek(int);`		| `pubseek dst, idx`| return the nth word from public tape where n is the argument	|
| `int PrivateTape.seek(int);` 		| `secseek dst, idx`| return the nth word from private tape where n is the argument	|


Finally, ZeroJava supports the ternary operation (`( a ) ? b : c ;`) which evaluates to `b` if the value of `a` is true, and otherwise to `c`.


## Build and Run Instructions

#### ZeroJava Dependencies:
To build the ZeroJava to zMIPS compiler you will need a Java Development Kit (JDK), such as [OpenJDK](https://openjdk.java.net/) >= 8, [Apache Maven](https://maven.apache.org/) and [JavaCC](https://javacc.github.io/javacc/).

```
$ apt install openjdk-8-jdk maven javacc
```

Then simply type:
```
$ mvn initialize package
```

The above command will create a `target` directory with all the build files, as well as with the `zerojava-compiler-1.0.jar` inside the `target` directory.


To compile a ZeroJava program type:
```
$ java -jar target/zerojava-compiler-1.0.jar /path-to-zerojava-example/example.java
```

Our compiler supports IR static analysis and optimizations.
In order to enable the optimizer pass the argument `--opts` command line argument.

We provide various ZeroJava examples in the [src/test/resources/](./src/test/resources/) directory.
Those examples include `if-else` statements, comparisons, `while` loops, examples with `int[]` accesses and others that will help get started with ZeroJava programming language.

For instance, a simple program that performs addition:
```
$ java -jar target/zerojava-compiler-1.0.jar src/test/resources/Add.java
```

```
$ cat src/test/resources/Add.java

class Add {

	public static void main(String[] a) {
		int sum;
		sum = 12 + 21;
		System.out.println(sum);
		Prover.answer(sum);
	}

}
```

Which generates the following lines of zMIPS assembly:
```
        .text
        .globl main
__main__:
        move $t0, 0
        move $t1, 12
        move $t2, 21
        add $v1, $t1, $t2
        move $t1, $v1
        move $t0, $t1
        move $t1, 10
        add $v1, $t0, $t1
        move $t0, $v1
        print $t0
        answer $t0
```

Passing the `-opts` argument to enable the optimizer, our compiler generates the following optimized code:
```
        .text
        .globl main
__main__:
        move $t0, 12
        add $v1, $t0, 21
        move $t0, $v1
        add $v1, $t0, 10
        move $t0, $v1
        print $t0
        answer $t0
```


<p align="center">
    <img src="./logos/twc.png" height="20%" width="20%">
</p>
<h4 align="center">Trustworthy Computing Group</h4>

