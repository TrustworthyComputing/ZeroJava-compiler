class pir {

	public static void main(String[] a){
		int i;
		int key;
		int k;
		int val;
		int x;
		int size;
		size = PublicTape.read();
		key = PublicTape.read();
		i = 0;
		while (i < size) {
			k = PrivateTape.read();
			val = PrivateTape.read();
			if (k == key) {
				Prover.answer(val);
			}
			i = i + 1;
		}
		Prover.answer(0);
	}
}
