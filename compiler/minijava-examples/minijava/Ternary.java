class Ternary {
	public static void main(String[] a){
		int x;
		x = ( 5 > 10 ) ? 1 : 13;
		x = ( x != 5) ? x * 2 : 0;
		Prover.answer(x);
	}
}
