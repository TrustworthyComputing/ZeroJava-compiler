void main(void) {
	int x;
	int y;
	x = 12;
	y = 13;
	x = foo();
	Prover.answer(x + y);
}

int foo() {
	int x;
	return 1;
}
