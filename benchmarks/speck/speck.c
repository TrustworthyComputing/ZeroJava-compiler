#include <stdio.h>
int main(void) {
FILE *pubfp = fopen("/home/jimouris/repos/zilch-compiler/benchmarks/speck/speck.pubtape", "r");
int pub_tape[1000];
int pub_tape_idx = 0;
while (!feof(pubfp)) {
    fscanf(pubfp, "%d", &pub_tape[pub_tape_idx]);
    pub_tape_idx++;
}
fclose(pubfp);
pub_tape_idx = 0;
FILE *auxfp = fopen("/home/jimouris/repos/zilch-compiler/benchmarks/speck/speck.auxtape", "r");
int priv_tape[1000];
int priv_tape_idx = 0;
while (!feof(auxfp)) {
    fscanf(auxfp, "%d", &priv_tape[priv_tape_idx]);
    priv_tape_idx++;
}
fclose(auxfp);
priv_tape_idx = 0;
	int i;
	unsigned short x;
	unsigned short y;
	int rounds;
	rounds = 22;
int K[rounds];
	i = 0;
	while (i < rounds) {
x = pub_tape[pub_tape_idx++];
		K[i] = x;
		i++;
	}
x = priv_tape[priv_tape_idx++];
y = priv_tape[priv_tape_idx++];

	i = 0;
	while (i < rounds) {
		y = (y >> 7) | (y << 9);
        y += x;
        y ^= K[i];
        x = (x >> 14) | (x << 2);
        x ^= y;
		i++;
	}
	
printf("x: %d\n", x);
printf("y: %d\n", y);
	
printf("%d\n", y);
return 0;
}
