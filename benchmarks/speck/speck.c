#include <stdio.h>
#include <stdint.h>

int main(void) {
    FILE *pubfp = fopen("./benchmarks/speck/public.tape", "r");
    int pub_tape[1000];
    int pub_tape_idx = 0;
    while (!feof(pubfp)) {
    fscanf(pubfp, "%d", &pub_tape[pub_tape_idx]);
    pub_tape_idx++;
    }
    fclose(pubfp);
    pub_tape_idx = 0;
    FILE *auxfp = fopen("./benchmarks/speck/private.tape", "r");
    int priv_tape[1000];
    int priv_tape_idx = 0;
    while (!feof(auxfp)) {
    fscanf(auxfp, "%d", &priv_tape[priv_tape_idx]);
    priv_tape_idx++;
    }
    fclose(auxfp);
    priv_tape_idx = 0;
    int i;
    uint16_t x;
    uint16_t y;
    int rounds;

    rounds = 22;
    int K[rounds];

    i = 0;
    while (i < rounds) {
        x = pub_tape[pub_tape_idx++];
    	K[i] = x;
        printf("k[%d]: %d\n", i, K[i]);
    	i++;
    }

    x = priv_tape[priv_tape_idx++];
    y = priv_tape[priv_tape_idx++];

    printf("x: %d\n", x);
    printf("y: %d\n", y);

    i = 0;
    while (i < rounds) {
    	y = (y >> 7) | (y << 9);
        y += x;
        y ^= K[i];
        x = (x >> 14) | (x << 2);
        x ^= y;
        printf("%d ", x);

    	i++;
    }
    printf("\n\n");

    printf("%d\n", 23);
    return 0;
}
