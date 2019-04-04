void main(void) {
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
