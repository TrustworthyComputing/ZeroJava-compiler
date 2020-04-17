class TestOperators {

	public static void main(String[] a){
		int x;

		System.out.println(1 & 3);
		System.out.println(1 | 2);
		System.out.println(1 ^ 2);
		System.out.println(4 << 1);
		System.out.println(8 >> 1);
		System.out.println(~10);
		System.out.println();

		x = 100;
		System.out.println(x);
		x++;
		System.out.println(x);
		x--;
		System.out.println(x);
		x += 10;
		System.out.println(x);
		x -= 10;
		System.out.println(x);
		x *= 10;
		System.out.println(x);
		x /= 10;
		System.out.println(x);
		x %= 95;
		System.out.println(x);
		x <<= 1;
		System.out.println(x);
		x >>= 1;
		System.out.println(x);
		x &= 1;
		System.out.println(x);
		x |= 2;
		System.out.println(x);
		x ^= 2;
		System.out.println(x);

		Prover.answer(x);
	}

}
