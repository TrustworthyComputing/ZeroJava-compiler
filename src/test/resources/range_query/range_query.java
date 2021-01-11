class RangeQuery {

	public static void main(String[] a){
		int min, max, val;
		min = PublicTape.read();
		max = PublicTape.read();
		val = PrivateTape.read();
		if ((min <= val) && (val <= max)) {
			Prover.answer(true);
		}
		Prover.answer(false);
	}
}
