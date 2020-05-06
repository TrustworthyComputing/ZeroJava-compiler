class TapesExample {

    public static void main(String[] a){
		int sum;
        sum = PrivateTape.read();
		sum += PublicTape.read();
		sum += PublicTape.seek(3);
		sum += PrivateTape.seek(4);
		Prover.answer(sum);
    }

}
