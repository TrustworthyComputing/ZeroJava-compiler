# ![alt text][zilch] Zilch to TinyRAM compiler [![License MIT][badge-license]](LICENSE)

A compiler to translate Zilch, a language designed for zero-knowledge proofs creation, to TinyRAM.

## Zilch Language
Zilch is a custom designed language for easy translation to TinyRAM and thus easy Zero-Knowledge Proofs creation.
Below we briefly describe the language.

Zilch has one main method and also supports arbitrary methods.
Methods should be defined before they are used (each definition should be before the first invocation).
Zilch supports three types for both methods and variables; int, boolean, and int [ ] which is an array of ints.
Variable definitions and variable assignments should be in different lines (i.e., `int x;` and in a following line `x = 4;`).
Arrays of integers are initialized as follows: `int [ ] arr;`, `arr = new int[10];`.
Function parameters are always passed by value.
Zilch program source text is free-format, using the semicolon as a statement terminator and curly braces for grouping blocks of statements, such as while loops and if-else statements.
Zilch supports comments like C99, where the delimiter `//` is used for a single line comment and delimiters `/*` and `*/` are used for a block of lines.
Zilch files use the `.zl` extension.

### Zilch Arithmetic Operators
| Operator | Description        											|
|----------|----------------------------------------------------------------|
| `+`      | Adds two operands.			 									|
| `-`      | Subtracts second operand from the first.				 		|
| `*`      | Multiplies both operands.			 							|
| `/`      | Divides numerator by de-numerator.			 					|
| `%`      | Modulus Operator and remainder of after an integer division.	|
| `++`     | Increment operator increases the integer value by one. 		|
| `--`     | Decrement operator decreases the integer value by one. 		|


### Zilch Comparison and Logical Operators
| Operator | Description        	|
|----------|------------------------|
| `==`     | Equal 					|
| `!=`     | Not Equal				|
| `<`      | Less than				|
| `>`      | Greater than			|
| `<=`     | Less or Equal than		|
| `>=`     | Greater or Equal than	|
| `&&`     | Logical `AND`			|
| `\|\|`   | Logical `OR`			|


### Zilch Bitwise Operators
| Operator | Description        			|
|----------|--------------------------------|
| `&`      | Binary AND 					|
| `\|`     | Binary OR						|
| `^`      | Binary XOR						|
| `<<`     | Binary Left shift operator. 	|
| `>>`     | Binary Right shift operator. 	|


### Zilch Assignment Operators
| Operator | Description        					|
|----------|----------------------------------------|
| `=`      | Simple assignment operator. 			|
| `+=`     | Add and assignment operator.  			|
| `-=`     | Subtract and assignment operator. 		|
| `*=`     | Multiply and assignment operator. 		|
| `/=`     | Divide and assignment operator. 		|
| `%=`     | Modulo and assignment operator. 		|
| `<<=`    | Left shift AND assignment operator.	|
| `>>=`    | Right shift AND assignment operator. 	|
| `&=`     | Bitwise AND assignment operator. 		|
| `^=`     | Bitwise XOR and assignment operator.	|
| `\|=`    | Bitwise OR and assignment operator. 	|


### Built in Zilch Functions
| Built in Zilch Function Name       	| Description and corresponding TinyRAM command 			|
|---------------------------------------|-----------------------------------------------------------|
| `Prover.answer(int result);`			| `ANSWER`: returns the result 								|
| `Out.print(int variable);`			| `PRINT`: prints the contents of `variable` 				|
| `PrimaryTape.read(int dst);`			| `READ dst dst 0`: consumes next word from public tape		|
| `PrivateTape.read(int dst);`			| `READ dst dst 1`: consumes next word from private tape 	|
| `PrimaryTape.seek(int dst, int idx);`	| `SEEK dst idx 0`: consumes `idx`th word from public tape	|
| `PrivateTape.seek(int dst, int idx);` | `SEEK dst idx 1`: consumes `idx`th word from private tape	|



## TinyRAM ISA
| Instruction    | Description                                          |
|----------------|------------------------------------------------------|
| AND ri rj A    | ri = rj & A                                          |
| OR ri rj A     | ri = rj \| A                                         |
| XOR ri rj A    | ri = rj ^ A                                          |
| NOT ri rj A    | ri = !A                                              |
| ADD ri rj A    | ri = rj + A                                          |
| SUB ri rj A    | ri = rj - A                                          |
| MULL ri rj A   | ri = rj * A                                          |
| SHL ri rj A    | ri = rj << A                                         |
| SHR ri rj A    | ri = rj >> A                                         |
| CMPE ri rj A   | flag = rj == A                                       |
| CMPNE ri rj A  | flag = rj != A                                       |
| CMPG ri rj A   | flag = rj > A                                        |
| CMPGE ri rj A  | flag = rj >= A                                       |
| MOV ri rj A    | ri = A                                               |
| READ ri rj A   | ri = (A == 0) ? next from public : next from private |
| SEEK ri rj A   | ri = (A == 0) ? public[rj] : ri = private[rj]        |
| JMP ri rj A    | goto label A                                         |
| CJMP ri rj A   | if (flag) then goto label A                          |
| CNJMP ri rj A  | if (!flag) then goto label A                         |
| STOREW ri rj A | [A] = ri                                             |
| LOADW ri rj A  | ri = [A]                                             |
| ANSWER ri rj A | return A                                             |


