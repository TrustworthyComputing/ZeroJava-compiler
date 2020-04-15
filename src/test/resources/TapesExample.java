class TapesExample {

    public static void main(String[] a){
		int x;
		int y;
		x = PrivateTape.read();
		y = PublicTape.read();
		Prover.answer(x + y);
    }

}
