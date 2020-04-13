class Simple {
    public static void main(String [] a) {
        Test1 t;
        t = new Test1();
        System.out.println(t.getFieldA());
        System.out.println(t.getFieldB());
        System.out.println(t.foo());
        System.out.println(t.bar(5));
        System.out.println(t.buz());
        System.out.println(t.getFieldA());
        System.out.println(t.getFieldB());
        Prover.answer(0);
    }
}

class Test1 {

    int fielda;
    int fieldb;

    public int foo() {
        return 5;
    }

    public boolean bar(int x) {
        fieldb = 30;
        return (x > 2);
    }

    public int buz() {
        fielda = 20;
        return 3;
    }

    public int getFieldA() {
        return fielda;
    }

    public int getFieldB() {
        return fieldb;
    }

}
