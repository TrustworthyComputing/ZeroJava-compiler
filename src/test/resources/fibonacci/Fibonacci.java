class Fibonacci {

	public static void main(String[] a){
		int fib;
		int temp;
		int prevFib;
		int i;
		int n;
		n = PublicTape.read();
		if (n <= 1) {
			Prover.answer(n);
		}
		fib = 1;
		prevFib = 1;
		i = 2;
		while (i < n) {
			temp = fib;
			fib = fib + prevFib;
			prevFib = temp;
			i = i + 1;
		}
		Prover.answer(fib);
	}
}
