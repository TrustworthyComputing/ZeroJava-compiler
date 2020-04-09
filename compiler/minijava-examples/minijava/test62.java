class test62{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    Test test;
	
    public int start(){

	test = this;

	return 0;
    }
}
