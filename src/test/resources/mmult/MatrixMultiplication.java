class MatrixMultiplication {

	public static void main(String[] arg){
		int i;
		int j;
		int k;
		int x;
		int y;
		int z;
		int rows_1;
		int cols_1_rows_2;
		int cols_2;
		int[] a;
		int[] b;
		int[] res;
		rows_1 = PublicTape.read();
		cols_1_rows_2 = PublicTape.read();
		cols_2 = PublicTape.read();
		a = new int[rows_1 * cols_1_rows_2];
		b = new int[cols_1_rows_2 * cols_2];
		res = new int[rows_1 * cols_2];

		i = 0;
		while (i < (rows_1 * cols_1_rows_2)) {
			x = PrivateTape.read();
			a[i] = x;
			// printf("%d, ", a[i]);
			i = i + 1;
		}
		// printf("\n");
		i = 0;
		while (i < (cols_1_rows_2 * cols_2)) {
			x = PrivateTape.read();
			b[i] = x;
			// printf("%d, ", b[i]);
			i = i + 1;
		}
		// printf("\n");
		i = 0;
		while (i < (rows_1 * cols_2)) {
			res[i] = 0;
			i = i + 1;
		}

		i = 0;
		while (i < rows_1) {
			j = 0;
			while (j < cols_2) {
				k = 0;
				while (k < cols_1_rows_2) {
					x = res[((i*cols_2) + j)];
					y = a[((i * cols_1_rows_2) + k)];
					y = b[((k * cols_2) + j)];
					res[((i*cols_2) + j)] = (x + y) + z;
					k = k + 1;
				}
				j = j + 1;
			}
			i = i + 1;
		}

		// i = 0;
		// while (i < rows_1 * cols_2) {
		// 	printf("%d, ", res[i]);
		// 	i = i + 1;
		// }
		// printf("\n");

		Prover.answer(res[0]);
	}

}
