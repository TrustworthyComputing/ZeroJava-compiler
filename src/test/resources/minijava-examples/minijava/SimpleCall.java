class Simple {
    public static void main(String [] a) {
        Test1 t;
        t = new Test1();
        System.out.println(t.bar(5));
        Prover.answer(0);
    }
}

class Test1 {

    public boolean bar(int x) {
        return (x > 2);
    }

}
