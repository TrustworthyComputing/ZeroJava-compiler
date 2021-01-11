class RangeQuery {

	public static void main(String [] a) {
		RQ rq = new RQ();
		Prover.answer( rq.inRange() );
	}
}

class RQ {

	int min, max, val;

	public boolean inRange() {
		min = PublicTape.read();
		max = PublicTape.read();
		val = PrivateTape.read();
		return ((min <= val) && (val <= max));
	}

	/* Other methods*/
}