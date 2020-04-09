class test82{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test {

    Test test;
    boolean b;

    public int start(){
	test = new Test();
	b = test.next();
	
	return 0;
    }

    public boolean next(){
	
	boolean b2;

	b2 = ((true && (7<8)) && !b);
	return b2; 
    }

}