## Compilation & Execution:
To compile the compiler type `make`.

In order to make the Zilch compiler script (`zc`) executable type `chmod +x ./zc`.

Then, use the `zc` script to compile Zilch programs to TinyRAM assembly code.

Our compiler also supports TinyRAM analysis and optimizations. In order to enable the optimizer pass the argument `-opts` to `zc` script after the zilch program.

Below are some usage examples and we also demonstrate the optimizer.

### Zilch Examples:
```
 _______ _      _       _____                       _ _           
|___  (_) |    | |     /  __ \                     (_) |          
   / / _| | ___| |__   | /  \/ ___  _ __ ___  _ __  _| | ___ _ __ 
  / / | | |/ __| '_ \  | |    / _ \| '_   _ \| '_ \| | |/ _ \ '__|
./ /__| | | (__| | | | | \__/\ (_) | | | | | | |_) | | |  __/ |   
\_____/_|_|\___|_| |_|  \____/\___/|_| |_| |_| .__/|_|_|\___|_|   
					     | |                  
					     |_|      
```

A simple program that performs addition:
```
./zc ./compiler/zilch-examples/simpleAdd.zl
```
```
void main(void) {
	int x;
	int y;
	x = foo();
	y = bar();
	x = 12;
	y = 13;
	Prover.answer(x + y);
}
```

Which generates the following four lines of TinyRAM assembly:
```
MOV r1 r1 12
MOV r2 r2 13
ADD r3 r1 r2
ANSWER r3 r3 r3
```
Passing the `-opts` argument to enable the optimizer, our compiler generates the following optimal code:
```
./zc ./compiler/zilch-examples/simpleAdd.zl -opts
```
```
MOV r1 r1 12
ADD r3 r1 13
ANSWER r3 r3 r3
```

A more complex program that reads inputs from the primary tape and adds them all together:
```
./zc ./compiler/zilch-examples/Addloop.zl
```
```
void main(void) {
	int i;
	int from_tape;
	int res;
	i = 0 ;
	while (i < 5) {
		PrimaryTape.read(from_tape);
		res = res + from_tape;
		i = i + 1;
	}
	Prover.answer(res);
}
```

Which generates the following TinyRAM code:
```
MOV r1 r1 0
__L1__
MOV r4 r4 5
CMPG r4 r4 r1
CNJMP r4 r4 __L2__
READ r2 r2 0
ADD r5 r3 r2
MOV r3 r3 r5
ADD r6 r1 1
MOV r1 r1 r6
JMP r0 r0 __L1__
__L2__
ANSWER r3 r3 r3
```

A final example that invokes methods is presented below:
```
./zc ./compiler/zilch-examples/methodCalls.zl
```
```
int bar() {
	return 30;
}

int foo() {
	int x;
	x = bar();
	return x + 40;
}

void main(void) {
	int x;
	int y;
	x = foo();
	y = bar();
	Prover.answer(x + y);
}
```

Which generates the following TinyRAM code:
```
MOV r1 r1 30
MOV r2 r2 r1
MOV r4 r4 40
ADD r3 r2 r4
MOV r5 r5 r3
MOV r1 r1 30
MOV r6 r6 r1
ADD r7 r5 r6
ANSWER r7 r7 r7
```
while enabling the optimizations:
```
./zc ./compiler/zilch-examples/methodCalls.zl -opts
```
```
MOV r1 r1 30
ADD r3 r1 40
MOV r5 r5 r3
MOV r1 r1 30
ADD r7 r3 30
ANSWER r7 r7 r7
```
the TinyRAM assembly output is optimized.


More Zilch examples can be found in the [zilch-examples](./zilch-examples) directory.
Those examples include `if-else` statements, comparisons, `while` loops, examples with `int [ ]` accesses and others that will help getting started with Zilch programming language. 


### ![alt text][twc-logo] An open-source project by Trustworthy Computing Group


[zilch]: ./logos/zilch_sm.png

[twc-logo]: ./logos/twc.png

[badge-license]: https://img.shields.io/badge/license-MIT-green.svg?style=flat-square
