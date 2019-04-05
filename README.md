# ![alt text][logo] Zilch to TinyRAM compiler [![License MIT][badge-license]](LICENSE)

A compiler to translate a custom Java-like subset (Zilch) to TinyRAM.

### Zilch:
Zilch is a custom designed language for easy translation to TinyRAM and thus easy Zero-Knowledge Proofs creation.
Below we briefly describe the language.

Zilch has one main class that contains a single main method and no fields.
Zilch supports three types: int, boolean, and int [ ] which is an array of int.


### TinyRAM ISA

| Instruction    | Definition                                           |
|----------------|------------------------------------------------------|
| AND ri rj A    | ri = rj & A                                          |
| OR ri rj A     | ri = rj or A                                         |
| XOR ri rj A    | ri = rj ^ A                                          |
| NOT ri rj A    | ri = !A                                              |
| ADD ri rj A    | ri = rj + A                                          |
| SUB ri rj A    | ri = rj - A                                          |
| MULL ri rj A   | ri = rj * A                                          |
| SHL ri rj A    | ri = rj << A                                         |
| SHR ri rj A    | ri = rj >> A                                         |
| CMPE ri rj A   | flag = rj == A                                       |
| CMPG ri rj A   | flag = rj > A                                        |
| CMPGE ri rj A  | flag = rj >= A                                       |
| MOV ri rj A    | ri = A                                               |
| READ ri rj A   | ri = (A == 0) ? next from public : next from private |
| RAREAD ri rj A | ri = (A == 0) ? public[rj] : ri = private[rj]        |
| JMP ri rj A    | goto label A                                         |
| CJMP ri rj A   | if (flag) then goto label A                          |
| CNJMP ri rj A  | if (!flag) then goto label A                         |
| STORE ri rj A  | [A] = ri                                             |
| LOAD ri rj A   | ri = [A]                                             |
| ANSWER ri rj A | return A                                             |


## Compilation & Execution:
To compile the compiler type `make`.

Then, use the `tjrc` script to compile Zilch programs to TinyRAM assembly code.
Below are some usage examples.


### Zilch Examples:

A simple program that performs addition:
```
./tjrc zilch-examples/simpleAdd.java
```
```
class SimpleAdd {
	public static void main(String[] a){
		int x;
		int y;
		x = 12;
		y = 13;
		Prover.answer(x + y);
	}
}
```

Which generates the following four lines of TinyRAM assembly:
```
MOV r1 r1 12
MOV r2 r2 13
ADD r3 r1 r2
ANSWER r3 r3 r3
```

A more complex program that reads inputs from the primary tape and adds them all together:
```
./tjrc zilch-examples/Addloop.java
```
```
class AddLoop {
	public static void main(String[] a){
		int i;
		int from_tape;
		int res;
		i = 0 ;
		while (5 > i) {
			PrimaryTape.read(from_tape);
			res = res + from_tape;
			i = i + 1;
		}
		Prover.answer(res);
	}
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

More Zilch examples can be found in the [zilch-examples](./zilch-examples) directory.


### ![alt text][twc-logo] An open-source project by Trustworthy Computing Group


[logo]: ./logos/logo.jpg

[twc-logo]: ./logos/twc.png

[badge-license]: https://img.shields.io/badge/license-MIT-green.svg?style=flat-square
