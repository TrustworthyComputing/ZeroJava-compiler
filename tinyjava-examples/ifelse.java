class IfElse {
	public static void main(String[] a){
		int cond;
		int x;
		cond = 1;
		if (cond == 1) {
			x = 3;
		} else {
			x = 4;
		}
		
		if ((x == 33) || (cond == 1)) {
			if ((cond > 0) && (x == 3)) {
				x = x * 2;
			} else {
				x = 0;
			}
		} else {
			x = 1;
		}
		Prover.answer(x);
	}
}
