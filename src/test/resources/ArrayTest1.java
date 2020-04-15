class ArrayTest1 {

    public static void main(String[] a){
        int[] b;
        b = new int[10];
        b[0] = 14;
        b[9] = (b[0]) * 2;
        System.out.println(b.length);
        System.out.println(b[0]);
        System.out.println(b[9]);
        Prover.answer(0);
    }

}
