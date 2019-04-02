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
