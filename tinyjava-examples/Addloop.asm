MOV r4 r4 0
MOV r1 r1 r4

__L1__
MOV r5 r5 5
CMPG r5 r5 r1
CNJMP r0 r0 __L2__
READ r2 r2 0
ADD r6 r3 r2
MOV r3 r3 r6
MOV r8 r8 1
ADD r7 r1 r8
MOV r1 r1 r7
JMP r0 r0 __L1__
__L2__
ANSWER r3 r3 r3
