class test06{
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

	op1bool = true;
	op2bool = false;
	result = op1bool && op2bool;

	return 0;
    }
}
