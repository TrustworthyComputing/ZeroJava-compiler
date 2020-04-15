class Read {
	public static void main(String[] a){
		int x;
		int y;

		x = PublicTape.read();
		y = PrivateTape.read();

		System.out.println(x + y);
	}
}
