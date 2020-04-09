class test99{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test {

    Test test;
    int i;

    public int start(){
	test = new Test();
	i = test.next(this);
	
	return 0;
    }

    public int next(Test t){

	return 0;
    }

}
