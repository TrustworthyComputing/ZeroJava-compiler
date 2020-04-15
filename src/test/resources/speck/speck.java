class speck {

	public static void main(String[] a){
		int i;
		int x;
		int y;
		int rounds;
		int[] K;
		rounds = 22;
		K = new int[rounds];
		i = 0;
		while (i < rounds) {
			x =PublicTape.read();
			K[i] = x;
			i = i + 1;
		}
		x = PrivateTape.read();
		y = PrivateTape.read();

		i = 0;
		while (i < rounds) {
			y = (y >> 7) | (y << 9);
	        y = y + x;
	        y = y ^ (K[i]);
	        x = (x >> 14) | (x << 2);
	        x = x ^ y;
			i = i + 1;
		}

		System.out.println(x);
		System.out.println(y);

		Prover.answer(y);
	}
}
