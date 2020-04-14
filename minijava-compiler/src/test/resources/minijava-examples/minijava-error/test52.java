class test52{
    public static void main(String[] a){
	System.out.println(new Test().start());
    }
}

class Test{

    int op;
    boolean result;
	
    public int start(){

	result = op;	// TE

	return 0;
    }
}
