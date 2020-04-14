class Example1 {
    public static void main(String [] a) {
        System.out.println(new Test1( ).Start(5, true));
    }
}

class Test1 {
    int fielda;

    public int Start(int b, boolean c) {
        boolean ntb ;
        int[] nti;
        int ourint;
        nti = new int[b];
        ourint = nti[0];
        System.out.println(ourint);
        return nti[0] ;
    }
}
