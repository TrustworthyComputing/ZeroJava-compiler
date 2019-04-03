class ReadFromTapes {
	public static void main(String[] a){
		int i ;
		int j ;
		PrivateTape.read(i);
		PrimaryTape.read(j);
		Prover.answer(i + j);
	}
}
