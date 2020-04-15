class Factorial {

	public static void main(String[] a){
		int fact;
		int i;
		int num;
		num = PublicTape.read();

		fact = 1;
		i = 1;
		while (i <= num) {
			fact = fact * i;
			i = i + 1;
		}

		Prover.answer(fact);
	}

}
