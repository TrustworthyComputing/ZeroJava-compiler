class test07{
    public static void main(String[] a){
	System.out.println(new Operator().compute());
    }
}

class Operator{
    
    boolean op1bool;
    boolean op2bool;
    int op1int;
    int op2int;
    boolean result;

    public int compute(){

	op1int = 10;
	op2int = 20;
	result = op1int < op2int;

	return 0;
    }
}
