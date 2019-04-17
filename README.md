# ![alt text][zilch] Zilch to ZMIPS compiler [![License MIT][badge-license]](LICENSE)

A compiler to translate Zilch, a language designed for zero-knowledge proofs creation, to ZMIPS.

## Zilch Language
Zilch is a custom designed language for easy translation to ZMIPS and thus easy Zero-Knowledge Proofs creation.
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
| `&&`     | Logical `and`			|
| `\|\|`   | Logical `or`			|


### Zilch Bitwise Operators
| Operator | Description        			|
|----------|--------------------------------|
| `&`      | Binary and 					|
| `\|`     | Binary or						|
| `^`      | Binary xor						|
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
| `<<=`    | Left shift and assignment operator.	|
| `>>=`    | Right shift and assignment operator. 	|
| `&=`     | Bitwise and assignment operator. 		|
| `^=`     | Bitwise xor and assignment operator.	|
| `\|=`    | Bitwise or and assignment operator. 	|


### Built in Zilch Functions
| Built in Zilch Function Name       	| Description and corresponding ZMIPS command 			|
|---------------------------------------|-----------------------------------------------------------|
| `Prover.answer(int result);`			| `answer`: returns the result 								|
| `Out.print(int variable);`			| `print`: prints the contents of `variable` 				|
| `PrimaryTape.read(int dst);`			| `read dst dst 0`: consumes next word from public tape		|
| `PrivateTape.read(int dst);`			| `read dst dst 1`: consumes next word from private tape 	|
| `PrimaryTape.seek(int dst, int idx);`	| `seek dst idx 0`: consumes `idx`th word from public tape	|
| `PrivateTape.seek(int dst, int idx);` | `seek dst idx 1`: consumes `idx`th word from private tape	|



## ZMIPS ISA
| Instruction    | Description                                          |
|----------------|------------------------------------------------------|
| and ri rj A    | ri = rj & A                                          |
| or ri rj A     | ri = rj \| A                                         |
| xor ri rj A    | ri = rj ^ A                                          |
| not ri rj A    | ri = !A                                              |
| add ri rj A    | ri = rj + A                                          |
| sub ri rj A    | ri = rj - A                                          |
| mult ri rj A   | ri = rj * A                                          |
| sll ri rj A    | ri = rj << A                                         |
| srl ri rj A    | ri = rj >> A                                         |
| cmpe ri rj A   | flag = rj == A                                       |
| cmpne ri rj A  | flag = rj != A                                       |
| cmpg ri rj A   | flag = rj > A                                        |
| cmpge ri rj A  | flag = rj >= A                                       |
| beq ri rj A    | if ri == rj goto A                                   |
| bne ri rj A    | if ri != rj goto A                                   |
| bgt ri rj A    | if ri > rj goto A                                    |
| bge ri rj A    | if ri >= rj goto A                                   |
| blt ri rj A    | if ri < rj goto A                                    |
| ble ri rj A    | if ri <= rj goto A                                   |
| move ri rj A   | ri = A                                               |
| read ri rj A   | ri = (A == 0) ? next from public : next from private |
| seek ri rj A   | ri = (A == 0) ? public[rj] : ri = private[rj]        |
| j ri rj A      | goto label A                                         |
| cjmp ri rj A   | if (flag) then goto label A                          |
| cnjmp ri rj A  | if (!flag) then goto label A                         |
| sw ri rj A     | [A] = ri                                             |
| lw ri rj A     | ri = [A]                                             |
| answer ri rj A | return A                                             |


## Compilation & Execution:
To compile the compiler type `make`.

In order to make the Zilch compiler script (`zc`) executable type `chmod +x ./zc`.

Then, use the `zc` script to compile Zilch programs to ZMIPS assembly code.

Our compiler also supports ZMIPS analysis and optimizations. In order to enable the optimizer pass the argument `-opts` to `zc` script after the zilch program.

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
	y = 13;
	y += 7;
	x = 12;
	x = x - 1;
	x--;
	x <<= 1;
	Prover.answer(x + y);
}
```

Which generates the following four lines of ZMIPS assembly:
```
move $r2, $r2, 13
add $r2, $r2, 7
move $r1, $r1, 12
sub $r3, $r1, 1
move $r1, $r1, $r3
sub $r1, $r1, 1
sll $r1, $r1, 1
add $r4, $r1, $r2
answer $r4, $r4, $r4
```
Passing the `-opts` argument to enable the optimizer, our compiler generates the following optimal code:
```
./zc ./compiler/zilch-examples/simpleAdd.zl -opts
```
```
move $r2, $r2, 13
add $r2, $r2, 7
move $r1, $r1, 12
sub $r3, $r1, 1
sub $r1, $r3, 1
sll $r1, $r1, 1
add $r4, $r1, $r2
answer $r4, $r4, $r4
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

Which generates the following ZMIPS code:
```
move $r1, $r1, 0
__L1__
move $r4, $r4, 5
cmpg $r4, $r4, $r1
cnjmp $r4, $r4, __L2__
read $r2, $r2, 0
add $r5, $r3, $r2
move $r3, $r3, $r5
add $r6, $r1, 1
move $r1, $r1, $r6
j $r0, $r0, __L1__
__L2__
answer $r3, $r3, $r3
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

Which generates the following ZMIPS code:
```
move $r1, $r1, 30
move $r2, $r2, $r1
move $r4, $r4, 40
add $r3, $r2, $r4
move $r5, $r5, $r3
move $r1, $r1, 30
move $r6, $r6, $r1
add $r7, $r5, $r6
answer $r7, $r7, $r7
```
while enabling the optimizations:
```
./zc ./compiler/zilch-examples/methodCalls.zl -opts
```
```
move $r1, $r1, 30
add $r3, $r1, 40
add $r7, $r3, 30
answer $r7, $r7, $r7
```
the ZMIPS assembly output is optimized.


More Zilch examples can be found in the [zilch-examples](./zilch-examples) directory.
Those examples include `if-else` statements, comparisons, `while` loops, examples with `int [ ]` accesses and others that will help getting started with Zilch programming language. 


### ![alt text][twc-logo] An open-source project by Trustworthy Computing Group


[zilch]: ./logos/zilch_sm.png

[twc-logo]: ./logos/twc.png

[badge-license]: https://img.shields.io/badge/license-MIT-green.svg?style=flat-square
