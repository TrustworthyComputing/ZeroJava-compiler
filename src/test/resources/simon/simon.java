class simon {

	public static void main(String[] a){
		int i;
		int x;
		int y;
		int tmp_x;
		int tmp_ror;
		int rounds;
		int[] K;
		rounds = 32;
		K = new int[rounds];
		i = 0;
		while (i < rounds) {
			x = PublicTape.read();
			K[i] = x;
			i = i + 1;
		}
		x = PrivateTape.read();
		y = PrivateTape.read();

		i = 0;
		while (i < rounds) {
			tmp_x = x;
			x = (tmp_x >> 15) | (tmp_x << 1);
			tmp_ror = (tmp_x >> 8) | (tmp_x << 8);
			x = x & tmp_ror;
			x = x ^ y;
			tmp_ror = (tmp_x >> 14) | (tmp_x << 2);
			x = x ^ tmp_ror;
			x = x ^ (K[i]);
			y = tmp_x;
			i = i + 1;
		}
		System.out.println(x);
		System.out.println(y);

		Prover.answer(y);
	}
}
