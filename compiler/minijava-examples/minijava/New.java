class New {
	public static void main(String[] a){
		Test1 t1;
		Test2 t2;
		t2 = new Test2();
		t1 = t2.foo2();
		Prover.answer(t1.foo1());
	}
}

class Test1 {
    int fielda;

    public int foo1() { return 5; }
	public int bar1() { return 10; }
	public int buz1() { return 15; }

}

class Test2 {
    int fielda;
    int fieldb;

    public Test1 foo2() {
		Test1 t;
        return new Test1();
    }

	public int bar2() { return 52; }

}
