class Simple {

    public static void main(String [] a) {
        System.out.println(new Test1().bar(11, 12));
        // System.out.println(new Test1().buz());
    }

}

class Test1 {

    int fielda;
    int fieldb;

    public int foo(int x) {
        return x;
    }

    public int bar(int p1, int p2) {
        fielda = 1000;
        return (p1 * p2);
    }

    public int buz() {
        return 3 * 4;
    }

}
