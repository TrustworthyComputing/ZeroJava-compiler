class test93{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test {

    Test test;
    int[] i;

    public int start(){

	i = new int[10];
	test = new Test();
	test = (test.next()).next();
	
	return 0;
    }

    public Test next() {
	test = new Test();
	return test;
    }
}
