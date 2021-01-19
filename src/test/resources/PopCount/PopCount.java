class PopCount {

	public static void main(String[] a) {
		int num = PrivateTape.read();
		int count = 0;
	    while (num > 0) {
			num &= num - 1;
			count++;
		}
		Prover.answer(count);
	}

}
