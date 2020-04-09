class test17{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    int i;

    public int start(){

	Test test;

	test = new Test();

	i = 10;

	i = i + ((test.first(this)).second());

	return i;
    }

    public Test first(Test test2){

	Test test3;

	test3 = test2;

	return test3;
    }

    public int second(){

	i = i + 10;

	return i;
    }
}
