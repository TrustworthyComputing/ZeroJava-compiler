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

// 100 + 10 + 13 + 104

/**
 * public
 10
 11
 12
 13
 14
 15
 16
 17
 18
 19

 * private
 100
 101
 102
 103
 104
 105
 106
 107
 108
 109

**/