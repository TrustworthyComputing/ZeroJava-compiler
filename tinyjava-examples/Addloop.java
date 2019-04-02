class Add {
	public static void main(String[] a){
		int j;
		int x;
		int res;
		j = 0 ;
		while (5 > j) {
			PrimaryTape.read(x);
			res = res + x;
			
			j = j + 1 ;
		}
		Prover.answer(res);
	}
}

