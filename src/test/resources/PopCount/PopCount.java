class PopCount {

	public static void main(String[] a) {
		int threshold = PublicTape.read();
		int num = PrivateTape.read();
		int count = 0;
	    while (num > 0) {
			num &= num - 1;
			count++;
		}
		System.out.println(count);
		Prover.answer(count > threshold);
	}

}
