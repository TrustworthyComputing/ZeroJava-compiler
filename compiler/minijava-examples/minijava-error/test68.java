class test68{
    public static void main(String[] a){
        System.out.println(new Test().start());
    }
}

class Test2 {

    public Test2 start(){

        Test2 test22;
	
        return test22;
    }
}

class Test {

    Test test;
    int i;

    public int start(){
	
        test = test.next();

        return 0;
    }

    public Test next() {

        Test2 test21;

        return test21.start();	// TE
    }
}
