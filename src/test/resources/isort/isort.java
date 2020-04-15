class ISort {

	public static void main(String[] a){
		int i;
		int j;
		int arrj;
		int x;
		int key;
		int N;
		int[] arr;
		N = PublicTape.read();
		arr = new int[N];
		i = 0;
		while (i < N) {
			x = PrivateTape.read();
			arr[i] = x;
			i = i + 1;
		}

		i = 1;
		while (i < N) {
			key = arr[i];
			j = i - 1;
			while ((j >= 0) && ((arr[j]) > key)) {
				arr[j + 1] = arr[j];
				j = j - 1;
			}
			arr[j + 1] = key;
			i = i + 1;
		}
		i = 0;
		while (i < N) {
			System.out.println(arr[i]);
			i = i + 1;
		}
		Prover.answer(arr[0]);
	}

}
