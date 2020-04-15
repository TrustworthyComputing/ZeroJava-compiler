class TestOperators {

	public static void main(String[] a){
		int x;
		int y;
		
		System.out.println(1 & 3);
		System.out.println(1 | 2);
		System.out.println(1 ^ 2);
		System.out.println(4 << 1);
		System.out.println(8 >> 1);
		System.out.println(~10);

		y = 13;
		y += 7;
		x = 12;
		x = x - 1;
		x--;
		x <<= 1;
		Prover.answer(x + y);
	}

}
