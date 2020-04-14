class Simple {
    public static void main(String [] a) {
        Test1 t;
        t = new Test1();
        System.out.println(t.init(4, 5));

        System.out.println(t.getSumOfFields());

        Prover.answer(0);
    }
}

class Test1 {

    int fielda;
    int fieldb;

    public int getSumOfFields() {
        int a;
        int b;
        a = this.getFieldA();
        System.out.println(a);

        b = this.getFieldB();
        System.out.println(b);

        return a + b;
    }

    public int init(int x, int y) {
        fielda = x;
        fieldb = y;
        return 0;
    }

    public int getFieldA() {
        return fielda;
    }

    public int getFieldB() {
        return fieldb;
    }

}
