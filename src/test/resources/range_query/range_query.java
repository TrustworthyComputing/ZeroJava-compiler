class range_query {

	public static void main(String[] a){
		int min;
		int max;
		int val;
		min = PublicTape.read();
		max = PublicTape.read();
		val = PrivateTape.read();
		if ((min <= val) && (val <= max)) {
			Prover.answer(1);
		}
		Prover.answer(0);
	}
}
